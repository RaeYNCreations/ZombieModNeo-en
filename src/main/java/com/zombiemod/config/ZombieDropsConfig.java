package com.zombiemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ZombieDropsConfig {

    public static class DropEntry {
        public String item; // Item ID (e.g., "minecraft:gunpowder)
        public double chance; // Drop chance (0.0 to 1.0)
        public int minCount; // Minimum number of items to drop
        public int maxCount; // Maximum number of items to drop
        public boolean enabled; // Enable/disable this drop

        public DropEntry(String item, double chance, int minCount, int maxCount, boolean enabled) {
            this.item = item;
            this.chance = chance;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.enabled = enabled;
        }
    }

    // Configuration par défaut
    private List<DropEntry> drops = new ArrayList<>();

    private static ZombieDropsConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public ZombieDropsConfig() {
        // Drops par défaut (désactivés)
        drops.add(new DropEntry("minecraft:gunpowder", 0.5, 1, 3, false));
        drops.add(new DropEntry("minecraft:copper_ingot", 0.3, 1, 2, false));
    }

    public static void init(File configDir) {
        configFile = new File(configDir, "zombiedrops.json");

        if (!configFile.exists()) {
            instance = new ZombieDropsConfig();
            save();
        } else {
            load();
        }
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile)) {
            instance = GSON.fromJson(reader, ZombieDropsConfig.class);
            if (instance == null) {
                instance = new ZombieDropsConfig();
            }
            System.out.println("[ZombieMod] Drop configuration loaded from " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("[ZombieMod] Error loading drops configuration, using default values");
            instance = new ZombieDropsConfig();
        }
    }

    public static void save() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(instance, writer);
            }
            System.out.println("[ZombieMod] Drop configuration saved in " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("[ZombieMod] Error saving Drops configuration");
            e.printStackTrace();
        }
    }

    public static ZombieDropsConfig get() {
        if (instance == null) {
            instance = new ZombieDropsConfig();
        }
        return instance;
    }

    // Getters
    public List<DropEntry> getDrops() {
        return drops;
    }

    // Setters (si besoin de modifier via commandes)
    public void addDrop(String item, double chance, int minCount, int maxCount, boolean enabled) {
        drops.add(new DropEntry(item, chance, minCount, maxCount, enabled));
        save();
    }

    public void clearDrops() {
        drops.clear();
        save();
    }
}
