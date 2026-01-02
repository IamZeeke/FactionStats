package com.thegoat.factionstats;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class FactionStats extends JavaPlugin {

    private final Map<UUID, Integer> animationIndex = new HashMap<>();
    private final List<String> animationFrames = Arrays.asList(
            "§c✦", "§6✦", "§e✦", "§a✦", "§b✦", "§d✦"
    );

    @Override
    public void onEnable() {
        getLogger().info("[FactionStats] Plugin enabled!");

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("[FactionStats] Plugin disabled!");
    }

    private void updateScoreboard(Player player) {
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        animationIndex.putIfAbsent(player.getUniqueId(), 0);
        int idx = animationIndex.get(player.getUniqueId());
        String frame = animationFrames.get(idx % animationFrames.size());
        animationIndex.put(player.getUniqueId(), idx + 1);

        Objective objective = board.registerNewObjective(
                "fstats", "dummy",
                frame + " §c⚔ §6Faction Stats §c⚔ " + frame
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (faction.isNone()) {
            objective.getScore("§cYou are not in a faction!").setScore(6);
        } else {
            objective.getScore("§eFaction: §f" + faction.getName()).setScore(6);
            objective.getScore("§eMembers: §f" + faction.getFPlayers().size()).setScore(5);
            objective.getScore("§ePower: §f" + faction.getPower() + "/" + faction.getMaxPower()).setScore(4);
        }

        List<Faction> topFactions = FPlayers.getInstance().getFPlayers()
                .stream()
                .map(FPlayer::getFaction)
                .distinct()
                .sorted(Comparator.comparingDouble(Faction::getPower).reversed())
                .limit(5)
                .collect(Collectors.toList());

        int scoreIndex = 3;
        objective.getScore("§6Top 5 Factions:").setScore(scoreIndex--);

        for (Faction top : topFactions) {
            objective.getScore("§e" + top.getName() + ": §f" + top.getPower()).setScore(scoreIndex--);
        }

        player.setScoreboard(board);
    }
}

