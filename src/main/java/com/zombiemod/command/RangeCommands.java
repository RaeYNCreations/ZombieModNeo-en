package com.zombiemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zombiemod.manager.GameManager;
import com.zombiemod.manager.InventoryManager;
import com.zombiemod.manager.PointsManager;
import com.zombiemod.manager.WaveManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RangeCommands {

    // Storage for saved inventories specific to range mode
    private static final Map<UUID, CompoundTag> rangeInventories = new HashMap<>();

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /zombierangestart [seconds] [mapName]
        dispatcher.register(Commands.literal("zombierangestart")
                .executes(RangeCommands::startRangeGame)
                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 300))
                        .executes(RangeCommands::startRangeGameWithTimer)
                        .then(Commands.argument("mapName", StringArgumentType.word())
                                .executes(RangeCommands::startRangeGameWithTimerAndMap))));

        // /zombierangestop
        dispatcher.register(Commands.literal("zombierangestop")
                .executes(RangeCommands::stopRangeGame));

        // /zombierangejoin
        dispatcher.register(Commands.literal("zombierangejoin")
                .executes(RangeCommands::joinRangeGame));

        // /zombierangeleave
        dispatcher.register(Commands.literal("zombierangeleave")
                .executes(RangeCommands::leaveRangeGame));

        // /zombierangestatus
        dispatcher.register(Commands.literal("zombierangestatus")
                .executes(RangeCommands::showRangeStatus));

        // /zombierangeskip
        dispatcher.register(Commands.literal("zombierangeskip")
                .requires(source -> source.hasPermission(2))
                .executes(RangeCommands::skipRangeWave));
    }

    private static int startRangeGame(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        if (GameManager.getGameState() != GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cGun range is already underway!"));
            return 0;
        }

        GameManager.startGame(level, null); // Pas de nom de map spécifié
        context.getSource().sendSuccess(() -> Component.literal("§aGun range On!"), true);

        return 1;
    }

    private static int startRangeGameWithTimer(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");

        if (GameManager.getGameState() != GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cGun range is already underway!"));
            return 0;
        }

        GameManager.startGameWithCustomTimer(level, null, seconds);
        context.getSource().sendSuccess(() -> Component.literal("§aGun range On! " + seconds + "-second counter started!"), true);

        return 1;
    }

    private static int startRangeGameWithTimerAndMap(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        String mapName = StringArgumentType.getString(context, "mapName");

        if (GameManager.getGameState() != GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cGun range is already underway!"));
            return 0;
        }

        GameManager.startGameWithCustomTimer(level, mapName, seconds);
        context.getSource().sendSuccess(() -> Component.literal("§aGun range launched on map §e" + mapName + " §a! Game starts in " + seconds + " seconds!"), true);

        return 1;
    }

    private static int stopRangeGame(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        if (GameManager.getGameState() == GameManager.GameState.WAITING) {
            context.getSource().sendFailure(Component.literal("§cNo Gun ranges currently in progress!"));
            return 0;
        }

        // Annonce de l'arrêt
        GameManager.broadcastToAll(level, "§c§l=== GUN RANGE ENDED ===");

        // Nettoyer tous les mobs avec la méthode dédiée
        WaveManager.killAllMobs();

        // Restaurer les inventaires et mettre tous les joueurs en survival
        for (UUID uuid : GameManager.getActivePlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                player.sendSystemMessage(Component.literal("§7The Gun range has been stopped."));
            }
        }

        for (UUID uuid : GameManager.getWaitingPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                player.sendSystemMessage(Component.literal("§7The Gun range has been stopped."));
            }
        }

        // Réinitialiser tous les managers
        GameManager.reset();
        WaveManager.reset();

        // Clear range inventories
        clearRangeInventories();

        // Réinitialiser les portes (fermer physiquement et réinitialiser l'état)
        DoorCommand.openAllDoorsAtGameEnd(level);

        context.getSource().sendSuccess(() -> Component.literal("§aGun range stopped, zombies cleared, and game reset."), true);

        return 1;
    }

    private static int joinRangeGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        saveRangeInventory(player);
        InventoryManager.clearInventory(player);
        GameManager.joinGame(player, level);

        return 1;
    }

    private static int leaveRangeGame(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        restoreRangeInventory(player);
        InventoryManager.clearInventory(player);
        GameManager.leaveGame(player, level);

        return 1;
    }

    private static int showRangeStatus(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) return 0;

        player.sendSystemMessage(Component.literal("§6§l=== GUN RANGE STATUS ==="));
        String mapName = GameManager.getCurrentMapName();
        if (mapName != null && !mapName.isEmpty()) {
            player.sendSystemMessage(Component.literal("§eRange: §6" + mapName));
        }
        player.sendSystemMessage(Component.literal("§eRange Status: §f" + getStateName(GameManager.getGameState())));
        player.sendSystemMessage(Component.literal("§eWave: §f" + WaveManager.getCurrentWave()));
        player.sendSystemMessage(Component.literal("§eZombies remaining: §f" + WaveManager.getZombiesRemaining()));
        player.sendSystemMessage(Component.literal("§ePlayers active: §f" + GameManager.getActivePlayers().size()));
        player.sendSystemMessage(Component.literal("§ePlayers waiting: §f" + GameManager.getWaitingPlayers().size()));

        if (GameManager.isPlayerActive(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§aYou're in the Gun range!"));
            player.sendSystemMessage(Component.literal("§eYour points: §f" + PointsManager.getPoints(player.getUUID())));
        } else if (GameManager.isPlayerWaiting(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§eYou are waiting..."));
        } else {
            player.sendSystemMessage(Component.literal("§7You are not in the Gun range. Step on the activator pad or type §e/zombierangejoin §7to join."));
        }

        return 1;
    }

    private static int skipRangeWave(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        GameManager.GameState state = GameManager.getGameState();

        if (state != GameManager.GameState.WAVE_ACTIVE) {
            context.getSource().sendFailure(Component.literal("§cNo waves currently! Current Status: " + getStateName(state)));
            return 0;
        }

        int currentWave = WaveManager.getCurrentWave();
        int zombiesRemaining = WaveManager.getZombiesRemaining();

        // Annonce du skip
        GameManager.broadcastToAll(level, "§6§l[ADMIN] §eWave " + currentWave + " finished! §7(" + zombiesRemaining + " zombies remaining)");

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

    /**
     * Saves a player's inventory specifically for range mode
     */
    private static void saveRangeInventory(ServerPlayer player) {
        CompoundTag inventoryData = new CompoundTag();

        // Save main inventory
        Inventory inventory = player.getInventory();
        ListTag itemsList = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(player.level().registryAccess(), itemTag);
                itemsList.add(itemTag);
            }
        }
        inventoryData.put("Items", itemsList);

        // Save armor
        ListTag armorList = new ListTag();
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(player.level().registryAccess(), itemTag);
                armorList.add(itemTag);
            }
        }
        inventoryData.put("Armor", armorList);

        // Save offhand
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            CompoundTag offhandTag = new CompoundTag();
            offhand.save(player.level().registryAccess(), offhandTag);
            inventoryData.put("Offhand", offhandTag);
        }

        // Sauvegarder le niveau d'XP
        inventoryData.putInt("XpLevel", player.experienceLevel);
        inventoryData.putFloat("XpProgress", player.experienceProgress);
        inventoryData.putInt("XpTotal", player.totalExperience);

        // Sauvegarder Curios si disponible
        if (isCuriosLoaded()) {
            try {
                saveCuriosInventory(player, inventoryData);
            } catch (Exception e) {
                System.err.println("[InventoryManager] ERROR: Could not save Curios: " + e.getMessage());
            }
        }

        rangeInventories.put(player.getUUID(), inventoryData);
        System.out.println("[RangeCommands] Saved inventory for player: " + player.getName().getString());
    }

    /**
     * Sauvegarde l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void saveCuriosInventory(ServerPlayer player, CompoundTag inventoryData) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            ListTag curiosData = handler.saveInventory(true);
            inventoryData.put("Curios", curiosData);
        });
    }

    /**
     * Restores a player's inventory from range mode storage
     */
    private static void restoreRangeInventory(ServerPlayer player) {
        CompoundTag inventoryData = rangeInventories.get(player.getUUID());
        if (inventoryData == null) {
            System.out.println("[RangeCommands] No saved inventory found for player: " + player.getName().getString());
            return;
        }

        // Clear current inventory first
        player.getInventory().clearContent();

        // Restore main inventory
        ListTag itemsList = inventoryData.getList("Items", 10); // 10 = TAG_COMPOUND
        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            ItemStack stack = ItemStack.parseOptional(player.level().registryAccess(), itemTag);
            if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                player.getInventory().setItem(slot, stack);
            }
        }

        // Restore armor
        ListTag armorList = inventoryData.getList("Armor", 10);
        for (int i = 0; i < armorList.size(); i++) {
            CompoundTag itemTag = armorList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            ItemStack stack = ItemStack.parseOptional(player.level().registryAccess(), itemTag);
            if (slot >= 0 && slot < player.getInventory().armor.size()) {
                player.getInventory().armor.set(slot, stack);
            }
        }

        // Restore offhand
        if (inventoryData.contains("Offhand")) {
            CompoundTag offhandTag = inventoryData.getCompound("Offhand");
            ItemStack offhand = ItemStack.parseOptional(player.level().registryAccess(), offhandTag);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, offhand);
        }

        // Restaurer l'XP
        if (inventoryData.contains("XpLevel")) {
            player.experienceLevel = inventoryData.getInt("XpLevel");
            player.experienceProgress = inventoryData.getFloat("XpProgress");
            player.totalExperience = inventoryData.getInt("XpTotal");
        }

        // Restaurer Curios si disponible
        if (isCuriosLoaded() && inventoryData.contains("Curios")) {
            try {
                restoreCuriosInventory(player, inventoryData);
            } catch (Exception e) {
                System.err.println("[InventoryManager] ERROR: Could not load Curios: " + e.getMessage());
            }
        }

        // Remove from storage after restoring
        rangeInventories.remove(player.getUUID());
        System.out.println("[RangeCommands] Restored inventory for player: " + player.getName().getString());
    }

    /**
     * Restaure l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void restoreCuriosInventory(ServerPlayer player, CompoundTag inventoryData) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            ListTag curiosData = inventoryData.getList("Curios", 10); // 10 = CompoundTag
            handler.loadInventory(curiosData);
        });
    }

    /**
     * Clears all saved range inventories
     */
    private static void clearRangeInventories() {
        rangeInventories.clear();
        System.out.println("[RangeCommands] Cleared all range inventories");
    }

    /**
     * Clear l'inventaire Curios (méthode séparée pour isolation)
     */
    private static void clearCuriosInventory(ServerPlayer player) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((slotId, slotHandler) -> {
                for (int i = 0; i < slotHandler.getSlots(); i++) {
                    slotHandler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                }
            });
        });
    }

    /**
     * Checks if a player has a saved range inventory
     */
    private static boolean hasSavedRangeInventory(UUID uuid) {
        return rangeInventories.containsKey(uuid);
    }

    /**
     * Supprime la sauvegarde d'inventaire d'un joueur (sans restaurer)
     */
    public static void removeSavedInventory(UUID uuid) {
        rangeInventories.remove(uuid);
    }

    /**
     * Reset complet (fin de partie)
     */
    public static void reset() {

        rangeInventories.clear();
    }
}