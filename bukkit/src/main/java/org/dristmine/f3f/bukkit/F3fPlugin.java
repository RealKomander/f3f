package org.dristmine.f3f.bukkit;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.dristmine.f3f.bukkit.config.BukkitF3fConfig;
import org.dristmine.f3f.bukkit.listener.F3fBukkitListener;
import org.dristmine.f3f.bukkit.listener.F3fPacketListener;
import org.dristmine.f3f.bukkit.util.PermissionUtil;

public class F3fPlugin extends JavaPlugin {
    private LuckPerms luckPerms;
    private F3fPacketListener packetListener;
    private F3fBukkitListener bukkitListener;
    private BukkitF3fConfig f3fConfig;
    private PermissionUtil permissionUtil;

    @Override
    public void onLoad() {
        // FIXED: Correct PacketEvents initialization
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // Load configuration first
        f3fConfig = new BukkitF3fConfig(this);

        // Initialize LuckPerms (optional)
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                luckPerms = LuckPermsProvider.get();
                getLogger().info(f3fConfig.getLogMessage("luckperms_enabled"));
            } catch (Exception e) {
                getLogger().warning(f3fConfig.getLogMessage("luckperms_error", e.getMessage()));
            }
        } else {
            getLogger().info(f3fConfig.getLogMessage("luckperms_not_detected"));
        }

        // Initialize permission utility
        permissionUtil = new PermissionUtil(luckPerms, f3fConfig);

        // Initialize listeners
        packetListener = new F3fPacketListener(this, permissionUtil, f3fConfig);
        bukkitListener = new F3fBukkitListener(this, permissionUtil, f3fConfig, packetListener);

        // FIXED: Register listeners separately
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        getServer().getPluginManager().registerEvents(bukkitListener, this);

        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        getLogger().info(f3fConfig.getLogMessage("initialization_complete"));
    }

    @Override
    public void onDisable() {
        // Terminate PacketEvents
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("F3F Bukkit plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("f3freload")) {
            if (!sender.hasPermission("f3f.reload")) {
                sender.sendMessage("§cYou don't have permission to reload F3F configuration!");
                return true;
            }

            f3fConfig.loadConfig();
            sender.sendMessage("§aF3F configuration reloaded successfully!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("f3finfo")) {
            sender.sendMessage("§6=== F3F Plugin Information ===");
            sender.sendMessage("§7Version: §f" + getDescription().getVersion());
            sender.sendMessage("§7Permission Node: §f" + f3fConfig.getPermissionNode());
            sender.sendMessage("§7Render Distance Range: §f" + f3fConfig.getMinRenderDistance() + "-" + f3fConfig.getMaxRenderDistance());
            sender.sendMessage("§7F3+F Keys: §f" + (f3fConfig.areF3FKeysEnabled() ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("§7Auto-Sync: §f" + (f3fConfig.isAutoSyncEnabled() ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("§7LuckPerms: §f" + (luckPerms != null ? "§aConnected" : "§cNot Available"));
            sender.sendMessage("§7PacketEvents: §aEnabled");
            return true;
        }

        return false;
    }

    // Getters for other classes
    public LuckPerms getLuckPerms() { return luckPerms; }
    public BukkitF3fConfig getF3fConfig() { return f3fConfig; }
}
