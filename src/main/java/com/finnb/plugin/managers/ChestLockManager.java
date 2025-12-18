package com.finnb.plugin.managers;

import com.finnb.plugin.BrakkeBoysCORE;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChestLockManager {

    private final BrakkeBoysCORE plugin;
    private final Map<String, UUID> lockedChests; // Location string -> Owner UUID
    private final Map<String, UUID> lockedDoors; // Location string -> Owner UUID (doors, trapdoors, fence gates)
    private final Map<String, Set<UUID>> allowedChestUsers; // Location string -> Set of allowed player UUIDs
    private final Map<String, Set<UUID>> allowedDoorUsers; // Location string -> Set of allowed player UUIDs
    private final Map<String, String> chestPasscodes; // Location string -> Passcode
    private final Map<String, String> doorPasscodes; // Location string -> Passcode
    private final Map<UUID, Integer> maxChestLocks; // Player UUID -> Max chest locks allowed
    private final Map<UUID, Integer> maxDoorLocks; // Player UUID -> Max door locks allowed
    private final File chestDataFile;
    private final File doorDataFile;
    private final File configFile;

    public ChestLockManager(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
        this.lockedChests = new HashMap<>();
        this.lockedDoors = new HashMap<>();
        this.allowedChestUsers = new HashMap<>();
        this.allowedDoorUsers = new HashMap<>();
        this.chestPasscodes = new HashMap<>();
        this.doorPasscodes = new HashMap<>();
        this.maxChestLocks = new HashMap<>();
        this.maxDoorLocks = new HashMap<>();
        this.chestDataFile = new File(plugin.getDataFolder(), "locked_chests.yml");
        this.doorDataFile = new File(plugin.getDataFolder(), "locked_doors.yml");
        this.configFile = new File(plugin.getDataFolder(), "block_lock_config.yml");
    }

    /**
     * Lock a chest at a location
     */
    public boolean lockChest(Location location, UUID ownerUuid) {
        return lockChest(location, ownerUuid, null);
    }

    /**
     * Lock a chest at a location with optional passcode
     */
    public boolean lockChest(Location location, UUID ownerUuid, String passcode) {
        String locKey = locationToString(location);
        
        // Check if already locked
        if (lockedChests.containsKey(locKey)) {
            return false;
        }

        // Check if player has reached max locks
        int currentLocks = getPlayerChestLockCount(ownerUuid);
        int max = getMaxChestLocks(ownerUuid);
        
        if (currentLocks >= max) {
            return false;
        }

        lockedChests.put(locKey, ownerUuid);
        if (passcode != null && !passcode.isEmpty()) {
            chestPasscodes.put(locKey, passcode);
        }
        saveChestData();
        return true;
    }

    /**
     * Lock a door/trapdoor/fence gate at a location
     */
    public boolean lockDoor(Location location, UUID ownerUuid) {
        return lockDoor(location, ownerUuid, null);
    }

    /**
     * Lock a door/trapdoor/fence gate at a location with optional passcode
     */
    public boolean lockDoor(Location location, UUID ownerUuid, String passcode) {
        String locKey = locationToString(location);
        
        // Check if already locked (check both blocks for doors)
        if (lockedDoors.containsKey(locKey)) {
            return false;
        }

        // Check if player has reached max door locks
        int currentLocks = getPlayerDoorLockCount(ownerUuid);
        int max = getMaxDoorLocks(ownerUuid);
        
        if (currentLocks >= max) {
            return false;
        }

        // Lock this block
        lockedDoors.put(locKey, ownerUuid);
        if (passcode != null && !passcode.isEmpty()) {
            doorPasscodes.put(locKey, passcode);
        }
        
        // If it's a door, also lock the other half
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            lockedDoors.put(otherKey, ownerUuid);
            if (passcode != null && !passcode.isEmpty()) {
                doorPasscodes.put(otherKey, passcode);
            }
        }
        
        saveDoorData();
        return true;
    }

    /**
     * Unlock a chest at a location
     */
    public void unlockChest(Location location) {
        String locKey = locationToString(location);
        lockedChests.remove(locKey);
        allowedChestUsers.remove(locKey);
        chestPasscodes.remove(locKey);
        saveChestData();
    }

    /**
     * Unlock a door at a location
     */
    public void unlockDoor(Location location) {
        String locKey = locationToString(location);
        lockedDoors.remove(locKey);
        allowedDoorUsers.remove(locKey);
        doorPasscodes.remove(locKey);
        
        // If it's a door, also unlock the other half
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            lockedDoors.remove(otherKey);
            allowedDoorUsers.remove(otherKey);
            doorPasscodes.remove(otherKey);
        }
        
        saveDoorData();
    }

    /**
     * Check if a chest is locked
     */
    public boolean isChestLocked(Location location) {
        return lockedChests.containsKey(locationToString(location));
    }

    /**
     * Check if a door is locked (checks both blocks for doors)
     */
    public boolean isDoorLocked(Location location) {
        String locKey = locationToString(location);
        if (lockedDoors.containsKey(locKey)) {
            return true;
        }
        
        // Check the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            return lockedDoors.containsKey(otherKey);
        }
        
        return false;
    }

    /**
     * Check if any block is locked (chest or door)
     */
    public boolean isLocked(Location location) {
        return isChestLocked(location) || isDoorLocked(location);
    }

    /**
     * Get the owner of a locked chest
     */
    public UUID getChestOwner(Location location) {
        return lockedChests.get(locationToString(location));
    }

    /**
     * Get the owner of a locked door (checks both blocks for doors)
     */
    public UUID getDoorOwner(Location location) {
        String locKey = locationToString(location);
        UUID owner = lockedDoors.get(locKey);
        if (owner != null) {
            return owner;
        }
        
        // Check the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            return lockedDoors.get(otherKey);
        }
        
        return null;
    }

    /**
     * Get the owner of any locked block
     */
    public UUID getOwner(Location location) {
        UUID chestOwner = getChestOwner(location);
        if (chestOwner != null) return chestOwner;
        return getDoorOwner(location);
    }

    /**
     * Check if a player owns a locked chest
     */
    public boolean isChestOwner(Location location, UUID playerUuid) {
        UUID owner = lockedChests.get(locationToString(location));
        return owner != null && owner.equals(playerUuid);
    }

    /**
     * Check if a player owns a locked door (checks both blocks for doors)
     */
    public boolean isDoorOwner(Location location, UUID playerUuid) {
        UUID owner = getDoorOwner(location);
        return owner != null && owner.equals(playerUuid);
    }

    /**
     * Check if a player owns any locked block
     */
    public boolean isOwner(Location location, UUID playerUuid) {
        return isChestOwner(location, playerUuid) || isDoorOwner(location, playerUuid);
    }

    /**
     * Add an allowed user to a chest
     */
    public void addAllowedChestUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        allowedChestUsers.computeIfAbsent(locKey, k -> new HashSet<>()).add(playerUuid);
        saveChestData();
    }

    /**
     * Remove an allowed user from a chest
     */
    public void removeAllowedChestUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        Set<UUID> users = allowedChestUsers.get(locKey);
        if (users != null) {
            users.remove(playerUuid);
            if (users.isEmpty()) {
                allowedChestUsers.remove(locKey);
            }
        }
        saveChestData();
    }

    /**
     * Check if a player is an allowed user of a chest
     */
    public boolean isAllowedChestUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        Set<UUID> users = allowedChestUsers.get(locKey);
        return users != null && users.contains(playerUuid);
    }

    /**
     * Check if a player can access a chest (owner or allowed user)
     */
    public boolean canAccessChest(Location location, UUID playerUuid) {
        if (isChestOwner(location, playerUuid)) {
            return true;
        }
        return isAllowedChestUser(location, playerUuid);
    }

    /**
     * Add an allowed user to a door
     */
    public void addAllowedDoorUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        allowedDoorUsers.computeIfAbsent(locKey, k -> new HashSet<>()).add(playerUuid);
        
        // Also add to the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            allowedDoorUsers.computeIfAbsent(otherKey, k -> new HashSet<>()).add(playerUuid);
        }
        
        saveDoorData();
    }

    /**
     * Remove an allowed user from a door
     */
    public void removeAllowedDoorUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        Set<UUID> users = allowedDoorUsers.get(locKey);
        if (users != null) {
            users.remove(playerUuid);
            if (users.isEmpty()) {
                allowedDoorUsers.remove(locKey);
            }
        }
        
        // Also remove from the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            Set<UUID> otherUsers = allowedDoorUsers.get(otherKey);
            if (otherUsers != null) {
                otherUsers.remove(playerUuid);
                if (otherUsers.isEmpty()) {
                    allowedDoorUsers.remove(otherKey);
                }
            }
        }
        
        saveDoorData();
    }

    /**
     * Check if a player is an allowed user of a door
     */
    public boolean isAllowedDoorUser(Location location, UUID playerUuid) {
        String locKey = locationToString(location);
        Set<UUID> users = allowedDoorUsers.get(locKey);
        if (users != null && users.contains(playerUuid)) {
            return true;
        }
        
        // Check the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            Set<UUID> otherUsers = allowedDoorUsers.get(otherKey);
            return otherUsers != null && otherUsers.contains(playerUuid);
        }
        
        return false;
    }

    /**
     * Check if a player can access a door (owner or allowed user)
     */
    public boolean canAccessDoor(Location location, UUID playerUuid) {
        if (isDoorOwner(location, playerUuid)) {
            return true;
        }
        return isAllowedDoorUser(location, playerUuid);
    }

    /**
     * Get passcode for a chest
     */
    public String getChestPasscode(Location location) {
        return chestPasscodes.get(locationToString(location));
    }

    /**
     * Get passcode for a door
     */
    public String getDoorPasscode(Location location) {
        String locKey = locationToString(location);
        String passcode = doorPasscodes.get(locKey);
        if (passcode != null) {
            return passcode;
        }
        // Check the other half if it's a door
        Location otherHalf = getDoorOtherHalf(location);
        if (otherHalf != null) {
            String otherKey = locationToString(otherHalf);
            return doorPasscodes.get(otherKey);
        }
        return null;
    }

    /**
     * Check if a chest has a passcode
     */
    public boolean hasChestPasscode(Location location) {
        return getChestPasscode(location) != null;
    }

    /**
     * Check if a door has a passcode
     */
    public boolean hasDoorPasscode(Location location) {
        return getDoorPasscode(location) != null;
    }

    /**
     * Verify passcode for a chest
     */
    public boolean verifyChestPasscode(Location location, String input) {
        String passcode = getChestPasscode(location);
        return passcode != null && passcode.equals(input);
    }

    /**
     * Verify passcode for a door
     */
    public boolean verifyDoorPasscode(Location location, String input) {
        String passcode = getDoorPasscode(location);
        return passcode != null && passcode.equals(input);
    }

    /**
     * Get how many chests a player has locked
     */
    public int getPlayerChestLockCount(UUID playerUuid) {
        int count = 0;
        for (UUID owner : lockedChests.values()) {
            if (owner.equals(playerUuid)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get how many doors a player has locked
     * Only counts bottom halves to avoid double-counting doors
     */
    public int getPlayerDoorLockCount(UUID playerUuid) {
        int count = 0;
        for (Map.Entry<String, UUID> entry : lockedDoors.entrySet()) {
            if (entry.getValue().equals(playerUuid)) {
                // Parse location from key
                String[] parts = entry.getKey().split(",");
                if (parts.length == 4) {
                    try {
                        World world = plugin.getServer().getWorld(parts[0]);
                        if (world != null) {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int z = Integer.parseInt(parts[3]);
                            Location loc = new Location(world, x, y, z);
                            
                            // Only count if it's a bottom half of a door (or not a door at all)
                            Block block = world.getBlockAt(loc);
                            BlockData blockData = block.getBlockData();
                            if (!(blockData instanceof Door) || ((Door) blockData).getHalf() == Door.Half.BOTTOM) {
                                count++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Invalid location, count it anyway
                        count++;
                    }
                } else {
                    // Invalid format, count it anyway
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get remaining chest locks for a player
     */
    public int getRemainingChestLocks(UUID playerUuid) {
        int max = getMaxChestLocks(playerUuid);
        int current = getPlayerChestLockCount(playerUuid);
        return Math.max(0, max - current);
    }

    /**
     * Get remaining door locks for a player
     */
    public int getRemainingDoorLocks(UUID playerUuid) {
        int max = getMaxDoorLocks(playerUuid);
        int current = getPlayerDoorLockCount(playerUuid);
        return Math.max(0, max - current);
    }

    /**
     * Get max chest locks for a player (default 5)
     */
    public int getMaxChestLocks(UUID playerUuid) {
        return maxChestLocks.getOrDefault(playerUuid, 5);
    }

    /**
     * Get max door locks for a player (default 5)
     */
    public int getMaxDoorLocks(UUID playerUuid) {
        return maxDoorLocks.getOrDefault(playerUuid, 5);
    }

    /**
     * Set max chest locks for a player
     */
    public void setMaxChestLocks(UUID playerUuid, int max) {
        if (max < 0) max = 0;
        maxChestLocks.put(playerUuid, max);
        saveConfig();
    }

    /**
     * Set max door locks for a player
     */
    public void setMaxDoorLocks(UUID playerUuid, int max) {
        if (max < 0) max = 0;
        maxDoorLocks.put(playerUuid, max);
        saveConfig();
    }

    /**
     * Remove all chest locks for a player
     */
    public void removeAllChestLocks(UUID playerUuid) {
        lockedChests.entrySet().removeIf(entry -> entry.getValue().equals(playerUuid));
        saveChestData();
    }

    /**
     * Remove all door locks for a player
     */
    public void removeAllDoorLocks(UUID playerUuid) {
        lockedDoors.entrySet().removeIf(entry -> entry.getValue().equals(playerUuid));
        saveDoorData();
    }

    /**
     * Convert location to string key
     */
    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }

    /**
     * Get the other half of a door (top or bottom)
     * Returns null if the block is not a door or if the other half doesn't exist
     */
    private Location getDoorOtherHalf(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Block block = world.getBlockAt(location);
        BlockData blockData = block.getBlockData();

        // Only handle actual doors (not trapdoors or fence gates)
        if (!(blockData instanceof Door)) {
            return null;
        }

        Door door = (Door) blockData;
        Door.Half half = door.getHalf();

        // Get the other half location
        if (half == Door.Half.BOTTOM) {
            // Other half is above
            return location.clone().add(0, 1, 0);
        } else if (half == Door.Half.TOP) {
            // Other half is below
            return location.clone().add(0, -1, 0);
        }

        return null;
    }

    /**
     * Save locked chests data to file
     */
    public void saveChestData() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UUID> entry : lockedChests.entrySet()) {
            String chestPath = "chests." + entry.getKey();
            config.set(chestPath + ".owner", entry.getValue().toString());

            // Save passcode
            String passcode = chestPasscodes.get(entry.getKey());
            if (passcode != null) {
                config.set(chestPath + ".passcode", passcode);
            }

            // Save allowed users
            Set<UUID> users = allowedChestUsers.get(entry.getKey());
            if (users != null && !users.isEmpty()) {
                List<String> userList = new ArrayList<>();
                for (UUID user : users) {
                    userList.add(user.toString());
                }
                config.set(chestPath + ".allowed", userList);
            }
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(chestDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locked chests data: " + e.getMessage());
        }
    }

    /**
     * Save locked doors data to file
     */
    public void saveDoorData() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UUID> entry : lockedDoors.entrySet()) {
            String doorPath = "doors." + entry.getKey();
            config.set(doorPath + ".owner", entry.getValue().toString());

            // Save passcode
            String passcode = doorPasscodes.get(entry.getKey());
            if (passcode != null) {
                config.set(doorPath + ".passcode", passcode);
            }

            // Save allowed users
            Set<UUID> users = allowedDoorUsers.get(entry.getKey());
            if (users != null && !users.isEmpty()) {
                List<String> userList = new ArrayList<>();
                for (UUID user : users) {
                    userList.add(user.toString());
                }
                config.set(doorPath + ".allowed", userList);
            }
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(doorDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locked doors data: " + e.getMessage());
        }
    }

    /**
     * Load locked chests data from file
     */
    public void loadChestData() {
        if (!chestDataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(chestDataFile);

        if (config.contains("chests")) {
            for (String locKey : config.getConfigurationSection("chests").getKeys(false)) {
                try {
                    String chestPath = "chests." + locKey;
                    // Support both old format (direct value) and new format (with owner)
                    String ownerStr = config.getString(chestPath + ".owner");
                    if (ownerStr == null) {
                        ownerStr = config.getString(chestPath);
                    }
                    if (ownerStr != null) {
                        UUID ownerUuid = UUID.fromString(ownerStr);
                        lockedChests.put(locKey, ownerUuid);
                    }

                    // Load passcode
                    String passcode = config.getString(chestPath + ".passcode");
                    if (passcode != null && !passcode.isEmpty()) {
                        chestPasscodes.put(locKey, passcode);
                    }

                    // Load allowed users
                    List<String> userList = config.getStringList(chestPath + ".allowed");
                    if (!userList.isEmpty()) {
                        Set<UUID> users = new HashSet<>();
                        for (String userStr : userList) {
                            try {
                                users.add(UUID.fromString(userStr));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in allowed users: " + userStr);
                            }
                        }
                        if (!users.isEmpty()) {
                            allowedChestUsers.put(locKey, users);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in locked chests data: " + locKey);
                }
            }
        }

        plugin.getLogger().info("Loaded " + lockedChests.size() + " locked chests.");
    }

    /**
     * Load locked doors data from file
     */
    public void loadDoorData() {
        if (!doorDataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(doorDataFile);

        if (config.contains("doors")) {
            for (String locKey : config.getConfigurationSection("doors").getKeys(false)) {
                try {
                    String doorPath = "doors." + locKey;
                    // Support both old format (direct value) and new format (with owner)
                    String ownerStr = config.getString(doorPath + ".owner");
                    if (ownerStr == null) {
                        ownerStr = config.getString(doorPath);
                    }
                    if (ownerStr != null) {
                        UUID ownerUuid = UUID.fromString(ownerStr);
                        lockedDoors.put(locKey, ownerUuid);
                    }

                    // Load passcode
                    String passcode = config.getString(doorPath + ".passcode");
                    if (passcode != null && !passcode.isEmpty()) {
                        doorPasscodes.put(locKey, passcode);
                    }

                    // Load allowed users
                    List<String> userList = config.getStringList(doorPath + ".allowed");
                    if (!userList.isEmpty()) {
                        Set<UUID> users = new HashSet<>();
                        for (String userStr : userList) {
                            try {
                                users.add(UUID.fromString(userStr));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in allowed users: " + userStr);
                            }
                        }
                        if (!users.isEmpty()) {
                            allowedDoorUsers.put(locKey, users);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in locked doors data: " + locKey);
                }
            }
        }

        plugin.getLogger().info("Loaded " + lockedDoors.size() + " locked doors.");
    }

    /**
     * Load all data (chests and doors)
     */
    public void loadData() {
        loadChestData();
        loadDoorData();
    }

    /**
     * Save max locks config
     */
    public void saveConfig() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : maxChestLocks.entrySet()) {
            config.set("max_chest_locks." + entry.getKey().toString(), entry.getValue());
        }

        for (Map.Entry<UUID, Integer> entry : maxDoorLocks.entrySet()) {
            config.set("max_door_locks." + entry.getKey().toString(), entry.getValue());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save block lock config: " + e.getMessage());
        }
    }

    /**
     * Load max locks config
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (config.contains("max_chest_locks")) {
            for (String playerUuidStr : config.getConfigurationSection("max_chest_locks").getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    int max = config.getInt("max_chest_locks." + playerUuidStr);
                    maxChestLocks.put(playerUuid, max);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in max chest locks config: " + playerUuidStr);
                }
            }
        }

        if (config.contains("max_door_locks")) {
            for (String playerUuidStr : config.getConfigurationSection("max_door_locks").getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    int max = config.getInt("max_door_locks." + playerUuidStr);
                    maxDoorLocks.put(playerUuid, max);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in max door locks config: " + playerUuidStr);
                }
            }
        }
    }
}
