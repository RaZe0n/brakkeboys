package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.HorseLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class HorseLockCommand implements CommandExecutor {

    private final BrakkeBoysCORE plugin;

    public HorseLockCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("WagWan, alleen spelers kunnen dit commando gebruiken!").color(NamedTextColor.RED));
            return true;
        }

        // Get the horse the player is riding or looking at
        AbstractHorse horse = getTargetHorse(player);

        if (horse == null) {
            player.sendMessage(Component.text("Je moet op dat kaulo paard zitten of kijken naar die ding om dit te doen!").color(NamedTextColor.RED));
            return true;
        }

        HorseLockManager manager = plugin.getHorseLockManager();

        // Title timing configuration
        Title.Times times = Title.Times.times(
                Duration.ofMillis(400),  // Fade in
                Duration.ofMillis(3000), // Stay
                Duration.ofMillis(800)   // Fade out
        );

        if (manager.isLocked(horse.getUniqueId())) {
            // Horse is already locked
            if (manager.isOwner(horse.getUniqueId(), player.getUniqueId())) {
                // Player owns this horse, unlock it
                manager.unlockHorse(horse.getUniqueId());

                Title title = Title.title(
                        Component.text("🔓 FREE FREE").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Ze kunnen je pfeerd jattn").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            } else {
                // Someone else owns this horse
                Title title = Title.title(
                        Component.text("🚫 KAN NIET G").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Wat denk je deze paard te kunnen jatten?").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        } else {
            // Horse is not locked, lock it
            manager.lockHorse(horse.getUniqueId(), player.getUniqueId());

            Title title = Title.title(
                    Component.text("🔒 GEKOLONISEERD").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text("Je bent nu de eigenaar van dit paard, de VOC is er niks bij").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);
        }

        return true;
    }

    /**
     * Get the horse the player is riding or looking at
     */
    private AbstractHorse getTargetHorse(Player player) {
        // First check if player is riding a horse
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractHorse horse) {
            return horse;
        }

        // Otherwise, check what the player is looking at (within 5 blocks)
        Entity target = player.getTargetEntity(5);
        if (target instanceof AbstractHorse horse) {
            return horse;
        }

        return null;
    }
}

