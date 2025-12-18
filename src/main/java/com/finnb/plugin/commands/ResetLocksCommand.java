package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResetLocksCommand implements CommandExecutor, TabCompleter {

    private final BrakkeBoysCORE plugin;

    public ResetLocksCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("brakkeboyscore.resetlocks")) {
            sender.sendMessage(Component.text("Je hebt geen toestemming voor dit commando!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Gebruik: /resetlocks <speler>").color(NamedTextColor.YELLOW));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Speler niet gevonden: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        UUID targetUuid = targetPlayer.getUniqueId();
        ChestLockManager manager = plugin.getChestLockManager();

        // Count locks before removal
        int chestLocks = manager.getPlayerChestLockCount(targetUuid);
        int doorLocks = manager.getPlayerDoorLockCount(targetUuid);

        // Remove all chest locks
        manager.removeAllChestLocks(targetUuid);
        
        // Remove all door locks
        manager.removeAllDoorLocks(targetUuid);

        sender.sendMessage(Component.text("Alle sloten van " + targetPlayer.getName() + " zijn gereset!")
                .color(NamedTextColor.GREEN)
                .append(Component.text(" (" + chestLocks + " kisten, " + doorLocks + " deuren)")
                        .color(NamedTextColor.GRAY)));

        targetPlayer.sendMessage(Component.text("Al je sloten zijn gereset door een admin.").color(NamedTextColor.YELLOW));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }
        return List.of();
    }
}

