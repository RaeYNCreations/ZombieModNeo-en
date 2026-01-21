package com.zombiemod.manager;

import com.zombiemod.network.NetworkHandler;
import com.zombiemod.network.packet.GameSyncPacket;
import com.zombiemod.system.ServerWeaponCrateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    public enum GameState {
        WAITING,        // Avant le /zombiestart
        STARTING,       // Countdown 60s
        WAVE_ACTIVE,    // Vague en cours
        WAVE_COOLDOWN   // 10s entre les vagues
    }

    private static boolean isRangeMode = false; // Track if current game is range mode
    private static GameState currentState = GameState.WAITING;
    private static Set<UUID> activePlayers = new HashSet<>();
    private static Set<UUID> waitingPlayers = new HashSet<>();
    private static int startCountdownTicks = 0;
    private static String currentMapName = null; // Nom de la map actuelle

    public static GameState getGameState() {
        return currentState;
    }

    public static void setGameState(GameState state) {
        currentState = state;
    }

    public static Set<UUID> getActivePlayers() {
        return new HashSet<>(activePlayers);
    }

    public static boolean isRangeMode() {
        return isRangeMode;
    }

    public static Set<UUID> getWaitingPlayers() {
        return new HashSet<>(waitingPlayers);
    }

    public static boolean isPlayerActive(UUID uuid) {
        return activePlayers.contains(uuid);
    }

    public static boolean isPlayerWaiting(UUID uuid) {
        return waitingPlayers.contains(uuid);
    }

    public static int getStartCountdownSeconds() {
        return startCountdownTicks / 20;
    }

    public static String getCurrentMapName() {
        return currentMapName;
    }

    public static void startGame(ServerLevel level, String mapName) {
        // NEW: Close all doors at game start
        com.zombiemod.command.DoorCommand.closeAllDoorsAtGameStart(level);
        if (currentState != GameState.WAITING) {
            return; // Déjà lancée
        }

        // Sélectionner la map si un nom est fourni
        if (mapName != null && !mapName.isEmpty()) {
            if (!com.zombiemod.map.MapManager.mapExists(mapName)) {
                System.err.println("[GameManager] ERROR: The map '" + mapName + "' does not exist!");
                broadcastToAll(level, "§c§lERROR: The map '" + mapName + "' does not exist!");
                return;
            }
            com.zombiemod.map.MapManager.selectMap(mapName);
            System.out.println("[GameManager] Map '" + mapName + "' selected");

            // Synchroniser les portes de la nouvelle map avec tous les clients
            com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();
        }

        currentState = GameState.STARTING;
        startCountdownTicks = 1200; // 60 secondes
        currentMapName = mapName;
        isRangeMode = false; // This is zombie mode

        // Recharger les weapon crates depuis la sauvegarde persistante
        System.out.println("[GameManager] Reloading weapon crates...");
        ServerWeaponCrateTracker.scanAllLoadedChunks(level);

        broadcastToAll(level, "§6§l=== ZOMBIE MAP SELECTION ===");
        if (mapName != null && !mapName.isEmpty()) {
            broadcastToAll(level, "§eMap: §6" + mapName);
        }
        broadcastToAll(level, "§eThe game starts in §c60 seconds§e!");
        broadcastToAll(level, "§7Step on the activator pad or type §6/zombiejoin §7to join!");
    }

    /**
     * Starts a game with a custom countdown timer
     * @param level The server level
     * @param mapName The map name (can be null for default)
     * @param seconds Custom countdown duration in seconds (will be converted to ticks)
     */
    public static void startGameWithCustomTimer(ServerLevel level, String mapName, int seconds) {
        // NEW: Close all doors at game start
        com.zombiemod.command.DoorCommand.closeAllDoorsAtGameStart(level);
        
        if (currentState != GameState.WAITING) {
            return; // Déjà lancée
        }

        // Sélectionner la map si un nom est fourni
        if (mapName != null && !mapName.isEmpty()) {
            if (!com.zombiemod.map.MapManager.mapExists(mapName)) {
                System.err.println("[GameManager] ERROR: The map '" + mapName + "' does not exist!");
                broadcastToAll(level, "§c§lERROR: The map '" + mapName + "' does not exist!");
                return;
            }
            com.zombiemod.map.MapManager.selectMap(mapName);
            System.out.println("[GameManager] Map '" + mapName + "' selected");

            // Synchroniser les portes de la nouvelle map avec tous les clients
            com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();
        }

        currentState = GameState.STARTING;
        startCountdownTicks = seconds * 20; // Convert seconds to ticks (20 ticks = 1 second)
        currentMapName = mapName;
        isRangeMode = true; // This is range mode

        // Recharger les weapon crates depuis la sauvegarde persistante
        System.out.println("[GameManager] Reloading weapon crates...");
        ServerWeaponCrateTracker.scanAllLoadedChunks(level);

        broadcastToAll(level, "§6§l=== GUN RANGE MODE ===");
        if (mapName != null && !mapName.isEmpty()) {
            broadcastToAll(level, "§eRange: §6" + mapName);
        }
        broadcastToAll(level, "§eThe game starts in §c" + seconds + " seconds§e!");
        broadcastToAll(level, "§7Step on the activator pad or type §6/zombierangejoin §7to join!");
    }

        private static int syncTickCounter = 0;

        public static void tick(ServerLevel level) {
            // Maintenir la saturation des joueurs actifs à 100% pour régénération constante
            if (currentState == GameState.WAVE_ACTIVE || currentState == GameState.WAVE_COOLDOWN) {
                for (UUID uuid : activePlayers) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                    if (player != null && !player.isSpectator() && !player.isDeadOrDying()) {
                        player.getFoodData().setFoodLevel(20);
                        player.getFoodData().setSaturation(20.0f);
                    }
                }
            }

            // Envoyer packet de sync toutes les 5 ticks (4 fois par seconde)
            syncTickCounter++;
            if (syncTickCounter >= 5) {
                sendSyncPacketToAll(level);
                syncTickCounter = 0;
            }

            if (currentState == GameState.STARTING) {
                startCountdownTicks--;
                int seconds = startCountdownTicks / 20;
            
                String joinCommand = isRangeMode ? "/zombierangejoin" : "/zombiejoin";
            
                // Messages toutes les 10 secondes
                if (startCountdownTicks % 200 == 0 && seconds > 5) {
                    broadcastToAll(level, "§eGame starts in §6" + seconds + "s §e! Step on the activator pad or type §6" + joinCommand + " §eto join!");playGlobalSound(level, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f);
                }

                // Messages chaque seconde pour les 5 dernières
                if (seconds <= 5 && startCountdownTicks % 20 == 0 && seconds > 0) {
                    broadcastToAll(level, "§c§l" + seconds + "...");
                    playGlobalSound(level, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f + (0.2f * (5 - seconds)));
                }

                // Démarrage !
                if (startCountdownTicks <= 0) {
                    activateAllPlayers(level);
                    WaveManager.startWave(level);
                    currentState = GameState.WAVE_ACTIVE;
                }
            }

            // Tick du WaveManager pour spawn progressif et countdown entre vagues
            if (currentState == GameState.WAVE_ACTIVE || currentState == GameState.WAVE_COOLDOWN) {
                WaveManager.tick(level);
            }
    }

    private static void activateAllPlayers(ServerLevel level) {
        // Tous les joueurs qui ont join deviennent actifs
        for (UUID uuid : new HashSet<>(waitingPlayers)) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                activePlayers.add(uuid);
                waitingPlayers.remove(uuid);

                player.setGameMode(GameType.ADVENTURE);
                BlockPos respawn = RespawnManager.getRespawnPoint();
                if (respawn != null) {
                    player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                }
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);

                PointsManager.setPoints(player.getUUID(), 500);

                player.sendSystemMessage(Component.literal("§a§l✦ QUE LET THE GAMES BEGIN! ✦"));
            }
        }

        broadcastToAll(level, "§6§l=== WAVE 1 ===");
    }

    public static void joinGame(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();

        if (activePlayers.contains(uuid) || waitingPlayers.contains(uuid)) {
            player.sendSystemMessage(Component.literal("§cYou are already in the game!"));
            return;
        }

        BlockPos respawn = RespawnManager.getRespawnPoint();
        if (respawn == null) {
            player.sendSystemMessage(Component.literal("§cERROR: Respawn point not defined! An admin must do §e/zombierespawn"));
            return;
        }

        switch (currentState) {
            case WAITING:
                String startCommand = isRangeMode ? "/zombierangestart" : "/zombiestart";
                player.sendSystemMessage(Component.literal("§cNo games are currently in progress! Type §e" + startCommand + "§c to start a game!"));
                break;

            case STARTING: // Countdown 60s
                waitingPlayers.add(uuid);
                player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                player.setGameMode(GameType.ADVENTURE); // Empêcher de casser des blocs
                player.sendSystemMessage(Component.literal("§aYou have joined the game!"));
                player.sendSystemMessage(Component.literal("§7Starting in §e" + (startCountdownTicks / 20) + "s"));
                broadcastToAll(level, "§7" + player.getName().getString() + " §ewith §7(" +
                        (activePlayers.size() + waitingPlayers.size()) + " players)");
                break;

            case WAVE_ACTIVE: // Vague en cours
                waitingPlayers.add(uuid);
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                player.sendSystemMessage(Component.literal("§eCurrent wave..."));
                player.sendSystemMessage(Component.literal("§7You will join at the end (§c" +
                        WaveManager.getZombiesRemaining() + " §7zombies remaining)"));
                broadcastToActivePlayers(level, "§7" + player.getName().getString() + " §ewaiting to rejoin");
                break;

            case WAVE_COOLDOWN: // Entre vagues (10s)
                activePlayers.add(uuid);
                player.setGameMode(GameType.ADVENTURE);
                player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                PointsManager.setPoints(uuid, 500);

                player.sendSystemMessage(Component.literal("§a§l✦ Welcome to the battle! ✦"));
                player.sendSystemMessage(Component.literal("§7Current wave: §e" + WaveManager.getCurrentWave()));
                broadcastToActivePlayers(level, "§a" + player.getName().getString() + " §ejoined the fight!");

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 1.0f, 1.2f);
                break;
        }
    }

    public static void leaveGame(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();

        if (!activePlayers.contains(uuid) && !waitingPlayers.contains(uuid)) {
            player.sendSystemMessage(Component.literal("§cYou are not in the game!"));
            return;
        }

        activePlayers.remove(uuid);
        waitingPlayers.remove(uuid);
        RespawnManager.removeDeadPlayer(uuid);

        player.setGameMode(GameType.ADVENTURE);
        player.sendSystemMessage(Component.literal("§7You quit the game!"));
        broadcastToAll(level, "§7" + player.getName().getString() + " §cleft the game!");
    }

    public static void activateWaitingPlayers(ServerLevel level) {
        for (UUID uuid : new HashSet<>(waitingPlayers)) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                activePlayers.add(uuid);
                waitingPlayers.remove(uuid);

                player.setGameMode(GameType.ADVENTURE);
                BlockPos respawn = RespawnManager.getRespawnPoint();
                if (respawn != null) {
                    player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                }
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                PointsManager.setPoints(uuid, 500);

                player.sendSystemMessage(Component.literal("§a§l✦ You are now in the battle! ✦"));
                broadcastToActivePlayers(level, "§a" + player.getName().getString() + " §ejoined the fight!");

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }
    }

    public static boolean areAllActivePlayersDead(ServerLevel level) {
        if (activePlayers.isEmpty()) {
            return false;
        }

        int deadCount = 0;
        int totalActivePlayers = 0;

        for (UUID uuid : activePlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                totalActivePlayers++;
                // Vérifier si le joueur est mort (en spectateur ET dans la liste des morts)
                if (RespawnManager.isPlayerDead(uuid)) {
                    deadCount++;
                }
            }
        }

        // Game over seulement si tous les joueurs actifs sont morts
        return totalActivePlayers > 0 && deadCount == totalActivePlayers;
    }

    /**
     * Vérifie si tous les joueurs actifs sont déconnectés
     */
    public static boolean areAllPlayersDisconnected(ServerLevel level) {
        if (activePlayers.isEmpty() && waitingPlayers.isEmpty()) {
            return true;
        }

        // Vérifier si au moins un joueur actif ou en attente est connecté
        for (UUID uuid : activePlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return false; // Au moins un joueur actif est connecté
            }
        }

        for (UUID uuid : waitingPlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return false; // Au moins un joueur en attente est connecté
            }
        }

        return true; // Tous les joueurs sont déconnectés
    }

    public static void stopGameAutomatic(ServerLevel level) {
        if (currentState == GameState.WAITING) {
            return; // Aucune partie en cours
        }
    
        System.out.println("[ZombieMod] Game ended automatically - All players are disconnected");
    
        // Nettoyer tous les mobs
        WaveManager.killAllMobs();
    
        // Restaurer les inventaires pour tous les joueurs (même déconnectés)
        for (UUID uuid : new HashSet<>(activePlayers)) {
            if (InventoryManager.hasSavedInventory(uuid)) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    InventoryManager.restoreInventory(player);
                }
            }
        }
    
        for (UUID uuid : new HashSet<>(waitingPlayers)) {
            if (InventoryManager.hasSavedInventory(uuid)) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    InventoryManager.restoreInventory(player);
                }
            }
        }
    
        // Réinitialiser tous les managers
        reset();
        WaveManager.reset();
        InventoryManager.reset(); // Clear any remaining saved inventories
    
        // Réinitialiser les portes (fermer physiquement et réinitialiser l'état)
        com.zombiemod.command.DoorCommand.openAllDoorsAtGameEnd(level);
    }

    public static void reset() {

        currentState = GameState.WAITING;
        activePlayers.clear();
        waitingPlayers.clear();
        startCountdownTicks = 0;
        currentMapName = null;
        isRangeMode = false; // Reset range mode flag
    
}

    public static void broadcastToAll(ServerLevel level, String message) {
        Component comp = Component.literal(message);
        level.getServer().getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(comp));
    }

    public static void broadcastToActivePlayers(ServerLevel level, String message) {
        Component comp = Component.literal(message);
        for (UUID uuid : activePlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(comp);
            }
        }
    }

    public static void playGlobalSound(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float pitch) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            level.playSound(null, player.blockPosition(), sound, SoundSource.MASTER, 1.0f, pitch);
        }
    }

    public static void playSoundToActivePlayers(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float pitch) {
        for (UUID uuid : activePlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.MASTER, 1.0f, pitch);
            }
        }
        // Aussi pour les joueurs en attente (spectateurs)
        for (UUID uuid : waitingPlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.MASTER, 1.0f, pitch);
            }
        }
    }

    public static void sendSyncPacketToAll(ServerLevel level) {
        // Créer la liste des joueurs actifs avec leurs données
        List<GameSyncPacket.PlayerData> playerDataList = new ArrayList<>();
        for (UUID uuid : activePlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                int points = PointsManager.getPoints(uuid);
                playerDataList.add(new GameSyncPacket.PlayerData(uuid, player.getName().getString(), points));
            }
        }

        // Envoyer à chaque joueur connecté
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            GameSyncPacket packet = new GameSyncPacket(
                    currentState.name(),
                    WaveManager.getCurrentWave(),
                    WaveManager.getZombiesRemaining(),
                    startCountdownTicks,
                    WaveManager.getCountdownSeconds() * 20,
                    playerDataList,
                    new ArrayList<>(waitingPlayers),
                    player.getUUID()
            );

            NetworkHandler.sendToPlayer(player, packet);
        }
    }
}
