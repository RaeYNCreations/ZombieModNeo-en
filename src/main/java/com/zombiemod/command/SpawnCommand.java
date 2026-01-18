package com.zombiemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zombiemod.map.MapConfig;
import com.zombiemod.map.MapManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class SpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /zombiespawn <mapname> [doorNumber]
        dispatcher.register(Commands.literal("zombiespawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mapname", StringArgumentType.string())
                        .executes(SpawnCommand::addSpawnPoint)
                        .then(Commands.argument("doorNumber", IntegerArgumentType.integer(0))
                                .executes(SpawnCommand::addSpawnPointWithDoor))));

        // /zombiespawn clear <mapname>
        dispatcher.register(Commands.literal("zombiespawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .executes(SpawnCommand::clearSpawnPoints))));
    }

    private static int addSpawnPoint(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        // Vérifier que la map existe
        if (!MapManager.mapExists(mapName)) {
            player.sendSystemMessage(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            player.sendSystemMessage(Component.literal("§7Use §f/zombiemap list §7to list available maps"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);
        BlockPos pos = player.blockPosition();

        // Ajouter le point de spawn
        map.addZombieSpawnPoint(pos);
        MapManager.save();

        player.sendSystemMessage(Component.literal("§aSpawn point added to map '§e" + mapName + "§a':"));
        player.sendSystemMessage(Component.literal("  §fX: §e" + pos.getX()));
        player.sendSystemMessage(Component.literal("  §fY: §e" + pos.getY()));
        player.sendSystemMessage(Component.literal("  §fZ: §e" + pos.getZ()));
        player.sendSystemMessage(Component.literal("  §fDoor: §7Always active"));
        player.sendSystemMessage(Component.literal("§7Total: §e" + map.getZombieSpawnPointCount() + " §7spawn point(s)"));

        return 1;
    }

    private static int addSpawnPointWithDoor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");
        int doorNumber = IntegerArgumentType.getInteger(context, "doorNumber");

        // Vérifier que la map existe
        if (!MapManager.mapExists(mapName)) {
            player.sendSystemMessage(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            player.sendSystemMessage(Component.literal("§7Use §f/zombiemap list §7to list the available maps"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);

        // Vérifier que la porte existe
        if (!map.hasDoor(doorNumber)) {
            player.sendSystemMessage(Component.literal("§cThe door n°" + doorNumber + " does not exist on this map!"));
            player.sendSystemMessage(Component.literal("§7Create a door with §f/zombiedoor add " + mapName + " " + doorNumber + " <cost>"));
            return 0;
        }

        BlockPos pos = player.blockPosition();

        // Ajouter le point de spawn avec le numéro de porte
        map.addZombieSpawnPoint(pos, doorNumber);
        MapManager.save();

        player.sendSystemMessage(Component.literal("§aSpawn point added to map '§e" + mapName + "§a':"));
        player.sendSystemMessage(Component.literal("  §fX: §e" + pos.getX()));
        player.sendSystemMessage(Component.literal("  §fY: §e" + pos.getY()));
        player.sendSystemMessage(Component.literal("  §fZ: §e" + pos.getZ()));
        player.sendSystemMessage(Component.literal("  §fDoor: §e#" + doorNumber + " §7(active when door is open)"));
        player.sendSystemMessage(Component.literal("§7Total: §e" + map.getZombieSpawnPointCount() + " §7spawn point(s)"));

        return 1;
    }

    private static int clearSpawnPoints(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");

        // Vérifier que la map existe
        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);
        int count = map.getZombieSpawnPointCount();

        map.clearZombieSpawnPoints();
        MapManager.save();

        context.getSource().sendSuccess(() -> Component.literal("§a" + count + " spawn point(s) removed from map '§e" + mapName + "§a'"), true);
        return 1;
    }
}
