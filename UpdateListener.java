package com.censorcell.updater;

import com.censorcell.CensorCell;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class UpdateListener implements Listener {
    private final CensorCell plugin;
    
    public UpdateListener(CensorCell plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Only notify admins (or ops)
        if (player.isOp() || player.hasPermission("censorcell.admin")) {
            String updateMsg = plugin.getUpdateNotification();
            if (updateMsg != null && !updateMsg.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + updateMsg);
            }
        }
    }
}
