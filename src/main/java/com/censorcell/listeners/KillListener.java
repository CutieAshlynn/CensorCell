package com.censorcell.listeners;

import com.censorcell.CensorCell;
import com.censorcell.managers.JailsManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class KillListener implements Listener {

    private final CensorCell plugin;
    private final JailsManager jailsManager;

    public KillListener(CensorCell plugin, JailsManager jailsManager) {
        this.plugin = plugin;
        this.jailsManager = jailsManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Ensure the victim is a player.
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        // Admin immunity: if killer has censorcell.admin permission, skip auto-jail.
        if (killer.hasPermission("censorcell.admin")) {
            return;
        }

        // Check the auto-jail-on-kill setting in the config.
        // If it is false, do not perform any jail logic.
        if (!plugin.getPluginConfig().getBoolean("auto-jail-on-kill", true)) {
            return;
        }

        // Get the killer's location and find the nearest jail.
        Location killerLocation = killer.getLocation();
        Location nearestJail = jailsManager.getClosestJail(killerLocation);
        if (nearestJail != null) {
            killer.teleport(nearestJail);
            // Set the player's gamemode to Adventure to indicate they're jailed.
            killer.setGameMode(GameMode.ADVENTURE);
            // Retrieve the jail message from the config and send it.
            String jailMessage = plugin.getPluginConfig().getString(
                    "messages.jail",
                    "§cYou have been jailed for killing a player! Please wait for an admin to release you."
            );
            killer.sendMessage(jailMessage);
        } else {
            killer.sendMessage("§cNo jail location found! Please contact an admin.");
        }
    }
}
