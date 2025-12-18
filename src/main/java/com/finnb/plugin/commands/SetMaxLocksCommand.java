package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetMaxLocksCommand implements CommandExecutor {

    private final BrakkeBoysCORE plugin;

    public SetMaxLocksCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("brakkeboyscore.setmaxlocks")) {
            sender.sendMessage(Component.text("Je hebt geen toestemming voor dit commando!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Gebruik: /setmaxlocks <speler> <aantal>").color(NamedTextColor.YELLOW));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Speler niet gevonden: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        try {
            int maxLocks = Integer.parseInt(args[1]);
            if (maxLocks < 0) {
                sender.sendMessage(Component.text("Aantal moet positief zijn!").color(NamedTextColor.RED));
                return true;
            }

            ChestLockManager manager = plugin.getChestLockManager();
            manager.setMaxChestLocks(targetPlayer.getUniqueId(), maxLocks);

            sender.sendMessage(Component.text("Max kisten voor " + targetPlayer.getName() + " ingesteld op: " + maxLocks).color(NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text("Je maximale aantal kisten is ingesteld op: " + maxLocks).color(NamedTextColor.GREEN));

        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Ongeldig aantal: " + args[1]).color(NamedTextColor.RED));
            return true;
        }

        return true;
    }
}

