package org.dristmine.f3f;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F3f implements ModInitializer {
    public static final String MOD_ID = "f3f";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing F3F mod - render distance packet handler");

        // Register the client-to-server payload
        PayloadTypeRegistry.playC2S().register(RenderDistanceChangePayload.ID, RenderDistanceChangePayload.CODEC);

        // Register packet receiver on server side
        ServerPlayNetworking.registerGlobalReceiver(RenderDistanceChangePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            // Get current server render distance
            int currentRenderDistance = player.getServer().getPlayerManager().getViewDistance();
            int maxRenderDistance = player.getServer().getPlayerManager().getViewDistance(); // Server's max view distance
            int newRenderDistance;

            if (payload.increase()) {
                // Increase render distance (max is server's view distance, usually 10-32)
                newRenderDistance = Math.min(currentRenderDistance + 1, Math.min(32, maxRenderDistance));
            } else {
                // Decrease render distance (min 2 chunks)
                newRenderDistance = Math.max(currentRenderDistance - 1, 2);
            }

            // Apply the new render distance if it changed
            if (newRenderDistance != currentRenderDistance) {
                // Set the view distance for the server
                player.getServer().getPlayerManager().setViewDistance(newRenderDistance);

                LOGGER.info("Player {} changed server render distance from {} to {} chunks",
                        player.getName().getString(), currentRenderDistance, newRenderDistance);

                // You might want to send a confirmation message to the player
                player.sendMessage(Text.of(
                        String.format("Render distance %s to %d chunks",
                                payload.increase() ? "increased" : "decreased", newRenderDistance)
                ));
            } else {
                // Notify player if at limits
                if (payload.increase() && currentRenderDistance >= Math.min(32, maxRenderDistance)) {
                    player.sendMessage(Text.of("Render distance already at maximum (" + currentRenderDistance + " chunks)"));
                } else if (!payload.increase() && currentRenderDistance <= 2) {
                    player.sendMessage(Text.of("Render distance already at minimum (2 chunks)"));
                }
            }
        });
    }
}