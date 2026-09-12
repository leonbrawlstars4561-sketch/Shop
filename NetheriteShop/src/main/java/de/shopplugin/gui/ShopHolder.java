package de.shopplugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert ein Inventory eindeutig als Shop-Hauptmenue.
 * Wird im ShopListener genutzt, um Klicks sicher zuzuordnen,
 * ohne auf (aenderbare) Inventar-Titel angewiesen zu sein.
 */
public class ShopHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
