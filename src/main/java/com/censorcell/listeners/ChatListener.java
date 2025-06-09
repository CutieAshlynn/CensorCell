package com.censorcell.listeners;

import com.censorcell.CensorCell;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {
    private final CensorCell plugin;
    
    // Map to track muted players (only cares if they are muted).
    private final ConcurrentHashMap<UUID, Boolean> mutedPlayers = new ConcurrentHashMap<>();
    
    // Map to track spam data for each player.
    private final ConcurrentHashMap<UUID, SpamData> spamDataMap = new ConcurrentHashMap<>();

    public ChatListener(CensorCell plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inner class used to store spam tracking info.
     */
    private static class SpamData {
        String lastMessage;
        int count;
        long firstMessageTime; // in milliseconds
        
        SpamData(String message, long now) {
            this.lastMessage = message;
            this.count = 1;
            this.firstMessageTime = now;
        }
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        long now = System.currentTimeMillis();
        
        // Admins are immune from spam, banned words, mute, and kick.
        if (player.hasPermission("censorcell.admin")) {
            return;
        }
        
        // ----- Spam Check -----
        int cooldownSeconds = plugin.getPluginConfig().getInt("spam.cooldown", 300);  // default: 300 sec (5 min)
        int maxRepeats = plugin.getPluginConfig().getInt("spam.max", 3);              // default: 3 allowed repeats
        String spamKickMessage = plugin.getPluginConfig().getString("spam.kick-message", "You were kicked for spamming.");
        
        // Retrieve current spam data for the player, or create new if missing.
        SpamData spamData = spamDataMap.get(player.getUniqueId());
        if (spamData == null) {
            spamData = new SpamData(message, now);
            spamDataMap.put(player.getUniqueId(), spamData);
        } else {
            // If the current message (ignoring case) matches the last message:
            if (spamData.lastMessage.equalsIgnoreCase(message)) {
                // If within cooldown, increment count; otherwise, reset tracking.
                if (now - spamData.firstMessageTime <= cooldownSeconds * 1000L) {
                    spamData.count++;
                } else {
                    spamData.count = 1;
                    spamData.firstMessageTime = now;
                }
            } else {
                // New message; reset spam tracking.
                spamData.lastMessage = message;
                spamData.count = 1;
                spamData.firstMessageTime = now;
            }
        }
        
        if (spamData.count > maxRepeats) {
            // Kick the player for spamming.
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(spamKickMessage));
            event.setCancelled(true);
            return;
        }
        // ----- End Spam Check -----
        
        // ----- Banned Words Check -----
        // Transform the message to lower-case for simplicity.
        String lowMessage = message.toLowerCase();
        List<String> bannedWords = plugin.getPluginConfig().getStringList("banned-words");
        for (String word : bannedWords) {
            if (lowMessage.contains(word.toLowerCase())) {
                event.setCancelled(true);
                
                // If the player is already muted, simply notify and exit.
                if (mutedPlayers.containsKey(player.getUniqueId())) {
                    player.sendMessage("§cYou are still muted.");
                    return;
                }
                
                int muteMinutes = plugin.getPluginConfig().getInt("mute-duration", 10);
                mutedPlayers.put(player.getUniqueId(), true);
                
                // Retrieve and send the mute message.
                String muteMsg = plugin.getPluginConfig().getString("messages.mute",
                        "§cYou have been muted for {minutes} minutes due to inappropriate language!")
                        .replace("{minutes}", String.valueOf(muteMinutes));
                player.sendMessage(muteMsg);
                
                // If kick-on-mute is enabled, schedule an immediate kick.
                if (plugin.getPluginConfig().getBoolean("kick-on-mute", true)) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.kickPlayer(plugin.getPluginConfig().getString("messages.kick",
                                "§cYou were kicked for violating chat rules!"));
                    }, 1L);
                }
                
                // Schedule unmute task after the configured mute duration.
                long unmuteDelayTicks = muteMinutes * 60L * 20L;  // convert minutes to ticks
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    mutedPlayers.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendMessage(plugin.getPluginConfig().getString("messages.unmute",
                                "§aYour mute has been removed by an admin."));
                    }
                }, unmuteDelayTicks);
                
                return;
            }
        }
        // ----- End Banned Words Check -----
        
        // If the player is muted, block any chat messages.
        if (mutedPlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cYou are muted and cannot chat!");
        }
    }
    
    /**
     * Called by the /unmute command to manually remove a player's mute status.
     */
    public void unmutePlayer(Player player) {
        mutedPlayers.remove(player.getUniqueId());
        player.sendMessage(plugin.getPluginConfig().getString("messages.unmute",
                "§aYour mute has been removed by an admin."));
    }
}
