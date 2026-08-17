package su.deworld.dwchat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.moderation.CheckResult;

public class PlayerConnectionListener implements Listener {

    private final DwChat plugin;

    public PlayerConnectionListener(DwChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getPluginConfig().sendJoinLeave) return;
        String player = event.getPlayer().getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getBridgeManager().onPlayerJoin(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Clear flood detection history
        plugin.getModerationManager().clearPlayer(event.getPlayer().getUniqueId());

        if (!plugin.getPluginConfig().sendJoinLeave) return;
        String player = event.getPlayer().getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getBridgeManager().onPlayerLeave(player));
    }
}