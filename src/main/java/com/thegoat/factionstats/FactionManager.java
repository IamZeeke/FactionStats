package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class FactionManager implements CommandExecutor {

    private final FactionStats plugin;
    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFaction = new HashMap<>();

    public FactionManager(FactionStats plugin) {
        this.plugin = plugin;
        loadAll(); // Load factions from config
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        if (args.length == 0) {
            p.sendMessage(ChatColor.AQUA + "Faction Commands: /f create, /f join, /f leave, /f stats");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { p.sendMessage("Usage: /f create <name>"); return true; }
                String name = args[1];
                if (playerFaction.containsKey(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "You are already in a faction!");
                    return true;
                }
                if (factions.containsKey(name)) {
                    p.sendMessage(ChatColor.RED + "Faction already exists!");
                    return true;
                }
                Faction f = new Faction(name, p.getUniqueId());
                factions.put(name, f);
                playerFaction.put(p.getUniqueId(), name);
                p.sendMessage(ChatColor.GREEN + "Faction " + name + " created! You are the owner.");
            }
            case "join" -> {
                if (args.length < 2) { p.sendMessage("Usage: /f join <name>"); return true; }
                String name = args[1];
                Faction f = factions.get(name);
                if (f == null) { p.sendMessage(ChatColor.RED + "Faction does not exist."); return true; }
                if (playerFaction.containsKey(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "You are already in a faction!");
                    return true;
                }
                f.addPending(p.getUniqueId());
                p.sendMessage(ChatColor.YELLOW + "Request sent to join " + name + "!");
                Player owner = Bukkit.getPlayer(f.getOwner());
                if (owner != null) owner.sendMessage(ChatColor.AQUA + p.getName() + " wants to join your faction! Use /f accept <player>");
            }
            case "leave" -> {
                String fname = playerFaction.get(p.getUniqueId());
                if (fname == null) { p.sendMessage(ChatColor.RED + "You are not in a faction."); return true; }
                Faction f = factions.get(fname);
                f.removeMember(p.getUniqueId());
                playerFaction.remove(p.getUniqueId());
                p.sendMessage(ChatColor.GREEN + "You left faction " + fname);
            }
            case "stats" -> {
                String fname = playerFaction.get(p.getUniqueId());
                if (fname == null) { p.sendMessage(ChatColor.RED + "You are not in a faction."); return true; }
                Faction f = factions.get(fname);
                p.sendMessage(ChatColor.GOLD + "Faction: " + f.getName() + " | Points: " + f.getPoints() + " | Members: " + f.getMembers().size());
            }
        }
        return true;
    }

    // Add loading/saving from config.yml
    public void saveAll() {
        factions.forEach((name, f) -> f.save(plugin.getConfig()));
        plugin.saveConfig();
    }

    public void loadAll() {
        // implement loading logic from config.yml
    }

    public Map<String, Faction> getFactions() { return factions; }
    public Map<UUID, String> getPlayerFaction() { return playerFaction; }
}
