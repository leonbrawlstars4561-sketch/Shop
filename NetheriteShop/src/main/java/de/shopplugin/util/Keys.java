package de.shopplugin.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Zentrale Verwaltung der NamespacedKeys, die zur Markierung von
 * Shop-Items im PersistentDataContainer verwendet werden.
 *
 * Diese Markierung verhindert Dupe-Exploits: nur Items, die exakt
 * diesen Key mit einer bekannten Shop-Item-ID tragen, werden als
 * "kaufbares Icon" im GUI erkannt. Alle Klicks im Shop- bzw.
 * Bestaetigungs-Inventar werden generell abgefangen (siehe ShopListener),
 * sodass diese Icons das Inventar niemals verlassen koennen.
 */
public final class Keys {

    private Keys() {
    }

    public static NamespacedKey SHOP_ITEM_ID;

    public static void init(Plugin plugin) {
        SHOP_ITEM_ID = new NamespacedKey(plugin, "shop_item_id");
    }
}
