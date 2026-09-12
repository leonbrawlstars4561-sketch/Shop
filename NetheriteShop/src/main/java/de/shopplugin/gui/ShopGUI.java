package de.shopplugin.gui;

import de.shopplugin.shop.ShopItem;
import de.shopplugin.shop.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Baut das 54-Slot Shop-Hauptmenue (/buy).
 */
public class ShopGUI {

    public static final int SIZE = 54;
    private static final Component TITLE = Component.text("Shop", NamedTextColor.DARK_AQUA);

    private final ShopManager shopManager;

    public ShopGUI(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    /**
     * Erstellt ein frisches Shop-Inventar mit allen aktuell konfigurierten Items.
     * Wird bewusst bei jedem Oeffnen neu gebaut, damit z.B. Config-Reloads
     * sofort im GUI sichtbar sind und keine Item-Referenzen "veralten" koennen.
     */
    public Inventory build() {
        ShopHolder holder = new ShopHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);

        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        boolean[] used = new boolean[SIZE];
        java.util.List<ShopItem> pendingAutoPlacement = new java.util.ArrayList<>();

        // Zuerst alle Items mit explizit konfiguriertem, gueltigem und noch freiem Slot platzieren.
        for (ShopItem shopItem : shopManager.getItems()) {
            int slot = shopItem.getSlot();
            if (slot >= 0 && slot < SIZE && !used[slot]) {
                inventory.setItem(slot, shopManager.buildDisplayIcon(shopItem));
                used[slot] = true;
            } else {
                pendingAutoPlacement.add(shopItem);
            }
        }

        // Danach alle restlichen Items (ohne gueltigen/freien Slot) auf freie Plaetze verteilen.
        int next = 0;
        for (ShopItem shopItem : pendingAutoPlacement) {
            while (next < SIZE && used[next]) {
                next++;
            }
            if (next >= SIZE) {
                break;
            }
            inventory.setItem(next, shopManager.buildDisplayIcon(shopItem));
            used[next] = true;
        }

        return inventory;
    }

    private ItemStack buildFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        return filler;
    }
}
