/*
 * Invokes and interacts with:
 * vazkii.botania.common.entity.BabylonWeaponEntity (Botania Mod by Vazkii)
 * Distributed under the Botania License: http://botaniamod.net/license.php
 */
package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerBladeEntity;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class UnlimitedBladeBarragePhase implements EnforcerAttackPhase {
    // Total blade count & spawn interval
    private static final int BLADE_COUNT = 36;
    private static final int SPAWN_INTERVAL_TICKS = 3;   // 1 blade every 0.15s (was 2 ticks)
    private static final int SPAWN_START_TICK = 4;
    private static final int POST_SPAWN_WAIT_TICKS = 100; // 5.0s after all blades are placed

    // Derived timeline milestones calculated from total blade count
    private static final int SPAWN_END_TICK = SPAWN_START_TICK + (BLADE_COUNT - 1) * SPAWN_INTERVAL_TICKS;
    private static final int UNBIND_TICK = SPAWN_END_TICK + 1;
    private static final int RELEASE_TICK = UNBIND_TICK + POST_SPAWN_WAIT_TICKS;
    private static final int DESCEND_TICK = RELEASE_TICK + 10;

    // Wider ring (was 14.5): blade spacing along the perimeter ~3.8 blocks (was ~2.5)
    private static final double RING_RADIUS = 22.0D;
    private static final double BLADE_HEIGHT_OFFSET = 7.5D;
    private static final double BOSS_HOVER_OFFSET = 12.0D;

    // Launch inaccuracy: each blade aims at a random point around the target
    // so the explosions are spread out instead of stacking knock-up on one spot.
    private static final double SCATTER_RADIUS = 4.0D;
    private static final double SCATTER_VERTICAL = 0.5D;
    private static final double BLADE_SPEED = 2.1D;

    // Astral cyan & projection gold palette
    private static final int CIRCLE_COLOR = 0xFF42E8FF;
    private static final float CYAN_R = 0.26F;
    private static final float CYAN_G = 0.91F;
    private static final float CYAN_B = 1.00F;

    private static final float GOLD_R = 1.00F;
    private static final float GOLD_G = 0.82F;
    private static final float GOLD_B = 0.20F;

    private final List<EnforcerBladeEntity> ringBlades = new ArrayList<>(BLADE_COUNT);

    @Nullable
    private MagicCircleEntity arenaCircle;
    private Vec3 circleCenter = Vec3.ZERO;
    private double hoverTargetY = 0.0D;

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        this.ringBlades.clear();
        this.hoverTargetY = mob.getY() + BOSS_HOVER_OFFSET;
        this.circleCenter = target.position();
        mob.setNoGravity(true);

        // Bind target with Slowness IV for the entire sequential deployment duration
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, UNBIND_TICK + 10, 4, false, true));

        // Wide ground magic circle lasting until simultaneous launch
        this.arenaCircle = new MagicCircleEntity(
                ModEntities.MAGIC_CIRCLE.get(), mob.level(), mob, (float) RING_RADIUS,
                CIRCLE_COLOR, DESCEND_TICK + 5, false, 0.0F, 0
        );
        this.arenaCircle.setPos(this.circleCenter.x, this.circleCenter.y + 0.05D, this.circleCenter.z);
        mob.level().addFreshEntity(this.arenaCircle);

        if (mob.level() instanceof ServerLevel serverLevel) {
            spawnLightweightRing(serverLevel, this.circleCenter.x, this.circleCenter.y + 0.2D, this.circleCenter.z,
                    RING_RADIUS, 48, CYAN_R, CYAN_G, CYAN_B);
        }

        mob.level().playSound(null, this.circleCenter.x, this.circleCenter.y, this.circleCenter.z,
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.3F, 1.15F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        faceTargetSmoothly(mob, target, 24.0F);
        mob.getNavigation().stop();

        // Boss hovers high above the arena until launch completes
        if (actionTick < DESCEND_TICK) {
            double diffY = this.hoverTargetY - mob.getY();
            if (Math.abs(diffY) > 0.1D) {
                double vy = Math.max(-0.5D, Math.min(0.95D, diffY * 0.35D));
                mob.setDeltaMovement(0.0D, vy, 0.0D);
            } else {
                mob.setDeltaMovement(Vec3.ZERO);
            }
            mob.hurtMarked = true;
        }

        // Phase 1: Spawn 1 blade every SPAWN_INTERVAL_TICKS along the circle perimeter
        if (actionTick >= SPAWN_START_TICK && actionTick <= SPAWN_END_TICK) {
            int offset = actionTick - SPAWN_START_TICK;
            if (offset % SPAWN_INTERVAL_TICKS == 0) {
                int index = offset / SPAWN_INTERVAL_TICKS;
                if (index >= 0 && index < BLADE_COUNT) {
                    spawnPerimeterBlade(mob, target, index);
                    float pitch = 1.25F + (0.45F * index / (float) BLADE_COUNT);
                    mob.level().playSound(null, this.circleCenter.x, this.circleCenter.y + BLADE_HEIGHT_OFFSET, this.circleCenter.z,
                            SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.85F, pitch);
                }
            }
        }

        // Phase 2: Remove Slowness right after the final blade is placed
        if (actionTick == UNBIND_TICK) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            mob.level().playSound(null, this.circleCenter.x, this.circleCenter.y + 1.0D, this.circleCenter.z,
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.1F, 1.6F);
        }

        // Keep all placed blades aimed inward until RELEASE_TICK
        if (actionTick >= SPAWN_START_TICK && actionTick < RELEASE_TICK) {
            holdBladesInPlace(target);
        }

        // Phase 3: After the wait, launch all blades simultaneously with scattered aim
        if (actionTick == RELEASE_TICK) {
            launchAllSimultaneously(mob, target);
            if (mob.level() instanceof ServerLevel serverLevel) {
                spawnLightweightRing(serverLevel, this.circleCenter.x, this.circleCenter.y + BLADE_HEIGHT_OFFSET, this.circleCenter.z,
                        RING_RADIUS, 48, GOLD_R, GOLD_G, GOLD_B);
            }
            mob.level().playSound(null, this.circleCenter.x, this.circleCenter.y + BLADE_HEIGHT_OFFSET, this.circleCenter.z,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.3F, 1.45F);
        }

        // Phase 4: Descend back to arena ground
        if (actionTick == DESCEND_TICK) {
            mob.setNoGravity(false);
            mob.setDeltaMovement(0.0D, -0.35D, 0.0D);
            mob.hurtMarked = true;
        } else if (actionTick > DESCEND_TICK && !mob.onGround()) {
            Vec3 motion = mob.getDeltaMovement();
            if (motion.y < -0.5D) {
                mob.setDeltaMovement(motion.x, -0.5D, motion.z);
            }
        }

        if (actionTick >= DESCEND_TICK + 3 && (mob.onGround() || actionTick >= DESCEND_TICK + 22)) {
            return 85;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        LivingEntity target = mob.getTarget();
        if (target != null && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (this.arenaCircle != null) {
            this.arenaCircle.discard();
            this.arenaCircle = null;
        }
        // Only discard blades that were never launched
        for (EnforcerBladeEntity blade : this.ringBlades) {
            if (blade != null && blade.isAlive() && blade.isCharging()) {
                blade.discard();
            }
        }
        this.ringBlades.clear();
        mob.setNoGravity(false);
    }

    private void spawnPerimeterBlade(AlfheimEnforcerEntity mob, LivingEntity target, int index) {
        double angle = (2.0D * Math.PI * index) / BLADE_COUNT;
        double x = this.circleCenter.x + Math.cos(angle) * RING_RADIUS;
        double y = this.circleCenter.y + BLADE_HEIGHT_OFFSET;
        double z = this.circleCenter.z + Math.sin(angle) * RING_RADIUS;

        Vec3 aimTarget = getTargetAimPos(target);

        EnforcerBladeEntity weapon = new EnforcerBladeEntity(ModEntities.ENFORCER_BLADE.get(), mob.level());
        weapon.setOwner(mob);
        weapon.setPos(x, y, z);
        weapon.setCharging(true);
        weapon.setVariety(index % 12);
        weapon.aimAt(aimTarget);

        mob.level().addFreshEntity(weapon);
        this.ringBlades.add(weapon);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    SparkleParticleData.sparkle(1.3F, CYAN_R, CYAN_G, CYAN_B, 8),
                    x, y, z, 3, 0.12D, 0.12D, 0.12D, 0.01D);
        }
    }

    private void holdBladesInPlace(LivingEntity target) {
        Vec3 aimTarget = target.isAlive() ? getTargetAimPos(target) : this.circleCenter.add(0.0D, 0.9D, 0.0D);
        for (EnforcerBladeEntity blade : this.ringBlades) {
            if (blade == null || !blade.isAlive()) {
                continue;
            }
            blade.aimAt(aimTarget);
        }
    }

    private void launchAllSimultaneously(AlfheimEnforcerEntity mob, LivingEntity target) {
        Vec3 impactCenter = target.isAlive() ? getTargetAimPos(target) : this.circleCenter.add(0.0D, 0.9D, 0.0D);
        RandomSource random = mob.getRandom();
        for (EnforcerBladeEntity blade : this.ringBlades) {
            if (blade == null || !blade.isAlive()) {
                continue;
            }
            blade.fire(impactCenter.add(randomScatter(random)), BLADE_SPEED);
        }
    }

    /** Uniformly distributed offset inside a disc (horizontal) with a small vertical jitter. */
    private static Vec3 randomScatter(RandomSource random) {
        double r = SCATTER_RADIUS * Math.sqrt(random.nextDouble());
        double a = random.nextDouble() * Math.PI * 2.0D;
        double dy = (random.nextDouble() * 2.0D - 1.0D) * SCATTER_VERTICAL;
        return new Vec3(Math.cos(a) * r, dy, Math.sin(a) * r);
    }

    private static Vec3 getTargetAimPos(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static void spawnLightweightRing(ServerLevel level, double cx, double cy, double cz, double radius, int points, float r, float g, float b) {
        WispParticleData wisp = WispParticleData.wisp(0.55F, r, g, b, 0.95F);
        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(wisp, px, cy, pz, 1, 0.02D, 0.02D, 0.02D, 0.005D);
        }
    }

    private static void faceTargetSmoothly(AlfheimEnforcerEntity mob, LivingEntity target, float maxTurnDeg) {
        mob.getLookControl().setLookAt(target, maxTurnDeg, maxTurnDeg);

        double dx = target.getX() - mob.getX();
        double dy = (target.getY() + target.getBbHeight() * 0.5D) - mob.getEyeY();
        double dz = target.getZ() - mob.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG));

        float nextYaw = Mth.approachDegrees(mob.getYRot(), targetYaw, maxTurnDeg);
        float nextPitch = Mth.approachDegrees(mob.getXRot(), targetPitch, maxTurnDeg);

        mob.setYRot(nextYaw);
        mob.setXRot(nextPitch);
        mob.yHeadRot = nextYaw;
        mob.yBodyRot = Mth.approachDegrees(mob.yBodyRot, nextYaw, maxTurnDeg * 0.75F);
    }
}