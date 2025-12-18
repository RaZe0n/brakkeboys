package com.finnb.plugin.managers;

import com.finnb.plugin.BrakkeBoysCORE;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HorseLockManager {

    private final BrakkeBoysCORE plugin;
    private final Map<UUID, UUID> lockedHorses; // Horse UUID -> Owner UUID
    private final Map<UUID, Set<UUID>> allowedRiders; // Horse UUID -> Set of allowed player UUIDs
    private final File dataFile;

    public HorseLockManager(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
        this.lockedHorses = new HashMap<>();
        this.allowedRiders = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "locked_horses.yml");
    }

    /**
     * Lock a horse to a player
     */
    public void lockHorse(UUID horseUuid, UUID ownerUuid) {
        lockedHorses.put(horseUuid, ownerUuid);
        saveData();
    }

    /**
     * Unlock a horse
     */
    public void unlockHorse(UUID horseUuid) {
        lockedHorses.remove(horseUuid);
        allowedRiders.remove(horseUuid);
        saveData();
    }

    /**
     * Check if a horse is locked
     */
    public boolean isLocked(UUID horseUuid) {
        return lockedHorses.containsKey(horseUuid);
    }

    /**
     * Get the owner of a locked horse
     */
    public UUID getOwner(UUID horseUuid) {
        return lockedHorses.get(horseUuid);
    }

    /**
     * Check if a player is the owner of a locked horse
     */
    public boolean isOwner(UUID horseUuid, UUID playerUuid) {
        UUID owner = lockedHorses.get(horseUuid);
        return owner != null && owner.equals(playerUuid);
    }

    /**
     * Add an allowed rider to a horse
     */
    public void addAllowedRider(UUID horseUuid, UUID playerUuid) {
        allowedRiders.computeIfAbsent(horseUuid, k -> new HashSet<>()).add(playerUuid);
        saveData();
    }

    /**
     * Remove an allowed rider from a horse
     */
    public void removeAllowedRider(UUID horseUuid, UUID playerUuid) {
        Set<UUID> riders = allowedRiders.get(horseUuid);
        if (riders != null) {
            riders.remove(playerUuid);
            if (riders.isEmpty()) {
                allowedRiders.remove(horseUuid);
            }
        }
        saveData();
    }

    /**
     * Check if a player is allowed to ride a horse (owner or in allowed list)
     */
    public boolean canRide(UUID horseUuid, UUID playerUuid) {
        // Owner can always ride
        if (isOwner(horseUuid, playerUuid)) {
            return true;
        }
        // Check allowed riders
        Set<UUID> riders = allowedRiders.get(horseUuid);
        return riders != null && riders.contains(playerUuid);
    }

    /**
     * Check if a player is in the allowed riders list (not owner)
     */
    public boolean isAllowedRider(UUID horseUuid, UUID playerUuid) {
        Set<UUID> riders = allowedRiders.get(horseUuid);
        return riders != null && riders.contains(playerUuid);
    }

    /**
     * Get all allowed riders for a horse
     */
    public Set<UUID> getAllowedRiders(UUID horseUuid) {
        return allowedRiders.getOrDefault(horseUuid, Collections.emptySet());
    }

    /**
     * Save locked horses data to file
     */
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, UUID> entry : lockedHorses.entrySet()) {
            String horsePath = "horses." + entry.getKey().toString();
            config.set(horsePath + ".owner", entry.getValue().toString());

            // Save allowed riders
            Set<UUID> riders = allowedRiders.get(entry.getKey());
            if (riders != null && !riders.isEmpty()) {
                List<String> riderList = new ArrayList<>();
                for (UUID rider : riders) {
                    riderList.add(rider.toString());
                }
                config.set(horsePath + ".allowed", riderList);
            }
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locked horses data: " + e.getMessage());
        }
    }

    /**
     * Load locked horses data from file
     */
    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (config.contains("horses")) {
            for (String horseUuidStr : config.getConfigurationSection("horses").getKeys(false)) {
                try {
                    UUID horseUuid = UUID.fromString(horseUuidStr);
                    String horsePath = "horses." + horseUuidStr;

                    // Load owner (support both old and new format)
                    String ownerStr = config.getString(horsePath + ".owner");
                    if (ownerStr == null) {
                        // Old format: direct value
                        ownerStr = config.getString("horses." + horseUuidStr);
                    }
                    if (ownerStr != null) {
                        UUID ownerUuid = UUID.fromString(ownerStr);
                        lockedHorses.put(horseUuid, ownerUuid);
                    }

                    // Load allowed riders
                    List<String> riderList = config.getStringList(horsePath + ".allowed");
                    if (!riderList.isEmpty()) {
                        Set<UUID> riders = new HashSet<>();
                        for (String riderStr : riderList) {
                            try {
                                riders.add(UUID.fromString(riderStr));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid rider UUID: " + riderStr);
                            }
                        }
                        if (!riders.isEmpty()) {
                            allowedRiders.put(horseUuid, riders);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in locked horses data: " + horseUuidStr);
                }
            }
        }

        plugin.getLogger().info("Loaded " + lockedHorses.size() + " locked horses.");
    }
}
