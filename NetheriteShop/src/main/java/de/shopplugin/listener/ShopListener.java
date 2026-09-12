package de.shopplugin.listener;

import de.shopplugin.economy.EconomyManager;
import de.shopplugin.gui.ConfirmGUI;
import de.shopplugin.gui.ConfirmHolder;
import de.shopplugin.gui.ShopGUI;
import de.shopplugin.gui.ShopHolder;
import de.shopplugin.shop.ShopItem;
import de.shopplugin.shop.ShopManager;
import de.shopplugin.util.Keys;
import de.shopplugin.util.PriceFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Behandelt alle Interaktionen mit dem Shop- und dem Bestaetigungs-Inventar.
 *
 * Anti-Dupe-Konzept:
 *  - Jeder Klick in beiden GUIs wird komplett abgefangen (event.setCancelled(true)),
 *    sodass niemals ein GUI-Icon in ein echtes Inventar wandern kann.
 *  - Die tatsaechlich gekaufte Ware wird als NEUES ItemStack erzeugt und per
 *    addItem() vergeben - niemals durch Verschieben des Icons.
 *  - Ein kurzer serverseitiger "Klick-Sperre" (pendingPurchases) verhindert,
 *    dass durch sehr schnelles Doppelklicken zweimal abgebucht wird.
 */
public class ShopListener implements Listener {

    private final ShopManager shopManager;
    private final EconomyManager economyManager;
    private final ConfirmGUI confirmGUI;
    private final ShopGUI shopGUI;

    // Verhindert doppelte Verarbeitung, falls ein Spieler den Bestaetigen-Button
    // mehrfach in sehr kurzer Zeit anklickt (Spam-Klick-Schutz).
    private final Map<UUID, Boolean> processingLock = new HashMap<>();

    public ShopListener(ShopManager shopManager, EconomyManager economyManager) {
        this.shopManager = shopManager;
        this.economyManager = economyManager;
        this.confirmGUI = new ConfirmGUI(shopManager);
        this.shopGUI = new ShopGUI(shopManager);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
            handleShopClick(event);
        } else if (event.getInventory().getHolder() instanceof ConfirmHolder confirmHolder) {
            event.setCancelled(true);
            handleConfirmClick(event, confirmHolder);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder
                || event.getInventory().getHolder() instanceof ConfirmHolder) {
            event.setCancelled(true);
        }
    }

    private void handleShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String itemId = extractShopId(event.getCurrentItem());
        if (itemId == null) {
            // Klick auf Fuellmaterial oder leeren Slot - ignorieren.
            return;
        }

        Optional<ShopItem> shopItemOpt = shopManager.getById(itemId);
        if (shopItemOpt.isEmpty()) {
            player.sendMessage(Component.text("Dieses Item ist nicht mehr verfuegbar.", NamedTextColor.RED));
            return;
        }

        player.openInventory(confirmGUI.build(shopItemOpt.get()));
    }

    private void handleConfirmClick(InventoryClickEvent event, ConfirmHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        ShopItem shopItem = holder.getShopItem();

        if (slot == ConfirmGUI.SLOT_CONFIRM) {
            if (Boolean.TRUE.equals(processingLock.get(player.getUniqueId()))) {
                return; // Kauf wird bereits verarbeitet - doppelten Klick ignorieren.
            }
            processingLock.put(player.getUniqueId(), true);
            try {
                player.closeInventory();
                attemptPurchase(player, shopItem);
            } finally {
                processingLock.remove(player.getUniqueId());
            }
        } else if (slot == ConfirmGUI.SLOT_CANCEL) {
            player.openInventory(shopGUI.build());
            player.sendMessage(Component.text("Kauf abgebrochen.", NamedTextColor.YELLOW));
        }
    }

    private void attemptPurchase(Player player, ShopItem shopItem) {
        if (!economyManager.isEnabled()) {
            player.sendMessage(Component.text(
                    "Das Economy-System ist aktuell nicht verfuegbar. Bitte kontaktiere einen Administrator.",
                    NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("shop.buy")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung mehr, diesen Kauf abzuschliessen.",
                    NamedTextColor.RED));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text(
                    "Dein Inventar ist voll! Schaffe Platz und versuche es erneut.", NamedTextColor.RED));
            return;
        }

        double price = shopItem.getPrice();
        if (!economyManager.hasEnough(player, price)) {
            double missing = price - economyManager.getBalance(player);
            player.sendMessage(Component.text("Du hast nicht genug Guthaben! Benoetigt: ", NamedTextColor.RED)
                    .append(Component.text(PriceFormatter.formatPrice(price) + " Coins", NamedTextColor.YELLOW))
                    .append(Component.text(" (dir fehlen " + PriceFormatter.formatPrice(Math.max(missing, 0)) + ")",
                            NamedTextColor.RED)));
            return;
        }

        boolean withdrawn = economyManager.withdraw(player, price);
        if (!withdrawn) {
            player.sendMessage(Component.text(
                    "Die Abbuchung ist fehlgeschlagen. Es wurde nichts abgebucht - bitte versuche es erneut.",
                    NamedTextColor.RED));
            return;
        }

        ItemStack reward = shopManager.buildRewardItem(shopItem);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

        if (!leftover.isEmpty()) {
            // Sicherheitsnetz fuer den seltenen Fall einer Race-Condition zwischen
            // der Pruefung auf freien Platz und der tatsaechlichen Vergabe.
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(Component.text(
                    "Dein Inventar wurde in der Zwischenzeit voll - das Item wurde vor dir fallen gelassen.",
                    NamedTextColor.YELLOW));
        }

        player.sendMessage(Component.text("✔ Kauf erfolgreich! ", NamedTextColor.GREEN)
                .append(Component.text("Du hast " + shopItem.getDisplayName() + " fuer "
                        + PriceFormatter.formatPrice(price) + " Coins gekauft.", NamedTextColor.WHITE)));
    }

    /**
     * Liest die Shop-Item-ID aus dem PersistentDataContainer eines Icons aus.
     * Gibt null zurueck, wenn es sich nicht um ein markiertes Shop-Icon handelt
     * (z.B. Fuellmaterial oder ein leerer Slot).
     */
    private String extractShopId(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(Keys.SHOP_ITEM_ID, PersistentDataType.STRING)) {
            return null;
        }
        return pdc.get(Keys.SHOP_ITEM_ID, PersistentDataType.STRING);
    }
}
