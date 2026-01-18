package com.zombiemod.client;

import com.zombiemod.network.packet.DoorSyncPacket;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache côté client pour les données des portes
 */
public class ClientDoorData {

    private static Map<BlockPos, DoorSyncPacket.DoorData> doors = new HashMap<>();

    public static void setDoor(BlockPos signPos, int doorNumber, int cost, boolean isOpen) {
        doors.put(signPos, new DoorSyncPacket.DoorData(doorNumber, cost, isOpen));
        System.out.println("[ClientDoorData] Door Added: " + signPos + " -> Door #" + doorNumber + ", cost: " + cost + ", status: " + isOpen);
    }

    public static void removeDoor(BlockPos signPos) {
        doors.remove(signPos);
        System.out.println("[ClientDoorData] Door removed: " + signPos);
    }

    public static boolean isDoor(BlockPos signPos) {
        return doors.containsKey(signPos);
    }

    public static int getCost(BlockPos signPos) {
        DoorSyncPacket.DoorData data = doors.get(signPos);
        return data != null ? data.cost() : 0;
    }

    public static int getDoorNumber(BlockPos signPos) {
        DoorSyncPacket.DoorData data = doors.get(signPos);
        return data != null ? data.doorNumber() : 0;
    }

    public static boolean isOpen(BlockPos signPos) {
        DoorSyncPacket.DoorData data = doors.get(signPos);
        return data != null && data.isOpen();
    }

    public static void clear() {
        doors.clear();
        System.out.println("[ClientDoorData] Cache cleared");
    }

    public static void setAll(Map<BlockPos, DoorSyncPacket.DoorData> newDoors) {
        doors.clear();
        doors.putAll(newDoors);
        System.out.println("[ClientDoorData] Cache updated with " + newDoors.size() + " door(s):");
        newDoors.forEach((pos, data) -> System.out.println("  - " + pos + " -> Door #" + data.doorNumber() + ", cost: " + data.cost() + ", status: " + data.isOpen()));
    }
}
