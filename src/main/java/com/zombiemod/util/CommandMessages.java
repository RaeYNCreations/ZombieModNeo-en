package com.zombiemod.util;

import com.zombiemod.manager.GameManager;

/**
 * Centralized helper for generating context-aware command messages
 * based on the current game mode (zombie vs range)
 */
public class CommandMessages {

    /**
     * Gets the appropriate join command based on game mode
     * @return "/zombiejoin" or "/zombierangejoin"
     */
    public static String getJoinCommand() {
        boolean isRange = GameManager.isRangeMode();
        String command = isRange ? "/zombierangejoin" : "/zombiejoin";
        System.out.println("[CommandMessages] getJoinCommand() - isRangeMode: " + isRange + " -> returning: " + command);
        return command;
    }

    /**
     * Gets the appropriate start command based on game mode
     * @return "/zombiestart" or "/zombierangestart"
     */
    public static String getStartCommand() {
        return GameManager.isRangeMode() ? "/zombierangestart" : "/zombiestart";
    }

    /**
     * Gets the appropriate leave command based on game mode
     * @return "/zombieleave" or "/zombierangeleave"
     */
    public static String getLeaveCommand() {
        return GameManager.isRangeMode() ? "/zombierangeleave" : "/zombieleave";
    }

    /**
     * Gets the appropriate status command based on game mode
     * @return "/zombiestatus" or "/zombierangestatus"
     */
    public static String getStatusCommand() {
        return GameManager.isRangeMode() ? "/zombierangestatus" : "/zombiestatus";
    }

    /**
     * Gets the appropriate skip command based on game mode
     * @return "/zombieskip" or "/zombierangeskip"
     */
    public static String getSkipCommand() {
        return GameManager.isRangeMode() ? "/zombierangeskip" : "/zombieskip";
    }

    /**
     * Gets the appropriate stop command based on game mode
     * @return "/zombiestop" or "/zombierangestop"
     */
    public static String getStopCommand() {
        return GameManager.isRangeMode() ? "/zombierangestop" : "/zombiestop";
    }

    /**
     * Gets the game mode name
     * @return "Zombie Mode" or "Gun Range"
     */
    public static String getGameModeName() {
        return GameManager.isRangeMode() ? "Gun Range" : "Zombie Mode";
    }

    /**
     * Gets the game type for messages (lowercase)
     * @return "gun range" or "game"
     */
    public static String getGameType() {
        return GameManager.isRangeMode() ? "gun range" : "game";
    }

    /**
     * Creates a "not in game" message with appropriate join command
     */
    public static String getNotInGameMessage() {
        String command = getJoinCommand();
        String gameType = getGameType();
        String message = "§cYou must be in a running, in-progress " + gameType + " to buy! Step on the activator pad or type §6" + command + " §cto join!";
        System.out.println("[CommandMessages] getNotInGameMessage() - returning: " + message);
        return message;
    }

    /**
     * Creates a "join to participate" message
     */
    public static String getJoinToParticipateMessage() {
        return "§cYou need to be in the " + getGameType() + " to open this door! §7Step on the activator pad or type §6" + getJoinCommand() + " §7to join!";
    }

    /**
     * Creates a "no game running" message
     */
    public static String getNoGameRunningMessage() {
        return "§cNo " + getGameType() + "s are currently in progress! Type §e" + getStartCommand() + "§c to start a " + getGameType() + "!";
    }
}