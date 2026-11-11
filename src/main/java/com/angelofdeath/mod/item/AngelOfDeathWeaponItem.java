package com.angelofdeath.mod.item;

import com.angelofdeath.mod.entity.SingularityEntity;
import com.angelofdeath.mod.registry.ModDataComponents;
import com.angelofdeath.mod.registry.ModSounds;
import com.angelofdeath.mod.util.SingularityPhysics;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AngelOfDeathWeaponItem extends Item {
    public static final double HOLD_DISTANCE = 2.5D;
    public static final double HELD_RADIUS = 8.0D;
    public static final double PLACE_REACH = 40.0D;
    public static final int PLACE_COOLDOWN_TICKS = 40;
    private static final float HELD_DAMAGE = 3.0F;
    private static final int COLOR_BLACK_HOLE = 0x8B0000;
    private static final int COLOR_WHITE_HOLE = 0xFFE9B0;

    public AngelOfDeathWeaponItem(Properties properties) {
        super(properties);
    }

    public static boolean isWhiteHole(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.IS_WHITE_HOLE.get(), Boolean.FALSE);
    }

    public static Vec3 getHoldCenter(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(HOLD_DISTANCE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            boolean next = !isWhiteHole(stack);
            stack.set(ModDataComponents.IS_WHITE_HOLE.get(), next);
            if (!level.isClientSide) {
                if (next) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.beaconActivate(), SoundSource.PLAYERS, 0.9F, 1.7F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.amethystChime(), SoundSource.PLAYERS, 1.0F, 0.6F);
                    player.displayClientMessage(Component.translatable("message.angelofdeath.mode.white_hole")
                            .withStyle(style -> style.withColor(COLOR_WHITE_HOLE)), true);
                } else {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.beaconDeactivate(), SoundSource.PLAYERS, 0.9F, 0.5F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.wardenHeartbeat(), SoundSource.PLAYERS, 1.2F, 0.6F);
                    player.displayClientMessage(Component.translatable("message.angelofdeath.mode.black_hole")
                            .withStyle(style -> style.withColor(COLOR_BLACK_HOLE)), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        player.startUsingItem(hand);
        if (!level.isClientSide) {
            boolean white = isWhiteHole(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    white ? ModSounds.beaconActivate() : ModSounds.wardenSonicCharge(),
                    SoundSource.PLAYERS, 0.8F, white ? 1.3F : 0.6F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(level instanceof ServerLevel server) || !(living instanceof Player player)) {
            return;
        }

        int used = getUseDuration(stack, living) - remainingUseDuration;
        boolean white = isWhiteHole(stack);
        Vec3 center = getHoldCenter(player);

        AABB box = new AABB(center, center).inflate(HELD_RADIUS);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator());

        for (LivingEntity target : targets) {
            if (white) {
                SingularityPhysics.applyForce(target, center, HELD_RADIUS, false, 0.12D, 0.65D, 1.8D, 0.8D);
            } else {
                double dist = SingularityPhysics.applyForce(target, center, HELD_RADIUS, true, 0.10D, 0.28D, 1.2D, 0.8D);
                if (dist >= 0.0D && dist < 2.5D && used % 10 == 0) {
                    target.hurt(server.damageSources().indirectMagic(player, player), HELD_DAMAGE);
                }
            }
        }

        if (used % 2 == 0) {
            if (white) {
                server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 4, 0.25D, 0.25D, 0.25D, 0.12D);
            } else {
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 10, 0.9D, 0.9D, 0.9D, 0.05D);
            }
        }
        if (used % 20 == 0) {
            if (white) {
                server.playSound(null, center.x, center.y, center.z, ModSounds.beaconAmbient(),
                        SoundSource.PLAYERS, 0.8F, 1.6F);
            } else {
                server.playSound(null, center.x, center.y, center.z, ModSounds.wardenHeartbeat(),
                        SoundSource.PLAYERS, 1.0F, 0.55F);
            }
        }
    }

    /** Called on the server when the client reports a left-click while channeling. */
    public static void placeSingularity(ServerPlayer player) {
        if (!player.isUsingItem()) {
            return;
        }
        ItemStack held = player.getUseItem();
        if (!(held.getItem() instanceof AngelOfDeathWeaponItem weapon)) {
            return;
        }

        boolean white = isWhiteHole(held);
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(PLACE_REACH));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        AABB searchBox = player.getBoundingBox().expandTowards(hit.subtract(eye)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, hit, searchBox,
                e -> !e.isSpectator() && e.isPickable() && e != player);
        if (entityHit != null) {
            hit = entityHit.getLocation();
        }

        SingularityEntity singularity = new SingularityEntity(level, hit.x, hit.y, hit.z, white, player);
        level.addFreshEntity(singularity);

        player.stopUsingItem();
        player.getCooldowns().addCooldown(weapon, PLACE_COOLDOWN_TICKS);

        SoundEvent launch = white ? ModSounds.enderTeleport() : ModSounds.wardenSonicBoom();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), launch,
                SoundSource.PLAYERS, 1.0F, white ? 1.5F : 0.7F);
        level.playSound(null, hit.x, hit.y, hit.z, white ? ModSounds.endPortalSpawn() : ModSounds.dragonGrowl(),
                SoundSource.PLAYERS, 1.5F, white ? 1.8F : 0.5F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean white = isWhiteHole(stack);
        MutableComponent mode = Component.translatable(white
                        ? "tooltip.angelofdeath.mode.white_hole"
                        : "tooltip.angelofdeath.mode.black_hole")
                .withStyle(style -> style.withColor(white ? COLOR_WHITE_HOLE : COLOR_BLACK_HOLE));
        tooltip.add(mode);
        tooltip.add(Component.translatable("tooltip.angelofdeath.switch")
                .withStyle(style -> style.withColor(0xAAAAAA)));
        tooltip.add(Component.translatable("tooltip.angelofdeath.channel")
                .withStyle(style -> style.withColor(0xAAAAAA)));
        tooltip.add(Component.translatable("tooltip.angelofdeath.place")
                .withStyle(style -> style.withColor(0xAAAAAA)));
    }
}
