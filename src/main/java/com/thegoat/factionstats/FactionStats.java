package me.factionstats;

import org.bukkit.plugin.java.JavaPlugin;

public class FactionStats extends JavaPlugin {

    private static FactionStats instance;
    private FactionManager factionManager;
    private ScoreboardManager scoreboardManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Make sure config.yml exists
        factionManager = new FactionManager(this);
        scoreboardManager = new ScoreboardManager(this, factionManager);
        npcManager = new NPCManager(this, factionManager);

        getCommand("f").setExecutor(factionManager); // All /f subcommands handled here
        getServer().getPluginManager().registerEvents(scoreboardManager, this);

        npcManager.spawnTestNPC(); // For testing join/kill/scoreboard locally
        scoreboardManager.start(); // Start animated scoreboard updater

        getLogger().info("FactionStats v2 enabled successfully!");
    }

    @Override
    public void onDisable() {
        scoreboardManager.stop();
        factionManager.saveAll();
        getLogger().info("FactionStats v2 disabled!");
    }

    public static FactionStats getInstance() { return instance; }
}
