package org.dristmine.f3f.mixin.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import org.dristmine.f3f.F3f;
import org.dristmine.f3f.RenderDistanceChangePayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public class DebugScreenMixin {

    @Shadow @Final private MinecraftClient client;

    private static long lastF3FActionTime = 0;
    private static final long ACTION_COOLDOWN = 500; // 500ms cooldown

    @Inject(method = "processF3", at = @At("HEAD"), cancellable = true)
    private void onProcessF3(int key, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is F3+F (key F is 70)
        if (key == 70) { // GLFW.GLFW_KEY_F = 70
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastF3FActionTime > ACTION_COOLDOWN) {
                boolean isShiftPressed = false;

                // Check if shift is pressed (we need to detect Shift+F3+F vs F3+F)
                long handle = this.client.getWindow().getHandle();
                if (org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS ||
                        org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    isShiftPressed = true;
                }

                // F3+F increases render distance, Shift+F3+F decreases it
                boolean increase = !isShiftPressed;

                // Send packet to server
                RenderDistanceChangePayload payload = new RenderDistanceChangePayload(increase);
                ClientPlayNetworking.send(payload);

                F3f.LOGGER.info("F3F: Sent render distance {} packet", increase ? "increase" : "decrease");
                lastF3FActionTime = currentTime;

                // Cancel the original F3+F behavior and indicate we handled this key
                cir.setReturnValue(true);
            } else {
                // Still in cooldown, but cancel the original behavior
                cir.setReturnValue(true);
            }
        }
    }
}