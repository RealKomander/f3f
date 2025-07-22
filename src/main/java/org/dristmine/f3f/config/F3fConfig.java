package org.dristmine.f3f.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.dristmine.f3f.F3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class F3fConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("f3f.json");

    private static F3fConfig INSTANCE;

    // Configuration fields with defaults
    public String permissionNode = "f3f.change";
    public int minRenderDistance = 2;
    public int maxRenderDistance = 32;
    public boolean enableAutoSync = true;
    public boolean enableF3FKeys = true;
    public int f3fCooldown = 1000;
    public int serverUpdateCooldown = 1000;

    public static F3fConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, F3fConfig.class);
                F3f.LOGGER.info("[F3F] Configuration loaded from {}", CONFIG_PATH);
            } else {
                INSTANCE = new F3fConfig();
                save();
                F3f.LOGGER.info("[F3F] Created default configuration at {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            F3f.LOGGER.error("[F3F] Failed to load configuration, using defaults: {}", e.getMessage());
            INSTANCE = new F3fConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            F3f.LOGGER.error("[F3F] Failed to save configuration: {}", e.getMessage());
        }
    }

    // Getters for easy access
    public String getPermissionNode() { return permissionNode; }
    public int getMinRenderDistance() { return minRenderDistance; }
    public int getMaxRenderDistance() { return maxRenderDistance; }
    public boolean isAutoSyncEnabled() { return enableAutoSync; }
    public boolean areF3FKeysEnabled() { return enableF3FKeys; }
    public int getF3FCooldown() { return f3fCooldown; }
    public int getServerUpdateCooldown() { return serverUpdateCooldown; }
}
