package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.dristmine.f3f.packet.RenderDistanceChangeC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceUpdateS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F3f implements ModInitializer {
    public static final String MOD_ID = "f3f";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing F3F mod");

        // Register packet types
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangeC2SPacket.ID, RenderDistanceChangeC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RenderDistanceUpdateS2CPacket.ID, RenderDistanceUpdateS2CPacket.CODEC);

        // Register packet handler on server side
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangeC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            context.server().execute(() -> {
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

                    // Send feedback message to player
                    Text message;
                    if (payload.increase()) {
                        message = Text.translatable("f3f.render_distance.increased", newRenderDistance);
                    } else {
                        message = Text.translatable("f3f.render_distance.decreased", newRenderDistance);
                    }
                    player.sendMessage(message, true); // true = overlay (like subtitle)

                    LOGGER.info("Player {} changed render distance from {} to {}",
                            player.getName().getString(), currentRenderDistance, newRenderDistance);
                } else {
                    // Send message when already at limit
                    Text message;
                    if (payload.increase()) {
                        message = Text.translatable("f3f.render_distance.max", newRenderDistance);
                    } else {
                        message = Text.translatable("f3f.render_distance.min", newRenderDistance);
                    }
                    player.sendMessage(message, true); // true = overlay
                }
            });
        });
    }
}
