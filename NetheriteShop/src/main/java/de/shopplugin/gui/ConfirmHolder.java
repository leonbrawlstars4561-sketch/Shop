package de.shopplugin.gui;

import de.shopplugin.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert ein Inventory als Kauf-Bestaetigungs-Dialog und traegt
 * eine Referenz auf das ShopItem, um das es dabei geht.
 */
public class ConfirmHolder implements InventoryHolder {

    private final ShopItem shopItem;
    private Inventory inventory;

    public ConfirmHolder(ShopItem shopItem) {
        this.shopItem = shopItem;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }
}
