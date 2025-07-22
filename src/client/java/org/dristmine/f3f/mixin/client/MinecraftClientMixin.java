package org.dristmine.f3f.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.dristmine.f3f.client.F3fClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void preventDebugScreen(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
        // Check if we're trying to open any debug-related screen after F3+F usage
        if (screen != null && F3fClient.wasF3FCombinationUsed()) {
            String screenName = screen.getClass().getSimpleName();
            if (screenName.toLowerCase().contains("debug")) {
                ci.cancel();
                F3fClient.resetF3FCombinationFlag();
            }
        }
    }
}
