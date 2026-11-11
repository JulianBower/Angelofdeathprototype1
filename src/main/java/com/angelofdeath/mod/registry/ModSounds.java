package com.angelofdeath.mod.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Looks up vanilla sound events by registry path (robust against Holder/SoundEvent field changes). */
public final class ModSounds {
    private ModSounds() {
    }

    public static SoundEvent vanilla(String path) {
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(path));
    }

    public static SoundEvent beaconActivate() { return vanilla("block.beacon.activate"); }
    public static SoundEvent beaconDeactivate() { return vanilla("block.beacon.deactivate"); }
    public static SoundEvent beaconAmbient() { return vanilla("block.beacon.ambient"); }
    public static SoundEvent wardenHeartbeat() { return vanilla("entity.warden.heartbeat"); }
    public static SoundEvent wardenSonicCharge() { return vanilla("entity.warden.sonic_charge"); }
    public static SoundEvent wardenSonicBoom() { return vanilla("entity.warden.sonic_boom"); }
    public static SoundEvent enderTeleport() { return vanilla("entity.enderman.teleport"); }
    public static SoundEvent endPortalSpawn() { return vanilla("block.end_portal.spawn"); }
    public static SoundEvent dragonGrowl() { return vanilla("entity.ender_dragon.growl"); }
    public static SoundEvent thunder() { return vanilla("entity.lightning_bolt.thunder"); }
    public static SoundEvent explode() { return vanilla("entity.generic.explode"); }
    public static SoundEvent amethystChime() { return vanilla("block.amethyst_block.chime"); }
    public static SoundEvent anchorCharge() { return vanilla("block.respawn_anchor.charge"); }
}
