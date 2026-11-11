package com.angelofdeath.mod.client;

import com.angelofdeath.mod.AngelOfDeathMod;
import com.angelofdeath.mod.item.AngelOfDeathWeaponItem;
import com.angelofdeath.mod.network.PlaceSingularityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the "place singularity" packet when the player left-clicks while channeling the weapon.
 * <p>
 * Vanilla swallows attack clicks while an item is being used, so LeftClickEmpty / LeftClickBlock may not
 * fire during a channel. The client-tick edge detection on the attack key is the reliable path; the event
 * listeners are kept as additional triggers. A short client debounce prevents duplicate packets and the
 * server re-validates everything (it only places while the player is still using the weapon).
 */
@EventBusSubscriber(modid = AngelOfDeathMod.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static final int DEBOUNCE_TICKS = 5;
    private static boolean attackWasDown;
    private static int lastSendTick = -1000;

    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        trySend(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (isChanneling(player)) {
            event.setCanceled(true);
            trySend(player);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean down = player != null && mc.screen == null && mc.options.keyAttack.isDown();
        if (down && !attackWasDown) {
            trySend(player);
        }
        attackWasDown = down;
    }

    private static boolean isChanneling(Player player) {
        return player != null && player.isUsingItem()
                && player.getUseItem().getItem() instanceof AngelOfDeathWeaponItem;
    }

    private static void trySend(Player player) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || player != local || !isChanneling(local)) {
            return;
        }
        if (local.tickCount - lastSendTick < DEBOUNCE_TICKS) {
            return;
        }
        lastSendTick = local.tickCount;
        PacketDistributor.sendToServer(new PlaceSingularityPayload());
    }
}
