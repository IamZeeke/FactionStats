package me.factionstats;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FriendlyFireListener implements Listener {

    private final ClanManager manager;

    public FriendlyFireListener(ClanManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        if (manager.getClan(damager.getUniqueId()) != null &&
            manager.getClan(damager.getUniqueId()) == manager.getClan(victim.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}
