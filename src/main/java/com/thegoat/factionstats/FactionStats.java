package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FactionStats extends JavaPlugin implements Listener {

    private FactionManager manager;
    private Map<UUID,Integer> kills = new HashMap<>();
    private Map<UUID,Integer> deaths = new HashMap<>();
    private File factionsFile;
    private FileConfiguration factionsConfig;
    private int animationTick = 0;

    @Override
    public void onEnable() {
        manager = new FactionManager();
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        // factions.yml
        factionsFile = new File(getDataFolder(), "factions.yml");
        if (!factionsFile.exists()) try { factionsFile.createNewFile(); } catch(IOException ignored){}
        factionsConfig = YamlConfiguration.loadConfiguration(factionsFile);
        manager.load(factionsConfig);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            animationTick++;
            for (Player p : Bukkit.getOnlinePlayers()) updateScoreboard(p);
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        manager.save(factionsConfig);
        try { factionsConfig.save(factionsFile); } catch(IOException ignored){}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        kills.putIfAbsent(p.getUniqueId(),0);
        deaths.putIfAbsent(p.getUniqueId(),0);

        // Auto Creative for faction owners
        String fName = manager.getPlayerFaction(p.getUniqueId());
        if (fName != null && manager.getFaction(fName).getOwner().equals(p.getUniqueId())) {
            p.setGameMode(GameMode.CREATIVE);
        }

        createScoreboard(p);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        deaths.put(dead.getUniqueId(), deaths.getOrDefault(dead.getUniqueId(),0)+1);
        Player killer = dead.getKiller();
        if (killer!=null) kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(),0)+1);
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;
        Player a = (Player)e.getDamager();
        Player b = (Player)e.getEntity();
        String fa = manager.getPlayerFaction(a.getUniqueId());
        String fb = manager.getPlayerFaction(b.getUniqueId());
        if (fa!=null && fa.equals(fb)) e.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player)sender;
        UUID uuid = p.getUniqueId();

        if (args.length==0) {
            p.sendMessage(ChatColor.GOLD+"===== Stats =====");
            p.sendMessage(ChatColor.GREEN+"Kills: "+ChatColor.WHITE+kills.getOrDefault(uuid,0));
            p.sendMessage(ChatColor.RED+"Deaths: "+ChatColor.WHITE+deaths.getOrDefault(uuid,0));
            p.sendMessage(ChatColor.AQUA+"Faction: "+ChatColor.WHITE+(manager.getPlayerFaction(uuid)!=null?manager.getPlayerFaction(uuid):"None"));
            return true;
        }

        if (args[0].equalsIgnoreCase("create") && args.length>=2) {
            if (manager.playerHasFaction(uuid)) { p.sendMessage(ChatColor.RED+"You already have a faction."); return true; }
            String name = args[1];
            ChatColor color = ChatColor.values()[new Random().nextInt(ChatColor.values().length)];
            manager.addFaction(new Faction(name,color,uuid));
            p.sendMessage(ChatColor.GREEN+"Faction "+color+name+ChatColor.GREEN+" created!");
            manager.save(factionsConfig);
            try { factionsConfig.save(factionsFile); } catch(IOException ignored){}
            return true;
        }

        if (args[0].equalsIgnoreCase("join") && args.length>=2) {
            String name = args[1];
            if (manager.getFaction(name)==null) { p.sendMessage(ChatColor.RED+"Faction not found."); return true; }
            if (manager.playerHasFaction(uuid)) { p.sendMessage(ChatColor.RED+"You are already in a faction."); return true; }
            manager.joinFaction(uuid,name);
            p.sendMessage(ChatColor.AQUA+"Request sent to join "+manager.getFaction(name).getColor()+name);
            return true;
        }

        return true;
    }

    /* Scoreboard */
    private void createScoreboard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("f","dummy",ChatColor.RED+"⚔ Factions ⚔");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        p.setScoreboard(board);
    }

    private void updateScoreboard(Player p) {
        Scoreboard board = p.getScoreboard();
        Objective obj = board.getObjective("f");
        if (obj==null) return;
        board.getEntries().forEach(board::resetScores);

        int line = 15;
        obj.getScore(ChatColor.YELLOW+"Kills: "+kills.getOrDefault(p.getUniqueId(),0)).setScore(line--);
        obj.getScore(ChatColor.RED+"Deaths: "+deaths.getOrDefault(p.getUniqueId(),0)).setScore(line--);
        obj.getScore(ChatColor.GRAY+" ").setScore(line--);

        List<Faction> top = new ArrayList<>(manager.getAllFactions());
        top.sort((a,b)->b.getMembers().size()-a.getMembers().size());

        obj.getScore(ChatColor.GOLD+"Top Factions").setScore(line--);
        for(int i=0;i<Math.min(5,top.size());i++) {
            Faction f = top.get(i);
            obj.getScore(f.getColor()+f.getName()+" §7("+f.getMembers().size()+")").setScore(line--);
        }
    }
}
