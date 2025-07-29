package org.dristmine.f3f.mixin.client;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.dristmine.f3f.client.F3fClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardHandlerMixin {

    @Shadow
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // Handle F key when F3 is held - prevent all F key actions
        if (key == GLFW.GLFW_KEY_F && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS) {
                ci.cancel(); // This prevents the F key from doing anything when F3 is held
                return;
            }
        }

        // Handle F3 key release - prevent debug screen if F3+F was used
        if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_RELEASE) {
            if (F3fClient.wasF3FCombinationUsed()) {
                ci.cancel(); // Cancel the F3 release to prevent debug screen
                F3fClient.resetF3FCombinationFlag();
            }
        }
    }
}
