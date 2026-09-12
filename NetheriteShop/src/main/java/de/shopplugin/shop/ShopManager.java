package de.shopplugin.shop;

import de.shopplugin.util.Keys;
import de.shopplugin.util.PriceFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Laedt die Shop-Artikel aus der config.yml und ist verantwortlich fuer den
 * Bau der Anzeige-Icons (GUI) sowie der tatsaechlichen Belohnungs-Items.
 */
public class ShopManager {

    private final JavaPlugin plugin;
    private final Map<String, ShopItem> items = new LinkedHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Laedt (bzw. laedt neu) alle Shop-Items aus der aktuellen config.yml.
     */
    public void load() {
        items.clear();

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("shop.items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Keine Shop-Items unter 'shop.items' in der config.yml gefunden!");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            String materialName = section.getString("material", "STONE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Ungueltiges Material '" + materialName + "' bei Shop-Item '" + id + "' - wird uebersprungen.");
                continue;
            }

            String displayName = section.getString("display-name", id);
            double price = section.getDouble("price", 0);
            int slot = section.getInt("slot", -1);

            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantKey : enchantSection.getKeys(false)) {
                    Enchantment enchantment = Registry.ENCHANTMENT.get(
                            NamespacedKey.minecraft(enchantKey.toLowerCase(Locale.ROOT)));
                    if (enchantment == null) {
                        plugin.getLogger().warning("Unbekannte Verzauberung '" + enchantKey
                                + "' bei Shop-Item '" + id + "' - wird ignoriert.");
                        continue;
                    }
                    enchantments.put(enchantment, enchantSection.getInt(enchantKey));
                }
            }

            items.put(id, new ShopItem(id, material, displayName, price, slot, enchantments));
        }
    }

    public Collection<ShopItem> getItems() {
        return items.values();
    }

    public Optional<ShopItem> getById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    /**
     * Baut das anklickbare Anzeige-Icon fuer das Shop-GUI.
     * Dieses Item wird NIEMALS an einen Spieler ausgegeben - es dient nur der
     * Darstellung und wird ueber einen PersistentDataContainer-Key markiert,
     * damit Klicks im GUI eindeutig einem Shop-Item zugeordnet werden koennen.
     */
    public ItemStack buildDisplayIcon(ShopItem shopItem) {
        ItemStack icon = new ItemStack(shopItem.getMaterial());
        ItemMeta meta = icon.getItemMeta();

        meta.displayName(Component.text(shopItem.getDisplayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Name: ", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(shopItem.getDisplayName(), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Verzauberungen:", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (shopItem.getEnchantments().isEmpty()) {
            lore.add(Component.text("  Keine", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            for (Map.Entry<Enchantment, Integer> entry : shopItem.getEnchantments().entrySet()) {
                lore.add(Component.text("  " + prettyEnchantName(entry.getKey()) + " " + toRoman(entry.getValue()),
                                NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("Preis: ", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(PriceFormatter.formatPrice(shopItem.getPrice()) + " Coins", NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("» Klicken zum Kaufen", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        // Echte Verzauberungen fuer den Glitzer-Effekt, Standard-Tooltip aber
        // ausblenden, da wir die Informationen bereits selbst in der Lore anzeigen.
        for (Map.Entry<Enchantment, Integer> entry : shopItem.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

        // Markierung zur eindeutigen Identifikation + Anti-Dupe-Schutz.
        meta.getPersistentDataContainer().set(Keys.SHOP_ITEM_ID, PersistentDataType.STRING, shopItem.getId());

        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * Baut das tatsaechliche Item, das ein Spieler nach einem erfolgreichen
     * Kauf erhaelt. Dieses Item traegt bewusst KEINE Shop-Markierung, da es
     * ein normales, frei benutzbares Spiel-Item werden soll.
     */
    public ItemStack buildRewardItem(ShopItem shopItem) {
        ItemStack reward = new ItemStack(shopItem.getMaterial());
        ItemMeta meta = reward.getItemMeta();

        meta.displayName(Component.text(shopItem.getDisplayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        for (Map.Entry<Enchantment, Integer> entry : shopItem.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        reward.setItemMeta(meta);
        return reward;
    }

    private String toRoman(int number) {
        String[] romans = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number < 0 || number >= romans.length) {
            return String.valueOf(number);
        }
        return romans[number];
    }

    private String prettyEnchantName(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
