package com.finnb.plugin.listeners;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.managers.LaserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class LaserListener implements Listener {

    private final BrakkeBoysCORE plugin;
    private final LaserManager laserManager;
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Long> lastAlertTime = new HashMap<>(); // Cooldown for alerts
    private BukkitTask particleTask;
    private final Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.2F); // Reuse particle options

    public LaserListener(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
        this.laserManager = plugin.getLaserManager();
        
        // Start particle display task
        startParticleTask();
    }

    /**
     * Cancel the particle task (called on plugin disable)
     */
    public void cancelTasks() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }

    /**
     * Display laser particles for all active lasers
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Use direct access instead of creating new HashMap
                Map<UUID, LaserManager.LaserData> lasers = laserManager.getAllLasers();
                
                for (LaserManager.LaserData data : lasers.values()) {
                    if (data.isComplete()) {
                        // Only display if players are nearby (within 15 blocks)
                        if (hasPlayersNearby(data, 15.0)) {
                            displayLaserParticles(data);
                        }
                    }
                }
                
                // Clean up old alert times (older than 1 minute)
                long currentTime = System.currentTimeMillis();
                lastAlertTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > 60000);
            }
        }.runTaskTimer(plugin, 0L, 5L); // Run every 5 ticks (0.25 seconds)
    }

    /**
     * Check if any players are nearby the laser (within specified distance)
     */
    private boolean hasPlayersNearby(LaserManager.LaserData data, double distance) {
        Location pos1 = data.getFirstPosition();
        Location pos2 = data.getSecondPosition();
        
        if (pos1 == null || pos2 == null) {
            return false;
        }
        
        World world = pos1.getWorld();
        if (world == null) return false;
        
        // Optimize: calculate midpoint once and reuse
        Vector pos1Vec = pos1.toVector();
        Vector pos2Vec = pos2.toVector();
        Vector midpointVec = pos1Vec.clone().add(pos2Vec).multiply(0.5);
        double distanceSquared = distance * distance; // Use squared distance to avoid sqrt
        
        for (Player player : world.getPlayers()) {
            Location playerLoc = player.getLocation();
            if (!playerLoc.getWorld().equals(world)) continue;
            
            Vector playerVec = playerLoc.toVector();
            // Check squared distances to avoid sqrt calculations
            if (playerVec.distanceSquared(pos1Vec) <= distanceSquared ||
                playerVec.distanceSquared(pos2Vec) <= distanceSquared ||
                playerVec.distanceSquared(midpointVec) <= distanceSquared) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Display red laser particles between two positions
     */
    private void displayLaserParticles(LaserManager.LaserData data) {
        Location pos1 = data.getFirstPosition();
        Location pos2 = data.getSecondPosition();
        
        if (pos1 == null || pos2 == null || !pos1.getWorld().equals(pos2.getWorld())) {
            return;
        }
        
        World world = pos1.getWorld();
        if (world == null) return;
        
        Vector direction = pos2.toVector().subtract(pos1.toVector());
        double length = direction.length();
        direction.normalize();
        
        // Spawn particles along the line (reuse redDust field)
        Vector directionScaled = direction.clone();
        for (double i = 0; i <= length; i += 0.2) {
            Location point = pos1.clone().add(directionScaled.clone().multiply(i));
            world.spawnParticle(
                Particle.DUST,
                point,
                1,
                0, 0, 0,
                0,
                redDust
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (to == null) return;
        
        Location lastLoc = lastLocation.get(player.getUniqueId());
        if (lastLoc == null) {
            lastLocation.put(player.getUniqueId(), to.clone());
            return;
        }
        
        // Only check if player moved significantly
        if (from.distance(to) < 0.1) {
            return;
        }
        
        // Check if player crossed any laser
        Map<UUID, LaserManager.LaserData> lasers = laserManager.getAllLasers();
        
        for (Map.Entry<UUID, LaserManager.LaserData> entry : lasers.entrySet()) {
            UUID ownerUuid = entry.getKey();
            LaserManager.LaserData data = entry.getValue();
            
            // Don't alert if player is the owner
            if (ownerUuid.equals(player.getUniqueId())) {
                continue;
            }
            
            if (data.isComplete() && crossedLaserLine(lastLoc, to, data)) {
                // Check cooldown (5 seconds)
                Long lastAlert = lastAlertTime.get(ownerUuid);
                if (lastAlert != null && System.currentTimeMillis() - lastAlert < 5000) {
                    continue;
                }
                
                // Player crossed a laser - alert the owner
                alertLaserOwner(ownerUuid, player);
                lastAlertTime.put(ownerUuid, System.currentTimeMillis());
                break; // Only alert once per movement
            }
        }
        
        lastLocation.put(player.getUniqueId(), to.clone());
    }

    /**
     * Check if a player crossed a laser line (was on one side, now on the other)
     */
    private boolean crossedLaserLine(Location from, Location to, LaserManager.LaserData data) {
        Location pos1 = data.getFirstPosition();
        Location pos2 = data.getSecondPosition();
        
        if (pos1 == null || pos2 == null || !pos1.getWorld().equals(from.getWorld())) {
            return false;
        }
        
        double tolerance = 0.5;
        
        // Check if either position is near the line
        boolean fromNear = isNearLine(from, pos1, pos2, tolerance);
        boolean toNear = isNearLine(to, pos1, pos2, tolerance);
        
        // If both are near, they didn't cross (they're just walking along it)
        if (fromNear && toNear) {
            return false;
        }
        
        // Check if the movement line crosses the laser line
        return lineSegmentsCross(from, to, pos1, pos2, tolerance);
    }

    /**
     * Check if a point is near a line segment
     */
    private boolean isNearLine(Location point, Location lineStart, Location lineEnd, double tolerance) {
        Vector pointVec = point.toVector();
        Vector lineStartVec = lineStart.toVector();
        Vector lineEndVec = lineEnd.toVector();
        
        Vector line = lineEndVec.clone().subtract(lineStartVec);
        double lineLengthSquared = line.lengthSquared(); // Use squared to avoid sqrt
        
        if (lineLengthSquared < 0.0001) { // Very small line
            return pointVec.distanceSquared(lineStartVec) <= tolerance * tolerance;
        }
        
        Vector pointToStart = pointVec.clone().subtract(lineStartVec);
        double t = Math.max(0, Math.min(1, pointToStart.dot(line) / lineLengthSquared));
        Vector projection = lineStartVec.clone().add(line.clone().multiply(t));
        double distanceSquared = pointVec.distanceSquared(projection);
        
        return distanceSquared <= tolerance * tolerance;
    }

    /**
     * Check if two line segments cross each other
     */
    private boolean lineSegmentsCross(Location seg1Start, Location seg1End, Location seg2Start, Location seg2End, double tolerance) {
        Vector p1 = seg1Start.toVector();
        Vector p2 = seg1End.toVector();
        Vector p3 = seg2Start.toVector();
        Vector p4 = seg2End.toVector();
        
        Vector d1 = p2.clone().subtract(p1);
        Vector d2 = p4.clone().subtract(p3);
        Vector d3 = p1.clone().subtract(p3);
        
        double cross = d1.getX() * d2.getZ() - d1.getZ() * d2.getX();
        
        if (Math.abs(cross) < 0.0001) {
            // Lines are parallel
            return false;
        }
        
        double t1 = (d3.getX() * d2.getZ() - d3.getZ() * d2.getX()) / cross;
        double t2 = (d3.getX() * d1.getZ() - d3.getZ() * d1.getX()) / cross;
        
        // Check if intersection point is within both segments
        return t1 >= -tolerance && t1 <= 1 + tolerance && t2 >= -tolerance && t2 <= 1 + tolerance;
    }

    /**
     * Alert the laser owner that someone crossed their laser
     */
    private void alertLaserOwner(UUID ownerUuid, Player intruder) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) {
            return;
        }
        
        // Title message
        Title.Times times = Title.Times.times(
            Duration.ofMillis(200),
            Duration.ofMillis(2400),
            Duration.ofMillis(600)
        );
        
        Title title = Title.title(
            Component.text("ALERT").color(NamedTextColor.RED).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD),
            Component.text("Iemand heeft je laser doorbroken!").color(NamedTextColor.GRAY),
            times
        );
        owner.showTitle(title);
        
        // Chat message
        String message = intruder.getName() + " is zojuist door je alarm heen gelopen";
        owner.sendMessage(Component.text(message).color(NamedTextColor.RED));
        
        // Sound - using NOTE_BLOCK_PLING for alarm sound (high pitch)
        owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 0.3f);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data when they leave
        UUID playerUuid = event.getPlayer().getUniqueId();
        lastLocation.remove(playerUuid);
        lastAlertTime.remove(playerUuid);
    }
}

