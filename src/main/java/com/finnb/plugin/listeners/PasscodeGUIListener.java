package com.finnb.plugin.listeners;

import com.finnb.plugin.BrakkeBoysCORE;
import com.finnb.plugin.gui.PasscodeGUI;
import com.finnb.plugin.managers.ChestLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PasscodeGUIListener implements Listener {

    private final BrakkeBoysCORE plugin;
    private final Map<UUID, String> playerInputs = new HashMap<>(); // Player UUID -> current input
    private final Map<UUID, Location> playerBlockLocations = new HashMap<>(); // Player UUID -> block location
    private final Map<UUID, Boolean> playerIsDoor = new HashMap<>(); // Player UUID -> is door
    private final Set<UUID> temporaryChestAccess = new HashSet<>(); // Players with temporary chest access
    private final Map<String, Long> temporaryDoorAccess = new HashMap<>(); // Location string -> timestamp when access expires
    private BukkitTask cleanupTask;

    public PasscodeGUIListener(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
        // Start periodic cleanup task for expired door access
        startCleanupTask();
    }

    /**
     * Start periodic cleanup task for expired door access entries
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                temporaryDoorAccess.entrySet().removeIf(entry -> currentTime > entry.getValue());
            }
        }.runTaskTimer(plugin, 200L, 200L); // Run every 10 seconds
    }

    /**
     * Cancel cleanup task (called on plugin disable)
     */
    public void cancelTasks() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
    }

    /**
     * Create location key efficiently
     */
    private String createLocationKey(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if player has an open passcode GUI
        if (!playerBlockLocations.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        Location blockLocation = playerBlockLocations.get(player.getUniqueId());
        boolean isDoor = playerIsDoor.getOrDefault(player.getUniqueId(), false);
        ChestLockManager manager = plugin.getChestLockManager();
        Inventory inv = event.getInventory();

        String currentInput = playerInputs.getOrDefault(player.getUniqueId(), "");

        // Check what was clicked
        ItemMeta meta = clicked.getItemMeta();
        if (clicked.getType() == Material.PLAYER_HEAD && meta != null && meta.hasCustomModelData()) {
            // Number button clicked (using custom model data)
            int number = meta.getCustomModelData();
            if (number >= 0 && number <= 9) {
                currentInput += number;
                playerInputs.put(player.getUniqueId(), currentInput);
                PasscodeGUI.updateDisplay(inv, currentInput);
            }
        } else if (clicked.getType() == Material.EMERALD_BLOCK) {
            // Submit button clicked
            String correctPasscode = isDoor ? manager.getDoorPasscode(blockLocation) : manager.getChestPasscode(blockLocation);
            
            if (correctPasscode != null && currentInput.equals(correctPasscode)) {
                // Correct passcode
                player.closeInventory();
                player.sendMessage(Component.text("✓ Pincode correct!").color(NamedTextColor.GREEN));

                if (isDoor) {
                    // Grant temporary door access (5 seconds) for this specific location and the other half
                    String locationKey = createLocationKey(blockLocation);
                    long expiryTime = System.currentTimeMillis() + 5000;
                    temporaryDoorAccess.put(locationKey, expiryTime);
                    
                    // Also grant access to the other half of the door
                    org.bukkit.block.Block block = blockLocation.getBlock();
                    org.bukkit.block.data.BlockData blockData = block.getBlockData();
                    String otherLocationKey = null;
                    if (blockData instanceof Door door) {
                        Door.Half half = door.getHalf();
                        org.bukkit.block.Block otherHalf;
                        if (half == Door.Half.BOTTOM) {
                            otherHalf = block.getRelative(0, 1, 0);
                        } else {
                            otherHalf = block.getRelative(0, -1, 0);
                        }
                        otherLocationKey = createLocationKey(otherHalf.getLocation());
                        temporaryDoorAccess.put(otherLocationKey, expiryTime);
                    }
                    
                    // Store the other location key for cleanup
                    final String finalOtherLocationKey = otherLocationKey;
                    
                    // Open the door
                    openDoor(blockLocation);
                    // Close after 5 seconds (cleanup task will handle removal)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            closeDoor(blockLocation);
                            // Cleanup task will remove expired entries, but remove immediately for consistency
                            temporaryDoorAccess.remove(locationKey);
                            if (finalOtherLocationKey != null) {
                                temporaryDoorAccess.remove(finalOtherLocationKey);
                            }
                        }
                    }.runTaskLater(plugin, 100L); // 5 seconds = 100 ticks
                } else {
                    // For chests, open the chest inventory
                    Block block = blockLocation.getBlock();
                    if (block.getState() instanceof Chest chest) {
                        temporaryChestAccess.add(player.getUniqueId());
                        org.bukkit.inventory.Inventory chestInv = chest.getInventory();
                        player.openInventory(chestInv);
                        
                        // Remove temporary access after 1 second (enough time to open)
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                temporaryChestAccess.remove(player.getUniqueId());
                            }
                        }.runTaskLater(plugin, 20L);
                    }
                }

                // Clean up
                playerInputs.remove(player.getUniqueId());
                playerBlockLocations.remove(player.getUniqueId());
                playerIsDoor.remove(player.getUniqueId());
            } else {
                // Incorrect passcode
                player.sendMessage(Component.text("Foute pincode").color(NamedTextColor.RED));
                currentInput = "";
                playerInputs.put(player.getUniqueId(), currentInput);
                PasscodeGUI.updateDisplay(inv, currentInput);
            }
        } else if (clicked.getType() == Material.REDSTONE_BLOCK) {
            // Clear button clicked
            currentInput = "";
            playerInputs.put(player.getUniqueId(), currentInput);
            PasscodeGUI.updateDisplay(inv, currentInput);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            playerInputs.remove(player.getUniqueId());
            playerBlockLocations.remove(player.getUniqueId());
            playerIsDoor.remove(player.getUniqueId());
        }
    }

    public void openPasscodeGUI(Player player, Location blockLocation, boolean isDoor) {
        ChestLockManager manager = plugin.getChestLockManager();
        String passcode = isDoor ? manager.getDoorPasscode(blockLocation) : manager.getChestPasscode(blockLocation);
        
        if (passcode == null) {
            return;
        }

        Inventory gui = PasscodeGUI.createPasscodeGUI(blockLocation, passcode);
        player.openInventory(gui);
        playerBlockLocations.put(player.getUniqueId(), blockLocation);
        playerIsDoor.put(player.getUniqueId(), isDoor);
        playerInputs.put(player.getUniqueId(), "");
    }

    public boolean hasTemporaryChestAccess(UUID playerUuid) {
        return temporaryChestAccess.contains(playerUuid);
    }

    public boolean hasTemporaryDoorAccess(Location location) {
        long currentTime = System.currentTimeMillis();
        
        // Check this location
        String locationKey = createLocationKey(location);
        Long expiryTime = temporaryDoorAccess.get(locationKey);
        if (expiryTime != null && currentTime <= expiryTime) {
            return true;
        }
        
        // Check the other half of the door (if it's a door)
        org.bukkit.block.Block block = location.getBlock();
        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        if (blockData instanceof Door door) {
            Door.Half half = door.getHalf();
            org.bukkit.block.Block otherHalf;
            if (half == Door.Half.BOTTOM) {
                otherHalf = block.getRelative(0, 1, 0);
            } else {
                otherHalf = block.getRelative(0, -1, 0);
            }
            
            String otherLocationKey = createLocationKey(otherHalf.getLocation());
            Long otherExpiryTime = temporaryDoorAccess.get(otherLocationKey);
            if (otherExpiryTime != null && currentTime <= otherExpiryTime) {
                return true;
            }
        }
        
        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data when they leave
        UUID playerUuid = event.getPlayer().getUniqueId();
        playerInputs.remove(playerUuid);
        playerBlockLocations.remove(playerUuid);
        playerIsDoor.remove(playerUuid);
        temporaryChestAccess.remove(playerUuid);
    }

    private void openDoor(Location location) {
        org.bukkit.block.Block block = location.getBlock();
        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        
        if (blockData instanceof Door door) {
            door.setOpen(true);
            block.setBlockData(door);
            
            // Also open the other half if it exists
            Door.Half half = door.getHalf();
            if (half == Door.Half.BOTTOM) {
                org.bukkit.block.Block topBlock = block.getRelative(0, 1, 0);
                if (topBlock.getBlockData() instanceof Door topDoor) {
                    topDoor.setOpen(true);
                    topBlock.setBlockData(topDoor);
                }
            } else if (half == Door.Half.TOP) {
                org.bukkit.block.Block bottomBlock = block.getRelative(0, -1, 0);
                if (bottomBlock.getBlockData() instanceof Door bottomDoor) {
                    bottomDoor.setOpen(true);
                    bottomBlock.setBlockData(bottomDoor);
                }
            }
        }
    }

    private void closeDoor(Location location) {
        org.bukkit.block.Block block = location.getBlock();
        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        
        if (blockData instanceof Door door) {
            door.setOpen(false);
            block.setBlockData(door);
            
            // Also close the other half if it exists
            Door.Half half = door.getHalf();
            if (half == Door.Half.BOTTOM) {
                org.bukkit.block.Block topBlock = block.getRelative(0, 1, 0);
                if (topBlock.getBlockData() instanceof Door topDoor) {
                    topDoor.setOpen(false);
                    topBlock.setBlockData(topDoor);
                }
            } else if (half == Door.Half.TOP) {
                org.bukkit.block.Block bottomBlock = block.getRelative(0, -1, 0);
                if (bottomBlock.getBlockData() instanceof Door bottomDoor) {
                    bottomDoor.setOpen(false);
                    bottomBlock.setBlockData(bottomDoor);
                }
            }
        }
    }
}

