package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
        LOGGER.info("[F3F] Initializing F3F mod");

        // Register packet types
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangeC2SPacket.ID, RenderDistanceChangeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RenderDistanceUpdateS2CPacket.ID, RenderDistanceUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RenderDistanceSyncC2SPacket.ID, RenderDistanceSyncC2SPacket.CODEC);

        // Register packet handlers
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, this::handleRenderDistanceChange);
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceSyncC2SPacket.ID, this::handleRenderDistanceSync);

        // Initialize LuckPerms when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        // Handle player join for auto-sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Request client render distance after player fully loads
            server.execute(() -> {
                if (PermissionUtils.canChange(player)) {
                    // Send a special packet to request client's current render distance
                    // We'll handle this in the client to automatically send back the sync packet
                    sender.sendPacket(new RenderDistanceUpdateS2CPacket(-1)); // -1 = request sync
                }
            });
        });

        LOGGER.info("[F3F] F3F mod initialization complete");
    }

    private void onServerStarted(MinecraftServer server) {
        LOGGER.info("[F3F] Server started, initializing LuckPerms integration...");
        PermissionUtils.initialize();
    }

    private void handleRenderDistanceChange(RenderDistanceChangeC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();

        context.server().execute(() -> {
            // Check permissions first
            if (!PermissionUtils.canChange(player)) {
                player.sendMessage(TextUtils.createPermissionDeniedMessage(), false);
                LOGGER.info("[F3F] Player {} attempted change – denied (no permission)",
                        player.getGameProfile().getName());
                return;
            }

            // Get current server view distance
            int serverViewDistance = player.getServer().getPlayerManager().getViewDistance();
            int currentRenderDistance = Math.min(serverViewDistance, 32);
            int newRenderDistance;

            if (payload.increase()) {
                newRenderDistance = Math.min(currentRenderDistance + 1, 32);
            } else {
                newRenderDistance = Math.max(currentRenderDistance - 1, 2);
            }

            if (newRenderDistance != currentRenderDistance) {
                // Update server view distance FIRST
                player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                // THEN send update packet to client to sync client render distance
                ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(newRenderDistance));

                // Log to server console
                LOGGER.info("[F3F] Player {} changed render distance from {} to {} (F3+F)",
                        player.getName().getString(), currentRenderDistance, newRenderDistance);
            } else {
                // Even when at limits, ensure client and server are synced
                ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(newRenderDistance));

                // Log attempt to server console
                if (payload.increase()) {
                    LOGGER.info("[F3F] Player {} attempted to increase render distance but already at maximum ({})",
                            player.getName().getString(), newRenderDistance);
                } else {
                    LOGGER.info("[F3F] Player {} attempted to decrease render distance but already at minimum ({})",
                            player.getName().getString(), newRenderDistance);
                }
            }
        });
    }

    private void handleRenderDistanceSync(RenderDistanceSyncC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();

        context.server().execute(() -> {
            // Check permissions for auto-sync
            if (!PermissionUtils.canChange(player)) {
                return; // Silently ignore if no permission
            }

            // Clamp render distance to valid range
            int newRenderDistance = Math.max(2, Math.min(32, payload.renderDistance()));
            int currentServerDistance = player.getServer().getPlayerManager().getViewDistance();

            // Only update if different to avoid unnecessary changes
            if (newRenderDistance != currentServerDistance) {
                // Update server view distance silently (no messages)
                player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                // Log auto-sync
                LOGGER.info("[F3F] Auto-synced render distance for player {} from {} to {} (options change)",
                        player.getName().getString(), currentServerDistance, newRenderDistance);
            }
        });
    }
}
