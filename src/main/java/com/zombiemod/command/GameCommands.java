package com.zombiemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zombiemod.manager.GameManager;
import com.zombiemod.manager.InventoryManager;
import com.zombiemod.manager.PointsManager;
import com.zombiemod.manager.WaveManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class GameCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /zombiestart [mapName]
        dispatcher.register(Commands.literal("zombiestart")
                .executes(GameCommands::startGame)
                .then(Commands.argument("mapName", StringArgumentType.word())
                        .executes(GameCommands::startGameWithMap)));

        // /zombiestop
        dispatcher.register(Commands.literal("zombiestop")
                .requires(source -> source.hasPermission(2))
                .executes(GameCommands::stopGame));

        // /zombiejoin
        dispatcher.register(Commands.literal("zombiejoin")
                .executes(GameCommands::joinGame));

        // /zombieleave
        dispatcher.register(Commands.literal("zombieleave")
                .executes(GameCommands::leaveGame));

        // /zombiestatus
        dispatcher.register(Commands.literal("zombiestatus")
                .executes(GameCommands::showStatus));

        // /zombieskip
        dispatcher.register(Commands.literal("zombieskip")
                .requires(source -> source.hasPermission(2))
                .executes(GameCommands::skipWave));
    }

    private static int startGame(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        if (GameManager.getGameState() != GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cA Game is already underway!"));
            return 0;
        }

        GameManager.startGame(level, null); // Pas de nom de map spécifié
        context.getSource().sendSuccess(() -> Component.literal("§aGame On!  60-second counter started!"), true);

        return 1;
    }

    private static int startGameWithMap(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        String mapName = StringArgumentType.getString(context, "mapName");

        if (GameManager.getGameState() != GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cA Game is already underway!"));
            return 0;
        }

        GameManager.startGame(level, mapName);
        context.getSource().sendSuccess(() -> Component.literal("§aGame launched on map §e" + mapName + " §a! Game starts in 60 seconds!"), true);

        return 1;
    }

    private static int stopGame(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
    
        if (GameManager.getGameState() == GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cNo games currently in progress!"));
            return 0;
        }
    
        // Annonce de l'arrêt
        GameManager.broadcastToAll(level, "§c§l=== GAME STOPPED BY ADMIN ===");
    
        // Nettoyer tous les mobs avec la méthode dédiée
        WaveManager.killAllMobs();
    
        // Restaurer les inventaires et mettre tous les joueurs en adventure
        for (UUID uuid : GameManager.getActivePlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                // Restaurer l'inventaire si sauvegardé (range mode)
                if (InventoryManager.hasSavedInventory(uuid)) {
                    InventoryManager.restoreInventory(player);
                }
                
                player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                player.sendSystemMessage(Component.literal("§7The game has been stopped."));
            }
        }
    
        for (UUID uuid : GameManager.getWaitingPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                // Restaurer l'inventaire si sauvegardé (range mode)
                if (InventoryManager.hasSavedInventory(uuid)) {
                    InventoryManager.restoreInventory(player);
                }
                
                player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                player.sendSystemMessage(Component.literal("§7The game has been stopped."));
            }
        }
    
        // Réinitialiser tous les managers
        GameManager.reset();
        WaveManager.reset();
        InventoryManager.reset(); // Clear any remaining saved inventories
    
        // Réinitialiser les portes (fermer physiquement et réinitialiser l'état)
        DoorCommand.openAllDoorsAtGameEnd(level);
    
        context.getSource().sendSuccess(() -> Component.literal("§aGame stopped, zombies cleared, and game reset."), true);
    
        return 1;
    }

    private static int joinGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        GameManager.joinGame(player, level);

        return 1;
    }

    private static int leaveGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        GameManager.leaveGame(player, level);

        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) return 0;

        player.sendSystemMessage(Component.literal("§6§l=== ZOMBIE STATUS ==="));
        String mapName = GameManager.getCurrentMapName();
        if (mapName != null && !mapName.isEmpty()) {
            player.sendSystemMessage(Component.literal("§eMap: §6" + mapName));
        }
        player.sendSystemMessage(Component.literal("§eGame Status: §f" + getStateName(GameManager.getGameState())));
        player.sendSystemMessage(Component.literal("§eWave: §f" + WaveManager.getCurrentWave()));
        player.sendSystemMessage(Component.literal("§eZombies remaining: §f" + WaveManager.getZombiesRemaining()));
        player.sendSystemMessage(Component.literal("§ePlayers active: §f" + GameManager.getActivePlayers().size()));
        player.sendSystemMessage(Component.literal("§ePlayuers waiting: §f" + GameManager.getWaitingPlayers().size()));

        if (GameManager.isPlayerActive(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§aYou're in the game!"));
            player.sendSystemMessage(Component.literal("§eYour points: §f" + PointsManager.getPoints(player.getUUID())));
        } else if (GameManager.isPlayerWaiting(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§eYou are waiting..."));
        } else {
            player.sendSystemMessage(Component.literal("§7You are not in the game. Step on the activator pad or type §e/zombiejoin §7to join."));
        }

        return 1;
    }

    private static int skipWave(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        GameManager.GameState state = GameManager.getGameState();

        if (state != GameManager.GameState.WAVE_ACTIVE) {
            context.getSource().sendFailure(Component.literal("§cNo waves currently! Current Status: " + getStateName(state)));
            return 0;
        }

        int currentWave = WaveManager.getCurrentWave();
        int zombiesRemaining = WaveManager.getZombiesRemaining();

        // Annonce du skip
        GameManager.broadcastToAll(level, "§6§l[ADMIN] §eWave " + currentWave + " finished! §7(" + zombiesRemaining + " zombies restants)");

        // Skip la vague
        WaveManager.skipWave(level);

        context.getSource().sendSuccess(() -> Component.literal("§aWave " + currentWave + " finished and completed fully!"), true);

        return 1;
    }

    private static String getStateName(GameManager.GameState state) {
        return switch (state) {
            case WAITING -> "Waiting";
            case STARTING -> "Startup (" + GameManager.getStartCountdownSeconds() + "s)";
            case WAVE_ACTIVE -> "Current wave";
            case WAVE_COOLDOWN -> "Between waves (" + WaveManager.getCountdownSeconds() + "s)";
        };
    }
}
