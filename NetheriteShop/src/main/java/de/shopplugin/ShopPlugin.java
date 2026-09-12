package de.shopplugin;

import de.shopplugin.command.BuyCommand;
import de.shopplugin.economy.EconomyManager;
import de.shopplugin.gui.ShopGUI;
import de.shopplugin.listener.ShopListener;
import de.shopplugin.shop.ShopManager;
import de.shopplugin.util.Keys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Haupt-Einstiegspunkt des Plugins.
 *
 * Verantwortlichkeiten:
 *  - Config laden
 *  - Vault-Economy anbinden (Plugin deaktiviert sich selbst, falls keine
 *    Economy verfuegbar ist, um Fehlkaeufe ohne Abbuchung zu verhindern)
 *  - Shop-Items aus der config.yml laden
 *  - Befehl /buy und den ShopListener registrieren
 */
public final class ShopPlugin extends JavaPlugin {

    private EconomyManager economyManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        economyManager = new EconomyManager(this);
        if (!economyManager.isEnabled()) {
            getLogger().severe("Vault mit einem registrierten Economy-Provider wurde nicht gefunden!");
            getLogger().severe("Bitte installiere Vault sowie ein Economy-Plugin (z.B. EssentialsX Economy).");
            getLogger().severe("Das Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(this);
        ShopGUI shopGUI = new ShopGUI(shopManager);

        PluginCommand buyCommand = getCommand("buy");
        if (buyCommand != null) {
            buyCommand.setExecutor(new BuyCommand(shopGUI));
        } else {
            getLogger().warning("Befehl 'buy' konnte nicht registriert werden - bitte plugin.yml pruefen.");
        }

        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, economyManager), this);

        getLogger().info("NetheriteShop wurde aktiviert. " + shopManager.getItems().size() + " Shop-Item(s) geladen.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NetheriteShop wurde deaktiviert.");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
