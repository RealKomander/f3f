package org.dristmine.f3f.mixin.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.dristmine.f3f.client.F3fClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {

    @Shadow
    private InputUtil.Key boundKey;

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void wasPressed(CallbackInfoReturnable<Boolean> cir) {
        // If this keybind uses the F key and F3 is currently held, cancel it
        if (boundKey.getCategory() == InputUtil.Type.KEYSYM &&
                boundKey.getCode() == GLFW.GLFW_KEY_F &&
                F3fClient.isF3CurrentlyHeld()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> ci) {
        // If this keybind uses the F key and F3 is currently held, cancel it
        if (boundKey.getCategory() == InputUtil.Type.KEYSYM &&
                boundKey.getCode() == GLFW.GLFW_KEY_F &&
                F3fClient.isF3CurrentlyHeld()) {
            ci.setReturnValue(false);
        }
    }
}
