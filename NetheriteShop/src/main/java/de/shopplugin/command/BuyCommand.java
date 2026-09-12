package de.shopplugin.command;

import de.shopplugin.gui.ShopGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Implementiert /buy - oeffnet das Shop-GUI, sofern der Spieler die
 * Permission "shop.buy" besitzt.
 */
public class BuyCommand implements CommandExecutor {

    public static final String PERMISSION = "shop.buy";

    private final ShopGUI shopGUI;

    public BuyCommand(ShopGUI shopGUI) {
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("Du hast keine Berechtigung, den Shop zu benutzen.", NamedTextColor.RED));
            return true;
        }

        player.openInventory(shopGUI.build());
        return true;
    }
}
