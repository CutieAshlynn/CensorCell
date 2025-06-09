package com.censorcell;

import com.censorcell.commands.CensorCellCommand;
import com.censorcell.listeners.ChatListener;
import com.censorcell.listeners.KillListener;
import com.censorcell.managers.JailsManager;
import com.censorcell.updater.UpdateChecker;
import com.censorcell.updater.UpdateListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;

public class CensorCell extends JavaPlugin {

    private JailsManager jailsManager;
    // Store the update notification message (if an update is available)
    private String updateNotification = null;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist.
        saveDefaultConfig();
        // Merge missing default config keys into the existing config file.
        updateConfig();

        // Only save jails.yml if it does not exist – leave existing jail definitions untouched.
        File jailsFile = new File(getDataFolder(), "jails.yml");
        if (!jailsFile.exists()) {
            saveResource("jails.yml", false);
        }

        // Create and register ChatListener.
        ChatListener chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Initialize JailsManager.
        jailsManager = new JailsManager(getDataFolder());
        // Register KillListener with JailsManager.
        getServer().getPluginManager().registerEvents(new KillListener(this, jailsManager), this);

        // Register the consolidated command executor (with tab completion).
        CensorCellCommand mainCmd = new CensorCellCommand(this, chatListener, jailsManager);
        getCommand("censorcell").setExecutor(mainCmd);
        getCommand("censorcell").setTabCompleter(mainCmd);

        // Register the update notifier listener.
        // This listener will send update notifications to admins when they join.
        getServer().getPluginManager().registerEvents(new UpdateListener(this), this);

        getLogger().info("CensorCell is active!");

        // Schedule the update checker asynchronously.
        // Replace "YourGitHubUsername" and "YourRepoName" with your GitHub repository details.
        getServer().getScheduler().runTaskAsynchronously(this,
                new UpdateChecker(this, "CutieAshlynn", "CensorCell"));
    }

    @Override
    public void onDisable() {
        getLogger().info("CensorCell shutting down.");
    }

    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    /**
     * Updates config.yml by merging in missing keys from the default config (bundled in the jar)
     * without overwriting existing admin-set values.
     */
    public void updateConfig() {
        // Load the default configuration from inside the jar.
        InputStreamReader defConfigStream = new InputStreamReader(getResource("config.yml"));
        if (defConfigStream == null) {
            getLogger().warning("Default config.yml not found inside jar!");
            return;
        }
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        // Set the default values for the current config.
        FileConfiguration config = getConfig();
        config.setDefaults(defConfig);
        // This will copy missing values from the default config into the current config.
        config.options().copyDefaults(true);
        saveConfig();
        getLogger().info("Config file has been merged with defaults.");
    }
    
    // --- Update Notification Methods ---
    
    /**
     * Sets the update notification message.
     *
     * @param message the update message to notify admins with.
     */
    public void setUpdateNotification(String message) {
        this.updateNotification = message;
    }
    
    /**
     * Retrieves the update notification message.
     *
     * @return the update notification message, or null if none is set.
     */
    public String getUpdateNotification() {
        return updateNotification;
    }
}
