package com.angelofdeath.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Procedural Black Hole / White Hole visuals. All geometry is unlit (full-bright by construction:
 * POSITION_COLOR render types with no lightmap) and the glow is drawn on camera-facing billboards.
 * The PoseStack passed in must already be translated to the orb's center.
 */
public final class SingularityVisuals {
    private static final int LAT = 14;
    private static final int LON = 24;
    private static final int RING_SEGMENTS = 48;
    private static final int TENDRILS = 4;
    private static final int TENDRIL_SEGMENTS = 5;

    private SingularityVisuals() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraOrientation,
                              boolean white, float radius, float time, int seed) {
        if (radius <= 0.001F) {
            return;
        }
        float pulse = 1.0F + 0.05F * Mth.sin(time * 0.4F);
        poseStack.pushPose();
        if (white) {
            renderWhite(poseStack, buffers, cameraOrientation, radius, pulse);
        } else {
            renderBlack(poseStack, buffers, cameraOrientation, radius, pulse, time, seed);
        }
        poseStack.popPose();
    }

    private static void renderBlack(PoseStack poseStack, MultiBufferSource buffers, Quaternionf camera,
                                    float radius, float pulse, float time, int seed) {
        Matrix4f worldMatrix = poseStack.last().pose();

        // 1) pitch-black sphere core
        sphere(buffers.getBuffer(RenderType.debugQuads()), worldMatrix, radius, 0, 0, 0, 255);

        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());

        // 2) camera-facing crimson halo hugging the silhouette
        poseStack.pushPose();
        poseStack.mulPose(camera);
        Matrix4f billboard = poseStack.last().pose();
        ring(glow, billboard, radius * 0.80F, radius * 1.10F, 160, 0, 20, 255, 215);
        ring(glow, billboard, radius * 1.10F, radius * 1.80F * pulse, 160, 0, 20, 215, 0);
        poseStack.popPose();

        // 3) crackling tendrils around the surface
        tendrils(glow, worldMatrix, radius, time, seed);
    }

    private static void renderWhite(PoseStack poseStack, MultiBufferSource buffers, Quaternionf camera,
                                    float radius, float pulse) {
        Matrix4f worldMatrix = poseStack.last().pose();

        // 1) dark charcoal / brownish core
        sphere(buffers.getBuffer(RenderType.debugQuads()), worldMatrix, radius, 35, 30, 25, 255);

        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.mulPose(camera);
        Matrix4f billboard = poseStack.last().pose();

        // 3) thick creamy-white corona
        ring(glow, billboard, radius * 0.80F, radius * 1.20F, 255, 248, 225, 255, 235);
        ring(glow, billboard, radius * 1.20F, radius * 2.10F * pulse, 255, 248, 225, 235, 0);
        ring(glow, billboard, radius * 2.10F * pulse, radius * 3.00F * pulse, 255, 225, 150, 70, 0);

        // 2) accretion bar across the middle, pushed in front of the sphere so it is never hidden
        float z = radius * 1.05F;
        bar(glow, billboard, radius * 2.6F * pulse, radius * 0.24F, z, 255, 225, 150, 150);
        bar(glow, billboard, radius * 2.2F * pulse, radius * 0.07F, z, 255, 250, 235, 255);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ geometry helpers

    private static void sphere(VertexConsumer vc, Matrix4f m, float r, int cr, int cg, int cb, int ca) {
        for (int i = 0; i < LAT; i++) {
            float t0 = (float) Math.PI * i / LAT;
            float t1 = (float) Math.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                float p0 = (float) (Math.PI * 2.0D) * j / LON;
                float p1 = (float) (Math.PI * 2.0D) * (j + 1) / LON;
                sphereVertex(vc, m, r, t0, p0, cr, cg, cb, ca);
                sphereVertex(vc, m, r, t1, p0, cr, cg, cb, ca);
                sphereVertex(vc, m, r, t1, p1, cr, cg, cb, ca);
                sphereVertex(vc, m, r, t0, p1, cr, cg, cb, ca);
            }
        }
    }

    private static void sphereVertex(VertexConsumer vc, Matrix4f m, float r, float theta, float phi,
                                     int cr, int cg, int cb, int ca) {
        float sinT = Mth.sin(theta);
        vc.addVertex(m, r * sinT * Mth.cos(phi), r * Mth.cos(theta), r * sinT * Mth.sin(phi))
                .setColor(cr, cg, cb, ca);
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float r0, float r1,
                             int cr, int cg, int cb, int a0, int a1) {
        for (int k = 0; k < RING_SEGMENTS; k++) {
            float ang0 = (float) (Math.PI * 2.0D) * k / RING_SEGMENTS;
            float ang1 = (float) (Math.PI * 2.0D) * (k + 1) / RING_SEGMENTS;
            float c0 = Mth.cos(ang0);
            float s0 = Mth.sin(ang0);
            float c1 = Mth.cos(ang1);
            float s1 = Mth.sin(ang1);
            vc.addVertex(m, c0 * r0, s0 * r0, 0.0F).setColor(cr, cg, cb, a0);
            vc.addVertex(m, c0 * r1, s0 * r1, 0.0F).setColor(cr, cg, cb, a1);
            vc.addVertex(m, c1 * r1, s1 * r1, 0.0F).setColor(cr, cg, cb, a1);
            vc.addVertex(m, c1 * r0, s1 * r0, 0.0F).setColor(cr, cg, cb, a0);
        }
    }

    /** Horizontal (camera-aligned) bar: brightest on its center line, fading toward all edges. */
    private static void bar(VertexConsumer vc, Matrix4f m, float halfLength, float halfHeight, float z,
                            int cr, int cg, int cb, int maxAlpha) {
        int steps = 16;
        for (int k = 0; k < steps; k++) {
            float x0 = -halfLength + 2.0F * halfLength * k / steps;
            float x1 = -halfLength + 2.0F * halfLength * (k + 1) / steps;
            int a0 = barAlpha(x0, halfLength, maxAlpha);
            int a1 = barAlpha(x1, halfLength, maxAlpha);
            for (int sign = -1; sign <= 1; sign += 2) {
                float y = sign * halfHeight;
                vc.addVertex(m, x0, 0.0F, z).setColor(cr, cg, cb, a0);
                vc.addVertex(m, x1, 0.0F, z).setColor(cr, cg, cb, a1);
                vc.addVertex(m, x1, y, z).setColor(cr, cg, cb, 0);
                vc.addVertex(m, x0, y, z).setColor(cr, cg, cb, 0);
            }
        }
    }

    private static int barAlpha(float x, float halfLength, int maxAlpha) {
        float t = 1.0F - Math.abs(x) / halfLength;
        return Mth.clamp((int) (maxAlpha * (float) Math.pow(Math.max(t, 0.0F), 0.6D)), 0, 255);
    }

    private static void tendrils(VertexConsumer vc, Matrix4f m, float radius, float time, int seed) {
        long step = (long) Math.floor(time / 2.0F);
        for (int t = 0; t < TENDRILS; t++) {
            RandomSource rnd = RandomSource.create(step * 7919L + seed * 104729L + t * 15485863L);
            Vector3f dir = randomUnit(rnd);
            Vector3f prev = new Vector3f(dir).mul(radius * 1.02F);
            for (int s = 0; s < TENDRIL_SEGMENTS; s++) {
                Vector3f jitter = new Vector3f(rnd.nextFloat() - 0.5F, rnd.nextFloat() - 0.5F, rnd.nextFloat() - 0.5F)
                        .mul(0.5F);
                dir = new Vector3f(dir).add(jitter);
                if (dir.lengthSquared() < 1.0E-6F) {
                    dir.set(0.0F, 1.0F, 0.0F);
                }
                dir.normalize();
                Vector3f next = new Vector3f(dir).mul(radius * (1.03F + rnd.nextFloat() * 0.12F));
                segment(vc, m, prev, next, radius * 0.018F, 170, 0, 25, 255);
                segment(vc, m, prev, next, radius * 0.05F, 160, 0, 20, 70);
                prev = next;
            }
        }
    }

    private static Vector3f randomUnit(RandomSource rnd) {
        Vector3f v = new Vector3f(rnd.nextFloat() * 2.0F - 1.0F, rnd.nextFloat() * 2.0F - 1.0F, rnd.nextFloat() * 2.0F - 1.0F);
        if (v.lengthSquared() < 1.0E-4F) {
            return new Vector3f(0.0F, 1.0F, 0.0F);
        }
        return v.normalize();
    }

    /** Two perpendicular quads so the segment is visible from any angle. */
    private static void segment(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, float width,
                                int cr, int cg, int cb, int ca) {
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1.0E-8F) {
            return;
        }
        d.normalize();
        Vector3f helper = Math.abs(d.y) > 0.9F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f p1 = new Vector3f(d).cross(helper).normalize().mul(width);
        Vector3f p2 = new Vector3f(d).cross(p1).normalize().mul(width);
        quad(vc, m, a, b, p1, cr, cg, cb, ca);
        quad(vc, m, a, b, p2, cr, cg, cb, ca);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f p,
                             int cr, int cg, int cb, int ca) {
        vc.addVertex(m, a.x + p.x, a.y + p.y, a.z + p.z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, a.x - p.x, a.y - p.y, a.z - p.z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, b.x - p.x, b.y - p.y, b.z - p.z).setColor(cr, cg, cb, ca);
        vc.addVertex(m, b.x + p.x, b.y + p.y, b.z + p.z).setColor(cr, cg, cb, ca);
    }
}
