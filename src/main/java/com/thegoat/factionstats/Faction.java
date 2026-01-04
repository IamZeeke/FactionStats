package me.factionstats;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class Faction {
    private final String name;
    private final UUID owner;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pending = new HashSet<>();
    private int points = 0;

    public Faction(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        members.add(owner);
    }

    public void addMember(UUID player) { members.add(player); pending.remove(player); }
    public void removeMember(UUID player) { members.remove(player); }
    public void addPending(UUID player) { pending.add(player); }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getPending() { return pending; }
    public int getPoints() { return points; }
    public void addPoints(int amount) { points += amount; }

    public void save(FileConfiguration config) {
        config.set("factions." + name + ".owner", owner.toString());
        List<String> memberList = new ArrayList<>();
        for (UUID m : members) memberList.add(m.toString());
        config.set("factions." + name + ".members", memberList);
        config.set("factions." + name + ".points", points);
    }
}
