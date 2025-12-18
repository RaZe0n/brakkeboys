package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AddCommand implements CommandExecutor, TabCompleter {

    private final BrakkeBoysCORE plugin;

    public AddCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Alleen spelers g!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Gebruik: /add <speler>").color(NamedTextColor.YELLOW));
            return true;
        }

        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlock(null, 5);

        if (targetBlock == null) {
            player.sendMessage(Component.text("Je moet naar een kist of deur kijken!").color(NamedTextColor.RED));
            return true;
        }

        ChestLockManager manager = plugin.getChestLockManager();
        BlockData blockData = targetBlock.getBlockData();
        boolean isChest = targetBlock.getState() instanceof Chest;
        boolean isDoor = blockData instanceof Door || blockData instanceof TrapDoor || 
                        targetBlock.getType().name().contains("FENCE_GATE");

        if (!isChest && !isDoor) {
            player.sendMessage(Component.text("Je moet naar een kist, deur, valdeur of hekpoort kijken!").color(NamedTextColor.RED));
            return true;
        }

        // Find the target player
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Speler niet gevonden: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Je bent al de eigenaar!").color(NamedTextColor.YELLOW));
            return true;
        }

        Title.Times times = Title.Times.times(
                Duration.ofMillis(400),
                Duration.ofMillis(3000),
                Duration.ofMillis(800)
        );

        if (isChest) {
            handleChest(player, targetBlock, targetPlayer, manager, times);
        } else if (isDoor) {
            handleDoor(player, targetBlock, targetPlayer, manager, times);
        }

        return true;
    }

    private void handleChest(Player player, Block block, Player targetPlayer, ChestLockManager manager, Title.Times times) {
        // Check if chest is locked
        if (!manager.isChestLocked(block.getLocation())) {
            player.sendMessage(Component.text("Deze kist is niet vergrendeld! Gebruik /lock eerst.").color(NamedTextColor.RED));
            return;
        }

        // Check if player owns this chest
        if (!manager.isChestOwner(block.getLocation(), player.getUniqueId())) {
            player.sendMessage(Component.text("Deze kist is niet van jou!").color(NamedTextColor.RED));
            return;
        }

        // Check if already added
        if (manager.isAllowedChestUser(block.getLocation(), targetPlayer.getUniqueId())) {
            // Remove them instead
            manager.removeAllowedChestUser(block.getLocation(), targetPlayer.getUniqueId());

            Title title = Title.title(
                    Component.text("➖ VERWIJDERD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " kan deze kist niet meer gebruiken").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);
        } else {
            // Add them
            manager.addAllowedChestUser(block.getLocation(), targetPlayer.getUniqueId());

            Title title = Title.title(
                    Component.text("➕ MACHTIGING GEGEVEN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " mag nu deze kist gebruiken").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);

            // Also notify the target player
            Title targetTitle = Title.title(
                    Component.text("📦 KIST PERMISSIE").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text(player.getName() + " heeft je toegang gegeven tot hun kist").color(NamedTextColor.GRAY),
                    times
            );
            targetPlayer.showTitle(targetTitle);
        }
    }

    private void handleDoor(Player player, Block block, Player targetPlayer, ChestLockManager manager, Title.Times times) {
        // Check if door is locked
        if (!manager.isDoorLocked(block.getLocation())) {
            player.sendMessage(Component.text("Deze deur is niet vergrendeld! Gebruik /lock eerst.").color(NamedTextColor.RED));
            return;
        }

        // Check if player owns this door
        if (!manager.isDoorOwner(block.getLocation(), player.getUniqueId())) {
            player.sendMessage(Component.text("Deze deur is niet van jou!").color(NamedTextColor.RED));
            return;
        }

        String blockType = getBlockTypeName(block);

        // Check if already added
        if (manager.isAllowedDoorUser(block.getLocation(), targetPlayer.getUniqueId())) {
            // Remove them instead
            manager.removeAllowedDoorUser(block.getLocation(), targetPlayer.getUniqueId());

            Title title = Title.title(
                    Component.text("➖ VERWIJDERD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " kan deze " + blockType.toLowerCase() + " niet meer gebruiken").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);
        } else {
            // Add them
            manager.addAllowedDoorUser(block.getLocation(), targetPlayer.getUniqueId());

            Title title = Title.title(
                    Component.text("➕ MACHTIGING GEGEVEN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " mag nu deze " + blockType.toLowerCase() + " gebruiken").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);

            // Also notify the target player
            Title targetTitle = Title.title(
                    Component.text("🚪 DEUR PERMISSIE").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text(player.getName() + " heeft je toegang gegeven tot hun " + blockType.toLowerCase()).color(NamedTextColor.GRAY),
                    times
            );
            targetPlayer.showTitle(targetTitle);
        }
    }

    private String getBlockTypeName(Block block) {
        String type = block.getType().name();
        if (type.contains("DOOR") && !type.contains("TRAP")) {
            return "Deur";
        } else if (type.contains("TRAP")) {
            return "Valdeur";
        } else if (type.contains("FENCE_GATE")) {
            return "Hekpoort";
        }
        return "Deur";
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

