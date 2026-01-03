package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, Clan> playerClan = new HashMap<>();
    private final File dataFile;

    public ClanManager() {
        dataFile = new File(FactionStats.get().getDataFolder(), "data.yml");
        loadData();
    }

    /* ---------------- LOOKUPS ---------------- */

    public boolean isInClan(UUID uuid) {
        return playerClan.containsKey(uuid);
    }

    public Clan getClan(UUID uuid) {
        return playerClan.get(uuid);
    }

    public Clan getClan(String name) {
        return clans.get(name.toLowerCase());
    }

    /* ---------------- CLAN ACTIONS ---------------- */

    public void createClan(Player player, String name, ChatColor color) {
        Clan clan = new Clan(name, player.getUniqueId(), color);
        clans.put(name.toLowerCase(), clan);
        playerClan.put(player.getUniqueId(), clan);
        updateNametag(player, clan);
        saveData();
    }

    public void invite(Player owner, Player target) {
        Clan clan = getClan(owner.getUniqueId());
        clan.getInvites().add(target.getUniqueId());
        saveData();
    }

    public boolean joinClan(Player player, Clan clan) {
        if (!clan.getInvites().remove(player.getUniqueId())) return false;
        clan.getMembers().add(player.getUniqueId());
        playerClan.put(player.getUniqueId(), clan);
        updateNametag(player, clan);
        saveData();
        return true;
    }

    public void leaveClan(Player player) {
        Clan clan = getClan(player.getUniqueId());
        clan.getMembers().remove(player.getUniqueId());
        playerClan.remove(player.getUniqueId());
        removeNametag(player);
        saveData();
    }

    public void kick(Player owner, Player target) {
        Clan clan = getClan(owner.getUniqueId());
        clan.getMembers().remove(target.getUniqueId());
        playerClan.remove(target.getUniqueId());
        removeNametag(target);
        saveData();
    }

    public void promote(Player owner, Player target) {
        Clan clan = getClan(owner.getUniqueId());
        clan.setOwner(target.getUniqueId());
        saveData();
    }

    /* ---------------- NAME TAGS ---------------- */

    private void updateNametag(Player player, Clan clan) {
        var sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(clan.getName());

        if (team == null) {
            team = sb.registerNewTeam(clan.getName());
            team.setPrefix(clan.getColor() + "[" + clan.getName() + "] ");
        }

        team.addEntry(player.getName());
    }

    private void removeNametag(Player player) {
        var sb = Bukkit.getScoreboardManager().getMainScoreboard();
        sb.getTeams().forEach(team -> team.removeEntry(player.getName()));
    }

    /* ---------------- TOP CLANS ---------------- */

    public Collection<Clan> getTopClans() {
        return clans.values().stream()
                .sorted(Comparator.comparingInt(Clan::getKills).reversed())
                .limit(5)
                .toList();
    }

    /* ---------------- SAVING ---------------- */

    public void saveData() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();

            for (Clan clan : clans.values()) {
                String path = "clans." + clan.getName();
                yaml.set(path + ".owner", clan.getOwner().toString());
                yaml.set(path + ".color", clan.getColor().name());
                yaml.set(path + ".kills", clan.getKills());

                List<String> members = new ArrayList<>();
                for (UUID u : clan.getMembers()) {
                    members.add(u.toString());
                }
                yaml.set(path + ".members", members);
            }

            yaml.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        if (!yaml.contains("clans")) return;

        for (String name : yaml.getConfigurationSection("clans").getKeys(false)) {
            String path = "clans." + name;

            UUID owner = UUID.fromString(yaml.getString(path + ".owner"));
            ChatColor color = ChatColor.valueOf(yaml.getString(path + ".color"));
            Clan clan = new Clan(name, owner, color);

            clan.getMembers().clear();
            for (String s : yaml.getStringList(path + ".members")) {
                UUID u = UUID.fromString(s);
                clan.getMembers().add(u);
                playerClan.put(u, clan);
            }

            clans.put(name.toLowerCase(), clan);
        }
    }
}
