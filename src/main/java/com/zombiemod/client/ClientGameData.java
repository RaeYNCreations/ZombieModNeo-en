package com.zombiemod.client;

import com.zombiemod.manager.GameManager;
import com.zombiemod.network.packet.GameSyncPacket;

import java.util.*;

/**
 * Stocke les données synchronisées depuis le serveur côté client
 */
public class ClientGameData {

    private static volatile GameManager.GameState gameState = GameManager.GameState.WAITING;
    private static volatile int currentWave = 0;
    private static volatile int zombiesRemaining = 0;
    private static volatile int startCountdownTicks = 0;
    private static volatile int waveCountdownTicks = 0;
    private static List<GameSyncPacket.PlayerData> activePlayers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Set<UUID> waitingPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static UUID localPlayerUUID = null;
    private static volatile boolean isRangeMode = false;

    public static void updateGameState(GameSyncPacket packet) {
        try {
            gameState = GameManager.GameState.valueOf(packet.gameState());
        } catch (IllegalArgumentException e) {
            gameState = GameManager.GameState.WAITING;
        }

        currentWave = packet.currentWave();
        zombiesRemaining = packet.zombiesRemaining();
        startCountdownTicks = packet.startCountdownTicks();
        waveCountdownTicks = packet.waveCountdownTicks();
        activePlayers = new ArrayList<>(packet.activePlayers());
        waitingPlayers = new HashSet<>(packet.waitingPlayers());
        localPlayerUUID = packet.localPlayerUUID();
        isRangeMode = packet.isRangeMode();
    }

    public static GameManager.GameState getGameState() {
        return gameState;
    }

    public static int getCurrentWave() {
        return currentWave;
    }

    public static int getZombiesRemaining() {
        return zombiesRemaining;
    }

    public static int getStartCountdownSeconds() {
        return startCountdownTicks / 20;
    }

    public static int getWaveCountdownSeconds() {
        return waveCountdownTicks / 20;
    }

    public static List<GameSyncPacket.PlayerData> getActivePlayers() {
        return new ArrayList<>(activePlayers);
    }

    public static boolean isLocalPlayerActive() {
        if (localPlayerUUID == null) return false;
        return activePlayers.stream().anyMatch(p -> p.uuid().equals(localPlayerUUID));
    }

    public static boolean isLocalPlayerWaiting() {
        if (localPlayerUUID == null) return false;
        return waitingPlayers.contains(localPlayerUUID);
    }

    public static int getLocalPlayerPoints() {
        if (localPlayerUUID == null) return 0;
        return activePlayers.stream()
                .filter(p -> p.uuid().equals(localPlayerUUID))
                .findFirst()
                .map(GameSyncPacket.PlayerData::points)
                .orElse(0);
    }

    public static boolean isRangeMode() {
        return isRangeMode;
    }

    public static void reset() {

        gameState = GameManager.GameState.WAITING;
        currentWave = 0;
        zombiesRemaining = 0;
        startCountdownTicks = 0;
        waveCountdownTicks = 0;
        activePlayers.clear();
        waitingPlayers.clear();
        localPlayerUUID = null;
        isRangeMode = false;
    
}
}
