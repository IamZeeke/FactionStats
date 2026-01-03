package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FactionStats extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();
    private File file;
    private YamlConfiguration config;
    private int tick = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        file = new File(getDataFolder(), "clans.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadClans();

        getCommand("f").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() { updateScoreboards(); updateTags(); }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info("FactionStats enabled!");
    }

    @Override
    public void onDisable() { saveClans(); }

    // ===================== CLAN COMMANDS =====================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length < 1) {
            player.sendMessage("§c/f create <name>");
            player.sendMessage("§c/f join <name>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (playerClan.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou already have a clan.");
                return true;
            }
            if (args.length < 2) { player.sendMessage("§c/f create <name>"); return true; }
            Clan clan = new Clan(args[1], player.getUniqueId());
            clans.put(args[1].toLowerCase(), clan);
            playerClan.put(player.getUniqueId(), args[1].toLowerCase());
            saveClans();
            player.sendMessage("§aClan created!");
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (playerClan.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou already have a clan.");
                return true;
            }
            if (args.length < 2) { player.sendMessage("§c/f join <name>"); return true; }
            Clan clan = clans.get(args[1].toLowerCase());
            if (clan == null) { player.sendMessage("§cClan not found."); return true; }
            clan.getMembers().add(player.getUniqueId());
            playerClan.put(player.getUniqueId(), clan.getName().toLowerCase());
            saveClans();
            player.sendMessage("§aJoined clan " + clan.getName());
            return true;
        }

        return true;
    }

    // ===================== EVENTS =====================
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Player killer = dead.getKiller();

        if (playerClan.containsKey(dead.getUniqueId())) clans.get(playerClan.get(dead.getUniqueId())).addDeath();
        if (killer != null && playerClan.containsKey(killer.getUniqueId())) clans.get(playerClan.get(killer.getUniqueId())).addKill();

        saveClans();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player target)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (playerClan.containsKey(attacker.getUniqueId()) &&
            playerClan.containsKey(target.getUniqueId()) &&
            playerClan.get(attacker.getUniqueId()).equals(playerClan.get(target.getUniqueId()))) {
            e.setCancelled(true); // prevent friendly-fire
        }
    }

    // ===================== SCOREBOARD =====================
    private void updateScoreboards() {
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective o = sb.registerNewObjective("f", "dummy", "§c⚔ §6Factions §c⚔");
            o.setDisplaySlot(DisplaySlot.SIDEBAR);

            int score = 10;
            clans.values().stream()
                    .sorted(Comparator.comparingInt(Clan::getKills).reversed())
                    .limit(5)
                    .forEach(c -> o.getScore("§e" + c.getName() + " §7- §c" + c.getKills()).setScore(score--));

            p.setScoreboard(sb);
        }
    }

    // ===================== NAME TAGS =====================
    private void updateTags() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playerClan.containsKey(p.getUniqueId())) {
                String cname = playerClan.get(p.getUniqueId());
                p.setPlayerListName("§6[" + cname + "] §f" + p.getName());
                p.setDisplayName("§6[" + cname + "] §f" + p.getName());
            } else {
                p.setPlayerListName(p.getName());
                p.setDisplayName(p.getName());
            }
        }
    }

    // ===================== SAVE / LOAD =====================
    private void saveClans() {
        config.set("clans", null);
        for (Clan c : clans.values()) {
            String path = "clans." + c.getName();
            config.set(path + ".owner", c.getOwner().toString());
            config.set(path + ".kills", c.getKills());
            config.set(path + ".deaths", c.getDeaths());
            List<String> members = new ArrayList<>();
            for (UUID u : c.getMembers()) members.add(u.toString());
            config.set(path + ".members", members);
        }
        try { config.save(file); } catch (IOException ignored) {}
    }

    private void loadClans() {
        if (!config.contains("clans")) return;
        for (String name : config.getConfigurationSection("clans").getKeys(false)) {
            UUID owner = UUID.fromString(config.getString("clans." + name + ".owner"));
            Clan c = new Clan(name, owner);
            c.kills = config.getInt("clans." + name + ".kills");
            c.deaths = config.getInt("clans." + name + ".deaths");
            for (String s : config.getStringList("clans." + name + ".members")) c.getMembers().add(UUID.fromString(s));
            clans.put(name.toLowerCase(), c);
            for (UUID u : c.getMembers()) playerClan.put(u, name.toLowerCase());
        }
    }

    // ===================== INNER CLASS =====================
    private static class Clan {
        private final String name;
        private final UUID owner;
        private final Set<UUID> members = new HashSet<>();
        private int kills = 0, deaths = 0;

        public Clan(String name, UUID owner) { this.name = name; this.owner = owner; members.add(owner); }
        public String getName() { return name; }
        public UUID getOwner() { return owner; }
        public Set<UUID> getMembers() { return members; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public void addKill() { kills++; }
        public void addDeath() { deaths++; }
    }
}
