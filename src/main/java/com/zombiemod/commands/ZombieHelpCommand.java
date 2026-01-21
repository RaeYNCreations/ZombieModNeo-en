package com.zombiemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ZombieHelpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zombiehelp")
                .requires(source -> source.hasPermission(2))
                .executes(ZombieHelpCommand::showHelp));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6§l============ ZOMBIE MODE - HELP ============"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // GESTION DE PARTIE
        source.sendSuccess(() -> Component.literal("§e§l► GAME MANAGEMENT:"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiestart [mapName] §7- Start a game"), false);
        source.sendSuccess(() -> Component.literal("  §760-second countdown timer, optional: specify a map"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/zombiestart §7or §f/zombiestart arena1"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiestop §7- Stop the current game (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiejoin §7- Join the game"), false);
        source.sendSuccess(() -> Component.literal("§a/zombieleave §7- Quit the game"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiestatus §7- Check the status of the game"), false);
        source.sendSuccess(() -> Component.literal("§a/zombieskip §7- Move on to the next wave (admin)"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Range
        source.sendSuccess(() -> Component.literal("§e§l► GUN RANGE MANAGEMENT:"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangestart [timer] [mapName] §7- Start the gun range"), false);
        source.sendSuccess(() -> Component.literal("  §7optional: specify a countdown timer; specify a gun range"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/zombierangestart §7or §f/zombierangestart 15 arena1"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangestop §7- Stop the current gun range"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangejoin §7- Join the gun range"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangeleave §7- Quit the gun range"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangestatus §7- Check the status of the gun range"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierangeskip §7- Move on to the next wave (admin)"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // GESTION DES MAPS
        source.sendSuccess(() -> Component.literal("§e§l► MAP MANAGEMENT:"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiemap create <nom> §7- Create new map (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiemap delete <nom> §7- Delete a map (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiemap select <nom> §7- Select active map"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiemap list §7- List all maps"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiemap info [nom] §7- Info of a map"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // CONFIGURATION: SPAWN POINTS
        source.sendSuccess(() -> Component.literal("§e§l► SPAWN POINTS:"), false);
        source.sendSuccess(() -> Component.literal("§a/zombierespawn set <mapname> §7- Set player respawn"), false);
        source.sendSuccess(() -> Component.literal("  §7At your current position (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiespawn add <mapname> [doorNumber] §7- Add zombie spawn (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Without doorNumber: spawn always active"), false);
        source.sendSuccess(() -> Component.literal("  §7With doorNumber: active only when the door is open"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/zombiespawn add arena1 §7or §f/zombiespawn add arena1 1"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiespawn clear <mapname> §7- Clear all zombie spawns (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiespawn list <mapname> §7- List all zombie spawns (admin)"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // CONFIGURATION: PORTES
        source.sendSuccess(() -> Component.literal("§e§l► DOORS:"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiedoor add <mapname> <number> <cost> (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Look at a wall sign, then type the command"), false);
        source.sendSuccess(() -> Component.literal("  §7Place the sign + 3x3 wall behind it"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/zombiedoor add arena1 1 750"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiedoor remove <mapname> <number> §7- Remove a door (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiedoor list <mapname> §7- List all doors (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiedoor open <mapname> <number> §7- Open door (destroy blocks) (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiedoor close <mapname> <number> §7- Closed door (restoring blocks) (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7The doors close automatically at the end of the game."), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // CAISSES D'ARMES
        source.sendSuccess(() -> Component.literal("§e§l► WEAPON CRATES (Mystery Box):"), false);
        source.sendSuccess(() -> Component.literal("§a/weaponcrate add <cost> <itemId> §7- Create a crate (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Look at a chest, then type the command"), false);
        source.sendSuccess(() -> Component.literal("  §7itemId: the weapon in your hand (§ftacz:xx§7 or §fminecraft:xx§7)"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/weaponcrate add 500 tacz:ak47"), false);
        source.sendSuccess(() -> Component.literal("§a/weaponcrate addammo <itemId> <quantity> <cost> (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Add ammunition (left-click purchase)"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/weaponcrate addammo tacz:ammo_9mm 30 100"), false);
        source.sendSuccess(() -> Component.literal("§a/weaponcrate remove §7- Remove a crate (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/weaponcrate scan §7- Scan all crates (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/weaponcrate reload §7- Reset all crates (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Useful after deleting all entities"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // JUKEBOXES
        source.sendSuccess(() -> Component.literal("§e§l► JUKEBOXES (Music):"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiejukebox add <cost> §7- Create a zombie jukebox (admin)"), false);
        source.sendSuccess(() -> Component.literal("  §7Look at a jukebox with a record, then type"), false);
        source.sendSuccess(() -> Component.literal("  §7Example: §f/zombiejukebox add 1000"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiejukebox remove §7- Remove a zombie jukebox (admin)"), false);
        source.sendSuccess(() -> Component.literal("§a/zombiejukebox list §7- List all jukeboxes (admin)"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // INFORMATIONS
        source.sendSuccess(() -> Component.literal("§e§l► GAME MECHANICS:"), false);
        source.sendSuccess(() -> Component.literal("§7• Starting points: §e500 points"), false);
        source.sendSuccess(() -> Component.literal("§7• Killed zombie: §e+100 points"), false);
        source.sendSuccess(() -> Component.literal("§7• Zombies per wave: §c6 + (wave × 6)"), false);
        source.sendSuccess(() -> Component.literal("§7• HP zombies: §c1 heart + 0.5 heart/wave"), false);
        source.sendSuccess(() -> Component.literal("§7• 15% chance zombie with armor"), false);
        source.sendSuccess(() -> Component.literal("§7• Cooldown between waves: §610 secondes"), false);
        source.sendSuccess(() -> Component.literal("§7• Dead players: §erespawn at the end of the wave"), false);
        source.sendSuccess(() -> Component.literal("§7• Doors: §eactivate new zombie spawns"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // GAME MODES
        source.sendSuccess(() -> Component.literal("§e§l► GAME MODES:"), false);
        source.sendSuccess(() -> Component.literal("§aZombie Mode: §7Standard survival waves"), false);
        source.sendSuccess(() -> Component.literal("  §7Commands: /zombiestart, /zombiejoin, /zombieleave"), false);
        source.sendSuccess(() -> Component.literal("  §7Inventory: §eKept between games"), false);
        source.sendSuccess(() -> Component.literal("§aGun Range: §7Practice mode with cleared inventory"), false);
        source.sendSuccess(() -> Component.literal("  §7Commands: /zombierangestart, /zombierangejoin, /zombierangeleave"), false);
        source.sendSuccess(() -> Component.literal("  §7Inventory: §cCleared on start/stop"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // FICHIERS
        source.sendSuccess(() -> Component.literal("§e§l► CONFIGURE FILES:"), false);
        source.sendSuccess(() -> Component.literal("§7Folder: §fconfig/"), false);
        source.sendSuccess(() -> Component.literal("  §f• zombiemod.json §7- Main Gameplay Config"), false);
        source.sendSuccess(() -> Component.literal("  §f• zombiemod-maps.json §7- Maps Config"), false);
        source.sendSuccess(() -> Component.literal("  §f• zombiemod-drops.json §7- Drops Config"), false);
        source.sendSuccess(() -> Component.literal("  §f• zombiemod-mobs.json §7- Mob Config"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        source.sendSuccess(() -> Component.literal("§6§l============================================"), false);

        return 1;
    }
}
