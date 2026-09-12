package de.shopplugin.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kapselt den Zugriff auf die Vault-Economy-API.
 * Alle Guthaben-Operationen des Plugins laufen ausschliesslich ueber diese Klasse.
 */
public class EconomyManager {

    private final Economy economy;

    public EconomyManager(JavaPlugin plugin) {
        this.economy = setup(plugin);
    }

    private Economy setup(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /**
     * Gibt an, ob eine gueltige Economy-Anbindung vorhanden ist.
     */
    public boolean isEnabled() {
        return economy != null;
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    /**
     * Bucht den angegebenen Betrag vom Spielerkonto ab.
     *
     * @return true, wenn die Abbuchung erfolgreich war
     */
    public boolean withdraw(Player player, double amount) {
        if (!hasEnough(player, amount)) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Erstattet dem Spieler den angegebenen Betrag (z.B. bei fehlgeschlagenem Kauf).
     */
    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }
}
