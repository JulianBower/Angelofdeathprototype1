package com.angelofdeath.mod.client;

import com.angelofdeath.mod.entity.SingularityEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SingularityRenderer extends EntityRenderer<SingularityEntity> {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float PLACED_RADIUS = 1.25F;

    public SingularityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SingularityEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(SingularityEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float time = entity.getAgeTicks() + partialTick;
        float grow = Mth.clamp(time / 6.0F, 0.0F, 1.0F);
        float fade = Mth.clamp((SingularityEntity.LIFETIME_TICKS - time) / 10.0F, 0.0F, 1.0F);
        float radius = PLACED_RADIUS * grow * fade;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
        SingularityVisuals.render(poseStack, buffer, this.entityRenderDispatcher.cameraOrientation(),
                entity.isWhiteHole(), radius, time, entity.getId());
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SingularityEntity entity) {
        return WHITE_TEXTURE;
    }
}
