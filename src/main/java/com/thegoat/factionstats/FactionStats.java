package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Scoreboard;
import org.bukkit.ScoreboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.*;

public class FactionStats extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("fstats")).setExecutor(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            initPlayer(player);
            createScoreboard(player);
        }

        getLogger().info("FactionStats ENABLED");
    }

    @Override
    public void onDisable() {
        getLogger().info("FactionStats DISABLED");
    }

    /* =========================
       EVENTS
       ========================= */

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        initPlayer(player);
        createScoreboard(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        deaths.put(dead.getUniqueId(), deaths.get(dead.getUniqueId()) + 1);

        if (killer != null) {
            kills.put(killer.getUniqueId(), kills.get(killer.getUniqueId()) + 1);
            updateScoreboard(killer);
        }

        updateScoreboard(dead);
    }

    /* =========================
       COMMAND
       ========================= */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        int k = kills.get(player.getUniqueId());
        int d = deaths.get(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "===== Your Stats =====");
        player.sendMessage(ChatColor.GREEN + "Kills: " + ChatColor.WHITE + k);
        player.sendMessage(ChatColor.RED + "Deaths: " + ChatColor.WHITE + d);

        return true;
    }

    /* =========================
       SCOREBOARD
       ========================= */

    private void createScoreboard(Player player) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("fstats", "dummy", ChatColor.RED + "FactionStats");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore(ChatColor.GREEN + "Kills:").setScore(3);
        obj.getScore(ChatColor.RED + "Deaths:").setScore(2);
        obj.getScore(ChatColor.GRAY + " ").setScore(1);

        player.setScoreboard(board);
        updateScoreboard(player);
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("fstats");

        if (obj == null) return;

        int k = kills.get(player.getUniqueId());
        int d = deaths.get(player.getUniqueId());

        board.resetScores(ChatColor.GREEN + "Kills:");
        board.resetScores(ChatColor.RED + "Deaths:");

        obj.getScore(ChatColor.GREEN + "Kills: " + ChatColor.WHITE + k).setScore(3);
        obj.getScore(ChatColor.RED + "Deaths: " + ChatColor.WHITE + d).setScore(2);
    }

    /* =========================
       UTILS
       ========================= */

    private void initPlayer(Player player) {
        kills.putIfAbsent(player.getUniqueId(), 0);
        deaths.putIfAbsent(player.getUniqueId(), 0);
    }
}

