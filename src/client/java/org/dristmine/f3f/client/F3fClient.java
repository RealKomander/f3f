package org.dristmine.f3f.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.dristmine.f3f.packet.RenderDistanceChangeC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceSyncC2SPacket;
import org.dristmine.f3f.packet.RenderDistanceUpdateS2CPacket;
import org.dristmine.f3f.util.TextUtils;
import org.lwjgl.glfw.GLFW;

public class F3fClient implements ClientModInitializer {
    private static boolean f3Pressed = false;
    private static boolean lastF3State = false;
    private static boolean lastFState = false;
    private static boolean f3fCombinationUsed = false;
    private static long lastF3FUsage = 0;
    private static final long F3F_COOLDOWN = 1000;

    // Auto-sync tracking
    private static int lastClientRenderDistance = -1;
    private static boolean serverUpdateReceived = false;
    private static long lastServerUpdate = 0;
    private static final long SERVER_UPDATE_COOLDOWN = 1000; // 1 second cooldown after server update

    @Override
    public void onInitializeClient() {
        // Register client packet handler
        ClientPlayNetworking.registerGlobalReceiver(RenderDistanceUpdateS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.options == null) return;

                if (payload.renderDistance() == -1) {
                    // Server is requesting current render distance (auto-sync on join)
                    int currentRenderDistance = client.options.getViewDistance().getValue();
                    ClientPlayNetworking.send(new RenderDistanceSyncC2SPacket(currentRenderDistance));
                } else {
                    // Normal render distance update from server
                    int newRenderDistance = payload.renderDistance();

                    // Set the client render distance
                    client.options.getViewDistance().setValue(newRenderDistance);

                    // Update our tracking variables to prevent auto-sync interference
                    lastClientRenderDistance = newRenderDistance;
                    serverUpdateReceived = true;
                    lastServerUpdate = System.currentTimeMillis();

                    // Save the options to ensure the change persists
                    client.options.write();

                    // Force chunk reload to apply new render distance
                    if (client.worldRenderer != null) {
                        client.worldRenderer.reload();
                    }

                    // Show chat message for manual F3+F changes only
                    if (client.player != null) {
                        Text message = TextUtils.createRenderDistanceMessage(newRenderDistance);
                        client.player.sendMessage(message, false); // Chat message
                    }
                }
            });
        });

        // Register client tick handler for key detection and auto-sync monitoring
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) return;

        // Handle F3+F key combinations (existing code)
        handleF3FKeys(client);

        // Handle auto-sync when render distance changes in options (but not immediately after server updates)
        handleAutoSync(client);
    }

    private void handleF3FKeys(MinecraftClient client) {
        // Get current key states
        long windowHandle = client.getWindow().getHandle();
        boolean currentF3State = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
        boolean currentFState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        boolean currentShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        // Update F3 state tracking
        f3Pressed = currentF3State;

        // Detect F key press while F3 is held
        if (currentF3State && currentFState && !lastFState) {
            // F was just pressed while F3 is held
            f3fCombinationUsed = true;
            lastF3FUsage = System.currentTimeMillis();

            if (currentShiftState) {
                // Shift + F3 + F = decrease render distance
                ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(false));
            } else {
                // F3 + F = increase render distance
                ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(true));
            }
        }

        // Auto-reset the flag after cooldown period
        if (f3fCombinationUsed && (System.currentTimeMillis() - lastF3FUsage) > F3F_COOLDOWN) {
            f3fCombinationUsed = false;
        }

        // Update previous states
        lastF3State = currentF3State;
        lastFState = currentFState;
    }

    private void handleAutoSync(MinecraftClient client) {
        if (client.options == null) return;

        // Don't auto-sync immediately after receiving a server update
        if (serverUpdateReceived && (System.currentTimeMillis() - lastServerUpdate) < SERVER_UPDATE_COOLDOWN) {
            return;
        }

        // Reset the server update flag after cooldown
        if (serverUpdateReceived && (System.currentTimeMillis() - lastServerUpdate) >= SERVER_UPDATE_COOLDOWN) {
            serverUpdateReceived = false;
        }

        int currentClientRenderDistance = client.options.getViewDistance().getValue();

        // Initialize tracking on first tick
        if (lastClientRenderDistance == -1) {
            lastClientRenderDistance = currentClientRenderDistance;
            return;
        }

        // Check if render distance changed in options (and it's not from a recent server update)
        if (currentClientRenderDistance != lastClientRenderDistance && !serverUpdateReceived) {
            // Send auto-sync packet (silently)
            ClientPlayNetworking.send(new RenderDistanceSyncC2SPacket(currentClientRenderDistance));
            lastClientRenderDistance = currentClientRenderDistance;
        }
    }

    // Public static methods for mixins to access
    public static boolean isF3CurrentlyHeld() {
        return f3Pressed;
    }

    public static boolean wasF3FCombinationUsed() {
        return f3fCombinationUsed && (System.currentTimeMillis() - lastF3FUsage) < F3F_COOLDOWN;
    }

    public static void resetF3FCombinationFlag() {
        f3fCombinationUsed = false;
    }
}
