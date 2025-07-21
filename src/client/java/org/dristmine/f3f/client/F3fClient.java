package org.dristmine.f3f.client;

import net.fabricmc.api.ClientModInitializer;
import org.dristmine.f3f.F3f;

public class F3fClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        F3f.LOGGER.info("Initializing F3F client mod");
        // The actual F3+F key handling is done through the DebugScreenMixin
        // This ensures proper integration with Minecraft's debug screen system
    }
}