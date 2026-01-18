package com.zombiemod.manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {

    // Stockage des inventaires sauvegardés (UUID -> NBT de l'inventaire complet)
    private static final Map<UUID, CompoundTag> savedInventories = new HashMap<>();

    // Cache pour savoir si Curios est installé
    private static Boolean curiosLoaded = null;

    /**
     * Vérifie si Curios API est installé
     */
    private static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }

    /**
     * Sauvegarde l'inventaire complet d'un joueur (inventaire + armure + offhand + curios si disponible)
     */
    public static void saveInventory(ServerPlayer player) {
    }

    /**
     * Sauvegarde l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void saveCuriosInventory(ServerPlayer player, CompoundTag inventoryData) {
    }

    /**
     * Restaure l'inventaire complet d'un joueur
     */
    public static void restoreInventory(ServerPlayer player) {
    }

    /**
     * Restaure l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void restoreCuriosInventory(ServerPlayer player, CompoundTag inventoryData) {
    }

    /**
     * Clear l'inventaire d'un joueur (pour le début de partie)
     */
    public static void clearInventory(ServerPlayer player) {
    }

    /**
     * Clear l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void clearCuriosInventory(ServerPlayer player) {
    }

    /**
     * Vérifie si un joueur a un inventaire sauvegardé
     */
    public static boolean hasSavedInventory(UUID uuid) {
        return savedInventories.containsKey(uuid);
    }

    /**
     * Supprime la sauvegarde d'inventaire d'un joueur (sans restaurer)
     */
    public static void removeSavedInventory(UUID uuid) {
    }

    /**
     * Reset complet (fin de partie)
     */
    public static void reset() {

        savedInventories.clear();
    
}
}
