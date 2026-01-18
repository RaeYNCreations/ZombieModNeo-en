package com.zombiemod.system;

import com.zombiemod.network.NetworkHandler;
import com.zombiemod.network.packet.JukeboxSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracker côté serveur pour les jukeboxes zombie
 * Garde une map des positions et coûts et synchronise avec les clients
 * Sauvegarde de manière persistante via JukeboxSavedData
 */
public class ServerJukeboxTracker {

    private static Map<BlockPos, Integer> jukeboxes = new HashMap<>();
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
            System.err.println("[ServerJukeboxTracker] Failed to load: Current level is null!");
            return;
        }

        JukeboxSavedData savedData = JukeboxSavedData.get(currentLevel);
        jukeboxes = savedData.getAllJukeboxes();

        System.out.println("[ServerJukeboxTracker] Loaded " + jukeboxes.size() + " jukeboxes from save");
        syncToAllPlayers();
    }

    public static void addJukebox(BlockPos pos, int cost) {
        jukeboxes.put(pos, cost);
        System.out.println("[ServerJukeboxTracker] Jukebox added: " + pos + " -> " + cost + " points");
        System.out.println("[ServerJukeboxTracker] Total jukeboxes: " + jukeboxes.size());

        // Sauvegarder de manière persistante
        if (currentLevel != null) {
            JukeboxSavedData savedData = JukeboxSavedData.get(currentLevel);
            savedData.addJukebox(pos, cost);
        }

        syncToAllPlayers();
    }

    public static void removeJukebox(BlockPos pos) {
        jukeboxes.remove(pos);
        System.out.println("[ServerJukeboxTracker] Jukebox removed: " + pos);

        // Supprimer de la sauvegarde persistante
        if (currentLevel != null) {
            JukeboxSavedData savedData = JukeboxSavedData.get(currentLevel);
            savedData.removeJukebox(pos);
        }

        syncToAllPlayers();
    }

    public static void clear() {
        jukeboxes.clear();
        System.out.println("[ServerJukeboxTracker] All the jukeboxes removed");

        // Effacer la sauvegarde persistante
        if (currentLevel != null) {
            JukeboxSavedData savedData = JukeboxSavedData.get(currentLevel);
            savedData.clearAll();
        }

        syncToAllPlayers();
    }

    public static Map<BlockPos, Integer> getAllJukeboxes() {
        return new HashMap<>(jukeboxes);
    }

    public static void syncToAllPlayers() {
        System.out.println("[ServerJukeboxTracker] Syncing " + jukeboxes.size() + " jukeboxes with all players");
        NetworkHandler.sendToAllPlayers(new JukeboxSyncPacket(getAllJukeboxes()));
    }

    public static void syncToPlayer(net.minecraft.server.level.ServerPlayer player) {
        System.out.println("[ServerJukeboxTracker] Syncing " + jukeboxes.size() + " jukeboxes with " + player.getName().getString());
        NetworkHandler.sendToPlayer(player, new JukeboxSyncPacket(getAllJukeboxes()));
    }

    public static void reset() {

        jukeboxes.clear();
    
}
}
