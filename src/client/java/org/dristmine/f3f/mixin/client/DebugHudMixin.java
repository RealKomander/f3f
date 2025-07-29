package org.dristmine.f3f.mixin.client;

import net.minecraft.client.gui.hud.DebugHud;
import org.dristmine.f3f.client.F3fClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(method = "shouldShowDebugHud", at = @At("HEAD"), cancellable = true)
    private void preventDebugHud(CallbackInfoReturnable<Boolean> cir) {
        if (F3fClient.wasF3FCombinationUsed()) {
            cir.setReturnValue(false);
        }
    }
}
