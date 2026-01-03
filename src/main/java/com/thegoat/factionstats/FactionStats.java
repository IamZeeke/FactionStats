import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionStats extends JavaPlugin implements Listener {

    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
        getLogger().info("FactionStats enabled");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    // COMMANDS
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
                player.sendMessage("§cYou are already in a clan.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /f create <name>");
                return true;
            }

            String name = args[1].toLowerCase();
            if (clans.containsKey(name)) {
                player.sendMessage("§cThat clan already exists.");
                return true;
            }

            Clan clan = new Clan(name, player.getUniqueId());
            clans.put(name, clan);
            playerClan.put(player.getUniqueId(), name);

            player.sendMessage("§aClan created successfully!");
            saveData();
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (playerClan.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou are already in a clan.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /f join <name>");
                return true;
            }

            String name = args[1].toLowerCase();
            Clan clan = clans.get(name);

            if (clan == null) {
                player.sendMessage("§cClan not found.");
                return true;
            }

            clan.members.add(player.getUniqueId());
            playerClan.put(player.getUniqueId(), name);

            player.sendMessage("§aYou joined clan " + clan.name);
            saveData();
            return true;
        }

        return true;
    }

    // KILLS / DEATHS
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        if (playerClan.containsKey(dead.getUniqueId())) {
            clans.get(playerClan.get(dead.getUniqueId())).deaths++;
        }

        if (killer != null && playerClan.containsKey(killer.getUniqueId())) {
            clans.get(playerClan.get(killer.getUniqueId())).kills++;
        }

        saveData();
    }

    // DATA SAVE / LOAD
    private void saveData() {
        data.set("clans", null);

        for (Clan clan : clans.values()) {
            String path = "clans." + clan.name;
            data.set(path + ".owner", clan.owner.toString());
            data.set(path + ".kills", clan.kills);
            data.set(path + ".deaths", clan.deaths);

            List<String> members = new ArrayList<>();
            for (UUID u : clan.members) members.add(u.toString());
            data.set(path + ".members", members);
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (!data.contains("clans")) return;

        for (String name : data.getConfigurationSection("clans").getKeys(false)) {
            UUID owner = UUID.fromString(data.getString("clans." + name + ".owner"));
            Clan clan = new Clan(name, owner);

            clan.kills = data.getInt("clans." + name + ".kills");
            clan.deaths = data.getInt("clans." + name + ".deaths");

            for (String s : data.getStringList("clans." + name + ".members")) {
                UUID u = UUID.fromString(s);
                clan.members.add(u);
                playerClan.put(u, name);
            }

            clans.put(name, clan);
        }
    }

    // CLAN CLASS (INNER — SAME FILE)
    private static class Clan {
        String name;
        UUID owner;
        Set<UUID> members = new HashSet<>();
        int kills = 0;
        int deaths = 0;

        Clan(String name, UUID owner) {
            this.name = name;
            this.owner = owner;
            members.add(owner);
        }
    }
}
