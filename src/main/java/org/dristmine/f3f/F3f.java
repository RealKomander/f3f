package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}
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

        // Register packet types and handlers
        registerPackets();

        // Initialize LuckPerms when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        // Handle player join for auto-sync (if enabled)
        if (F3fConfig.getInstance().isAutoSyncEnabled()) {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayerEntity player = handler.getPlayer();

                // Request client render distance after player fully loads
                server.execute(() -> {
                    if (PermissionUtils.canChange(player)) {
                        //? if >=1.20.5 {
                                                sender.sendPacket(new RenderDistanceUpdateS2CPacket(-1)); // -1 = request sync
                        //?} else {
                        /*net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                        new RenderDistanceUpdateS2CPacket(-1).write(buf);
                        ServerPlayNetworking.send(player, RenderDistanceUpdateS2CPacket.ID, buf);*/
                        //?}
                    }
                });
            });
        }

        LOGGER.info(TextUtils.getLogMessage("f3f.log.initialization_complete"));
    }

    private void registerPackets() {
        //? if >=1.20.5 {
        // Modern packet registration (1.20.5+)
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangeC2SPacket.ID, RenderDistanceChangeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RenderDistanceUpdateS2CPacket.ID, RenderDistanceUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RenderDistanceSyncC2SPacket.ID, RenderDistanceSyncC2SPacket.CODEC);

        // Register packet handlers
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, this::handleRenderDistanceChange);
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceSyncC2SPacket.ID, this::handleRenderDistanceSync);
        //?} else {
        /*// Legacy packet registration (1.20.1)
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RenderDistanceChangeC2SPacket packet = RenderDistanceChangeC2SPacket.read(buf);
            server.execute(() -> {
                handleRenderDistanceChangeLegacy(packet, player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceSyncC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            RenderDistanceSyncC2SPacket packet = RenderDistanceSyncC2SPacket.read(buf);
            server.execute(() -> {
                handleRenderDistanceSyncLegacy(packet, player);
            });
        });*/
        //?}
    }

    private void onServerStarted(MinecraftServer server) {
        LOGGER.info(TextUtils.getLogMessage("f3f.log.server_started"));
        PermissionUtils.initialize();
    }

    //? if >=1.20.5 {
    private void handleRenderDistanceChange(RenderDistanceChangeC2SPacket payload, ServerPlayNetworking.Context context) {
        handleRenderDistanceChangeCommon(payload, context.player());
    }

    private void handleRenderDistanceSync(RenderDistanceSyncC2SPacket payload, ServerPlayNetworking.Context context) {
        handleRenderDistanceSyncCommon(payload, context.player());
    }
    //?} else {
    /*private void handleRenderDistanceChangeLegacy(RenderDistanceChangeC2SPacket payload, ServerPlayerEntity player) {
        handleRenderDistanceChangeCommon(payload, player);
    }

    private void handleRenderDistanceSyncLegacy(RenderDistanceSyncC2SPacket payload, ServerPlayerEntity player) {
        handleRenderDistanceSyncCommon(payload, player);
    }*/
    //?}

    private void handleRenderDistanceChangeCommon(RenderDistanceChangeC2SPacket payload, ServerPlayerEntity player) {
        F3fConfig config = F3fConfig.getInstance();

        if (!config.areF3FKeysEnabled()) {
            return;
        }

        if (!PermissionUtils.canChange(player)) {
            player.sendMessage(TextUtils.createPermissionDeniedMessage(), false);
            LOGGER.info(TextUtils.getLogMessage("f3f.log.player_denied", player.getGameProfile().getName()));
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        int currentRenderDistance = server.getPlayerManager().getViewDistance();
        int newRenderDistance = payload.increase()
                ? Math.min(currentRenderDistance + 1, config.getMaxRenderDistance())
                : Math.max(currentRenderDistance - 1, config.getMinRenderDistance());

        if (newRenderDistance != currentRenderDistance) {
            server.getPlayerManager().setViewDistance(newRenderDistance);

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                sendRenderDistanceUpdate(p, newRenderDistance);
                // Send message only to player who requested the change
                if (p.equals(player)) {
                    p.sendMessage(TextUtils.createRenderDistanceMessage(newRenderDistance), false);
                }
            }

            LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_changed_all",
                    player.getName().getString(), currentRenderDistance, newRenderDistance));
        } else {
            // Already at the limit, send packets but no message
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                sendRenderDistanceUpdate(p, newRenderDistance);
            }

            if (payload.increase()) {
                LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_max",
                        player.getName().getString(), newRenderDistance));
            } else {
                LOGGER.info(TextUtils.getLogMessage("f3f.log.render_distance_min",
                        player.getName().getString(), newRenderDistance));
            }
        }
    }

    private void handleRenderDistanceSyncCommon(RenderDistanceSyncC2SPacket payload, ServerPlayerEntity player) {
        F3fConfig config = F3fConfig.getInstance();

        if (!config.isAutoSyncEnabled()) {
            return;
        }

        if (!PermissionUtils.canChange(player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        int currentServerDistance = server.getPlayerManager().getViewDistance();
        int newRenderDistance = Math.max(config.getMinRenderDistance(),
                Math.min(config.getMaxRenderDistance(), payload.renderDistance()));

        if (newRenderDistance != currentServerDistance) {
            server.getPlayerManager().setViewDistance(newRenderDistance);

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                sendRenderDistanceUpdate(p, newRenderDistance);
            }

            LOGGER.info(TextUtils.getLogMessage("f3f.log.auto_sync",
                    player.getName().getString(), currentServerDistance, newRenderDistance));
        }
    }

    private void sendRenderDistanceUpdate(ServerPlayerEntity player, int renderDistance) {
        //? if >=1.20.5 {
        ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(renderDistance));
        //?} else {
        /*net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        new RenderDistanceUpdateS2CPacket(renderDistance).write(buf);
        ServerPlayNetworking.send(player, RenderDistanceUpdateS2CPacket.ID, buf);*/
        //?}
    }
}
