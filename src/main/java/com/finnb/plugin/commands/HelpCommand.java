package com.finnb.plugin.commands;

import com.finnb.plugin.BrakkeBoysCORE;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HelpCommand implements CommandExecutor {

    private final BrakkeBoysCORE plugin;

    public HelpCommand(BrakkeBoysCORE plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("BrakkeBoysCORE Commands")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());

        // Player commands
        sender.sendMessage(Component.text("Paard Commando's:")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(formatCommand("/horselock", "Vergrendel of ontgrendel je paard", "/hl, /lockh"));
        sender.sendMessage(formatCommand("/horseadd <speler>", "Geef een speler toegang tot je paard", "/ha, /addh"));
        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text("Kist & Deur Commando's:")
                .color(NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(formatCommand("/lock", "Vergrendel of ontgrendel een kist/deur"));
        sender.sendMessage(formatCommand("/add <speler>", "Geef een speler toegang tot je kist/deur"));
        sender.sendMessage(Component.empty());

        // Admin commands (only for operators)
        if (sender.isOp() || sender.hasPermission("brakkeboyscore.admin")) {
            sender.sendMessage(Component.text("Admin Commando's:")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));
            sender.sendMessage(formatCommand("/setmaxlocks <speler> <aantal>", "Stel max kisten in voor een speler"));
            sender.sendMessage(formatCommand("/setmaxdoorlocks <speler> <aantal>", "Stel max deuren in voor een speler"));
            sender.sendMessage(formatCommand("/resetlocks <speler>", "Reset alle sloten van een speler"));
            sender.sendMessage(Component.empty());
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .color(NamedTextColor.GRAY));

        return true;
    }

    private Component formatCommand(String command, String description) {
        return formatCommand(command, description, null);
    }

    private Component formatCommand(String command, String description, String aliases) {
        Component cmd = Component.text(command)
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
        
        Component desc = Component.text(" - " + description)
                .color(NamedTextColor.WHITE);
        
        Component result = cmd.append(desc);
        
        if (aliases != null && !aliases.isEmpty()) {
            Component alias = Component.text(" (" + aliases + ")")
                    .color(NamedTextColor.GRAY);
            result = result.append(alias);
        }
        
        return result;
    }
}

