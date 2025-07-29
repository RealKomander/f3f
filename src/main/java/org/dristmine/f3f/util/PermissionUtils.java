package org.dristmine.f3f.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.network.ServerPlayerEntity;
import org.dristmine.f3f.F3f;
import org.dristmine.f3f.config.F3fConfig;

public final class PermissionUtils {
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
            F3f.LOGGER.info(TextUtils.getLogMessage("f3f.log.luckperms_not_detected"));
            return;
        }

        try {
            luckPerms = LuckPermsProvider.get();
            luckPermsAvailable = true;
            F3f.LOGGER.info(TextUtils.getLogMessage("f3f.log.luckperms_enabled"));
        } catch (IllegalStateException ex) {
            F3f.LOGGER.warn(TextUtils.getLogMessage("f3f.log.luckperms_not_initialized"));
            luckPerms = null;
            luckPermsAvailable = false;
        } catch (Exception ex) {
            F3f.LOGGER.error(TextUtils.getLogMessage("f3f.log.luckperms_error", ex.getMessage()));
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

            // Use configurable permission node
            String permissionNode = F3fConfig.getInstance().getPermissionNode();
            return user.getCachedData().getPermissionData()
                    .checkPermission(permissionNode).asBoolean();

        } catch (Exception ex) {
            F3f.LOGGER.error(TextUtils.getLogMessage("f3f.log.permission_error",
                    player.getName().getString(), ex.getMessage()));
            return false;
        }
    }

    public static boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }
}
