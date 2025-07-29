package org.dristmine.f3f.bukkit.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.dristmine.f3f.bukkit.F3fPlugin;
import org.dristmine.f3f.bukkit.config.BukkitF3fConfig;
import org.dristmine.f3f.bukkit.util.PermissionUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// FIXED: Extend PacketListenerAbstract instead of implementing PacketListener
public class F3fPacketListener extends PacketListenerAbstract {
    private final F3fPlugin plugin;
    private final PermissionUtil permissionUtil;
    private final BukkitF3fConfig config;

    // Track last usage timestamps per player to prevent spamming
    private final ConcurrentMap<Player, Long> lastChangeTimestamps = new ConcurrentHashMap<>();

    public F3fPacketListener(F3fPlugin plugin, PermissionUtil permissionUtil, BukkitF3fConfig config) {
        // FIXED: Call super constructor with priority
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.permissionUtil = permissionUtil;
        this.config = config;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only handle plugin message packets
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) {
            return;
        }

        try {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            String channelName = packet.getChannelName();

            // Only handle F3F packets
            if (!channelName.startsWith("f3f:")) {
                return;
            }

            Player player = (Player) event.getPlayer();
            byte[] data = packet.getData();

            plugin.getLogger().info("Received F3F packet: " + channelName + " from " + player.getName());

            switch (channelName) {
                case "f3f:render_distance_change":
                    handleRenderDistanceChange(player, data);
                    break;
                case "f3f:render_distance_sync":
                    handleRenderDistanceSync(player, data);
                    break;
                default:
                    plugin.getLogger().fine("Unknown F3F channel: " + channelName);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling F3F packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRenderDistanceChange(Player player, byte[] data) {
        if (!permissionUtil.canChange(player)) {
            player.sendMessage(config.getMessage("permission_denied"));
            return;
        }

        if (!config.areF3FKeysEnabled()) {
            plugin.getLogger().info(config.getLogMessage("f3f_keys_disabled", player.getName()));
            return;
        }

        if (data == null || data.length < 1) {
            plugin.getLogger().warning(config.getLogMessage("invalid_packet_data", "render_distance_change", player.getName()));
            return;
        }

        // Check cooldown
        final long now = System.currentTimeMillis();
        Long lastTime = lastChangeTimestamps.get(player);
        if (lastTime != null && now - lastTime < config.getF3FCooldown()) {
            plugin.getLogger().fine(config.getLogMessage("cooldown_active", player.getName()));
            return;
        }
        lastChangeTimestamps.put(player, now);

        boolean increase = data[0] != 0;
        plugin.getLogger().info(config.getLogMessage("f3f_command_received", increase ? "increase" : "decrease", player.getName()));

        // Get current global server render distance
        int currentDistance = getGlobalRenderDistance();
        int minDistance = config.getMinRenderDistance();
        int maxDistance = config.getMaxRenderDistance();

        int newDistance = increase ?
                Math.min(currentDistance + 1, maxDistance) :
                Math.max(currentDistance - 1, minDistance);

        if (newDistance == currentDistance) {
            // At limit - still inform client
            sendRenderDistanceUpdate(player, currentDistance);
            player.sendMessage(config.getMessage("render_distance", currentDistance));
            plugin.getLogger().info(config.getLogMessage("render_distance_max", player.getName(), currentDistance));
            return;
        }


        // Apply the change globally
        setGlobalRenderDistance(newDistance);

        // Notify ALL players of the change
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            sendRenderDistanceUpdate(p, newDistance);
            p.sendMessage(config.getMessage("render_distance", newDistance));
        }

        plugin.getLogger().info(config.getLogMessage("render_distance_changed_all", player.getName(), currentDistance, newDistance));
    }

    private void handleRenderDistanceSync(Player player, byte[] data) {
        if (!permissionUtil.canChange(player)) {
            return;
        }

        if (!config.isAutoSyncEnabled()) {
            plugin.getLogger().info(config.getLogMessage("auto_sync_disabled"));
            return;
        }

        if (data == null || data.length < 4) {
            plugin.getLogger().warning(config.getLogMessage("invalid_packet_data", "render_distance_sync", player.getName()));
            return;
        }

        // Convert bytes to int (big-endian)
        int clientDistance = ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8) |
                (data[3] & 0xFF);

        plugin.getLogger().info(config.getLogMessage("client_sync_request", clientDistance, player.getName()));

        // Server inherits FROM client
        int minDistance = config.getMinRenderDistance();
        int maxDistance = config.getMaxRenderDistance();

        // Clamp client distance to server limits
        int clampedDistance = Math.max(minDistance, Math.min(clientDistance, maxDistance));
        int currentDistance = getGlobalRenderDistance();

        if (clampedDistance == currentDistance) {
            plugin.getLogger().info(config.getLogMessage("sync_matches_current", currentDistance));
            return;
        }

        // Server adopts client's render distance
        setGlobalRenderDistance(clampedDistance);

        // Notify all players
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            sendRenderDistanceUpdate(p, clampedDistance);
        }

        plugin.getLogger().info(config.getLogMessage("server_adopted_distance", clampedDistance, player.getName(), currentDistance));
    }

    public void sendRenderDistanceUpdate(Player player, int renderDistance) {
        try {
            // Create data buffer
            byte[] data = new byte[4];
            data[0] = (byte) (renderDistance >> 24);
            data[1] = (byte) (renderDistance >> 16);
            data[2] = (byte) (renderDistance >> 8);
            data[3] = (byte) renderDistance;

            // Create and send packet using PacketEvents
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(
                    "f3f:render_distance_update",
                    data
            );

            // FIXED: Use correct PacketEvents sending method
            com.github.retrooper.packetevents.PacketEvents.getAPI()
                    .getPlayerManager().sendPacket(player, packet);

            plugin.getLogger().fine("Sent render distance update (" + renderDistance + ") to " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send render distance update to " + player.getName() + ": " + e.getMessage());
        }
    }

    private int getGlobalRenderDistance() {
        World world = plugin.getServer().getWorlds().get(0);
        return world.getViewDistance();
    }

    private void setGlobalRenderDistance(int distance) {
        for (World world : plugin.getServer().getWorlds()) {
            world.setViewDistance(distance);
        }
    }
}
