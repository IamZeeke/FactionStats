package me.factionstats;

import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Clan {

    private final String name;
    private UUID owner;
    private final ChatColor color;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> invites = new HashSet<>();
    private int kills;

    public Clan(String name, UUID owner, ChatColor color) {
        this.name = name;
        this.owner = owner;
        this.color = color;
        members.add(owner);
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public ChatColor getColor() {
        return color;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getInvites() {
        return invites;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }
}
