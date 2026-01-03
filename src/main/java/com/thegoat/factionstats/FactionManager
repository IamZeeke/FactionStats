package me.factionstats;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public class FactionManager {
    private Map<String, Faction> factions = new HashMap<>();
    private Map<UUID, String> playerFaction = new HashMap<>();

    public void addFaction(Faction faction) {
        factions.put(faction.getName(), faction);
        playerFaction.put(faction.getOwner(), faction.getName());
    }

    public Faction getFaction(String name) {
        return factions.get(name);
    }

    public boolean playerHasFaction(UUID uuid) {
        return playerFaction.containsKey(uuid);
    }

    public String getPlayerFaction(UUID uuid) {
        return playerFaction.get(uuid);
    }

    public void joinFaction(UUID player, String factionName) {
        Faction f = factions.get(factionName);
        if (f != null) f.getJoinRequests().add(player);
    }

    public void acceptPlayer(UUID player, String factionName) {
        Faction f = factions.get(factionName);
        if (f != null) {
            f.getJoinRequests().remove(player);
            f.getMembers().add(player);
            playerFaction.put(player, factionName);
        }
    }

    // Save/load simplified
    public void save(FileConfiguration config) {
        for (Faction f : factions.values()) {
            config.set(f.getName() + ".owner", f.getOwner().toString());
            config.set(f.getName() + ".color", f.getColor().name());
            List<String> mems = new ArrayList<>();
            for (UUID u : f.getMembers()) mems.add(u.toString());
            config.set(f.getName() + ".members", mems);

            List<String> reqs = new ArrayList<>();
            for (UUID u : f.getJoinRequests()) reqs.add(u.toString());
            config.set(f.getName() + ".requests", reqs);
        }
        for (Map.Entry<UUID,String> entry : playerFaction.entrySet()) {
            config.set("playerFaction." + entry.getKey(), entry.getValue());
        }
    }

    public void load(FileConfiguration config) {
        playerFaction.clear();
        factions.clear();

        if (config.getConfigurationSection("playerFaction") != null) {
            for (String key : config.getConfigurationSection("playerFaction").getKeys(false)) {
                playerFaction.put(UUID.fromString(key), config.getString("playerFaction." + key));
            }
        }

        for (String key : config.getKeys(false)) {
            if (key.equals("playerFaction")) continue;
            UUID owner = UUID.fromString(config.getString(key + ".owner"));
            ChatColor color = ChatColor.valueOf(config.getString(key + ".color"));
            Faction f = new Faction(key, color, owner);

            List<String> mems = config.getStringList(key + ".members");
            for (String s : mems) f.getMembers().add(UUID.fromString(s));

            List<String> reqs = config.getStringList(key + ".requests");
            for (String s : reqs) f.getJoinRequests().add(UUID.fromString(s));

            factions.put(key, f);
        }
    }

    public Collection<Faction> getAllFactions() {
        return factions.values();
    }
}
