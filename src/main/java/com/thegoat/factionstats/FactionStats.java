package me.yourname.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class FactionStats extends JavaPlugin implements Listener {

    private final HashMap<UUID, Integer> kills = new HashMap<>();
    private final HashMap<UUID, Integer> deaths = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FactionStats enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("FactionStats disabled");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        deaths.put(dead.getUniqueId(),
                deaths.getOrDefault(dead.getUniqueId(), 0) + 1);

        if (killer != null) {
            kills.put(killer.getUniqueId(),
                    kills.getOrDefault(killer.getUniqueId(), 0) + 1);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        int k = kills.getOrDefault(player.getUniqueId(), 0);
        int d = deaths.getOrDefault(player.getUniqueId(), 0);

        player.sendMessage("§6§lYour Stats");
        player.sendMessage("§7Kills: §a" + k);
        player.sendMessage("§7Deaths: §c" + d);

        return true;
    }
}
