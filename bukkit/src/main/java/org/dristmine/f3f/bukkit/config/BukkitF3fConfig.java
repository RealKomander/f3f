package org.dristmine.f3f.bukkit.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BukkitF3fConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    // Cached values for performance
    private String permissionNode;
    private int minRenderDistance;
    private int maxRenderDistance;
    private boolean enableAutoSync;
    private boolean enableF3FKeys;
    private int f3fCooldown;
    private int serverUpdateCooldown;

    // Message maps
    private Map<String, String> messages;
    private Map<String, String> logMessages;

    public BukkitF3fConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Reload config from file
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Cache basic values
        this.permissionNode = config.getString("permission-node", "f3f.change");
        this.minRenderDistance = config.getInt("min-render-distance", 2);
        this.maxRenderDistance = config.getInt("max-render-distance", 32);
        this.enableAutoSync = config.getBoolean("enable-auto-sync", true);
        this.enableF3FKeys = config.getBoolean("enable-f3f-keys", true);
        this.f3fCooldown = config.getInt("f3f-cooldown", 100);
        this.serverUpdateCooldown = config.getInt("server-update-cooldown", 1000);

        // Load messages
        loadMessages();
        loadLogMessages();

        plugin.getLogger().info(getLogMessage("config_loaded"));
    }

    private void loadMessages() {
        messages = new HashMap<>();

        // Load messages from config with defaults
        messages.put("permission_denied", config.getString("messages.permission_denied", "&l&e[Debug]: &rUnable to change render distance; no permission"));
        messages.put("render_distance", config.getString("messages.render_distance", "&l&e[Debug]: &rRender Distance: %d"));
        messages.put("server_render_distance", config.getString("messages.server_render_distance", "&l&e[Debug]: &rServer Render Distance: %d"));
        messages.put("client_render_distance", config.getString("messages.client_render_distance", "&l&e[Debug]: &rClient Render Distance: %d"));
        messages.put("render_distance_changed", config.getString("messages.render_distance_changed", "&aRender distance changed to %d"));
        messages.put("render_distance_changed_by", config.getString("messages.render_distance_changed_by", "&6%s changed render distance to %d"));
        messages.put("render_distance_at_maximum", config.getString("messages.render_distance_at_maximum", "&eRender distance already at maximum (%d)"));
        messages.put("render_distance_at_minimum", config.getString("messages.render_distance_at_minimum", "&eRender distance already at minimum (%d)"));
    }

    private void loadLogMessages() {
        logMessages = new HashMap<>();

        // Load log messages from config with defaults
        logMessages.put("initializing", config.getString("log_messages.initializing", "Initializing F3F plugin"));
        logMessages.put("initialization_complete", config.getString("log_messages.initialization_complete", "F3F plugin initialization complete"));
        logMessages.put("server_started", config.getString("log_messages.server_started", "Server started, initializing LuckPerms integration..."));
        logMessages.put("luckperms_not_detected", config.getString("log_messages.luckperms_not_detected", "LuckPerms not detected - using Bukkit permissions"));
        logMessages.put("luckperms_enabled", config.getString("log_messages.luckperms_enabled", "LuckPerms integration enabled"));
        logMessages.put("luckperms_not_initialized", config.getString("log_messages.luckperms_not_initialized", "LuckPerms present but API not initialized - using Bukkit permissions"));
        logMessages.put("luckperms_error", config.getString("log_messages.luckperms_error", "Error initializing LuckPerms: %s"));
        logMessages.put("permission_error", config.getString("log_messages.permission_error", "Error checking permissions for player %s: %s"));
        logMessages.put("player_denied", config.getString("log_messages.player_denied", "Player %s attempted change – denied (no permission)"));
        logMessages.put("render_distance_changed", config.getString("log_messages.render_distance_changed", "Player %s changed render distance from %d to %d (F3+F)"));
        logMessages.put("render_distance_max", config.getString("log_messages.render_distance_max", "Player %s attempted to increase render distance but already at maximum (%d)"));
        logMessages.put("render_distance_min", config.getString("log_messages.render_distance_min", "Player %s attempted to decrease render distance but already at minimum (%d)"));
        logMessages.put("auto_sync", config.getString("log_messages.auto_sync", "Auto-synced render distance for player %s from %d to %d (options change)"));
        logMessages.put("config_loaded", config.getString("log_messages.config_loaded", "Configuration loaded successfully"));
        logMessages.put("config_created", config.getString("log_messages.config_created", "Created default configuration"));
        logMessages.put("config_load_error", config.getString("log_messages.config_load_error", "Failed to load configuration, using defaults: %s"));
        logMessages.put("config_save_error", config.getString("log_messages.config_save_error", "Failed to save configuration: %s"));
        logMessages.put("render_distance_changed_all", config.getString("log_messages.render_distance_changed_all", "Player %s changed render distance from %d to %d"));
        logMessages.put("player_joined", config.getString("log_messages.player_joined", "Player %s joined"));
        logMessages.put("auto_sync_disabled", config.getString("log_messages.auto_sync_disabled", "Auto-sync disabled"));
        logMessages.put("no_permission_for_sync", config.getString("log_messages.no_permission_for_sync", "Player %s doesn't have permission for sync"));
        logMessages.put("requesting_sync", config.getString("log_messages.requesting_sync", "Requesting render distance sync from %s"));
        logMessages.put("f3f_keys_disabled", config.getString("log_messages.f3f_keys_disabled", "F3+F keys are disabled in config, ignoring request from %s"));
        logMessages.put("invalid_packet_data", config.getString("log_messages.invalid_packet_data", "Invalid %s packet data from %s"));
        logMessages.put("cooldown_active", config.getString("log_messages.cooldown_active", "F3+F cooldown active for %s"));
        logMessages.put("f3f_command_received", config.getString("log_messages.f3f_command_received", "F3+F command: %s from %s"));
        logMessages.put("client_sync_request", config.getString("log_messages.client_sync_request", "Client sync request: %d from %s"));
        logMessages.put("sync_matches_current", config.getString("log_messages.sync_matches_current", "Client sync request matches current server distance (%d)"));
        logMessages.put("server_adopted_distance", config.getString("log_messages.server_adopted_distance", "Server adopted render distance %d from client %s (was %d)"));
    }

    // Message methods with placeholder support
    public String getMessage(String key, Object... args) {
        String message = messages.getOrDefault(key, "Missing message: " + key);
        if (args.length > 0) {
            message = String.format(message, args);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLogMessage(String key, Object... args) {
        String message = logMessages.getOrDefault(key, "Missing log message: " + key);
        if (args.length > 0) {
            message = String.format(message, args);
        }
        return message;
    }

    public void saveConfig() {
        // Update config values
        config.set("permission-node", permissionNode);
        config.set("min-render-distance", minRenderDistance);
        config.set("max-render-distance", maxRenderDistance);
        config.set("enable-auto-sync", enableAutoSync);
        config.set("enable-f3f-keys", enableF3FKeys);
        config.set("f3f-cooldown", f3fCooldown);
        config.set("server-update-cooldown", serverUpdateCooldown);

        plugin.saveConfig();
    }

    // Existing getters remain the same
    public String getPermissionNode() { return permissionNode; }
    public int getMinRenderDistance() { return minRenderDistance; }
    public int getMaxRenderDistance() { return maxRenderDistance; }
    public boolean isAutoSyncEnabled() { return enableAutoSync; }
    public boolean areF3FKeysEnabled() { return enableF3FKeys; }
    public int getF3FCooldown() { return f3fCooldown; }
    public int getServerUpdateCooldown() { return serverUpdateCooldown; }

    // Existing setters remain the same
    public void setPermissionNode(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    public void setMinRenderDistance(int minRenderDistance) {
        this.minRenderDistance = minRenderDistance;
    }

    public void setMaxRenderDistance(int maxRenderDistance) {
        this.maxRenderDistance = maxRenderDistance;
    }

    public void setAutoSyncEnabled(boolean enableAutoSync) {
        this.enableAutoSync = enableAutoSync;
    }

    public void setF3FKeysEnabled(boolean enableF3FKeys) {
        this.enableF3FKeys = enableF3FKeys;
    }

    public void setF3FCooldown(int f3fCooldown) {
        this.f3fCooldown = f3fCooldown;
    }

    public void setServerUpdateCooldown(int serverUpdateCooldown) {
        this.serverUpdateCooldown = serverUpdateCooldown;
    }
}
