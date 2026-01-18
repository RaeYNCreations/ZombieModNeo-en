package com.zombiemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zombiemod.system.ServerWeaponCrateTracker;
import com.zombiemod.system.WeaponCrateAnimationManager;
import com.zombiemod.system.WeaponCrateManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeaponCrateCommand {

    // Suggestion provider pour tous les items (vanilla + mods)
    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ITEM.keySet().stream(),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /weaponcrate create <cost>
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                .executes(WeaponCrateCommand::createCrate))));

        // /weaponcrate addweapon <item> [count] [weight] [name]
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("addweapon")
                        .then(Commands.literal("hand")
                                // /weaponcrate addweapon hand [weight] [name]
                                .executes(WeaponCrateCommand::addWeaponFromHand)
                                .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                        .executes(WeaponCrateCommand::addWeaponFromHandWithWeight)
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(WeaponCrateCommand::addWeaponFromHandWithName))))
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests(ITEM_SUGGESTIONS)
                                // Version simple : juste l'item (count=1, weight=10, nom auto)
                                .executes(WeaponCrateCommand::addWeaponSimple)
                                // Version avec count
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(WeaponCrateCommand::addWeaponWithCount)
                                        // Version avec count et weight
                                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                                .executes(WeaponCrateCommand::addWeaponWithWeight)
                                                // Version complète avec name
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(WeaponCrateCommand::addWeapon)))))));

        // /weaponcrate preset starter
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preset")
                        .then(Commands.literal("starter")
                                .executes(WeaponCrateCommand::createStarterPreset))));

        // /weaponcrate preset advanced
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preset")
                        .then(Commands.literal("advanced")
                                .executes(WeaponCrateCommand::createAdvancedPreset))));

        // /weaponcrate preset legendary
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preset")
                        .then(Commands.literal("legendary")
                                .executes(WeaponCrateCommand::createLegendaryPreset))));

        // /weaponcrate addammo hand <prix>
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("addammo")
                        .then(Commands.literal("hand")
                                .then(Commands.argument("prix", IntegerArgumentType.integer(0))
                                        .executes(WeaponCrateCommand::addAmmoFromHand)))));

        // /weaponcrate reload
        dispatcher.register(Commands.literal("weaponcrate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(WeaponCrateCommand::reloadDisplays)));
    }

    private static int createCrate(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        int cost = IntegerArgumentType.getInteger(context, "cost");
        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        WeaponCrateManager.setWeaponCrate(player.level(), chestPos, cost);
        player.sendSystemMessage(Component.literal("§aWeapons crate created with a cost of §e" + cost + " point(s)§a."));
        player.sendSystemMessage(Component.literal("§7Use §e/weaponcrate addweapon §7to add items or weapons."));

        return 1;
    }

    private static int addWeapon(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String item = StringArgumentType.getString(context, "item");
        int count = IntegerArgumentType.getInteger(context, "count");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        String name = StringArgumentType.getString(context, "name");

        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        if (!WeaponCrateManager.isWeaponCrate(player.level(), chestPos)) {
            player.sendSystemMessage(Component.literal("§cThis chest is not a weapons crate! Use §e/weaponcrate create <cost>"));
            return 0;
        }

        WeaponCrateManager.addWeapon(player.level(), chestPos, item, count, weight, name, null);
        player.sendSystemMessage(Component.literal("§aWeapon/items added: §e" + name + " §7(weight: " + weight + ")"));

        return 1;
    }

    // Version simple : /weaponcrate addweapon minecraft:wooden_sword
    private static int addWeaponSimple(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String item = StringArgumentType.getString(context, "item");
        int count = 1;
        int weight = 10;
        String name = generateDefaultName(item);

        return addWeaponInternal(player, item, count, weight, name);
    }

    // Version avec count : /weaponcrate addweapon minecraft:wooden_sword 1
    private static int addWeaponWithCount(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String item = StringArgumentType.getString(context, "item");
        int count = IntegerArgumentType.getInteger(context, "count");
        int weight = 10;
        String name = generateDefaultName(item);

        return addWeaponInternal(player, item, count, weight, name);
    }

    // Version avec weight : /weaponcrate addweapon minecraft:wooden_sword 1 20
    private static int addWeaponWithWeight(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String item = StringArgumentType.getString(context, "item");
        int count = IntegerArgumentType.getInteger(context, "count");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        String name = generateDefaultName(item);

        return addWeaponInternal(player, item, count, weight, name);
    }

    // /weaponcrate addweapon hand - Ajoute l'item en main (weight=10, nom de l'item)
    private static int addWeaponFromHand(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        return addWeaponFromHandInternal(player, 10, null);
    }

    // /weaponcrate addweapon hand <weight>
    private static int addWeaponFromHandWithWeight(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        int weight = IntegerArgumentType.getInteger(context, "weight");
        return addWeaponFromHandInternal(player, weight, null);
    }

    // /weaponcrate addweapon hand <weight> <name>
    private static int addWeaponFromHandWithName(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        int weight = IntegerArgumentType.getInteger(context, "weight");
        String name = StringArgumentType.getString(context, "name");
        return addWeaponFromHandInternal(player, weight, name);
    }

    // Méthode pour ajouter l'item en main avec tous ses tags
    private static int addWeaponFromHandInternal(ServerPlayer player, int weight, String customName) {
        // Vérifier le coffre
        BlockPos chestPos = getTargetedChest(player);
        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        if (!WeaponCrateManager.isWeaponCrate(player.level(), chestPos)) {
            player.sendSystemMessage(Component.literal("§cThis chest is not a weapons crate! Use §e/weaponcrate create <cost>"));
            return 0;
        }

        // Récupérer l'item en main
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item in your hand!"));
            return 0;
        }

        // Extraire les informations de l'item
        String itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
        int count = heldItem.getCount();

        // Utiliser le nom custom fourni, ou le nom de l'item, ou générer un nom par défaut
        String displayName;
        if (customName != null && !customName.isEmpty()) {
            displayName = customName;
        } else if (heldItem.has(DataComponents.CUSTOM_NAME)) {
            displayName = heldItem.get(DataComponents.CUSTOM_NAME).getString();
        } else {
            displayName = generateDefaultName(itemId);
        }

        // Utiliser la nouvelle méthode qui sauvegarde l'ItemStack complet avec tous ses DataComponents
        WeaponCrateManager.addWeaponFromItemStack(player.level(), chestPos, heldItem, weight, displayName);

        // Compter les enchantements pour le message
        ItemEnchantments itemEnchantments = heldItem.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int enchantmentCount = itemEnchantments.size();

        // Message de confirmation
        String enchInfo = enchantmentCount > 0 ? " §7with §e" + enchantmentCount + " enchantement(s)" : "";
        player.sendSystemMessage(Component.literal("§aWeapons/items added: §e" + displayName + " §7(x" + count + ", weight: " + weight + ")" + enchInfo));
        player.sendSystemMessage(Component.literal("§7All tags and DataComponents have been preserved."));

        return 1;
    }

    // Méthode interne partagée
    private static int addWeaponInternal(ServerPlayer player, String item, int count, int weight, String name) {
        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        if (!WeaponCrateManager.isWeaponCrate(player.level(), chestPos)) {
            player.sendSystemMessage(Component.literal("§cThis chest is not a weapons crate! Use §e/weaponcrate create <cost"));
            return 0;
        }

        WeaponCrateManager.addWeapon(player.level(), chestPos, item, count, weight, name, null);
        player.sendSystemMessage(Component.literal("§aWeapons/items added: §e" + name + " §7(x" + count + ", weight: " + weight + ")"));

        return 1;
    }

    // Génère un nom par défaut basé sur l'ID de l'item
    private static String generateDefaultName(String itemId) {
        // Retirer "minecraft:" si présent
        String cleanId = itemId.replace("minecraft:", "");

        // Remplacer underscores par espaces et capitaliser
        String[] parts = cleanId.split("_");
        StringBuilder name = new StringBuilder();

        for (String part : parts) {
            if (!name.isEmpty()) {
                name.append(" ");
            }
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0)));
                name.append(part.substring(1));
            }
        }

        return "§f" + name.toString();
    }

    private static int createStarterPreset(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        WeaponCrateManager.createStarterCrate(player.level(), chestPos);
        player.sendSystemMessage(Component.literal("§aCrate §6STARTER §acreated (500 points)!"));

        return 1;
    }

    private static int createAdvancedPreset(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        WeaponCrateManager.createAdvancedCrate(player.level(), chestPos);
        player.sendSystemMessage(Component.literal("§aCrate §bADVANCED §acreated (1500 points)!"));

        return 1;
    }

    private static int createLegendaryPreset(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        BlockPos chestPos = getTargetedChest(player);

        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        WeaponCrateManager.createLegendaryCrate(player.level(), chestPos);
        player.sendSystemMessage(Component.literal("§aCrate §4§lLEGENDARY §acreate (5000 points)!"));

        return 1;
    }

    // /weaponcrate addammo hand <prix>
    private static int addAmmoFromHand(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        int prix = IntegerArgumentType.getInteger(context, "prix");

        // Vérifier le coffre
        BlockPos chestPos = getTargetedChest(player);
        if (chestPos == null) {
            player.sendSystemMessage(Component.literal("§cYou need to look at a chest!"));
            return 0;
        }

        if (!WeaponCrateManager.isWeaponCrate(player.level(), chestPos)) {
            player.sendSystemMessage(Component.literal("§cThis chest is not a weapons crate! Use §e/weaponcrate create <cost>"));
            return 0;
        }

        // Récupérer l'item en main
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item in your hand!"));
            return 0;
        }

        // Ajouter les munitions
        WeaponCrateManager.addAmmo(player.level(), chestPos, heldItem, prix);

        // Récupérer le nom de l'item pour l'affichage
        String displayName;
        if (heldItem.has(DataComponents.CUSTOM_NAME)) {
            displayName = heldItem.get(DataComponents.CUSTOM_NAME).getString();
        } else {
            String itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
            displayName = generateDefaultName(itemId);
        }

        player.sendSystemMessage(Component.literal("§aAmmo added: §e" + displayName + " §7(x" + heldItem.getCount() + ", cost: " + prix + ")"));
        player.sendSystemMessage(Component.literal("§7All tags and DataComponents have been preserved."));

        return 1;
    }

    // /weaponcrate reload - Recharge tous les items display au-dessus des weapon crates
    private static int reloadDisplays(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Récupérer toutes les weapon crates enregistrées
        Map<BlockPos, Integer> allCrates = ServerWeaponCrateTracker.getAllCrates();

        if (allCrates.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo weapon crates were found on the server."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§7Reloading the displays for §e" + allCrates.size() + " §7weapon crate(s)..."));

        int reloadedCount = 0;

        // Pour chaque weapon crate
        for (Map.Entry<BlockPos, Integer> entry : allCrates.entrySet()) {
            BlockPos pos = entry.getKey();

            // Arrêter l'animation/display existant
            WeaponCrateAnimationManager.stopAnimation(level, pos);

            // Récupérer toutes les armes de la crate
            List<WeaponCrateManager.WeaponConfig> allWeapons = WeaponCrateManager.getAllWeapons(level, pos);

            // Si la crate a exactement 1 arme, créer un affichage statique
            if (allWeapons.size() == 1) {
                WeaponCrateManager.WeaponConfig weapon = allWeapons.get(0);
                ItemStack itemStack = weapon.toItemStack(level);

                if (!itemStack.isEmpty()) {
                    WeaponCrateAnimationManager.startStaticDisplay(level, pos, itemStack);
                    reloadedCount++;
                }
            }
            // Si plusieurs armes : ne rien faire, l'affichage se fera lors de l'ouverture
        }

        player.sendSystemMessage(Component.literal("§a✓ §e" + reloadedCount + " §adisplay(s) successfully reloaded!"));
        player.sendSystemMessage(Component.literal("§7Note: Crates with multiple weapons will display their roulette wheel when they are opening."));

        return 1;
    }

    private static BlockPos getTargetedChest(ServerPlayer player) {
        HitResult hit = player.pick(5.0, 0, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();

            if (player.level().getBlockState(pos).getBlock() instanceof ChestBlock) {
                return pos;
            }
        }

        return null;
    }
}
