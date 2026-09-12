package de.shopplugin.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Repraesentiert einen konfigurierten Shop-Artikel (Definition aus der config.yml).
 * Enthaelt keinerlei GUI-Logik - reine Datenklasse.
 */
public class ShopItem {

    private final String id;
    private final Material material;
    private final String displayName;
    private final double price;
    private final int slot;
    private final Map<Enchantment, Integer> enchantments;

    public ShopItem(String id, Material material, String displayName, double price,
                     int slot, Map<Enchantment, Integer> enchantments) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.price = price;
        this.slot = slot;
        this.enchantments = new LinkedHashMap<>(enchantments);
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPrice() {
        return price;
    }

    public int getSlot() {
        return slot;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
}
