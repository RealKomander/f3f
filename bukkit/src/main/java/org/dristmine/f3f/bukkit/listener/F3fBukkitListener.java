package org.dristmine.f3f.bukkit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.dristmine.f3f.bukkit.F3fPlugin;
import org.dristmine.f3f.bukkit.config.BukkitF3fConfig;
import org.dristmine.f3f.bukkit.util.PermissionUtil;

public class F3fBukkitListener implements Listener {
    private final F3fPlugin plugin;
    private final PermissionUtil permissionUtil;
    private final BukkitF3fConfig config;
    private final F3fPacketListener packetListener;

    public F3fBukkitListener(F3fPlugin plugin, PermissionUtil permissionUtil, BukkitF3fConfig config, F3fPacketListener packetListener) {
        this.plugin = plugin;
        this.permissionUtil = permissionUtil;
        this.config = config;
        this.packetListener = packetListener;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getLogger().info(config.getLogMessage("player_joined", player.getName()));

        if (!config.isAutoSyncEnabled()) {
            plugin.getLogger().info(config.getLogMessage("auto_sync_disabled"));
            return;
        }

        if (!permissionUtil.canChange(player)) {
            plugin.getLogger().info(config.getLogMessage("no_permission_for_sync", player.getName()));
            return;
        }

        // Request sync FROM client (send -1 to request client's render distance)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info(config.getLogMessage("requesting_sync", player.getName()));
            packetListener.sendRenderDistanceUpdate(player, -1);
        }, 60L); // 3 seconds delay
    }
}
