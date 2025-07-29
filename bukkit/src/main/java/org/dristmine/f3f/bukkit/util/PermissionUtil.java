package org.dristmine.f3f.bukkit.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.dristmine.f3f.bukkit.config.BukkitF3fConfig;

public class PermissionUtil {
    private final LuckPerms luckPerms;
    private final BukkitF3fConfig config;

    public PermissionUtil(LuckPerms luckPerms, BukkitF3fConfig config) {
        this.luckPerms = luckPerms;
        this.config = config;
    }

    public boolean canChange(Player player) {
        String permission = config.getPermissionNode();

        // Fallback to Bukkit permissions if LuckPerms not available
        if (luckPerms == null) {
            return player.hasPermission(permission);
        }

        try {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            if (user == null) {
                // Fallback to Bukkit if user not found in LuckPerms
                return player.hasPermission(permission);
            }

            return user.getCachedData().getPermissionData()
                    .checkPermission(permission).asBoolean();

        } catch (Exception e) {
            // Fallback to Bukkit permissions on any error
            return player.hasPermission(permission);
        }
    }
}
