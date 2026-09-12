package de.shopplugin.gui;

import de.shopplugin.shop.ShopItem;
import de.shopplugin.shop.ShopManager;
import de.shopplugin.util.PriceFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Baut den Kauf-Bestaetigungsdialog, der vor jedem Kauf angezeigt wird
 * (empfohlen, da die Shop-Preise sehr hoch sein koennen).
 */
public class ConfirmGUI {

    public static final int SIZE = 27;
    public static final int SLOT_ITEM = 13;
    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_CANCEL = 15;

    private static final Component TITLE = Component.text("Kauf bestätigen", NamedTextColor.DARK_RED);

    private final ShopManager shopManager;

    public ConfirmGUI(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public Inventory build(ShopItem shopItem) {
        ConfirmHolder holder = new ConfirmHolder(shopItem);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);

        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Vorschau des Items (dasselbe markierte Icon wie im Shop-GUI).
        inventory.setItem(SLOT_ITEM, shopManager.buildDisplayIcon(shopItem));

        List<Component> confirmLore = List.of(
                Component.text("Preis: ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(PriceFormatter.formatPrice(shopItem.getPrice()) + " Coins", NamedTextColor.YELLOW))
        );
        inventory.setItem(SLOT_CONFIRM, pane(Material.LIME_STAINED_GLASS_PANE,
                "✔ Kauf bestätigen", confirmLore));

        inventory.setItem(SLOT_CANCEL, pane(Material.RED_STAINED_GLASS_PANE,
                "✘ Abbrechen", null));

        return inventory;
    }

    private ItemStack pane(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        NamedTextColor color = material == Material.LIME_STAINED_GLASS_PANE ? NamedTextColor.GREEN
                : material == Material.RED_STAINED_GLASS_PANE ? NamedTextColor.RED
                : NamedTextColor.GRAY;
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        if (lore != null) {
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
