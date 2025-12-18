package com.finnb.plugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PasscodeGUI {

    private static final String GUI_TITLE = "Pincode Invoeren";

    public static Inventory createPasscodeGUI(Location blockLocation, String correctPasscode) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE));

        // Number buttons (0-9) - using player heads
        // Layout: 1-7 in slots 10-16, 8-9-0 in slots 20-22
        int[] numberSlots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0}; // Numbers corresponding to slots
        String[] numberTextures = {
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 1
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 2
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 3
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 4
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 5
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 6
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 7
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 8
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ==", // 9
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE3YjE0YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5YjQ5In19fQ=="  // 0
        };

        // Create number buttons using player heads
        for (int i = 0; i < 10; i++) {
            ItemStack numberItem = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) numberItem.getItemMeta();
            if (skullMeta != null) {
                skullMeta.displayName(Component.text(String.valueOf(numbers[i])).color(NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                skullMeta.lore(Arrays.asList(
                    Component.text("Klik om " + numbers[i] + " toe te voegen").color(NamedTextColor.GRAY)
                ));
                // Store the number in custom model data for easy retrieval
                skullMeta.setCustomModelData(numbers[i]);
                numberItem.setItemMeta(skullMeta);
            }
            inv.setItem(numberSlots[i], numberItem);
        }

        // Display slot (shows current input) - slot 4
        ItemStack displayItem = new ItemStack(Material.PAPER);
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            displayMeta.displayName(Component.text("Pincode: ").color(NamedTextColor.YELLOW));
            displayMeta.lore(Arrays.asList(
                Component.text("Voer je pincode in").color(NamedTextColor.GRAY)
            ));
            displayItem.setItemMeta(displayMeta);
        }
        inv.setItem(4, displayItem);

        // Submit button - slot 24 (bottom center)
        ItemStack submitItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta submitMeta = submitItem.getItemMeta();
        if (submitMeta != null) {
            submitMeta.displayName(Component.text("✓ Bevestigen").color(NamedTextColor.GREEN));
            submitMeta.lore(Arrays.asList(
                Component.text("Klik om pincode te controleren").color(NamedTextColor.GRAY)
            ));
            submitItem.setItemMeta(submitMeta);
        }
        inv.setItem(24, submitItem);

        // Clear button - slot 18 (left of submit)
        ItemStack clearItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta clearMeta = clearItem.getItemMeta();
        if (clearMeta != null) {
            clearMeta.displayName(Component.text("✗ Wissen").color(NamedTextColor.RED));
            clearMeta.lore(Arrays.asList(
                Component.text("Klik om pincode te wissen").color(NamedTextColor.GRAY)
            ));
            clearItem.setItemMeta(clearMeta);
        }
        inv.setItem(18, clearItem);

        return inv;
    }

    public static void updateDisplay(Inventory inv, String currentInput) {
        ItemStack displayItem = inv.getItem(4);
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
        Component title = inv.getViewers().isEmpty() ? null : null;
        // Check by title component
        try {
            String titleStr = inv.getViewers().isEmpty() ? "" : "";
            // We'll check by the presence of our custom items instead
            ItemStack display = inv.getItem(4);
            return display != null && display.getType() == Material.PAPER;
        } catch (Exception e) {
            return false;
        }
    }
}

