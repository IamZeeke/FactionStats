package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionStats extends JavaPlugin {

    private File factionsFile;
    private FileConfiguration factionsConfig;
    private Map<UUID, String> playerFaction = new HashMap<>();
    private Map<String, Faction> factions = new HashMap<>();
    private Map<UUID, String> joinRequests = new HashMap<>(); // Player -> Faction request

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createFactionConfig();
        loadFactions();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("f").setExecutor(new FCommand(this));

        startScoreboardUpdate();

        // All online players in creative
        Bukkit.getOnlinePlayers().forEach(p -> p.setGameMode(GameMode.CREATIVE));

        getLogger().info("FactionStats enabled!");
    }

    @Override
    public void onDisable() {
        saveFactions();
        getLogger().info("FactionStats disabled!");
    }

    private void createFactionConfig() {
        factionsFile = new File(getDataFolder(), "factions.yml");
        if (!factionsFile.exists()) {
            factionsFile.getParentFile().mkdirs();
            saveResource("factions.yml", false);
        }
        factionsConfig = YamlConfiguration.loadConfiguration(factionsFile);
    }

    // ----------------- Faction Methods -----------------
    public boolean createFaction(String name, Player owner) {
        if (playerFaction.containsKey(owner.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "You already have a clan!");
            return false;
        }
        if (factions.containsKey(name.toLowerCase())) {
            owner.sendMessage(ChatColor.RED + "A clan with that name already exists!");
            return false;
        }

        Faction faction = new Faction(name, owner.getUniqueId());
        factions.put(name.toLowerCase(), faction);
        playerFaction.put(owner.getUniqueId(), name.toLowerCase());
        owner.sendMessage(ChatColor.GREEN + "Clan " + name + " created! You are the owner.");
        updateNameTags();
        return true;
    }

    public boolean requestJoin(String factionName, Player player) {
        if (!factions.containsKey(factionName.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Clan does not exist!");
            return false;
        }
        if (playerFaction.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in a clan!");
            return false;
        }

        joinRequests.put(player.getUniqueId(), factionName.toLowerCase());
        player.sendMessage(ChatColor.YELLOW + "Requested to join " + factionName + ". Waiting for owner approval.");
        Faction faction = factions.get(factionName.toLowerCase());
        Player owner = Bukkit.getPlayer(faction.getOwner());
        if (owner != null) {
            owner.sendMessage(ChatColor.AQUA + player.getName() + " wants to join your clan. Use /f accept <player> or /f deny <player>");
        }
        return true;
    }

    public boolean acceptJoin(Player owner, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return false;
        String factionName = playerFaction.get(owner.getUniqueId());
        if (factionName == null) return false;
        Faction faction = factions.get(factionName.toLowerCase());
        if (!faction.isOwner(owner.getUniqueId())) return false;
        if (!joinRequests.containsKey(player.getUniqueId())) return false;

        String requestedFaction = joinRequests.remove(player.getUniqueId());
        if (!requestedFaction.equalsIgnoreCase(factionName)) return false;

        faction.addMember(player.getUniqueId());
        playerFaction.put(player.getUniqueId(), factionName.toLowerCase());
        player.sendMessage(ChatColor.GREEN + "You have joined clan " + factionName + "!");
        owner.sendMessage(ChatColor.GREEN + player.getName() + " joined your clan.");
        updateNameTags();
        return true;
    }

    public boolean denyJoin(Player owner, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return false;
        String factionName = playerFaction.get(owner.getUniqueId());
        if (factionName == null) return false;
        Faction faction = factions.get(factionName.toLowerCase());
        if (!faction.isOwner(owner.getUniqueId())) return false;
        if (!joinRequests.containsKey(player.getUniqueId())) return false;

        String requestedFaction = joinRequests.remove(player.getUniqueId());
        if (!requestedFaction.equalsIgnoreCase(factionName)) return false;

        player.sendMessage(ChatColor.RED + "Your request to join " + factionName + " was denied.");
        owner.sendMessage(ChatColor.GREEN + "Denied " + player.getName() + " from joining.");
        return true;
    }

    public boolean disbandFaction(Player player) {
        String name = playerFaction.get(player.getUniqueId());
        if (name == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return false;
        }
        Faction faction = factions.get(name.toLowerCase());
        if (!faction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the owner can disband the clan!");
            return false;
        }

        for (UUID member : faction.getAllMembers()) {
            playerFaction.remove(member);
        }
        factions.remove(name.toLowerCase());
        player.sendMessage(ChatColor.GREEN + "Clan " + name + " has been disbanded!");
        updateNameTags();
        return true;
    }

    public void assignRole(String factionName, UUID member, String role) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction != null) {
            faction.setRole(member, role);
        }
    }

    public Faction getFaction(String name) {
        return factions.get(name.toLowerCase());
    }

    // ----------------- Name Tags -----------------
    public void updateNameTags() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String factionName = playerFaction.get(p.getUniqueId());
            if (factionName != null) {
                Faction f = factions.get(factionName);
                ChatColor color = f.getColor();
                p.setCustomName(color + "[" + f.getName() + "] " + ChatColor.RESET + p.getName());
                p.setCustomNameVisible(true);
            } else {
                p.setCustomName(p.getName());
            }
        }
    }

    // ----------------- Factions Storage -----------------
    private void loadFactions() {
        if (factionsConfig.contains("factions")) {
            for (String key : factionsConfig.getConfigurationSection("factions").getKeys(false)) {
                UUID owner = UUID.fromString(factionsConfig.getString("factions." + key + ".owner"));
                Faction faction = new Faction(key, owner);

                if (factionsConfig.contains("factions." + key + ".members")) {
                    for (String uuidStr : factionsConfig.getStringList("factions." + key + ".members")) {
                        UUID member = UUID.fromString(uuidStr);
                        faction.addMember(member);
                        playerFaction.put(member, key.toLowerCase());
                    }
                }

                factions.put(key.toLowerCase(), faction);
                playerFaction.put(owner, key.toLowerCase());
            }
        }
    }

    public void saveFactions() {
        try {
            for (Faction f : factions.values()) {
                factionsConfig.set("factions." + f.getName() + ".owner", f.getOwner().toString());
                List<String> members = new ArrayList<>();
                for (UUID u : f.getAllMembers()) members.add(u.toString());
                factionsConfig.set("factions." + f.getName() + ".members", members);
            }
            factionsConfig.save(factionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----------------- Scoreboard -----------------
    private void startScoreboardUpdate() {
        new BukkitRunnable() {
            private int animationIndex = 0;
            private String[] anim = {"§c♦ §6Faction Stats §c♦", "§6♦ §cFaction Stats §6♦"};

            @Override
            public void run() {
                animationIndex = (animationIndex + 1) % anim.length;
                String title = anim[animationIndex];
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setPlayerListName(title + " " + player.getName());
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }
}
