package com.angelofdeath.mod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SingularityPhysics {
    private SingularityPhysics() {
    }

    /**
     * Applies an attracting or repelling force around {@code center}. Force grows as the entity gets closer.
     *
     * @return the distance from the entity's body center to {@code center}, or -1 if outside {@code radius}
     */
    public static double applyForce(Entity entity, Vec3 center, double radius, boolean attract,
                                    double base, double scale, double maxSpeed, double damping) {
        Vec3 body = entity.getBoundingBox().getCenter();
        Vec3 toCenter = center.subtract(body);
        double dist = toCenter.length();
        if (dist > radius) {
            return -1.0D;
        }

        Vec3 dir;
        if (dist < 1.0E-4D) {
            dir = attract ? Vec3.ZERO : new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            dir = toCenter.scale(1.0D / dist);
            if (!attract) {
                dir = dir.reverse();
            }
        }

        double factor = 1.0D - dist / radius;
        double strength = base + scale * factor;
        Vec3 velocity = entity.getDeltaMovement().scale(damping).add(dir.scale(strength));
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
            velocity = velocity.normalize().scale(maxSpeed);
        }
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
        if (attract) {
            entity.fallDistance = 0.0F;
        }
        return dist;
    }
}
