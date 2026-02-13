package com.zombiemod.event;

import com.zombiemod.manager.GameManager;
import com.zombiemod.manager.InventoryManager;
import com.zombiemod.system.ServerWeaponCrateTracker;
import com.zombiemod.network.NetworkHandler;
import com.zombiemod.network.packet.WeaponCrateSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerConnectionHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Envoyer la liste des weapon crates au joueur qui vient de se connecter
            ServerWeaponCrateTracker.syncToPlayer(player);

            // Envoyer la liste des jukeboxes au joueur qui vient de se connecter
            com.zombiemod.system.ServerJukeboxTracker.syncToPlayer(player);

            // Envoyer la liste des portes au joueur qui vient de se connecter
            com.zombiemod.system.ServerDoorTracker.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            java.util.UUID playerUUID = player.getUUID();

            // Vérifier si une partie est en cours
            if (GameManager.getGameState() != GameManager.GameState.WAITING) {
                // Vérifier si tous les joueurs sont déconnectés
                if (GameManager.areAllPlayersDisconnected(level)) {
                    System.out.println("[ZombieMod] All players are disconnected - Game automatically stopped");
                    GameManager.stopGameAutomatic(level);
                }
            }

            // MEMORY LEAK FIX: Nettoyer les données du joueur déconnecté si pas dans une partie active
            // Si le joueur n'est pas actif ou en attente, on peut nettoyer ses données
            if (!GameManager.isPlayerActive(playerUUID) && !GameManager.isPlayerWaiting(playerUUID)) {
                // Nettoyer les points (seulement si le joueur n'a pas de points ou très peu)
                int points = com.zombiemod.manager.PointsManager.getPoints(playerUUID);
                if (points == 0) {
                    com.zombiemod.manager.PointsManager.resetPlayer(playerUUID);
                }
                
                // Nettoyer les inventaires sauvegardés si le joueur n'est pas dans la partie
                if (InventoryManager.hasSavedInventory(playerUUID)) {
                    InventoryManager.removeSavedInventory(playerUUID);
                    System.out.println("[PlayerConnection] Cleaned saved inventory for disconnected player: " + player.getName().getString());
                }
                
                // Nettoyer le statut de mort
                com.zombiemod.manager.RespawnManager.removeDeadPlayer(playerUUID);
            }
        }
    }
}
