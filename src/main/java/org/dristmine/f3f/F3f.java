package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.dristmine.f3f.config.F3fConfig;
import org.dristmine.f3f.packet.RenderDistanceChangeC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceSyncC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceUpdateS2CPacket;
import org.dristmine.f3f.util.PermissionUtils;
import org.dristmine.f3f.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F3f implements ModInitializer {
    public static final String MOD_ID = "f3f";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Load configuration first
        F3fConfig.load();

        LOGGER.info(TextUtils.getLogMessage("f3f.log.initializing"));

        // Register packet types
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangeC2SPacket.ID, RenderDistanceChangeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RenderDistanceUpdateS2CPacket.ID, RenderDistanceUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RenderDistanceSyncC2SPacket.ID, RenderDistanceSyncC2SPacket.CODEC);

        // Register packet handlers
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, this::handleRenderDistanceChange);
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceSyncC2SPacket.ID, this::handleRenderDistanceSync);

        // Initialize LuckPerms when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        // Handle player join for auto-sync (if enabled)
        if (F3fConfig.getInstance().isAutoSyncEnabled()) {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayerEntity player = handler.getPlayer();

                // Request client render distance after player fully loads
                server.execute(() -> {
                    if (PermissionUtils.canChange(player)) {
                        sender.sendPacket(new RenderDistanceUpdateS2CPacket(-1)); // -1 = request sync
                    }
                });
            });
        }

        LOGGER.info(TextUtils.getLogMessage("f3f.log.initialization_complete"));
    }

    private void onServerStarted(MinecraftServer server) {
        LOGGER.info(TextUtils.getLogMessage("f3f.log.server_started"));
        PermissionUtils.initialize();
    }

    private void handleRenderDistanceChange(RenderDistanceChangeC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        F3fConfig config = F3fConfig.getInstance();

        // Check if F3+F keys are enabled
        if (!config.areF3FKeysEnabled()) {
            return;
        }

        context.server().execute(() -> {
            // Check permissions first
            if (!PermissionUtils.canChange(player)) {
                player.sendMessage(TextUtils.createPermissionDeniedMessage(), false);
                LOGGER.info(TextUtils.getLogMessage("f3f.log.player_denied",
                        player.getGameProfile().getName()));
                return;
            }

            // Get current server view distance
            int serverViewDistance = player.getServer().getPlayerManager().getViewDistance();
            int currentRenderDistance = Math.min(serverViewDistance, config.getMaxRenderDistance());
            int newRenderDistance;

            if (payload.increase()) {
                newRenderDistance = Math.min(currentRenderDistance + 1, config.getMaxRenderDistance());
            } else {
                newRenderDistance = Math.max(currentRenderDistance - 1, config.getMinRenderDistance());
            }

            if (newRenderDistance != currentRenderDistance) {
                // Update server view distance FIRST
                player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                // THEN send update packet to client to sync client render distance
                ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(newRenderDistance));

                // Log to server console
                LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_changed",
                        player.getName().getString(), currentRenderDistance, newRenderDistance));
            } else {
                // Even when at limits, ensure client and server are synced
                ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(newRenderDistance));

                // Log attempt to server console
                if (payload.increase()) {
                    LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_max",
                            player.getName().getString(), newRenderDistance));
                } else {
                    LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_min",
                            player.getName().getString(), newRenderDistance));
                }
            }
        });
    }

    private void handleRenderDistanceSync(RenderDistanceSyncC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        F3fConfig config = F3fConfig.getInstance();

        // Check if auto-sync is enabled
        if (!config.isAutoSyncEnabled()) {
            return;
        }

        context.server().execute(() -> {
            // Check permissions for auto-sync
            if (!PermissionUtils.canChange(player)) {
                return; // Silently ignore if no permission
            }

            // Clamp render distance to configured range
            int newRenderDistance = Math.max(config.getMinRenderDistance(),
                    Math.min(config.getMaxRenderDistance(), payload.renderDistance()));
            int currentServerDistance = player.getServer().getPlayerManager().getViewDistance();

            // Only update if different to avoid unnecessary changes
            if (newRenderDistance != currentServerDistance) {
                // Update server view distance silently (no messages)
                player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                // Log auto-sync
                LOGGER.info(TextUtils.getLogMessage("f3f.log.auto_sync",
                        player.getName().getString(), currentServerDistance, newRenderDistance));
            }
        });
    }
}
