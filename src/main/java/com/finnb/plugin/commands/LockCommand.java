package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class LockCommand implements CommandExecutor {

    private final BrakkeBoysCORE plugin;

    public LockCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Alleen spelers g!").color(NamedTextColor.RED));
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

        Title.Times times = Title.Times.times(
                Duration.ofMillis(400),
                Duration.ofMillis(3000),
                Duration.ofMillis(800)
        );

        if (isChest) {
            handleChest(player, targetBlock, manager, times);
        } else if (isDoor) {
            handleDoor(player, targetBlock, manager, times);
        }

        return true;
    }

    private void handleChest(Player player, Block block, ChestLockManager manager, Title.Times times) {
        // Check if already locked
        if (manager.isChestLocked(block.getLocation())) {
            if (manager.isChestOwner(block.getLocation(), player.getUniqueId())) {
                // Unlock it
                manager.unlockChest(block.getLocation());

                Title title = Title.title(
                        Component.text("🔓 KIST ONTVERGENDELD").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Kist is nu vrij toegankelijk").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            } else {
                player.sendMessage(Component.text("Deze kist is al vergrendeld door iemand anders!").color(NamedTextColor.RED));
            }
            return;
        }

        // Try to lock the chest
        if (!manager.lockChest(block.getLocation(), player.getUniqueId())) {
            int remaining = manager.getRemainingChestLocks(player.getUniqueId());
            if (remaining == 0) {
                player.sendMessage(Component.text("Je hebt je maximale aantal kisten vergrendeld!").color(NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("Er ging iets mis bij het vergrendelen.").color(NamedTextColor.RED));
            }
            return;
        }

        // Success - show title
        int remaining = manager.getRemainingChestLocks(player.getUniqueId());

        Title title = Title.title(
                Component.text("🔒 KIST VERGRENDELD").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Je hebt nog " + remaining + " over").color(NamedTextColor.GRAY),
                times
        );
        player.showTitle(title);
    }

    private void handleDoor(Player player, Block block, ChestLockManager manager, Title.Times times) {
        // Check if already locked
        if (manager.isDoorLocked(block.getLocation())) {
            if (manager.isDoorOwner(block.getLocation(), player.getUniqueId())) {
                // Unlock it
                manager.unlockDoor(block.getLocation());

                String blockType = getBlockTypeName(block);
                Title title = Title.title(
                        Component.text("🔓 " + blockType.toUpperCase() + " ONTVERGENDELD").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text(blockType + " is nu vrij toegankelijk").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            } else {
                player.sendMessage(Component.text("Deze deur is al vergrendeld door iemand anders!").color(NamedTextColor.RED));
            }
            return;
        }

        // Try to lock the door
        if (!manager.lockDoor(block.getLocation(), player.getUniqueId())) {
            int remaining = manager.getRemainingDoorLocks(player.getUniqueId());
            if (remaining == 0) {
                player.sendMessage(Component.text("Je hebt je maximale aantal deuren vergrendeld!").color(NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("Er ging iets mis bij het vergrendelen.").color(NamedTextColor.RED));
            }
            return;
        }

        // Success - show title
        int remaining = manager.getRemainingDoorLocks(player.getUniqueId());
        String blockType = getBlockTypeName(block);

        Title title = Title.title(
                Component.text("🔒 " + blockType.toUpperCase() + " VERGRENDELD").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Je hebt nog " + remaining + " over").color(NamedTextColor.GRAY),
                times
        );
        player.showTitle(title);
    }

    private String getBlockTypeName(Block block) {
        Material type = block.getType();
        if (type.name().contains("DOOR") && !type.name().contains("TRAP")) {
            return "Deur";
        } else if (type.name().contains("TRAP")) {
            return "Valdeur";
        } else if (type.name().contains("FENCE_GATE")) {
            return "Hekpoort";
        }
        return "Deur";
    }
}
