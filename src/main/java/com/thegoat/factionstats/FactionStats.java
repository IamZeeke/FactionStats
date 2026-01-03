package me.yourname.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FactionStats extends JavaPlugin implements Listener {

    // Player kills/deaths
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    // Simple faction system
    private final Map<String, Set<UUID>> factions = new HashMap<>();  // FactionName -> PlayerUUIDs
    private final Map<String, Integer> factionKills = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FactionStats enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("FactionStats disabled");
    }

    // Handle kills and deaths
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        deaths.put(dead.getUniqueId(), deaths.getOrDefault(dead.getUniqueId(), 0) + 1);

        if (killer != null) {
            kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);

            // Add to faction kills
            String faction = getFaction(killer);
            if (faction != null) {
                factionKills.put(faction, factionKills.getOrDefault(faction, 0) + 1);
            }
        }
    }

    // Join or create a faction
    private String getFaction(Player player) {
        for (Map.Entry<String, Set<UUID>> entry : factions.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) return entry.getKey();
        }
        return null; // No faction yet
    }

    private void joinFaction(Player player, String factionName) {
        factions.computeIfAbsent(factionName, k -> new HashSet<>()).add(player.getUniqueId());
    }

    // Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            // /fstats → show personal stats
            int k = kills.getOrDefault(player.getUniqueId(), 0);
            int d = deaths.getOrDefault(player.getUniqueId(), 0);

            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Your Stats");
            player.sendMessage(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + k);
            player.sendMessage(ChatColor.GRAY + "Deaths: " + ChatColor.RED + d);
            player.sendMessage(ChatColor.GRAY + "Faction: " + ChatColor.AQUA + Optional.ofNullable(getFaction(player)).orElse("None"));
            return true;
        }

        if (args.length == 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("top")) {
                showTopFactions(player);
                return true;
            }
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String factionName = args[1];
            if (sub.equals("join")) {
                joinFaction(player, factionName);
                player.sendMessage(ChatColor.AQUA + "You joined faction: " + factionName);
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Usage: /fstats, /fstats top, /fstats join <faction>");
        return true;
    }

    // Top 5 factions animated scoreboard
    private void showTopFactions(Player player) {
        BukkitRunnable task = new BukkitRunnable() {

            int tick = 0;
            final String[] animation = {ChatColor.RED + "⚔", ChatColor.GOLD + "⚔", ChatColor.GREEN + "⚔", ChatColor.AQUA + "⚔"};

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                tick++;
                player.sendTitle(
                        animation[tick % animation.length] + ChatColor.BOLD + " Top Factions " + animation[(tick + 1) % animation.length],
                        getTopFactionsText(),
                        5, 20, 5
                );
            }
        };
        task.runTaskTimer(this, 0, 20); // updates every second
    }

    private String getTopFactionsText() {
        StringBuilder sb = new StringBuilder();
        factionKills.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(entry -> sb.append(ChatColor.YELLOW).append(entry.getKey())
                        .append(ChatColor.GRAY).append(" - Kills: ")
                        .append(ChatColor.GREEN).append(entry.getValue())
                        .append("\n"));
        return sb.toString();
    }
}
