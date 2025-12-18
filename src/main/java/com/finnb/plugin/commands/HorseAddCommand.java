package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.HorseLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HorseAddCommand implements CommandExecutor, TabCompleter {

    private final BrakkeBoysCORE plugin;

    public HorseAddCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Alleen spelers g!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Gebruik: /horseadd <speler>").color(NamedTextColor.YELLOW));
            return true;
        }

        // Get the horse the player is riding or looking at
        AbstractHorse horse = getTargetHorse(player);

        if (horse == null) {
            player.sendMessage(Component.text("Je moet op dat kaulo paard zitten of kijken naar die ding om dit te doen!").color(NamedTextColor.RED));
            return true;    
        }

        HorseLockManager manager = plugin.getHorseLockManager();

        // Check if horse is locked
        if (!manager.isLocked(horse.getUniqueId())) {
            player.sendMessage(Component.text("Shit man, dat paard is niet gekoloniseerd! Gebruik /horselock eerst.").color(NamedTextColor.RED));
            return true;
        }

        // Check if player owns this horse
        if (!manager.isOwner(horse.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(Component.text("(Werk)paard is nog niet jouw eigendom!").color(NamedTextColor.RED));
            return true;
        }

        // Find the target player
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Speler niet gevonden: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Je hebt dit paard al gekoloniseerd!").color(NamedTextColor.YELLOW));
            return true;
        }

        // Check if already added
        if (manager.isAllowedRider(horse.getUniqueId(), targetPlayer.getUniqueId())) {
            // Remove them instead
            manager.removeAllowedRider(horse.getUniqueId(), targetPlayer.getUniqueId());

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(400),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(800)
            );

            Title title = Title.title(
                    Component.text("➖ VERWIJDERD").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " kan dit paard niet meer rijden").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);
        } else {
            // Add them
            manager.addAllowedRider(horse.getUniqueId(), targetPlayer.getUniqueId());

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(400),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(800)
            );

            Title title = Title.title(
                    Component.text("➕ MACHTIGING GEGEVEN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text(targetPlayer.getName() + " mag nu op dit paard rijden").color(NamedTextColor.GRAY),
                    times
            );
            player.showTitle(title);

            // Also notify the target player
            Title targetTitle = Title.title(
                    Component.text("🐴 PFEERD PERMISSIE").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text(player.getName() + " heeft je toegang gegeven tot hun paard").color(NamedTextColor.GRAY),
                    times
            );
            targetPlayer.showTitle(targetTitle);
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

