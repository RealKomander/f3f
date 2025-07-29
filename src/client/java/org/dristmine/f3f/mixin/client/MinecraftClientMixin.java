package org.dristmine.f3f.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.dristmine.f3f.client.F3fClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void preventDebugScreen(Screen screen, CallbackInfo ci) {
        // Check if we're trying to open any debug-related screen after F3+F usage
        if (screen != null && F3fClient.wasF3FCombinationUsed()) {
            String screenName = screen.getClass().getName().toLowerCase();
            // More comprehensive check for debug screens
            if (screenName.contains("debug") || screenName.contains("profiler") ||
                    screenName.contains("chart") || screenName.contains("metrics") ||
                    screenName.contains("overlay") || screenName.contains("hud")) {
                ci.cancel();
                F3fClient.resetF3FCombinationFlag();
            }
        }
    }
}
