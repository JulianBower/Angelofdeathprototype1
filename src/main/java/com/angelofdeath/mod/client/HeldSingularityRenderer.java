package com.angelofdeath.mod.client;

import com.angelofdeath.mod.AngelOfDeathMod;
import com.angelofdeath.mod.item.AngelOfDeathWeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws the small channeled orb 2.5 blocks in front of every player who is holding the weapon. */
@EventBusSubscriber(modid = AngelOfDeathMod.MOD_ID, value = Dist.CLIENT)
public final class HeldSingularityRenderer {
    private static final float HELD_RADIUS = 0.35F;

    private HeldSingularityRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drewAnything = false;

        for (Player player : level.players()) {
            if (!player.isUsingItem()) {
                continue;
            }
            ItemStack stack = player.getUseItem();
            if (!(stack.getItem() instanceof AngelOfDeathWeaponItem)) {
                continue;
            }

            Vec3 center = player.getEyePosition(partial)
                    .add(player.getViewVector(partial).scale(AngelOfDeathWeaponItem.HOLD_DISTANCE));
            float charge = Mth.clamp((player.getTicksUsingItem() + partial) / 8.0F, 0.0F, 1.0F);
            float time = level.getGameTime() + partial;

            poseStack.pushPose();
            poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
            SingularityVisuals.render(poseStack, buffers, mc.getEntityRenderDispatcher().cameraOrientation(),
                    AngelOfDeathWeaponItem.isWhiteHole(stack), HELD_RADIUS * charge, time, player.getId());
            poseStack.popPose();
            drewAnything = true;
        }

        if (drewAnything) {
            buffers.endBatch(RenderType.debugQuads());
            buffers.endBatch(RenderType.lightning());
        }
    }
}
