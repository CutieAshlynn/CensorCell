package com.censorcell.updater;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker implements Runnable {
    private final JavaPlugin plugin;
    private final String repoOwner;
    private final String repoName;
    private final String repoLink; // GitHub releases link

    public UpdateChecker(JavaPlugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.repoLink = "https://github.com/" + repoOwner + "/" + repoName + "/releases";
    }
    
    @Override
    public void run() {
        try {
            // GitHub API endpoint for the latest official release.
            URL url = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                String response = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                        .lines().collect(Collectors.joining("\n"));
                
                JSONObject json = new JSONObject(response);
                String latestVersion = json.getString("tag_name");
                
                // Retrieve the current version from plugin.yml.
                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    String updateMessage = "A new update is available: " + latestVersion +
                            " (current: " + currentVersion + "). Download at: " + repoLink;
                    
                    // Log update to console.
                    plugin.getLogger().info(updateMessage);
                    
                    // On the main thread, store the update message in the plugin so that UpdateListener will use it.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (plugin instanceof com.censorcell.CensorCell) {
                            com.censorcell.CensorCell censorCellPlugin = (com.censorcell.CensorCell) plugin;
                            censorCellPlugin.setUpdateNotification(ChatColor.GREEN + "[CensorCell Update] " +
                                    ChatColor.RED + updateMessage);
                        }
                        
                        // Optional: If admins are already online, notify them immediately.
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            if (player.isOp() || player.hasPermission("censorcell.admin")) {
                                player.sendMessage(ChatColor.GREEN + "[CensorCell Update] " +
                                        ChatColor.RED + updateMessage);
                            }
                        });
                    });
                } else {
                    plugin.getLogger().info("Plugin is up to date (version " + currentVersion + ").");
                }
            } else {
                plugin.getLogger().warning("UpdateChecker: Received HTTP response code " 
                        + connection.getResponseCode());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error checking for updates: " + ex.getMessage());
        }
    }
}
