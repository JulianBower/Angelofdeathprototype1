package com.angelofdeath.mod.entity;

import com.angelofdeath.mod.registry.ModEntities;
import com.angelofdeath.mod.registry.ModSounds;
import com.angelofdeath.mod.util.SingularityPhysics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SingularityEntity extends Entity {
    public static final int LIFETIME_TICKS = 100;
    public static final double BLACK_RADIUS = 14.0D;
    public static final double CORE_RADIUS = 2.5D;
    public static final double WHITE_SHOCK_RADIUS = 14.0D;
    public static final double WHITE_FIELD_RADIUS = 8.0D;
    public static final int BLOCK_BLAST_RADIUS = 5;
    private static final int MAX_FALLING_BLOCKS = 400;
    private static final float BLACK_DAMAGE = 8.0F;
    private static final float WHITE_MAX_DAMAGE = 20.0F;

    private static final EntityDataAccessor<Boolean> IS_WHITE_HOLE =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE_TICKS =
            SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;

    public SingularityEntity(EntityType<? extends SingularityEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Spawns with its center at (x, y, z). */
    public SingularityEntity(Level level, double x, double y, double z, boolean white, @Nullable Player owner) {
        this(ModEntities.SINGULARITY.get(), level);
        this.setPos(x, y - this.getBbHeight() / 2.0D, z);
        this.setWhiteHole(white);
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_WHITE_HOLE, Boolean.FALSE);
        builder.define(AGE_TICKS, 0);
    }

    public boolean isWhiteHole() {
        return this.entityData.get(IS_WHITE_HOLE);
    }

    public void setWhiteHole(boolean white) {
        this.entityData.set(IS_WHITE_HOLE, white);
    }

    public int getAgeTicks() {
        return this.entityData.get(AGE_TICKS);
    }

    public void setAgeTicks(int age) {
        this.entityData.set(AGE_TICKS, age);
    }

    public Vec3 getCenter() {
        return this.position().add(0.0D, this.getBbHeight() / 2.0D, 0.0D);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }

        int age = this.getAgeTicks() + 1;
        this.setAgeTicks(age);
        Vec3 center = this.getCenter();

        if (age >= LIFETIME_TICKS) {
            server.playSound(null, center.x, center.y, center.z,
                    this.isWhiteHole() ? ModSounds.beaconDeactivate() : ModSounds.anchorCharge(),
                    SoundSource.NEUTRAL, 1.5F, this.isWhiteHole() ? 1.8F : 0.5F);
            if (this.isWhiteHole()) {
                server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 40, 0.4D, 0.4D, 0.4D, 0.4D);
            } else {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 0.6D, 0.6D, 0.6D, 0.3D);
            }
            this.discard();
            return;
        }

        if (this.isWhiteHole()) {
            this.tickWhiteHole(server, center, age);
        } else {
            this.tickBlackHole(server, center, age);
        }
    }

    private Player getOwner(ServerLevel server) {
        return this.ownerUUID == null ? null : server.getPlayerByUUID(this.ownerUUID);
    }

    private DamageSource magicSource(ServerLevel server) {
        Player owner = this.getOwner(server);
        return owner != null ? server.damageSources().indirectMagic(this, owner) : server.damageSources().magic();
    }

    private boolean isOwner(Entity entity) {
        return this.ownerUUID != null && entity.getUUID().equals(this.ownerUUID);
    }

    // ------------------------------------------------------------------ Black Hole

    private void tickBlackHole(ServerLevel server, Vec3 center, int age) {
        AABB box = new AABB(center, center).inflate(BLACK_RADIUS);

        if (age == 1) {
            server.playSound(null, center.x, center.y, center.z, ModSounds.wardenSonicCharge(),
                    SoundSource.NEUTRAL, 2.0F, 0.5F);
            server.playSound(null, center.x, center.y, center.z, ModSounds.dragonGrowl(),
                    SoundSource.NEUTRAL, 1.5F, 0.4F);
        }

        DamageSource source = this.magicSource(server);
        List<LivingEntity> living = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator());
        for (LivingEntity entity : living) {
            double dist = SingularityPhysics.applyForce(entity, center, BLACK_RADIUS, true, 0.14D, 0.50D, 1.6D, 0.75D);
            if (dist < 0.0D) {
                continue;
            }
            entity.fallDistance = 0.0F;
            if (dist < CORE_RADIUS) {
                // keep trapped mobs suspended inside the core
                Vec3 v = entity.getDeltaMovement();
                entity.setDeltaMovement(v.x * 0.6D, v.y * 0.6D + 0.04D, v.z * 0.6D);
                entity.hurtMarked = true;
                if (age % 10 == 0 && !this.isOwner(entity)) {
                    entity.hurt(source, BLACK_DAMAGE);
                }
            }
        }

        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive);
        for (ItemEntity item : items) {
            SingularityPhysics.applyForce(item, center, BLACK_RADIUS, true, 0.12D, 0.40D, 1.4D, 0.75D);
        }

        if (age % 2 == 0) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 30, 4.0D, 4.0D, 4.0D, 0.6D);
        }
        if (age % 15 == 0) {
            server.playSound(null, center.x, center.y, center.z, ModSounds.wardenHeartbeat(),
                    SoundSource.NEUTRAL, 2.0F, 0.5F + (age % 30 == 0 ? 0.0F : 0.15F));
        }
        if (age % 25 == 0) {
            server.playSound(null, center.x, center.y, center.z, ModSounds.beaconAmbient(),
                    SoundSource.NEUTRAL, 1.5F, 0.5F);
        }
    }

    // ------------------------------------------------------------------ White Hole

    private void tickWhiteHole(ServerLevel server, Vec3 center, int age) {
        if (age == 1) {
            this.whiteHoleExplosion(server, center);
            return;
        }

        AABB box = new AABB(center, center).inflate(WHITE_FIELD_RADIUS);
        List<LivingEntity> living = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator());
        for (LivingEntity entity : living) {
            SingularityPhysics.applyForce(entity, center, WHITE_FIELD_RADIUS, false, 0.15D, 0.70D, 2.0D, 0.85D);
        }
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive);
        for (ItemEntity item : items) {
            SingularityPhysics.applyForce(item, center, WHITE_FIELD_RADIUS, false, 0.12D, 0.50D, 1.8D, 0.85D);
        }

        if (age % 2 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 10, 0.6D, 0.6D, 0.6D, 0.35D);
        }
        if (age % 20 == 0) {
            server.playSound(null, center.x, center.y, center.z, ModSounds.beaconAmbient(),
                    SoundSource.NEUTRAL, 1.5F, 1.8F);
        }
    }

    private void whiteHoleExplosion(ServerLevel server, Vec3 center) {
        server.playSound(null, center.x, center.y, center.z, ModSounds.explode(), SoundSource.NEUTRAL, 4.0F, 0.6F);
        server.playSound(null, center.x, center.y, center.z, ModSounds.thunder(), SoundSource.NEUTRAL, 4.0F, 1.4F);
        server.playSound(null, center.x, center.y, center.z, ModSounds.wardenSonicBoom(), SoundSource.NEUTRAL, 3.0F, 1.2F);
        server.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 3, 0.2D, 0.2D, 0.2D, 0.0D);
        server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 300, 0.5D, 0.5D, 0.5D, 1.1D);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        // 1) launch living entities
        DamageSource source = this.magicSource(server);
        AABB box = new AABB(center, center).inflate(WHITE_SHOCK_RADIUS);
        List<LivingEntity> living = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator());
        for (LivingEntity entity : living) {
            Vec3 body = entity.getBoundingBox().getCenter();
            Vec3 away = body.subtract(center);
            double dist = away.length();
            if (dist > WHITE_SHOCK_RADIUS) {
                continue;
            }
            double factor = Math.pow(1.0D - dist / WHITE_SHOCK_RADIUS, 1.5D);
            Vec3 dir = dist < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : away.scale(1.0D / dist);
            double speed = 0.6D + 3.6D * factor;
            Vec3 velocity = dir.scale(speed).add(0.0D, 0.4D + 0.8D * factor, 0.0D);
            if (velocity.lengthSqr() > 36.0D) {
                velocity = velocity.normalize().scale(6.0D);
            }
            entity.setDeltaMovement(velocity);
            entity.hurtMarked = true;
            entity.setOnGround(false);
            if (!this.isOwner(entity)) {
                entity.hurt(source, (float) (4.0D + (WHITE_MAX_DAMAGE - 4.0D) * factor));
            }
        }

        // 2) launch dropped items
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive);
        for (ItemEntity item : items) {
            Vec3 away = item.position().subtract(center);
            double dist = away.length();
            if (dist > WHITE_SHOCK_RADIUS) {
                continue;
            }
            double factor = 1.0D - dist / WHITE_SHOCK_RADIUS;
            Vec3 dir = dist < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : away.scale(1.0D / dist);
            item.setDeltaMovement(dir.scale(0.5D + 2.5D * factor).add(0.0D, 0.3D + 0.5D * factor, 0.0D));
            item.hurtMarked = true;
        }

        // 3) blast blocks into falling blocks
        this.blastBlocks(server, center);
    }

    private static boolean isBlastable(ServerLevel server, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.is(BlockTags.WITHER_IMMUNE)) {
            return false;
        }
        if (state.getDestroySpeed(server, pos) < 0.0F) {
            return false;
        }
        if (state.getBlock().getExplosionResistance() >= 1200.0F) {
            return false; // obsidian, crying obsidian, reinforced deepslate, bedrock, ...
        }
        return server.getBlockEntity(pos) == null;
    }

    private void blastBlocks(ServerLevel server, Vec3 center) {
        BlockPos origin = BlockPos.containing(center);
        double maxDist = BLOCK_BLAST_RADIUS + 0.5D;
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -BLOCK_BLAST_RADIUS; dx <= BLOCK_BLAST_RADIUS; dx++) {
            for (int dy = -BLOCK_BLAST_RADIUS; dy <= BLOCK_BLAST_RADIUS; dy++) {
                for (int dz = -BLOCK_BLAST_RADIUS; dz <= BLOCK_BLAST_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    double dist = Vec3.atCenterOf(pos).distanceTo(center);
                    if (dist > maxDist) {
                        continue;
                    }
                    BlockState state = server.getBlockState(pos);
                    if (isBlastable(server, pos, state)) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(center)));

        int count = 0;
        for (BlockPos pos : candidates) {
            if (count >= MAX_FALLING_BLOCKS) {
                break;
            }
            BlockState state = server.getBlockState(pos);
            if (!isBlastable(server, pos, state)) {
                continue;
            }
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            double dist = blockCenter.distanceTo(center);
            double factor = Math.max(0.0D, 1.0D - dist / maxDist);

            Vec3 horizontal = new Vec3(blockCenter.x - center.x, 0.0D, blockCenter.z - center.z);
            if (horizontal.lengthSqr() < 1.0E-4D) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                horizontal = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            }
            horizontal = horizontal.normalize();

            FallingBlockEntity falling = FallingBlockEntity.fall(server, pos, state);
            falling.setHurtsEntities(1.0F, 20);
            double speed = 0.35D + 1.2D * factor;
            double up = 0.35D + 0.6D * factor + this.random.nextDouble() * 0.25D;
            falling.setDeltaMovement(horizontal.x * speed, up, horizontal.z * speed);
            falling.hurtMarked = true;
            count++;
        }
    }

    // ------------------------------------------------------------------ Persistence

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setWhiteHole(tag.getBoolean("WhiteHole"));
        this.setAgeTicks(tag.getInt("AgeTicks"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("WhiteHole", this.isWhiteHole());
        tag.putInt("AgeTicks", this.getAgeTicks());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }
}
