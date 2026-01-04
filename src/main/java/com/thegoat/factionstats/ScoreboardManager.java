package me.factionstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager implements Listener {

    private final FactionStats plugin;
    private final FactionManager manager;
    private boolean running = true;
    private final List<String> animations = Arrays.asList("§a§lTop Factions", "§b§lTop Factions", "§c§lTop Factions");
    private int tick = 0;

    public ScoreboardManager(FactionStats plugin, FactionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick++;
            for (Player p : Bukkit.getOnlinePlayers()) updateBoard(p);
        }, 0L, 20L);
    }

    public void stop() { running = false; }

    private void updateBoard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("factions", "dummy", animations.get(tick % animations.size()));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Faction> topFactions = new ArrayList<>(manager.getFactions().values());
        topFactions.sort(Comparator.comparingInt(Faction::getPoints).reversed());
        for (int i = 0; i < Math.min(5, topFactions.size()); i++) {
            Faction f = topFactions.get(i);
            obj.getScore(f.getName() + " §7[" + f.getPoints() + "]").setScore(5 - i);
        }

        p.setScoreboard(board);
    }
}
