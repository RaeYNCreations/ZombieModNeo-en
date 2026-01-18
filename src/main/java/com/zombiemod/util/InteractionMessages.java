// Add this helper class to provide contextual messages based on game state

package main.java.com.zombiemod.util;

import com.zombiemod.manager.GameManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Provides contextual messages for player interactions based on game state
 */
public class InteractionMessages {

    /**
     * Gets the appropriate "cannot interact" message based on player and game state
     */
    public static Component getCannotInteractMessage(ServerPlayer player, InteractionType type) {
        GameManager.GameState gameState = GameManager.getGameState();
        boolean isActive = GameManager.isPlayerActive(player.getUUID());
        boolean isWaiting = GameManager.isPlayerWaiting(player.getUUID());

        // Player is not in the game at all
        if (!isActive && !isWaiting) {
            return getNotInGameMessage(gameState, type);
        }

        // Player has joined but game hasn't started yet
        if (isWaiting && gameState == GameManager.GameState.STARTING) {
            return getWaitingForStartMessage(type);
        }

        // Player is waiting to respawn during active game
        if (isWaiting && gameState == GameManager.GameState.WAVE_ACTIVE) {
            return getWaitingToRespawnMessage(type);
        }

        // Default fallback (shouldn't normally reach here)
        return Component.literal("§cYou cannot interact with this right now!");
    }

    /**
     * Message when player is not in the game
     */
    private static Component getNotInGameMessage(GameManager.GameState gameState, InteractionType type) {
        String itemName = getItemName(type);
        
        if (gameState == GameManager.GameState.WAITING) {
            // No game running at all
            return Component.literal("§cNo game is running! Step on an activator portal to begin.");
        } else if (gameState == GameManager.GameState.STARTING) {
            // Game is starting - can still join
            return Component.literal("§cYou must join the game to use " + itemName + "! §7Step on the activator pad or type §6/zombiejoin §7to join!");
        } else {
            // Game is active - too late to join normally
            return Component.literal("§cYou must be in the game to use " + itemName + "! §7Type §6/zombiejoin §7to join (you'll spawn at the end of this wave).");
        }
    }

    /**
     * Message when player is waiting for game to start (countdown)
     */
    private static Component getWaitingForStartMessage(InteractionType type) {
        String itemName = getItemName(type);
        int seconds = GameManager.getStartCountdownSeconds();
        
        return Component.literal("§eYou're in the game, but it hasn't started yet! §7Hold on... (§6" + seconds + "s §7remaining)");
    }

    /**
     * Message when player is waiting to respawn during active game
     */
    private static Component getWaitingToRespawnMessage(InteractionType type) {
        String itemName = getItemName(type);
        
        return Component.literal("§eYou're waiting to respawn! §7You cannot use " + itemName + " until you respawn at the end of this wave.");
    }

    /**
     * Gets the friendly name for the interaction type
     */
    private static String getItemName(InteractionType type) {
        return switch (type) {
            case WEAPON_CRATE -> "§6weapon crates";
            case AMMO -> "§6ammo";
            case JUKEBOX -> "§6jukeboxes";
            case DOOR -> "§6doors";
        };
    }

    /**
     * Types of interactions that can be blocked
     */
    public enum InteractionType {
        WEAPON_CRATE,
        AMMO,
        JUKEBOX,
        DOOR
    }
}