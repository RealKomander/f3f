package org.dristmine.f3f.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.dristmine.f3f.packet.RenderDistanceChangeC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceUpdateS2CPacket;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;


public class F3fClient implements ClientModInitializer {
    private boolean f3Pressed = false;
    private boolean fPressed = false;
    private boolean shiftPressed = false;
    private boolean lastF3State = false;
    private boolean lastFState = false;
    private boolean lastShiftState = false;

    @Override
    public void onInitializeClient() {
        // Register client packet handler
        ClientPlayNetworking.registerGlobalReceiver(RenderDistanceUpdateS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Update client render distance setting
                MinecraftClient client = context.client();
                if (client.options != null) {
                    int oldDistance = client.options.getViewDistance().getValue();
                    client.options.getViewDistance().setValue(payload.renderDistance());

                    // Force chunk reload to apply new render distance
                    if (client.worldRenderer != null) {
                        client.worldRenderer.reload();
                    }

                    // Optional: Additional client-side feedback
                    if (client.player != null && oldDistance != payload.renderDistance()) {
                        Text message = Text.translatable("f3f.render_distance.changed",
                                oldDistance, payload.renderDistance());
                        client.player.sendMessage(message, false); // false = chat message
                    }
                }
            });
        });

        // Register client tick handler for key detection
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) return;

        // Get current key states
        long windowHandle = client.getWindow().getHandle();
        boolean currentF3State = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
        boolean currentFState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        boolean currentShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        // Detect F key press while F3 is held
        if (currentF3State && currentFState && !lastFState) {
            // F was just pressed while F3 is held
            if (currentShiftState) {
                // Shift + F3 + F = decrease render distance
                ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(false));
            } else {
                // F3 + F = increase render distance
                ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(true));
            }
        }

        // Update previous states
        lastF3State = currentF3State;
        lastFState = currentFState;
        lastShiftState = currentShiftState;
    }
}
