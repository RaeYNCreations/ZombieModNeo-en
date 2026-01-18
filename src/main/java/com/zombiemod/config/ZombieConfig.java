package com.zombiemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ZombieConfig {

    // Configuration par défaut
    private int maxZombiesOnMap = 32;
    private double spawnDelaySeconds = 2.0;
    private double mobFollowRange = 32.0; // Player detection range in blocks (all mobs)
    private double armoredZombieChance = 0.15; // 15% chance to spawn with armor
    private int waveTimeoutSeconds = 50; // Maximum time to finish a wave (0 = disabled)
    private int glowingZombiesCount = 5; // Number of last zombies with the glowing effect

    private static ZombieConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static void init(File configDir) {
        configFile = new File(configDir, "zombiemod.json");

        if (!configFile.exists()) {
            instance = new ZombieConfig();
            save();
        } else {
            load();
        }
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile)) {
            instance = GSON.fromJson(reader, ZombieConfig.class);
            if (instance == null) {
                instance = new ZombieConfig();
            }
            System.out.println("[ZombieMod] Configuration loaded from " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("[ZombieMod] Error loading configuration, using default values");
            instance = new ZombieConfig();
        }
    }

    public static void save() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(instance, writer);
            }
            System.out.println("[ZombieMod] Configuration saved in " + configFile.getPath());
        } catch (IOException e) {
            System.err.println("[ZombieMod] Error saving configuration");
            e.printStackTrace();
        }
    }

    public static ZombieConfig get() {
        if (instance == null) {
            instance = new ZombieConfig();
        }
        return instance;
    }

    // Getters
    public int getMaxZombiesOnMap() {
        return maxZombiesOnMap;
    }

    public double getSpawnDelaySeconds() {
        return spawnDelaySeconds;
    }

    public int getSpawnDelayTicks() {
        return (int) (spawnDelaySeconds * 20);
    }

    public double getMobFollowRange() {
        return mobFollowRange;
    }

    public double getArmoredZombieChance() {
        return armoredZombieChance;
    }

    public int getWaveTimeoutSeconds() {
        return waveTimeoutSeconds;
    }

    public int getWaveTimeoutTicks() {
        return waveTimeoutSeconds * 20;
    }

    public int getGlowingZombiesCount() {
        return glowingZombiesCount;
    }

    // Setters (pour modification via commandes si besoin)
    public void setMaxZombiesOnMap(int value) {
        this.maxZombiesOnMap = value;
        save();
    }

    public void setSpawnDelaySeconds(double value) {
        this.spawnDelaySeconds = value;
        save();
    }
}
