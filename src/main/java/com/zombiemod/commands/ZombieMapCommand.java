package com.zombiemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zombiemod.map.MapConfig;
import com.zombiemod.map.MapManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class ZombieMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zombiemap")
                .then(Commands.literal("create")
                .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ZombieMapCommand::createMap)))
                .then(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ZombieMapCommand::deleteMap)))
                .then(Commands.literal("select")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ZombieMapCommand::selectMap)))
                .then(Commands.literal("list")
                        .executes(ZombieMapCommand::listMaps))
                .then(Commands.literal("info")
                .requires(source -> source.hasPermission(2))
                        .executes(context -> showMapInfo(context, null))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(context -> showMapInfo(context, StringArgumentType.getString(context, "name"))))));
    }

    private static int createMap(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        if (MapManager.mapExists(name)) {
            source.sendFailure(Component.literal("§cA map named '" + name + "' already exists!"));
            return 0;
        }

        if (MapManager.createMap(name)) {
            source.sendSuccess(() -> Component.literal("§aMap '§e" + name + "§a' successfully created!"), true);
            source.sendSuccess(() -> Component.literal("§7Use §f/zombiemap select " + name + " §7 selected!"), false);
            source.sendSuccess(() -> Component.literal("§7Then configure it with §f/respawnpoint " + name + " §7and §f/zombiespawn " + name), false);
            return 1;
        }

        source.sendFailure(Component.literal("§cError creating map"));
        return 0;
    }

    private static int deleteMap(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        if (!MapManager.mapExists(name)) {
            source.sendFailure(Component.literal("§cThe map '" + name + "' does not exist!"));
            return 0;
        }

        if (MapManager.getMapCount() <= 1) {
            source.sendFailure(Component.literal("§cYou cannot delete the last map!"));
            source.sendFailure(Component.literal("§7First, create another map with §f/zombiemap create <name>"));
            return 0;
        }

        if (MapManager.deleteMap(name)) {
            source.sendSuccess(() -> Component.literal("§aMap '§e" + name + "§a' successfully removed!"), true);

            String selected = MapManager.getSelectedMapName();
            if (selected != null) {
                source.sendSuccess(() -> Component.literal("§7Map selected: §e" + selected), false);
            }
            return 1;
        }

        source.sendFailure(Component.literal("§cError deleting map"));
        return 0;
    }

    private static int selectMap(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        if (!MapManager.mapExists(name)) {
            source.sendFailure(Component.literal("§cThe map '" + name + "' does not exist!"));
            source.sendFailure(Component.literal("§7Use §f/zombiemap list §7to see the available maps!"));
            return 0;
        }

        if (MapManager.selectMap(name)) {
            source.sendSuccess(() -> Component.literal("§aMap '§e" + name + "§a' selected! !"), true);

            MapConfig map = MapManager.getMap(name);
            if (map != null) {
                boolean hasRespawn = map.hasRespawnPoint();
                boolean hasSpawns = map.hasZombieSpawnPoints();

                if (!hasRespawn || !hasSpawns) {
                    source.sendSuccess(() -> Component.literal("§e⚠ Configuration incomplète:"), false);
                    if (!hasRespawn) {
                        source.sendSuccess(() -> Component.literal("§7  • Respawn point: §cNot defined §7(§f/respawnpoint " + name + "§7)"), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("§7  • Respawn point: §a✓ defined"), false);
                    }
                    if (!hasSpawns) {
                        source.sendSuccess(() -> Component.literal("§7  • Spawn zombies: §cNone §7(§f/zombiespawn " + name + "§7)"), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("§7  • Spawn zombies: §a✓ " + map.getZombieSpawnPointCount() + " point(s)"), false);
                    }
                }
            }
            return 1;
        }

        source.sendFailure(Component.literal("§cError selecting map"));
        return 0;
    }

    private static int listMaps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<MapConfig> maps = MapManager.getAllMaps();
        String selectedName = MapManager.getSelectedMapName();

        if (maps.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cNo map available"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l===== MAPS AVAILABLE ====="), false);
        source.sendSuccess(() -> Component.literal(""), false);

        for (MapConfig map : maps) {
            String name = map.getName();
            boolean isSelected = name.equals(selectedName);

            String prefix = isSelected ? "§a▶ " : "§7  ";
            String status = isSelected ? " §e(active)" : "";

            source.sendSuccess(() -> Component.literal(prefix + "§f" + name + status), false);

            if (map.hasRespawnPoint()) {
                BlockPos pos = map.getRespawnPoint();
                source.sendSuccess(() -> Component.literal("    §7Respawn: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
            } else {
                source.sendSuccess(() -> Component.literal("    §7Respawn: §cNot defined"), false);
            }

            int spawnCount = map.getZombieSpawnPointCount();
            if (spawnCount > 0) {
                source.sendSuccess(() -> Component.literal("    §7Spawns: §f" + spawnCount + " point(s)"), false);
            } else {
                source.sendSuccess(() -> Component.literal("    §7Spawns: §cNone"), false);
            }

            source.sendSuccess(() -> Component.literal(""), false);
        }

        source.sendSuccess(() -> Component.literal("§7Total: §f" + maps.size() + " map(s)"), false);
        source.sendSuccess(() -> Component.literal("§7Use §f/zombiemap select <name> §7to change the map"), false);

        return 1;
    }

    private static int showMapInfo(CommandContext<CommandSourceStack> context, String mapName) {
        CommandSourceStack source = context.getSource();

        MapConfig map;
        String name;

        if (mapName == null) {
            map = MapManager.getSelectedMap();
            name = MapManager.getSelectedMapName();

            if (map == null) {
                source.sendFailure(Component.literal("§cNo map selected!"));
                source.sendFailure(Component.literal("§7Use §f/zombiemap select <name> §7to select a map"));
                return 0;
            }
        } else {
            map = MapManager.getMap(mapName);
            name = mapName;

            if (map == null) {
                source.sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
                return 0;
            }
        }

        boolean isSelected = name.equals(MapManager.getSelectedMapName());

        source.sendSuccess(() -> Component.literal("§6§l===== MAP INFO: " + name.toUpperCase() + " ====="), false);
        source.sendSuccess(() -> Component.literal(""), false);

        if (isSelected) {
            source.sendSuccess(() -> Component.literal("§aStatus: §e✓ Active"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7Status: Inactive"), false);
        }

        source.sendSuccess(() -> Component.literal(""), false);

        if (map.hasRespawnPoint()) {
            BlockPos pos = map.getRespawnPoint();
            source.sendSuccess(() -> Component.literal("§e§lRespawn Point:"), false);
            source.sendSuccess(() -> Component.literal("§f  X: " + pos.getX()), false);
            source.sendSuccess(() -> Component.literal("§f  Y: " + pos.getY()), false);
            source.sendSuccess(() -> Component.literal("§f  Z: " + pos.getZ()), false);
        } else {
            source.sendSuccess(() -> Component.literal("§c§lRespawn Point: Not defined "), false);
            source.sendSuccess(() -> Component.literal("§7  Use: §f/respawnpoint " + name), false);
        }

        source.sendSuccess(() -> Component.literal(""), false);

        int spawnCount = map.getZombieSpawnPointCount();
        if (spawnCount > 0) {
            source.sendSuccess(() -> Component.literal("§e§lZombie Spawns: §f" + spawnCount + " point(s)"), false);
            int i = 1;
            for (BlockPos pos : map.getZombieSpawnPoints()) {
                final int index = i;
                source.sendSuccess(() -> Component.literal("§7  " + index + ". §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
                i++;
            }
        } else {
            source.sendSuccess(() -> Component.literal("§c§lZombie Spawns: None"), false);
            source.sendSuccess(() -> Component.literal("§7  Use: §f/zombiespawn " + name), false);
        }

        return 1;
    }
}
