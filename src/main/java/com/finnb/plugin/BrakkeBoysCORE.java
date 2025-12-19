package com.finnb.plugin;

import com.finnb.plugin.commands.AddCommand;
import com.finnb.plugin.commands.HelpCommand;
import com.finnb.plugin.commands.HorseAddCommand;
import com.finnb.plugin.commands.HorseLockCommand;
import com.finnb.plugin.commands.LaserCommand;
import com.finnb.plugin.commands.LockCommand;
import com.finnb.plugin.commands.ResetLocksCommand;
import com.finnb.plugin.commands.SetMaxDoorLocksCommand;
import com.finnb.plugin.commands.SetMaxLasersCommand;
import com.finnb.plugin.commands.SetMaxLocksCommand;
import com.finnb.plugin.listeners.ChestProtectionListener;
import com.finnb.plugin.listeners.HorseProtectionListener;
import com.finnb.plugin.listeners.LaserListener;
import com.finnb.plugin.listeners.PasscodeGUIListener;
import com.finnb.plugin.managers.ChestLockManager;
import com.finnb.plugin.managers.HorseLockManager;
import com.finnb.plugin.managers.LaserManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BrakkeBoysCORE extends JavaPlugin {

    private HorseLockManager horseLockManager;
    private ChestLockManager chestLockManager;
    private LaserManager laserManager;
    private PasscodeGUIListener passcodeGUIListener;
    private LaserListener laserListener;

    @Override
    public void onEnable() {
        getLogger().info("BrakkeBoysCORE has been enabled!");

        // Initialize the horse lock manager
        horseLockManager = new HorseLockManager(this);
        horseLockManager.loadData();

        // Initialize the chest lock manager
        chestLockManager = new ChestLockManager(this);
        chestLockManager.loadData();
        chestLockManager.loadConfig();

        // Initialize the laser manager
        laserManager = new LaserManager(this);
        laserManager.loadData();
        laserManager.loadConfig();

        // Register commands
        getCommand("horselock").setExecutor(new HorseLockCommand(this));

        HorseAddCommand horseAddCommand = new HorseAddCommand(this);
        getCommand("horseadd").setExecutor(horseAddCommand);
        getCommand("horseadd").setTabCompleter(horseAddCommand);

        getCommand("lock").setExecutor(new LockCommand(this));
        getCommand("setmaxlocks").setExecutor(new SetMaxLocksCommand(this));
        getCommand("setmaxdoorlocks").setExecutor(new SetMaxDoorLocksCommand(this));
        getCommand("setmaxlasers").setExecutor(new SetMaxLasersCommand(this));

        ResetLocksCommand resetLocksCommand = new ResetLocksCommand(this);
        getCommand("resetlocks").setExecutor(resetLocksCommand);
        getCommand("resetlocks").setTabCompleter(resetLocksCommand);

        AddCommand addCommand = new AddCommand(this);
        getCommand("add").setExecutor(addCommand);
        getCommand("add").setTabCompleter(addCommand);

        getCommand("help").setExecutor(new HelpCommand(this));
        getCommand("bbc").setExecutor(new HelpCommand(this));
        getCommand("brakkeboyscore").setExecutor(new HelpCommand(this));

        getCommand("laser").setExecutor(new LaserCommand(this));
        getCommand("removelaser").setExecutor(new LaserCommand(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new HorseProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(this), this);
        
        laserListener = new LaserListener(this);
        getServer().getPluginManager().registerEvents(laserListener, this);
        
        passcodeGUIListener = new PasscodeGUIListener(this);
        getServer().getPluginManager().registerEvents(passcodeGUIListener, this);
    }

    @Override
    public void onDisable() {
        // Cancel tasks before shutdown
        if (laserListener != null) {
            laserListener.cancelTasks();
        }
        if (passcodeGUIListener != null) {
            passcodeGUIListener.cancelTasks();
        }
        
        // Save data before shutdown
        if (horseLockManager != null) {
            horseLockManager.saveData();
        }
        if (chestLockManager != null) {
            chestLockManager.saveChestData();
            chestLockManager.saveDoorData();
            chestLockManager.saveConfig();
        }
        if (laserManager != null) {
            laserManager.saveData();
            laserManager.saveConfig();
        }
        getLogger().info("BrakkeBoysCORE has been disabled!");
    }

    public HorseLockManager getHorseLockManager() {
        return horseLockManager;
    }

    public ChestLockManager getChestLockManager() {
        return chestLockManager;
    }

    public PasscodeGUIListener getPasscodeGUIListener() {
        return passcodeGUIListener;
    }

    public LaserManager getLaserManager() {
        return laserManager;
    }
}

