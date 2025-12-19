package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.LaserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LaserCommand implements CommandExecutor {

    private final BrakkeBoysCORE plugin;
    private final LaserManager laserManager;

    public LaserCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
        this.laserManager = plugin.getLaserManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("removelaser")) {
            if (laserManager.hasLaser(player.getUniqueId())) {
                laserManager.removeLaser(player.getUniqueId());
                player.sendMessage(Component.text("Je laser is verwijderd!").color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Je hebt geen laser gezet!").color(NamedTextColor.RED));
            }
            return true;
        }

        // /laser command
        LaserManager.LaserData data = laserManager.getLaser(player.getUniqueId());
        
        if (data == null || !data.isComplete()) {
            // Setting first or second position
            Location feetLocation = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
            feetLocation.setY(player.getLocation().getBlockY());
            
            if (data == null || data.getFirstPosition() == null) {
                // Set first position
                int remaining = laserManager.getRemainingLasers(player.getUniqueId());
                if (laserManager.setFirstPosition(player.getUniqueId(), feetLocation)) {
                    player.sendMessage(Component.text("Eerste positie gezet! Gebruik /laser nog een keer om de tweede positie te zetten.").color(NamedTextColor.GREEN));
                    if (remaining > 1) {
                        player.sendMessage(Component.text("Je hebt nog " + (remaining - 1) + " lasers over.").color(NamedTextColor.GRAY));
                    }
                } else {
                    int max = laserManager.getMaxLasers(player.getUniqueId());
                    int current = laserManager.getPlayerLaserCount(player.getUniqueId());
                    if (current >= max) {
                        player.sendMessage(Component.text("Je hebt je maximale aantal lasers bereikt (" + max + ")! Gebruik /removelaser om een laser te verwijderen.").color(NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Je hebt al een complete laser! Gebruik /removelaser om deze te verwijderen.").color(NamedTextColor.RED));
                    }
                }
            } else {
                // Set second position
                Location firstPos = data.getFirstPosition();
                double distance = firstPos.distance(feetLocation);
                
                if (distance > 10.0) {
                    player.sendMessage(Component.text("De laser kan maximaal 10 blokken lang zijn! Afstand: " + String.format("%.1f", distance) + " blokken").color(NamedTextColor.RED));
                } else if (laserManager.setSecondPosition(player.getUniqueId(), feetLocation)) {
                    int remaining = laserManager.getRemainingLasers(player.getUniqueId());
                    player.sendMessage(Component.text("Tweede positie gezet! Je laser is nu actief.").color(NamedTextColor.GREEN));
                    if (remaining > 0) {
                        player.sendMessage(Component.text("Je hebt nog " + remaining + " lasers over.").color(NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("Er is een fout opgetreden bij het zetten van de tweede positie.").color(NamedTextColor.RED));
                }
            }
        } else {
            int remaining = laserManager.getRemainingLasers(player.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(Component.text("Je hebt al een complete laser! Gebruik /removelaser om deze te verwijderen voordat je een nieuwe zet.").color(NamedTextColor.RED));
            } else {
                int max = laserManager.getMaxLasers(player.getUniqueId());
                player.sendMessage(Component.text("Je hebt je maximale aantal lasers bereikt (" + max + ")! Gebruik /removelaser om een laser te verwijderen.").color(NamedTextColor.RED));
            }
        }

        return true;
    }
}

