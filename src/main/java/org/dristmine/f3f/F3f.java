package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.dristmine.f3f.packet.RenderDistanceChangeC2SPacket;
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

        // Initialize LuckPerms integration
        PermissionUtils.initialize();

        // Register packet types
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangeC2SPacket.ID, RenderDistanceChangeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RenderDistanceUpdateS2CPacket.ID, RenderDistanceUpdateS2CPacket.CODEC);

        // Register packet handler on server side
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            context.server().execute(() -> {
                // Check permissions first
                if (!PermissionUtils.canChange(player)) {
                    player.sendMessage(TextUtils.createPermissionDeniedMessage(), true);
                    LOGGER.info("[F3F] Player {} attempted change – denied (no permission)",
                            player.getGameProfile().getName());
                    return;
                }

                // Get current server view distance
                int serverViewDistance = player.getServer().getPlayerManager().getViewDistance();

                // Calculate new render distance based on current server setting
                int currentRenderDistance = Math.min(serverViewDistance, 32);
                int newRenderDistance;

                if (payload.increase()) {
                    newRenderDistance = Math.min(currentRenderDistance + 1, 32);
                } else {
                    newRenderDistance = Math.max(currentRenderDistance - 1, 2);
                }

                if (newRenderDistance != currentRenderDistance) {
                    // Update server view distance
                    player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                    // Send update packet to client to change their render distance
                    ServerPlayNetworking.send(player, new RenderDistanceUpdateS2CPacket(newRenderDistance));

                    // Send styled feedback message to player
                    player.sendMessage(TextUtils.createRenderDistanceMessage(newRenderDistance), true);

                    // Log to server console
                    LOGGER.info("[F3F] Player {} changed render distance from {} to {}",
                            player.getName().getString(), currentRenderDistance, newRenderDistance);
                } else {
                    // Send message when already at limit (but still with styled format)
                    player.sendMessage(TextUtils.createRenderDistanceMessage(newRenderDistance), true);

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
        });

        LOGGER.info("[F3F] F3F mod initialization complete");
    }
}
