package me.factionstats;

import org.bukkit.ChatColor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    private String name;
    private ChatColor color;
    private UUID owner;
    private Set<UUID> members;
    private Set<UUID> joinRequests;

    public Faction(String name, ChatColor color, UUID owner) {
        this.name = name;
        this.color = color;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.joinRequests = new HashSet<>();
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public UUID getOwner() { return owner; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getJoinRequests() { return joinRequests; }
}
