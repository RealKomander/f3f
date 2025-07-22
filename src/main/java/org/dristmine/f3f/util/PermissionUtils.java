package org.dristmine.f3f.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.network.ServerPlayerEntity;
import org.dristmine.f3f.F3f;

public final class PermissionUtils {
    private static final String CHANGE_NODE = "f3f.change";
    private static LuckPerms luckPerms;
    private static boolean luckPermsAvailable = false;
    private static boolean initializeAttempted = false;

    private PermissionUtils() {}

    public static void initialize() {
        if (initializeAttempted) {
            return;
        }
        initializeAttempted = true;

        // Never touch LP classes on the physical client
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return;
        }

        // Check if LuckPerms mod is present
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            F3f.LOGGER.info("[F3F] LuckPerms not detected - permission checks disabled");
            return;
        }

        try {
            luckPerms = LuckPermsProvider.get();
            luckPermsAvailable = true;
            F3f.LOGGER.info("[F3F] LuckPerms integration enabled");
        } catch (IllegalStateException ex) {
            F3f.LOGGER.warn("[F3F] LuckPerms present but API not initialized - permission checks disabled");
            luckPerms = null;
            luckPermsAvailable = false;
        } catch (Exception ex) {
            F3f.LOGGER.error("[F3F] Error initializing LuckPerms: {}", ex.getMessage());
            luckPerms = null;
            luckPermsAvailable = false;
        }
    }

    public static boolean canChange(ServerPlayerEntity player) {
        // If LuckPerms is not available, allow everyone (fallback behavior)
        if (!luckPermsAvailable || luckPerms == null) {
            return true;
        }

        try {
            User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
            if (user == null) {
                return false;
            }

            return user.getCachedData().getPermissionData()
                    .checkPermission(CHANGE_NODE).asBoolean();

        } catch (Exception ex) {
            F3f.LOGGER.error("[F3F] Error checking permissions for player {}: {}",
                    player.getName().getString(), ex.getMessage());
            return false;
        }
    }

    public static boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }
}
