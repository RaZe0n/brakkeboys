package com.finnb.plugin.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

public class PasscodeGUI {

    private static final String GUI_TITLE = "Pincode Invoeren";
    
    // Base64 textures for number heads (0-9)
    // Note: These are placeholder textures. For production, use actual number head textures
    // You can find number head textures at: https://minecraft-heads.com/custom/heads/numbers
    private static final String[] NUMBER_TEXTURES = {
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 0
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 1
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 2
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 3
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 4
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 5
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 6
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 7
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 8
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ=="  // 9
    };

    public static Inventory createPasscodeGUI(Location blockLocation, String correctPasscode) {
        Inventory inv = Bukkit.createInventory(null, 36, Component.text(GUI_TITLE));

        // Layout requested:
        //   1 2 3
        //   4 5 6
        //   7 8 9
        //     0
        //
        // 36 slots = 4 rows of 9:
        // Row 1 (slots 0-8): Display (0), empty, empty, 1 (3), 2 (4), 3 (5), empty, empty, Submit (8)
        // Row 2 (slots 9-17): empty, empty, empty, 4 (12), 5 (13), 6 (14), empty, empty, empty
        // Row 3 (slots 18-26): empty, empty, empty, 7 (21), 8 (22), 9 (23), empty, empty, empty
        // Row 4 (slots 27-35): empty, empty, empty, empty, 0 (31), empty, empty, empty, Clear (35)
        //
        // Final layout:
        // Row 1: 1(3), 2(4), 3(5)
        // Row 2: 4(12), 5(13), 6(14)
        // Row 3: 7(21), 8(22), 9(23)
        // Row 4: 0(31) - centered below 8
        
        int[] numberSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23, 31};
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
        
        // Create number buttons using player heads with proper textures
        for (int i = 0; i < 10; i++) {
            ItemStack numberItem = createNumberHead(numbers[i], NUMBER_TEXTURES[numbers[i]]);
            inv.setItem(numberSlots[i], numberItem);
        }

        // Display slot (shows current input) - slot 0 (top left)
        ItemStack displayItem = new ItemStack(Material.PAPER);
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            displayMeta.displayName(Component.text("Pincode: ").color(NamedTextColor.YELLOW));
            displayMeta.lore(Arrays.asList(
                Component.text("Voer je pincode in").color(NamedTextColor.GRAY)
            ));
            displayItem.setItemMeta(displayMeta);
        }
        inv.setItem(0, displayItem);

        // Submit button - slot 8 (top right)
        ItemStack submitItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta submitMeta = submitItem.getItemMeta();
        if (submitMeta != null) {
            submitMeta.displayName(Component.text("✓ Bevestigen").color(NamedTextColor.GREEN));
            submitMeta.lore(Arrays.asList(
                Component.text("Klik om pincode te controleren").color(NamedTextColor.GRAY)
            ));
            submitItem.setItemMeta(submitMeta);
        }
        inv.setItem(8, submitItem);

        // Clear button - slot 35 (bottom right)
        ItemStack clearItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta clearMeta = clearItem.getItemMeta();
        if (clearMeta != null) {
            clearMeta.displayName(Component.text("✗ Wissen").color(NamedTextColor.RED));
            clearMeta.lore(Arrays.asList(
                Component.text("Klik om pincode te wissen").color(NamedTextColor.GRAY)
            ));
            clearItem.setItemMeta(clearMeta);
        }
        inv.setItem(35, clearItem);

        return inv;
    }

    private static ItemStack createNumberHead(int number, String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Set the texture using reflection
            // GameProfile requires a non-null name, so we use a dummy name
            GameProfile profile = new GameProfile(UUID.randomUUID(), "NumberHead" + number);
            
            // Use reflection to set properties to avoid version compatibility issues
            // Try multiple approaches to handle different authlib versions
            boolean textureSet = false;
            
            // Approach 1: Try direct field access (works in most versions)
            try {
                Field propertiesField = profile.getClass().getDeclaredField("properties");
                propertiesField.setAccessible(true);
                Object properties = propertiesField.get(profile);
                if (properties != null) {
                    properties.getClass().getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", new Property("textures", texture));
                    textureSet = true;
                }
            } catch (Exception e) {
                // Field access failed, try method access
            }
            
            // Approach 2: Try getProperties() method (if field access didn't work)
            if (!textureSet) {
                try {
                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    if (properties != null) {
                        properties.getClass().getMethod("put", Object.class, Object.class)
                            .invoke(properties, "textures", new Property("textures", texture));
                        textureSet = true;
                    }
                } catch (NoSuchMethodError | Exception e) {
                    // Method doesn't exist at runtime or other error - texture won't be set
                    // Display name will still work
                }
            }
            
            try {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (NoSuchFieldException e) {
                // Try alternative field names for different Paper versions
                try {
                    Field profileField = meta.getClass().getDeclaredField("serializedProfile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                } catch (Exception ex) {
                    // If reflection fails, texture won't be set but display name will work
                }
            } catch (Exception e) {
                // If reflection fails, texture won't be set but display name will work
            }
            
            meta.displayName(Component.text(String.valueOf(number)).color(NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(Arrays.asList(
                Component.text("Klik om " + number + " toe te voegen").color(NamedTextColor.GRAY)
            ));
            // Store the number in custom model data for easy retrieval
            meta.setCustomModelData(number);
            head.setItemMeta(meta);
        }
        return head;
    }

    public static void updateDisplay(Inventory inv, String currentInput) {
        ItemStack displayItem = inv.getItem(0);
        if (displayItem != null && displayItem.getType() == Material.PAPER) {
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                String display = currentInput.isEmpty() ? "..." : "*".repeat(currentInput.length());
                meta.displayName(Component.text("Pincode: " + display).color(NamedTextColor.YELLOW));
                displayItem.setItemMeta(meta);
            }
        }
    }

    public static boolean isPasscodeGUI(Inventory inv) {
        if (inv == null) return false;
        ItemStack display = inv.getItem(0);
        return display != null && display.getType() == Material.PAPER;
    }
}
