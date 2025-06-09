package com.censorcell.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JailsManager {
    private final File jailFile;
    private final FileConfiguration jailConfig;
    private final Map<String, Location> jails;

    public JailsManager(File dataFolder) {
        // Ensure the plugin data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // The jails.yml file will reside in the plugin’s data folder.
        jailFile = new File(dataFolder, "jails.yml");
        if (!jailFile.exists()) {
            try {
                jailFile.createNewFile();
                // Create a default "jails" section
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(jailFile);
                defaultConfig.createSection("jails");
                defaultConfig.save(jailFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        jailConfig = YamlConfiguration.loadConfiguration(jailFile);
        jails = new HashMap<>();
        loadJails();
    }

    /**
     * Loads jail locations from jails.yml.
     * Expected format: 
     * jails:
     *   JailName:
     *     world: world
     *     x: 100.5
     *     y: 64.0
     *     z: 200.5
     *     yaw: 0    # optional
     *     pitch: 0  # optional
     */
    private void loadJails() {
        jails.clear();
        if (jailConfig.contains("jails")) {
            for (String key : jailConfig.getConfigurationSection("jails").getKeys(false)) {
                String path = "jails." + key;
                if (jailConfig.contains(path + ".world") && jailConfig.contains(path + ".x")) {
                    String world = jailConfig.getString(path + ".world");
                    double x = jailConfig.getDouble(path + ".x");
                    double y = jailConfig.getDouble(path + ".y");
                    double z = jailConfig.getDouble(path + ".z");
                    float yaw = (float) jailConfig.getDouble(path + ".yaw", 0);
                    float pitch = (float) jailConfig.getDouble(path + ".pitch", 0);
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                    jails.put(key, loc);
                }
            }
        }
    }

    /**
     * Returns a map of all jail names to their locations.
     */
    public Map<String, Location> getJails() {
        return jails;
    }

    /**
     * Returns the jail location closest to the given location. Only jails in the same world are considered.
     */
    public Location getClosestJail(Location playerLocation) {
        Location closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Location jailLoc : jails.values()) {
            if (!jailLoc.getWorld().equals(playerLocation.getWorld())) continue;
            double distance = playerLocation.distance(jailLoc);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = jailLoc;
            }
        }
        return closest;
    }

    /**
     * Adds a jail with the given name and location.
     * Updates the in-memory map and saves changes to jails.yml.
     */
    public void addJail(String name, Location location) {
        jails.put(name, location);
        String path = "jails." + name;
        jailConfig.set(path + ".world", location.getWorld().getName());
        jailConfig.set(path + ".x", location.getX());
        jailConfig.set(path + ".y", location.getY());
        jailConfig.set(path + ".z", location.getZ());
        jailConfig.set(path + ".yaw", location.getYaw());
        jailConfig.set(path + ".pitch", location.getPitch());
        saveJails();
    }

    /**
     * Removes the jail with the given name.
     * Returns true if successfully removed.
     */
    public boolean removeJail(String name) {
        if (jails.containsKey(name)) {
            jails.remove(name);
            jailConfig.set("jails." + name, null);
            saveJails();
            return true;
        }
        return false;
    }

    /**
     * Saves updates to jails.yml.
     */
    private void saveJails() {
        try {
            jailConfig.save(jailFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Reloads jails from file, updating the in-memory map.
     */
    public void reloadJails() {
        try {
            jailConfig.load(jailFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadJails();
    }
}
