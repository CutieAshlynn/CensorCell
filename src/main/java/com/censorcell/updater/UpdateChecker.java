package com.censorcell.updater;

import com.censorcell.CensorCell;
import org.bukkit.Bukkit;
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
    
    public UpdateChecker(JavaPlugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }
    
    @Override
    public void run() {
        try {
            // GitHub API endpoint for the latest release
            URL url = new URL("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                // Read the response into a String.
                String response = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                        .lines().collect(Collectors.joining("\n"));
                // Parse JSON (using org.json)
                JSONObject json = new JSONObject(response);
                String latestVersion = json.getString("tag_name");
                // Compare with current plugin version.
                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    String updateMessage = "A new update is available: " + latestVersion +
                            " (current: " + currentVersion + ")";
                    plugin.getLogger().info(updateMessage);
                    // Broadcast to online players on the main thread.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.broadcastMessage("§a[Update] " + updateMessage);
                    });
                } else {
                    plugin.getLogger().info("Plugin is up to date (version " + currentVersion + ").");
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error checking for updates: " + ex.getMessage());
        }
    }
}
