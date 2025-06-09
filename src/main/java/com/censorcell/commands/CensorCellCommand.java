package com.censorcell.commands;

import com.censorcell.CensorCell;
import com.censorcell.listeners.ChatListener;
import com.censorcell.managers.JailsManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CensorCellCommand implements CommandExecutor, TabCompleter {

    private final CensorCell plugin;
    private final ChatListener chatListener;
    private final JailsManager jailsManager;

    public CensorCellCommand(CensorCell plugin, ChatListener chatListener, JailsManager jailsManager) {
        this.plugin = plugin;
        this.chatListener = chatListener;
        this.jailsManager = jailsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /" + label + " <reload|about|unmute|unjail|addjail|removejail>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            jailsManager.reloadJails();
            sender.sendMessage("§aConfiguration and jails reloaded.");
            return true;
        } else if (sub.equals("about")) {
            String version = plugin.getDescription().getVersion();
            String author = String.join(", ", plugin.getDescription().getAuthors());
            String desc = plugin.getDescription().getDescription();
            sender.sendMessage("§aCensorCell v" + version + " by " + author);
            sender.sendMessage("§a" + desc);
            return true;
        } else if (sub.equals("unmute")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /" + label + " unmute <player>");
                return true;
            }
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            chatListener.unmutePlayer(target);
            sender.sendMessage("§a" + target.getName() + " has been unmuted.");
            return true;
        } else if (sub.equals("addjail")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /" + label + " addjail <jailName>");
                return true;
            }
            Player admin = (Player) sender;
            Location loc = admin.getLocation();
            String jailName = args[1];
            jailsManager.addJail(jailName, loc);
            sender.sendMessage("§aJail '" + jailName + "' added at your current location.");
            return true;
        } else if (sub.equals("removejail")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /" + label + " removejail <jailName>");
                return true;
            }
            String jailName = args[1];
            if (jailsManager.removeJail(jailName)) {
                sender.sendMessage("§aJail '" + jailName + "' has been removed.");
            } else {
                sender.sendMessage("§cJail '" + jailName + "' does not exist.");
            }
            return true;
        } else if (sub.equals("unjail")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /" + label + " unjail <player>");
                return true;
            }
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            // This unjail command does NOT teleport the player.
            // It simply sets them back to Survival mode.
            target.setGameMode(GameMode.SURVIVAL);
            // Retrieve unjail message from config (with a default if missing)
            String unjailMessage = plugin.getPluginConfig().getString(
                    "messages.unjail",
                    "You have been released from jail. Welcome back to Survival!"
            );
            sender.sendMessage("§a" + target.getName() + " has been unjailed (set to Survival mode).");
            target.sendMessage(unjailMessage);
            return true;
        }
        sender.sendMessage("§cUnknown subcommand. Available: reload, about, unmute, unjail, addjail, removejail.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // If the admin is typing the subcommand.
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            Collections.addAll(subs, "reload", "about", "unmute", "unjail", "addjail", "removejail");
            String current = args[0].toLowerCase();
            return subs.stream()
                    .filter(s -> s.startsWith(current))
                    .collect(Collectors.toList());
        }
        // If the admin is typing the second argument.
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("unmute") || sub.equals("unjail")) {
                List<String> names = new ArrayList<>();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(p.getName());
                    }
                }
                return names;
            } else if (sub.equals("removejail")) {
                // Return a list of jail names directly from the jailsManager from jails.yml
                Set<String> jailNames = jailsManager.getJails().keySet();
                return jailNames.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
