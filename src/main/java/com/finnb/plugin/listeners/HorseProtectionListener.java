package com.finnb.plugin.listeners;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.HorseLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;

import java.time.Duration;

public class HorseProtectionListener implements Listener {

    private final BrakkeBoysCORE plugin;

    public HorseProtectionListener(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityMount(EntityMountEvent event) {
        // Only care about players mounting horses
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getMount() instanceof AbstractHorse horse)) {
            return;
        }

        HorseLockManager manager = plugin.getHorseLockManager();

        // Check if the horse is locked
        if (manager.isLocked(horse.getUniqueId())) {
            // Check if the player can ride (owner or allowed rider)
            if (!manager.canRide(horse.getUniqueId(), player.getUniqueId())) {
                // Not allowed - cancel the mount
                event.setCancelled(true);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200),
                        Duration.ofMillis(2400),
                        Duration.ofMillis(600)
                );

                Title title = Title.title(
                        Component.text("🔒 LOCKED").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Dit beest is niet van jou bigman, jat maar een ander.").color(NamedTextColor.GRAY),
                        times
                );
                player.showTitle(title);
            }
        }
    }
}
