package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FactionStats extends JavaPlugin implements Listener {

    private Map<UUID, String> playerFaction = new HashMap<>();
    private Map<String, ChatColor> factionColors = new HashMap<>();
    private Map<String, Set<UUID>> factionMembers = new HashMap<>();
    private Map<String, Set<UUID>> factionJoinRequests = new HashMap<>();
    private Map<UUID, Integer> kills = new HashMap<>();
    private Map<UUID, Integer> deaths = new HashMap<>();
    private int animationTick = 0;

    private File file;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        // Setup saving file
        file = new File(getDataFolder(), "factions.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadFactions();

        // Start scoreboard animation
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            animationTick++;
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateScoreboard(p);
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        saveFactions();
    }

    /* =========================
       EVENTS
       ========================= */

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        kills.putIfAbsent(uuid, 0);
        deaths.putIfAbsent(uuid, 0);
        createScoreboard(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        deaths.put(dead.getUniqueId(), deaths.getOrDefault(dead.getUniqueId(), 0) + 1);

        Player killer = dead.getKiller();
        if (killer != null) {
            kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
            String fac = playerFaction.get(killer.getUniqueId());
            if (fac != null) {
                // increment faction kills (for top factions)
                // handled by scoreboard display
            }
        }
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;

        Player a = (Player) e.getDamager();
        Player b = (Player) e.getEntity();

        String fa = playerFaction.get(a.getUniqueId());
        String fb = playerFaction.get(b.getUniqueId());

        if (fa != null && fa.equals(fb)) {
            e.setCancelled(true);
        }
    }

    /* =========================
       COMMANDS
       ========================= */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "===== Your Stats =====");
            player.sendMessage(ChatColor.GREEN + "Kills: " + ChatColor.WHITE + kills.getOrDefault(uuid, 0));
            player.sendMessage(ChatColor.RED + "Deaths: " + ChatColor.WHITE + deaths.getOrDefault(uuid, 0));
            player.sendMessage(ChatColor.AQUA + "Faction: " + ChatColor.WHITE + (playerFaction.get(uuid) != null ? playerFaction.get(uuid) : "None"));
            return true;
        }

        if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
            if (playerFaction.containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "You already have a faction.");
                return true;
            }
            String name = args[1];
            ChatColor color = ChatColor.values()[new Random().nextInt(ChatColor.values().length)];
            playerFaction.put(uuid, name);
            factionColors.put(name, color);
            Set<UUID> members = new HashSet<>();
            members.add(uuid);
            factionMembers.put(name, members);
            factionJoinRequests.put(name, new HashSet<>());
            player.sendMessage(ChatColor.GREEN + "Faction " + color + name + ChatColor.GREEN + " created!");
            saveFactions();
            return true;
        }

        if (args[0].equalsIgnoreCase("join") && args.length >= 2) {
            String name = args[1];
            if (!factionMembers.containsKey(name)) {
                player.sendMessage(ChatColor.RED + "Faction not found.");
                return true;
            }
            if (playerFaction.containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "You are already in a faction.");
                return true;
            }
            factionJoinRequests.get(name).add(uuid);
            player.sendMessage(ChatColor.AQUA + "Request sent to join " + factionColors.get(name) + name);
            return true;
        }

        return true;
    }

    /* =========================
       SCOREBOARD
       ========================= */

    private void createScoreboard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("f", "dummy", ChatColor.RED + "⚔ Factions ⚔");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        p.setScoreboard(board);
    }

    private void updateScoreboard(Player p) {
        Scoreboard board = p.getScoreboard();
        Objective obj = board.getObjective("f");
        if (obj == null) return;

        board.getEntries().forEach(board::resetScores);

        int line = 15;
        obj.getScore(ChatColor.YELLOW + "Kills: " + kills.getOrDefault(p.getUniqueId(), 0)).setScore(line--);
        obj.getScore(ChatColor.RED + "Deaths: " + deaths.getOrDefault(p.getUniqueId(), 0)).setScore(line--);
        obj.getScore(ChatColor.GRAY + " ").setScore(line--);

        // Top factions
        List<Map.Entry<String, Set<UUID>>> top = new ArrayList<>(factionMembers.entrySet());
        top.sort((a, b) -> b.getValue().size() - a.getValue().size()); // by members count

        obj.getScore(ChatColor.GOLD + "Top Factions").setScore(line--);
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            String fname = top.get(i).getKey();
            ChatColor color = factionColors.getOrDefault(fname, ChatColor.WHITE);
            obj.getScore(color + fname + " §7(" + factionMembers.get(fname).size() + ")").setScore(line--);
        }
    }

    /* =========================
       SAVE / LOAD
       ========================= */

    private void saveFactions() {
        for (String key : factionMembers.keySet()) {
            config.set(key + ".color", factionColors.get(key).name());
            config.set(key + ".members", factionMembers.get(key).stream().map(UUID::toString).toList());
            config.set(key + ".requests", factionJoinRequests.get(key).stream().map(UUID::toString).toList());
        }
        for (UUID u : playerFaction.keySet()) {
            config.set("playerFaction." + u.toString(), playerFaction.get(u));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().warning("Could not save factions.yml");
        }
    }

    private void loadFactions() {
        if (config.getConfigurationSection("") == null) return;

        // Load playerFaction
        if (config.getConfigurationSection("playerFaction") != null) {
            for (String key : config.getConfigurationSection("playerFaction").getKeys(false)) {
                playerFaction.put(UUID.fromString(key), config.getString("playerFaction." + key));
            }
        }

        // Load factions
        for (String key : config.getKeys(false)) {
            if (key.equals("playerFaction")) continue;

            factionColors.put(key, ChatColor.valueOf(config.getString(key + ".color")));
            factionMembers.put(key, new HashSet<>());
            if (config.getStringList(key + ".members") != null) {
                for (String s : config.getStringList(key + ".members")) {
                    factionMembers.get(key).add(UUID.fromString(s));
                }
            }
            factionJoinRequests.put(key, new HashSet<>());
            if (config.getStringList(key + ".requests") != null) {
                for (String s : config.getStringList(key + ".requests")) {
                    factionJoinRequests.get(key).add(UUID.fromString(s));
                }
            }
        }
    }
}
