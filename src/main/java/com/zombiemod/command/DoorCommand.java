package com.zombiemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zombiemod.map.DoorConfig;
import com.zombiemod.map.MapConfig;
import com.zombiemod.map.MapManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

public class DoorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /zombiedoor add <mapname> <doorNumber> <cost>
        dispatcher.register(Commands.literal("zombiedoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .then(Commands.argument("doorNumber", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                                .executes(DoorCommand::addDoor))))));

        // /zombiedoor remove <mapname> <doorNumber>
        dispatcher.register(Commands.literal("zombiedoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("remove")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .then(Commands.argument("doorNumber", IntegerArgumentType.integer(0))
                                        .executes(DoorCommand::removeDoor)))));

        // /zombiedoor list <mapname>
        dispatcher.register(Commands.literal("zombiedoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .executes(DoorCommand::listDoors))));

        // /zombiedoor open <mapname> <doorNumber>
        dispatcher.register(Commands.literal("zombiedoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .then(Commands.argument("doorNumber", IntegerArgumentType.integer(0))
                                        .executes(DoorCommand::openDoor)))));

        // /zombiedoor close <mapname> <doorNumber>
        dispatcher.register(Commands.literal("zombiedoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("close")
                        .then(Commands.argument("mapname", StringArgumentType.string())
                                .then(Commands.argument("doorNumber", IntegerArgumentType.integer(0))
                                        .executes(DoorCommand::closeDoor)))));
                                        // /zombiedoor mode <mapname> <open|closed>
        dispatcher.register(Commands.literal("zombiedoor")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("mode")
                .then(Commands.argument("mapname", StringArgumentType.string())
                        .then(Commands.literal("open")
                                .executes(context -> setDoorMode(context, MapConfig.DoorMode.OPEN_BY_DEFAULT)))
                        .then(Commands.literal("closed")
                                .executes(context -> setDoorMode(context, MapConfig.DoorMode.CLOSED_BY_DEFAULT)))
                        .executes(DoorCommand::showDoorMode))));
    }

    private static int addDoor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");
        int doorNumber = IntegerArgumentType.getInteger(context, "doorNumber");
        int cost = IntegerArgumentType.getInteger(context, "cost");

        // Vérifier que la map existe
        if (!MapManager.mapExists(mapName)) {
            player.sendSystemMessage(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        // Vérifier que le joueur regarde une pancarte
        HitResult hit = player.pick(5.0, 0, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("§cYou need to look at the sign!"));
            return 0;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos signPos = blockHit.getBlockPos();
        ServerLevel level = player.serverLevel();
        BlockState signState = level.getBlockState(signPos);

        // Vérifier que c'est une pancarte murale
        if (!(signState.getBlock() instanceof WallSignBlock)) {
            player.sendSystemMessage(Component.literal("§cYou need to look at the wall sign!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);

        // Vérifier si la porte existe déjà
        if (map.hasDoor(doorNumber)) {
            player.sendSystemMessage(Component.literal("§cThe door n°" + doorNumber + " already exists on this map!"));
            return 0;
        }

        // Créer la porte
        DoorConfig door = new DoorConfig(doorNumber, signPos, cost);

        // Sauvegarder l'état de la pancarte (direction, texte, etc.)
        door.setSignBlock(signPos, signState);

        // Sauvegarder le texte de la pancarte (BlockEntity)
        var blockEntity = level.getBlockEntity(signPos);
        if (blockEntity != null) {
            net.minecraft.nbt.CompoundTag nbt = blockEntity.saveWithoutMetadata(level.registryAccess());
            door.getSignBlock().setBlockEntityData(nbt);
            System.out.println("[DoorCommand] Sign text saved!");
        }

        // Récupérer la direction de la pancarte
        Direction signFacing = signState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction wallDirection = signFacing.getOpposite(); // Le mur est derrière la pancarte

        // Scanner les blocs 3x3 derrière la pancarte
        scanWallBlocks(level, signPos, wallDirection, door);

        // Ajouter la porte à la map
        map.addDoor(door);
        MapManager.save();

        // Synchroniser avec tous les clients
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();

        int blockCount = door.getWallBlocks().size();
        player.sendSystemMessage(Component.literal("§aDoor n°" + doorNumber + " created on map '§e" + mapName + "§a':"));
        player.sendSystemMessage(Component.literal("  §fSign: §e" + signPos.getX() + ", " + signPos.getY() + ", " + signPos.getZ()));
        player.sendSystemMessage(Component.literal("  §fDirection: §e" + signFacing));
        player.sendSystemMessage(Component.literal("  §fBlocks saved: §e" + blockCount));
        player.sendSystemMessage(Component.literal("  §fCost: §e" + cost + " points"));
        player.sendSystemMessage(Component.literal("  §fState: §c§lCLOSED"));

        return 1;
    }

    /**
     * Scanne et sauvegarde les blocs 3x3 derrière la pancarte
     */
    private static void scanWallBlocks(ServerLevel level, BlockPos signPos, Direction wallDirection, DoorConfig door) {
        // Position de départ : 1 bloc dans la direction du mur
        BlockPos startPos = signPos.relative(wallDirection);

        // Déterminer les axes perpendiculaires
        Direction right, up;
        up = Direction.UP;

        // Déterminer la direction "droite" selon la direction du mur
        if (wallDirection == Direction.NORTH || wallDirection == Direction.SOUTH) {
            right = Direction.EAST;
        } else {
            right = Direction.SOUTH;
        }

        // Scanner 3x3 (centré sur startPos)
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos blockPos = startPos.relative(up, dy).relative(right, dx);
                BlockState state = level.getBlockState(blockPos);

                // Ne sauvegarder que les blocs solides (pas l'air)
                if (!state.isAir()) {
                    door.addWallBlock(blockPos, state);
                }
            }
        }
    }

    private static int removeDoor(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");
        int doorNumber = IntegerArgumentType.getInteger(context, "doorNumber");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);

        if (!map.hasDoor(doorNumber)) {
            context.getSource().sendFailure(Component.literal("§cThe door n°" + doorNumber + " does not exist on the map!"));
            return 0;
        }

        map.removeDoor(doorNumber);
        MapManager.save();

        // Synchroniser avec tous les clients
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();

        context.getSource().sendSuccess(() -> Component.literal("§aDoor n°" + doorNumber + " removed from map '§e" + mapName + "§a'"), true);
        return 1;
    }

    private static int listDoors(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String mapName = StringArgumentType.getString(context, "mapname");

        if (!MapManager.mapExists(mapName)) {
            player.sendSystemMessage(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);
        Map<Integer, DoorConfig> doors = map.getDoors();

        if (doors.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No doors on map '§e" + mapName + "§7'"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6§l=== DOORS OF " + mapName.toUpperCase() + " ==="));
        for (Map.Entry<Integer, DoorConfig> entry : doors.entrySet()) {
            DoorConfig door = entry.getValue();
            String status = door.isOpen() ? "§a§lOPEN" : "§c§lCLOSED";
            BlockPos pos = door.getSignPosition();

            player.sendSystemMessage(Component.literal("§eSoor #" + door.getDoorNumber() + " " + status));
            player.sendSystemMessage(Component.literal("  §7Sign: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            player.sendSystemMessage(Component.literal("  §7Blocks: §e" + door.getWallBlocks().size()));
            player.sendSystemMessage(Component.literal("  §7Cost: §e" + door.getCost() + " points"));
        }

        return 1;
    }

    private static int openDoor(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");
        int doorNumber = IntegerArgumentType.getInteger(context, "doorNumber");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);

        if (!map.hasDoor(doorNumber)) {
            context.getSource().sendFailure(Component.literal("§cThe door n°" + doorNumber + " does not exist on this map!"));
            return 0;
        }

        DoorConfig door = map.getDoor(doorNumber);
        ServerLevel level = context.getSource().getLevel();

        // Ouvrir physiquement la porte (détruire les blocs + pancarte)
        openDoorPhysically(level, door);

        // Marquer comme ouverte
        map.openDoor(doorNumber);
        MapManager.save();

        // Synchroniser avec tous les clients
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();

        context.getSource().sendSuccess(() -> Component.literal("§aDoor n°" + doorNumber + " open! Blocks removed."), true);
        return 1;
    }

    private static int closeDoor(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");
        int doorNumber = IntegerArgumentType.getInteger(context, "doorNumber");

        if (!MapManager.mapExists(mapName)) {
            context.getSource().sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }

        MapConfig map = MapManager.getMap(mapName);

        if (!map.hasDoor(doorNumber)) {
            context.getSource().sendFailure(Component.literal("§cThe door n°" + doorNumber + " does not exist on this map!"));
            return 0;
        }

        DoorConfig door = map.getDoor(doorNumber);
        ServerLevel level = context.getSource().getLevel();

        // Fermer physiquement la porte (remettre les blocs)
        closeDoorPhysically(level, door);

        // Marquer comme fermée
        map.closeDoor(doorNumber);
        MapManager.save();

        // Synchroniser avec tous les clients
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();

        context.getSource().sendSuccess(() -> Component.literal("§aDoor n°" + doorNumber + " closed! Blocks restored."), true);
        return 1;
    }

    /**
     * Ouvre physiquement la porte : détruit les blocs du mur et la pancarte
     */
    private static void openDoorPhysically(ServerLevel level, DoorConfig door) {
        // Détruire la pancarte
        BlockPos signPos = door.getSignPosition();
        if (signPos != null) {
            level.setBlock(signPos, Blocks.AIR.defaultBlockState(), 3);
        }

        // Détruire les blocs du mur
        for (DoorConfig.SavedBlock savedBlock : door.getWallBlocks()) {
            BlockPos pos = savedBlock.getPosition();
            if (pos != null) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Ferme physiquement la porte : remet les blocs du mur et la pancarte
     */
    private static void closeDoorPhysically(ServerLevel level, DoorConfig door) {
        // Remettre les blocs du mur
        for (DoorConfig.SavedBlock savedBlock : door.getWallBlocks()) {
            BlockPos pos = savedBlock.getPosition();
            BlockState state = savedBlock.getBlockState();
            if (pos != null && state != null) {
                level.setBlock(pos, state, 3);
            }
        }

        // Remettre la pancarte avec son texte
        DoorConfig.SavedBlock signBlock = door.getSignBlock();
        if (signBlock != null) {
            BlockPos signPos = signBlock.getPosition();
            BlockState signState = signBlock.getBlockState();

            if (signPos != null && signState != null) {
                // Placer le bloc de la pancarte
                level.setBlock(signPos, signState, 3);

                // Restaurer le texte de la pancarte (BlockEntity)
                net.minecraft.nbt.CompoundTag nbt = signBlock.getBlockEntityData();
                if (nbt != null) {
                    var blockEntity = level.getBlockEntity(signPos);
                    if (blockEntity != null) {
                        blockEntity.loadWithComponents(nbt, level.registryAccess());
                        blockEntity.setChanged();
                        System.out.println("[DoorCommand] Restored sign with text");
                    }
                }
            }
        }
    }
    public static void openAllDoorsAtGameEnd(ServerLevel level) {
        MapConfig map = MapManager.getSelectedMap();
        if (map == null) {
            return;
        }
    
        Map<Integer, DoorConfig> doors = map.getDoors();
        if (doors.isEmpty()) {
            return;
        }
    
        MapConfig.DoorMode mode = map.getDoorMode();
    
        if (mode == MapConfig.DoorMode.OPEN_BY_DEFAULT) {
            System.out.println("[DoorCommand] Opening " + doors.size() + " door(s) at game end (OPEN_BY_DEFAULT mode)");
    
            for (DoorConfig door : doors.values()) {
                if (!door.isOpen()) {
                    openDoorPhysically(level, door);
                    System.out.println("[DoorCommand] Door #" + door.getDoorNumber() + " physically opened");
                }
            }
    
            for (DoorConfig door : doors.values()) {
                map.openDoor(door.getDoorNumber());
            }
        } else {
            System.out.println("[DoorCommand] Closing " + doors.size() + " door(s) at game end (CLOSED_BY_DEFAULT mode)");
    
            for (DoorConfig door : doors.values()) {
                if (door.isOpen()) {
                    closeDoorPhysically(level, door);
                    System.out.println("[DoorCommand] Door #" + door.getDoorNumber() + " physically closed (reset)");
                }
            }
    
            for (DoorConfig door : doors.values()) {
                map.closeDoor(door.getDoorNumber());
            }
        }
    
        MapManager.save();
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();
    
        System.out.println("[DoorCommand] All doors reset for game end!");
    }
    public static void closeAllDoorsAtGameStart(ServerLevel level) {
        MapConfig map = MapManager.getSelectedMap();
        if (map == null) {
            return;
        }
    
        if (map.getDoorMode() != MapConfig.DoorMode.OPEN_BY_DEFAULT) {
            System.out.println("[DoorCommand] Skipping door closure - map is in CLOSED_BY_DEFAULT mode");
            return;
        }
    
        Map<Integer, DoorConfig> doors = map.getDoors();
        if (doors.isEmpty()) {
            return;
        }
    
        System.out.println("[DoorCommand] Closing " + doors.size() + " door(s) at game start (OPEN_BY_DEFAULT mode)");
    
        for (DoorConfig door : doors.values()) {
            if (door.isOpen()) {
                closeDoorPhysically(level, door);
                System.out.println("[DoorCommand] Door #" + door.getDoorNumber() + " physically closed");
            }
        }
    
        for (DoorConfig door : doors.values()) {
            map.closeDoor(door.getDoorNumber());
        }
        MapManager.save();
    
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();
    
        System.out.println("[DoorCommand] All doors closed for game start!");
    }
    /**
     * Réinitialise toutes les portes de la map active (en fin de partie)
     * Ferme physiquement toutes les portes ouvertes et réinitialise leur état
     */
    public static void resetAllDoors(ServerLevel level) {
        MapConfig map = MapManager.getSelectedMap();
        if (map == null) {
            return;
        }

        Map<Integer, DoorConfig> doors = map.getDoors();
        if (doors.isEmpty()) {
            return;
        }

        System.out.println("[DoorCommand] Resetting " + doors.size() + " door(s)");

        for (DoorConfig door : doors.values()) {
            // Si la porte est ouverte, la fermer physiquement
            if (door.isOpen()) {
                closeDoorPhysically(level, door);
                System.out.println("[DoorCommand] Door #" + door.getDoorNumber() + " physically closed");
            }
        }

        // Réinitialiser l'état de toutes les portes dans la config
        map.resetDoors();
        openAllDoorsAtGameEnd(level);
        MapManager.save();

        // Synchroniser avec tous les clients
        com.zombiemod.system.ServerDoorTracker.syncToAllPlayers();

        System.out.println("[DoorCommand] All doors reset!");
    }
    private static int setDoorMode(CommandContext<CommandSourceStack> context, MapConfig.DoorMode mode) {
        String mapName = StringArgumentType.getString(context, "mapname");
        CommandSourceStack source = context.getSource();
    
        if (!MapManager.mapExists(mapName)) {
            source.sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }
    
        MapConfig map = MapManager.getMap(mapName);
        MapConfig.DoorMode oldMode = map.getDoorMode();
        map.setDoorMode(mode);
        MapManager.save();
    
        String modeText = mode == MapConfig.DoorMode.OPEN_BY_DEFAULT ? "§aOPEN BY DEFAULT" : "§cCLOSED BY DEFAULT";
        String oldModeText = oldMode == MapConfig.DoorMode.OPEN_BY_DEFAULT ? "open by default" : "closed by default";
    
        source.sendSuccess(() -> Component.literal("§aDoor mode for map '§e" + mapName + "§a' changed:"), true);
        source.sendSuccess(() -> Component.literal("  §7Previous: §f" + oldModeText), false);
        source.sendSuccess(() -> Component.literal("  §7New: " + modeText), false);
    
        if (mode == MapConfig.DoorMode.OPEN_BY_DEFAULT) {
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§e§lBehavior:"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors are §aOPEN §7when no game is running"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors §cCLOSE §7automatically when game starts"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors §aOPEN §7automatically when game ends"), false);
        } else {
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§e§lBehavior:"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors are §cCLOSED §7when no game is running"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors stay §cCLOSED §7when game starts"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors reset to §cCLOSED §7when game ends"), false);
        }
    
        return 1;
    }
    
    private static int showDoorMode(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "mapname");
        CommandSourceStack source = context.getSource();
    
        if (!MapManager.mapExists(mapName)) {
            source.sendFailure(Component.literal("§cThe map '" + mapName + "' does not exist!"));
            return 0;
        }
    
        MapConfig map = MapManager.getMap(mapName);
        MapConfig.DoorMode mode = map.getDoorMode();
    
        String modeText = mode == MapConfig.DoorMode.OPEN_BY_DEFAULT ? "§aOPEN BY DEFAULT" : "§cCLOSED BY DEFAULT";
    
        source.sendSuccess(() -> Component.literal("§6§l=== DOOR MODE FOR " + mapName.toUpperCase() + " ==="), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§eCurrent Mode: " + modeText), false);
        source.sendSuccess(() -> Component.literal(""), false);
    
        if (mode == MapConfig.DoorMode.OPEN_BY_DEFAULT) {
            source.sendSuccess(() -> Component.literal("§e§lBehavior:"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors are §aOPEN §7when no game is running"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors §cCLOSE §7automatically when game starts"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors §aOPEN §7automatically when game ends"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7To change to closed by default:"), false);
            source.sendSuccess(() -> Component.literal("§f/zombiedoor mode " + mapName + " closed"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§e§lBehavior:"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors are §cCLOSED §7when no game is running"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors stay §cCLOSED §7when game starts"), false);
            source.sendSuccess(() -> Component.literal("  §7• Doors reset to §cCLOSED §7when game ends"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7To change to open by default:"), false);
            source.sendSuccess(() -> Component.literal("§f/zombiedoor mode " + mapName + " open"), false);
        }
    
        return 1;
    }
}
