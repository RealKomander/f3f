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
    private static LuckPerms luckPerms;   // null = LP not available

    private PermissionUtils() {}

    public static void initialize() {
        /* ➊ Never touch LP classes on the physical client */
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return;
        }

        /* ➋ Only proceed if the LuckPerms mod is sitting in /mods */
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            F3f.LOGGER.info("[F3F] LuckPerms not detected – all players will be allowed.");
            return;
        }

        try {
            luckPerms = LuckPermsProvider.get();                    // dedicated-server only
            F3f.LOGGER.info("[F3F] LuckPerms integration enabled.");
        } catch (IllegalStateException ex) {                        // LP jar there but not ready
            F3f.LOGGER.warn("[F3F] LuckPerms present but API not initialised – ignoring.");
        }
    }

    public static boolean canChange(ServerPlayerEntity player) {
        if (luckPerms == null) return true;                         // LP missing → allow
        User user = luckPerms.getPlayerAdapter(ServerPlayerEntity.class).getUser(player);
        return user.getCachedData().getPermissionData()
                .checkPermission(CHANGE_NODE).asBoolean();
    }
}
