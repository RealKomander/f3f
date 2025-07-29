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

    // Simple timing for debug menu prevention
    private static final long DEBUG_PREVENTION_WINDOW = 500; // Increased to 500ms
    private static boolean debugPreventionActive = false;
    private static long debugPreventionStart = 0;
    private static boolean waitingForF3Release = false;

    // Detect if connected server has this mod installed
    private static boolean serverModPresent = false;
    private static long lastServerPacketTime = 0;
    private static final long SERVER_PACKET_TIMEOUT = 5000;

    // Suppress duplicate server messages on client
    private static long lastServerMessageTime = 0;
    private static final long SERVER_MESSAGE_COOLDOWN = 3000;
    private static boolean suppressNextServerMessage = false;

    // Tracking render distance syncing
    private static int lastClientRenderDistance = -1;
    private static boolean serverUpdateReceived = false;
    private static long lastServerUpdate = 0;

    // Fix for singleplayer message priority
    private static boolean localChangeInProgress = false;

    @Override
    public void onInitializeClient() {
        F3fConfig.load();
        registerPacketHandlers();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetServerModState());
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void registerPacketHandlers() {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(RenderDistanceUpdateS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                handleRenderDistanceUpdate(payload.renderDistance(), context.client());
            });
        });
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(RenderDistanceUpdateS2CPacket.ID, (client, handler, buf, responseSender) -> {
            RenderDistanceUpdateS2CPacket packet = RenderDistanceUpdateS2CPacket.read(buf);
            client.execute(() -> {
                handleRenderDistanceUpdate(packet.renderDistance(), client);
            });
        });*/
        //?}
    }

    private void handleRenderDistanceUpdate(int newRenderDistance, MinecraftClient client) {
        if (client.options == null) return;

        serverModPresent = true;
        lastServerPacketTime = System.currentTimeMillis();

        if (newRenderDistance == -1) {
            if (F3fConfig.getInstance().isAutoSyncEnabled()) {
                int currentRenderDistance = client.options.getViewDistance().getValue();
                sendRenderDistanceSync(currentRenderDistance);
            }
        } else {
            client.options.getViewDistance().setValue(newRenderDistance);
            lastClientRenderDistance = newRenderDistance;
            serverUpdateReceived = true;
            lastServerUpdate = System.currentTimeMillis();
            client.options.write();

            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }

            if (client.player != null) {
                // Fix for singleplayer: Don't show server message if local change is in progress
                if (localChangeInProgress) {
                    localChangeInProgress = false; // Reset flag
                    F3fClient.resetF3FCombinationFlag();
                } else if (F3fClient.wasF3FCombinationUsed()) {
                    F3fClient.resetF3FCombinationFlag();
                } else {
                    long now = System.currentTimeMillis();
                    if (!suppressNextServerMessage && (now - lastServerMessageTime > SERVER_MESSAGE_COOLDOWN)) {
                        Text message = TextUtils.createServerRenderDistanceMessage(newRenderDistance);
                        client.player.sendMessage(message, false);
                        lastServerMessageTime = now;
                    }
                    suppressNextServerMessage = false;
                }
            }
        }
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
        localChangeInProgress = false;
        debugPreventionActive = false;
        waitingForF3Release = false;
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

        // Detect F3+F combination press
        if (currentF3State && currentFState && !lastFState) {
            f3fCombinationUsed = true;
            lastF3FUsage = System.currentTimeMillis();
            localChangeInProgress = true;

            boolean hasServerMod = serverModPresent
                    || (System.currentTimeMillis() - lastServerPacketTime) < SERVER_PACKET_TIMEOUT;

            if (currentShiftState) {
                if (hasServerMod) {
                    sendRenderDistanceChange(false);
                } else {
                    changeClientRenderDistance(false, client);
                }
            } else {
                if (hasServerMod) {
                    sendRenderDistanceChange(true);
                } else {
                    changeClientRenderDistance(true, client);
                }
            }
        }

        // Detect F3 release ONLY after F3+F was used
        if (f3fCombinationUsed && lastF3State && !currentF3State) {
            debugPreventionActive = true;
            debugPreventionStart = System.currentTimeMillis();
        }

        // Clean up expired flags
        long now = System.currentTimeMillis();
        if (f3fCombinationUsed && (now - lastF3FUsage) > config.getF3FCooldown()) {
            // Only reset if we're not preventing debug
            if (!debugPreventionActive && !currentF3State) {
                f3fCombinationUsed = false;
            }
        }

        if (debugPreventionActive && (now - debugPreventionStart) > DEBUG_PREVENTION_WINDOW) {
            debugPreventionActive = false;
            f3fCombinationUsed = false; // Reset both flags when timer expires
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
            serverUpdateReceived = false;
        }

        localChangeInProgress = false; // Reset after local change
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
            sendRenderDistanceSync(currentClientRenderDistance);
            lastClientRenderDistance = currentClientRenderDistance;
        }
    }

    private void sendRenderDistanceChange(boolean increase) {
        //? if >=1.20.5 {
        ClientPlayNetworking.send(new RenderDistanceChangeC2SPacket(increase));
        //?} else {
        /*net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        new RenderDistanceChangeC2SPacket(increase).write(buf);
        ClientPlayNetworking.send(RenderDistanceChangeC2SPacket.ID, buf);*/
        //?}
    }

    private void sendRenderDistanceSync(int renderDistance) {
        //? if >=1.20.5 {
        ClientPlayNetworking.send(new RenderDistanceSyncC2SPacket(renderDistance));
        //?} else {
        /*net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        new RenderDistanceSyncC2SPacket(renderDistance).write(buf);
        ClientPlayNetworking.send(RenderDistanceSyncC2SPacket.ID, buf);*/
        //?}
    }

    // Simplified accessors
    public static boolean isF3CurrentlyHeld() {
        return f3Pressed;
    }

    public static boolean wasF3FCombinationUsed() {
        long now = System.currentTimeMillis();

        // Priority 1: If F3+F was used and F3 is still held, always prevent debug
        if (f3fCombinationUsed && f3Pressed) {
            return true;
        }

        // Priority 2: If in debug prevention window after F3 release
        if (debugPreventionActive) {
            return true;
        }

        return false;
    }

    // Simplified reset - only reset when appropriate
    public static void resetF3FCombinationFlag() {
        // Let the timer handle flag management
    }
}
