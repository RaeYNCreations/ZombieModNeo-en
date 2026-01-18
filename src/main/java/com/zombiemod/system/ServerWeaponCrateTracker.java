package com.zombiemod.system;

import com.zombiemod.network.NetworkHandler;
import com.zombiemod.network.packet.WeaponCrateSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracker côté serveur pour les weapon crates
 * Garde une map des positions et coûts et synchronise avec les clients
 * Sauvegarde de manière persistante via WeaponCrateSavedData
 */
public class ServerWeaponCrateTracker {

    private static Map<BlockPos, Integer> weaponCrates = new HashMap<>();
    private static ServerLevel currentLevel = null;

    /**
     * Initialise le tracker avec le niveau serveur (appelé au démarrage)
     */
    public static void initialize(ServerLevel level) {
        currentLevel = level;
        loadFromSavedData();
    }

    /**
     * Charge les données depuis la sauvegarde persistante
     */
    public static void loadFromSavedData() {
        if (currentLevel == null) {
            System.err.println("[ServerWeaponCrateTracker] Unable to load: Current level is null!");
            return;
        }

        WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
        weaponCrates = savedData.getAllCrates();

        System.out.println("[ServerWeaponCrateTracker] Loaded " + weaponCrates.size() + " weapon crates from the save file");
        syncToAllPlayers();
    }

    /**
     * Scanner tous les chunks chargés pour trouver les weapon crates
     */
    public static void scanAllLoadedChunks(ServerLevel level) {
        currentLevel = level;
        WeaponCrateSavedData savedData = WeaponCrateSavedData.get(level);

        // Charger depuis la sauvegarde
        weaponCrates = savedData.getAllCrates();

        System.out.println("[ServerWeaponCrateTracker] Scan terminated. " + weaponCrates.size() + " weapon crates loaded");
        syncToAllPlayers();
    }

    public static void addWeaponCrate(BlockPos pos, int cost) {
        weaponCrates.put(pos, cost);
        System.out.println("[ServerWeaponCrateTracker] Crate added: " + pos + " -> " + cost + " points");
        System.out.println("[ServerWeaponCrateTracker] Total crates: " + weaponCrates.size());

        // Sauvegarder de manière persistante
        if (currentLevel != null) {
            WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
            savedData.addCrate(pos, cost);
        }

        syncToAllPlayers();
    }

    /**
     * Ajoute une weapon crate SANS synchroniser (pour scan initial)
     */
    public static void addWeaponCrateNoSync(BlockPos pos, int cost) {
        weaponCrates.put(pos, cost);

        // Sauvegarder de manière persistante
        if (currentLevel != null) {
            WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
            savedData.addCrate(pos, cost);
        }
    }

    public static void removeWeaponCrate(BlockPos pos) {
        weaponCrates.remove(pos);
        System.out.println("[ServerWeaponCrateTracker] Crates removed: " + pos);

        // Supprimer de la sauvegarde persistante
        if (currentLevel != null) {
            WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
            savedData.removeCrate(pos);
        }

        syncToAllPlayers();
    }

    /**
     * Supprime une weapon crate SANS synchroniser
     */
    public static void removeWeaponCrateNoSync(BlockPos pos) {
        weaponCrates.remove(pos);

        // Supprimer de la sauvegarde persistante
        if (currentLevel != null) {
            WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
            savedData.removeCrate(pos);
        }
    }

    public static void clear() {
        weaponCrates.clear();
        System.out.println("[ServerWeaponCrateTracker] All crates removed");

        // Effacer la sauvegarde persistante
        if (currentLevel != null) {
            WeaponCrateSavedData savedData = WeaponCrateSavedData.get(currentLevel);
            savedData.clearAll();
        }

        syncToAllPlayers();
    }

    public static Map<BlockPos, Integer> getAllCrates() {
        return new HashMap<>(weaponCrates);
    }

    private static Map<BlockPos, WeaponCrateSyncPacket.CrateData> buildCrateDataMap() {
        if (currentLevel == null) {
            System.err.println("[ServerWeaponCrateTracker] ERROR: Current Level is null, unable to construct data");
            return new HashMap<>();
        }

        // Créer la map avec toutes les données (coût + munitions)
        Map<BlockPos, WeaponCrateSyncPacket.CrateData> crateDataMap = new HashMap<>();
        for (Map.Entry<BlockPos, Integer> entry : weaponCrates.entrySet()) {
            BlockPos pos = entry.getKey();
            int cost = entry.getValue();
            ListTag ammo = WeaponCrateManager.getAmmo(currentLevel, pos);
            crateDataMap.put(pos, new WeaponCrateSyncPacket.CrateData(cost, ammo));
        }
        return crateDataMap;
    }

    public static void syncToAllPlayers() {
        System.out.println("[ServerWeaponCrateTracker] Syncing " + weaponCrates.size() + " crates to all players");
        NetworkHandler.sendToAllPlayers(new WeaponCrateSyncPacket(buildCrateDataMap()));
    }

    public static void syncToPlayer(net.minecraft.server.level.ServerPlayer player) {
        System.out.println("[ServerWeaponCrateTracker] Syncing " + weaponCrates.size() + " crates with " + player.getName().getString());
        NetworkHandler.sendToPlayer(player, new WeaponCrateSyncPacket(buildCrateDataMap()));
    }

    public static void reset() {

        weaponCrates.clear();
    
}
}
