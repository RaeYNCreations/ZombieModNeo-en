package com.zombiemod.client;

import com.zombiemod.network.packet.WeaponCrateSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache côté client pour les données des weapon crates
 */
public class ClientWeaponCrateData {

    private static Map<BlockPos, WeaponCrateSyncPacket.CrateData> weaponCrates = new HashMap<>();

    public static void setWeaponCrate(BlockPos pos, int cost, ListTag ammo) {
        weaponCrates.put(pos, new WeaponCrateSyncPacket.CrateData(cost, ammo));
        System.out.println("[ClientWeaponCrateData] Crate added: " + pos + " -> " + cost + " points, " + ammo.size() + " ammo");
    }

    public static void removeWeaponCrate(BlockPos pos) {
        weaponCrates.remove(pos);
        System.out.println("[ClientWeaponCrateData] Crate removed: " + pos);
    }

    public static boolean isWeaponCrate(BlockPos pos) {
        return weaponCrates.containsKey(pos);
    }

    public static int getCost(BlockPos pos) {
        WeaponCrateSyncPacket.CrateData data = weaponCrates.get(pos);
        return data != null ? data.cost() : 0;
    }

    public static ListTag getAmmo(BlockPos pos) {
        WeaponCrateSyncPacket.CrateData data = weaponCrates.get(pos);
        return data != null ? data.ammo() : new ListTag();
    }

    public static void clear() {
        weaponCrates.clear();
        System.out.println("[ClientWeaponCrateData] Cache clear");
    }

    public static void setAll(Map<BlockPos, WeaponCrateSyncPacket.CrateData> crates) {
        weaponCrates.clear();
        weaponCrates.putAll(crates);
        System.out.println("[ClientWeaponCrateData] Cache updated with " + crates.size() + " crates:");
        crates.forEach((pos, data) -> System.out.println("  - " + pos + " -> " + data.cost() + " points, " + data.ammo().size() + " ammo"));
    }
}
