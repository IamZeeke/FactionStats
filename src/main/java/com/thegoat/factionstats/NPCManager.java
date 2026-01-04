package me.factionstats;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class NPCManager {

    private final JavaPlugin plugin;
    private final FactionManager manager;

    public NPCManager(JavaPlugin plugin, FactionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void spawnTestNPC() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) return;

        Location loc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(net.citizensnpcs.api.npc.NPCType.PLAYER, "TestNPC");
        npc.spawn(loc);
    }
}
