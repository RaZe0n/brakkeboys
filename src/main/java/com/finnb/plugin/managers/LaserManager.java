package com.finnb.plugin.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserManager {

    private final JavaPlugin plugin;
    private final Map<UUID, LaserData> lasers = new HashMap<>();
    private final Map<UUID, Integer> maxLasers; // Player UUID -> Max lasers allowed
    private File laserFile;
    private FileConfiguration laserConfig;
    private File configFile;
    private FileConfiguration config;

    public LaserManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxLasers = new HashMap<>();
        laserFile = new File(plugin.getDataFolder(), "lasers.yml");
        laserConfig = YamlConfiguration.loadConfiguration(laserFile);
        configFile = new File(plugin.getDataFolder(), "laser_config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Set the first position for a player's laser
     */
    public boolean setFirstPosition(UUID playerUuid, Location location) {
        LaserData data = lasers.get(playerUuid);
        
        // Check if player has reached max lasers
        int currentLasers = getPlayerLaserCount(playerUuid);
        int max = getMaxLasers(playerUuid);
        
        if (currentLasers >= max) {
            return false;
        }
        
        if (data == null) {
            data = new LaserData();
            lasers.put(playerUuid, data);
        } else if (data.isComplete()) {
            // Player already has a complete laser, need to remove it first or increase max
            return false;
        }
        
        data.setFirstPosition(location);
        saveData();
        return true;
    }

    /**
     * Set the second position for a player's laser
     */
    public boolean setSecondPosition(UUID playerUuid, Location location) {
        LaserData data = lasers.get(playerUuid);
        if (data == null || data.getFirstPosition() == null) {
            return false;
        }
        
        Location firstPos = data.getFirstPosition();
        double distance = firstPos.distance(location);
        
        if (distance > 10.0) {
            return false; // Too long
        }
        
        data.setSecondPosition(location);
        saveData();
        return true;
    }

    /**
     * Check if a player has a complete laser
     */
    public boolean hasLaser(UUID playerUuid) {
        LaserData data = lasers.get(playerUuid);
        return data != null && data.isComplete();
    }

    /**
     * Get the laser data for a player
     */
    public LaserData getLaser(UUID playerUuid) {
        return lasers.get(playerUuid);
    }

    /**
     * Remove a player's laser
     */
    public void removeLaser(UUID playerUuid) {
        lasers.remove(playerUuid);
        saveData();
    }

    /**
     * Get how many lasers a player has
     */
    public int getPlayerLaserCount(UUID playerUuid) {
        LaserData data = lasers.get(playerUuid);
        return (data != null && data.isComplete()) ? 1 : 0;
    }

    /**
     * Get remaining lasers a player can set
     */
    public int getRemainingLasers(UUID playerUuid) {
        int max = getMaxLasers(playerUuid);
        int current = getPlayerLaserCount(playerUuid);
        return Math.max(0, max - current);
    }

    /**
     * Get the maximum number of lasers a player can have
     */
    public int getMaxLasers(UUID playerUuid) {
        return maxLasers.getOrDefault(playerUuid, 1); // Default to 1
    }

    /**
     * Set the maximum number of lasers a player can have
     */
    public void setMaxLasers(UUID playerUuid, int max) {
        if (max < 0) {
            max = 0;
        }
        maxLasers.put(playerUuid, max);
        saveConfig();
    }

    /**
     * Get all lasers (for listener to check crossings)
     */
    public Map<UUID, LaserData> getAllLasers() {
        return new HashMap<>(lasers);
    }

    /**
     * Get the owner of a laser at a specific location
     */
    public UUID getLaserOwner(Location location) {
        for (Map.Entry<UUID, LaserData> entry : lasers.entrySet()) {
            LaserData data = entry.getValue();
            if (data.isComplete() && isOnLaserLine(location, data)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Check if a location is on a laser line
     */
    private boolean isOnLaserLine(Location location, LaserData data) {
        Location pos1 = data.getFirstPosition();
        Location pos2 = data.getSecondPosition();
        
        if (pos1 == null || pos2 == null) {
            return false;
        }
        
        // Check if location is on the line segment between pos1 and pos2
        // Using a tolerance of 0.5 blocks
        double tolerance = 0.5;
        
        // Calculate distance from point to line segment
        double dist = distanceToLineSegment(
            location.toVector(),
            pos1.toVector(),
            pos2.toVector()
        );
        
        return dist <= tolerance;
    }

    /**
     * Calculate distance from a point to a line segment
     */
    private double distanceToLineSegment(org.bukkit.util.Vector point, org.bukkit.util.Vector lineStart, org.bukkit.util.Vector lineEnd) {
        org.bukkit.util.Vector line = lineEnd.clone().subtract(lineStart);
        double lineLength = line.length();
        
        if (lineLength == 0) {
            return point.distance(lineStart);
        }
        
        double t = Math.max(0, Math.min(1, point.clone().subtract(lineStart).dot(line) / (lineLength * lineLength)));
        org.bukkit.util.Vector projection = lineStart.clone().add(line.clone().multiply(t));
        return point.distance(projection);
    }

    /**
     * Load laser data from file
     */
    public void loadData() {
        if (!laserFile.exists()) {
            return;
        }

        lasers.clear();
        
        if (laserConfig.contains("lasers")) {
            for (String key : laserConfig.getConfigurationSection("lasers").getKeys(false)) {
                UUID playerUuid = UUID.fromString(key);
                String path = "lasers." + key;
                
                if (laserConfig.contains(path + ".pos1") && laserConfig.contains(path + ".pos2")) {
                    Location pos1 = (Location) laserConfig.get(path + ".pos1");
                    Location pos2 = (Location) laserConfig.get(path + ".pos2");
                    
                    LaserData data = new LaserData();
                    data.setFirstPosition(pos1);
                    data.setSecondPosition(pos2);
                    lasers.put(playerUuid, data);
                }
            }
        }
    }

    /**
     * Load config data (max lasers)
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            return;
        }

        maxLasers.clear();
        
        if (config.contains("maxLasers")) {
            for (String key : config.getConfigurationSection("maxLasers").getKeys(false)) {
                UUID playerUuid = UUID.fromString(key);
                int max = config.getInt("maxLasers." + key, 1);
                maxLasers.put(playerUuid, max);
            }
        }
    }

    /**
     * Save config data (max lasers)
     */
    public void saveConfig() {
        config.set("maxLasers", null);
        
        for (Map.Entry<UUID, Integer> entry : maxLasers.entrySet()) {
            config.set("maxLasers." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save laser_config.yml: " + e.getMessage());
        }
    }

    /**
     * Save laser data to file
     */
    public void saveData() {
        laserConfig.set("lasers", null);
        
        for (Map.Entry<UUID, LaserData> entry : lasers.entrySet()) {
            LaserData data = entry.getValue();
            if (data.isComplete()) {
                String path = "lasers." + entry.getKey().toString();
                laserConfig.set(path + ".pos1", data.getFirstPosition());
                laserConfig.set(path + ".pos2", data.getSecondPosition());
            }
        }
        
        try {
            laserConfig.save(laserFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save lasers.yml: " + e.getMessage());
        }
    }

    /**
     * Inner class to store laser position data
     */
    public static class LaserData {
        private Location firstPosition;
        private Location secondPosition;

        public Location getFirstPosition() {
            return firstPosition;
        }

        public void setFirstPosition(Location firstPosition) {
            this.firstPosition = firstPosition;
        }

        public Location getSecondPosition() {
            return secondPosition;
        }

        public void setSecondPosition(Location secondPosition) {
            this.secondPosition = secondPosition;
        }

        public boolean isComplete() {
            return firstPosition != null && secondPosition != null;
        }
    }
}

