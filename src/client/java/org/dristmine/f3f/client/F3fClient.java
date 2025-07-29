package org.dristmine.f3f.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.dristmine.f3f.config.F3fConfig;
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

    // Detect if connected server has this mod installed
    private static boolean serverModPresent = false;
    private static long lastServerPacketTime = 0;
    private static final long SERVER_PACKET_TIMEOUT = 5000; // milliseconds timeout to reset mod presence

    // Suppress duplicate server messages on client
    private static long lastServerMessageTime = 0;
    private static final long SERVER_MESSAGE_COOLDOWN = 3000; // ms cooldown to reduce message spam
    private static boolean suppressNextServerMessage = false;

    // Tracking render distance syncing
    private static int lastClientRenderDistance = -1;
    private static boolean serverUpdateReceived = false;
    private static long lastServerUpdate = 0;

    @Override
    public void onInitializeClient() {
        F3fConfig.load();

        // Register packet handler for render distance updates from the server
        ClientPlayNetworking.registerGlobalReceiver(RenderDistanceUpdateS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.options == null) return;

                serverModPresent = true;
                lastServerPacketTime = System.currentTimeMillis();

                int newRenderDistance = payload.renderDistance();

                if (newRenderDistance == -1) {
                    // Server requests current client render distance (auto-sync on join)
                    if (F3fConfig.getInstance().isAutoSyncEnabled()) {
                        int currentRenderDistance = client.options.getViewDistance().getValue();
                        ClientPlayNetworking.send(new RenderDistanceSyncC2SPacket(currentRenderDistance));
                    }
                } else {
                    // Update client render distance value
                    client.options.getViewDistance().setValue(newRenderDistance);

                    // Update tracking vars to avoid reacting to server-induced changes
                    lastClientRenderDistance = newRenderDistance;
                    serverUpdateReceived = true;
                    lastServerUpdate = System.currentTimeMillis();

                    client.options.write();

                    if (client.worldRenderer != null) {
                        client.worldRenderer.reload();
                    }

                    if (client.player != null) {
                        if (F3fClient.wasF3FCombinationUsed()) {
                            // Server already sent message for manual change, suppress duplicate
                            F3fClient.resetF3FCombinationFlag();
                        } else {
                            long now = System.currentTimeMillis();
                            if (suppressNextServerMessage) {
                                suppressNextServerMessage = false;
                            } else if (now - lastServerMessageTime > SERVER_MESSAGE_COOLDOWN) {
                                Text message = TextUtils.createServerRenderDistanceMessage(newRenderDistance);
                                client.player.sendMessage(message, false);
                                lastServerMessageTime = now;
                                suppressNextServerMessage = true;
                            }
                        }
                    }
                }
            });
        });

        // Reset server mod presence and related flags on disconnect from any server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetServerModState());

        // Register client tick for key handling and auto-sync
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void resetServerModState() {
        serverModPresent = false;
        lastServerPacketTime = 0;
        suppressNextServerMessage = false;
        lastServerMessageTime = 0;
        serverUpdateReceived = false;
        lastServerUpdate = 0;
        lastClientRenderDistance = -1;
        f3fCombinationUsed = false;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) return;

        if (F3fConfig.getInstance().areF3FKeysEnabled()) {
            handleF3FKeys(client);
        }

        if (F3fConfig.getInstance().isAutoSyncEnabled()) {
            handleAutoSync(client);
        }
    }

    private void handleF3FKeys(MinecraftClient client) {
        F3fConfig config = F3fConfig.getInstance();

        long windowHandle = client.getWindow().getHandle();
        boolean currentF3State = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
        boolean currentFState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        boolean currentShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        f3Pressed = currentF3State;

        if (currentF3State && currentFState && !lastFState) {
            f3fCombinationUsed = true;
            lastF3FUsage = System.currentTimeMillis();

            boolean hasServerMod = serverModPresent
                    || (System.currentTimeMillis() - lastServerPacketTime) < SERVER_PACKET_TIMEOUT;

            if (currentShiftState) {
                if (hasServerMod) {
                    ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(false));
                } else {
                    changeClientRenderDistance(false, client);
                }
            } else {
                if (hasServerMod) {
                    ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(true));
                } else {
                    changeClientRenderDistance(true, client);
                }
            }
        }

        if (f3fCombinationUsed && (System.currentTimeMillis() - lastF3FUsage) > config.getF3FCooldown()) {
            f3fCombinationUsed = false;
        }

        lastF3State = currentF3State;
        lastFState = currentFState;
    }

    private void changeClientRenderDistance(boolean increase, MinecraftClient client) {
        if (client.options == null) return;

        int current = client.options.getViewDistance().getValue();
        F3fConfig config = F3fConfig.getInstance();

        int min = config.getMinRenderDistance();
        int max = config.getMaxRenderDistance();

        int newValue = increase ? Math.min(current + 1, max) : Math.max(current - 1, min);

        if (newValue != current) {
            client.options.getViewDistance().setValue(newValue);
            client.options.write();

            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }

            if (client.player != null) {
                Text msg = TextUtils.createClientRenderDistanceMessage(newValue);
                client.player.sendMessage(msg, false);
            }

            lastClientRenderDistance = newValue;
            serverUpdateReceived = false; // Mark local change

            f3fCombinationUsed = true;
            lastF3FUsage = System.currentTimeMillis();
        }
    }

    private void handleAutoSync(MinecraftClient client) {
        if (client.options == null) return;

        F3fConfig config = F3fConfig.getInstance();

        if (serverUpdateReceived && (System.currentTimeMillis() - lastServerUpdate) < config.getServerUpdateCooldown()) {
            return;
        }

        if (serverUpdateReceived && (System.currentTimeMillis() - lastServerUpdate) >= config.getServerUpdateCooldown()) {
            serverUpdateReceived = false;
        }

        int currentClientRenderDistance = client.options.getViewDistance().getValue();

        if (lastClientRenderDistance == -1) {
            lastClientRenderDistance = currentClientRenderDistance;
            return;
        }

        if (currentClientRenderDistance != lastClientRenderDistance && !serverUpdateReceived) {
            ClientPlayNetworking.send(new RenderDistanceSyncC2SPacket(currentClientRenderDistance));
            lastClientRenderDistance = currentClientRenderDistance;
        }
    }

    // Accessors for mixins or other classes
    public static boolean isF3CurrentlyHeld() {
        return f3Pressed;
    }

    public static boolean wasF3FCombinationUsed() {
        F3fConfig config = F3fConfig.getInstance();
        return f3fCombinationUsed && (System.currentTimeMillis() - lastF3FUsage) < config.getF3FCooldown();
    }

    public static void resetF3FCombinationFlag() {
        f3fCombinationUsed = false;
    }
}
