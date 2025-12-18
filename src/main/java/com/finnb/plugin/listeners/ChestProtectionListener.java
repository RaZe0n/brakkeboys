package com.finnb.plugin.listeners;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;

public class ChestProtectionListener implements Listener {

    private final BrakkeBoysCORE plugin;

    public ChestProtectionListener(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-clicking blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        ChestLockManager manager = plugin.getChestLockManager();
        BlockData blockData = event.getClickedBlock().getBlockData();
        boolean isChest = event.getClickedBlock().getState() instanceof Chest;
        boolean isDoor = blockData instanceof Door || blockData instanceof TrapDoor || 
                        event.getClickedBlock().getType().name().contains("FENCE_GATE");

        if (!isChest && !isDoor) {
            return;
        }

        // Check if the block is locked
        if (isChest && manager.isChestLocked(event.getClickedBlock().getLocation())) {
            // Check if the player can access (owner or allowed user)
            if (!manager.canAccessChest(event.getClickedBlock().getLocation(), player.getUniqueId())) {
                // Not allowed - cancel the interaction
                event.setCancelled(true);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofMillis(2400),
                        Duration.ofMillis(600)
                );

                Title title = Title.title(
                        Component.text("🔒 VERGRENDELD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Deze kist is niet van jou!").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        } else if (isDoor && manager.isDoorLocked(event.getClickedBlock().getLocation())) {
            // Check if the player can access (owner or allowed user)
            if (!manager.canAccessDoor(event.getClickedBlock().getLocation(), player.getUniqueId())) {
                // Not allowed - cancel the interaction
                event.setCancelled(true);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofMillis(2400),
                        Duration.ofMillis(600)
                );

                Title title = Title.title(
                        Component.text("🔒 VERGRENDELD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Deze deur is niet van jou!").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        ChestLockManager manager = plugin.getChestLockManager();
        BlockData blockData = event.getBlock().getBlockData();
        boolean isChest = event.getBlock().getState() instanceof Chest;
        boolean isDoor = blockData instanceof Door || blockData instanceof TrapDoor || 
                        event.getBlock().getType().name().contains("FENCE_GATE");

        if (!isChest && !isDoor) {
            return;
        }

        // Check if the block is locked
        if (isChest && manager.isChestLocked(event.getBlock().getLocation())) {
            // Check if the player can access (owner or allowed user)
            if (!manager.canAccessChest(event.getBlock().getLocation(), player.getUniqueId())) {
                // Not allowed - cancel the break
                event.setCancelled(true);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofMillis(2400),
                        Duration.ofMillis(600)
                );

                Title title = Title.title(
                        Component.text("🔒 VERGRENDELD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Deze kist is niet van jou!").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        } else if (isDoor && manager.isDoorLocked(event.getBlock().getLocation())) {
            // Check if the player can access (owner or allowed user)
            if (!manager.canAccessDoor(event.getBlock().getLocation(), player.getUniqueId())) {
                // Not allowed - cancel the break
                event.setCancelled(true);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofMillis(2400),
                        Duration.ofMillis(600)
                );

                Title title = Title.title(
                        Component.text("🔒 VERGRENDELD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Deze deur is niet van jou!").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        }
    }
}
