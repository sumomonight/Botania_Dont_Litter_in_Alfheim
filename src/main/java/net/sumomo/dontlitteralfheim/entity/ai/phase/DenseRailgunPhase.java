package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

import javax.annotation.Nullable;

public class DenseRailgunPhase implements EnforcerAttackPhase {
    private Vec3 lockedLaserDir = Vec3.ZERO;
    @Nullable
    private ManaBeamEntity sightBeam;
    @Nullable
    private MagicCircleEntity chargeCircle;

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        if (actionTick < 30) {
            if (actionTick <= 20) {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                Vec3 eyePos = mob.getEyePosition();
                Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                this.lockedLaserDir = targetCenter.subtract(eyePos).normalize();
            }

            float chargeProgress = Math.min(1.0F, (actionTick - 1) / 28.0F);
            Vec3 start = mob.getEyePosition().add(this.lockedLaserDir.scale(1.4D));
            Vec3 end = start.add(this.lockedLaserDir.scale(30.0D));

            int r = 255;
            int g = (int) (255 - (45 * chargeProgress));
            int b = (int) (255 - (229 * chargeProgress));
            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
            float beamRadius = 0.08F + (0.22F * chargeProgress);
            float circleRadius = 0.6F + (1.8F * chargeProgress);

            if (this.sightBeam == null || !this.sightBeam.isAlive()) {
                this.sightBeam = ManaBeamEntity.createSightBeam(
                        mob.level(), mob, start, end, beamRadius, color, 35
                );
                mob.level().addFreshEntity(this.sightBeam);
            } else {
                this.sightBeam.setPos(start.x, start.y, start.z);
                this.sightBeam.setEndPos(end);
                this.sightBeam.setColor(color);
                this.sightBeam.setRadius(beamRadius);
            }

            if (this.chargeCircle == null || !this.chargeCircle.isAlive()) {
                this.chargeCircle = new MagicCircleEntity(
                        ModEntities.MAGIC_CIRCLE.get(), mob.level(), mob, circleRadius,
                        MagicCircleEntity.COLOR_BABYLON, 35, true, 0.0F, 0
                );
                this.chargeCircle.setPos(start.x, start.y, start.z);
                this.chargeCircle.setYRot(mob.getYRot());
                this.chargeCircle.setXRot(mob.getXRot() + 90.0F);
                mob.level().addFreshEntity(this.chargeCircle);
            } else {
                this.chargeCircle.setPos(start.x, start.y, start.z);
                this.chargeCircle.setYRot(mob.getYRot());
                this.chargeCircle.setXRot(mob.getXRot() + 90.0F);
                this.chargeCircle.setRadius(circleRadius);
            }

            if (mob.level() instanceof ServerLevel serverLevel) {
                spawnChargeParticles(serverLevel, start, this.lockedLaserDir, circleRadius, chargeProgress, actionTick);
            }
        } else if (actionTick == 30) {
            discardTelegraph();
            fireManaLaser(mob, this.lockedLaserDir);
        }

        if (actionTick >= 38) {
            return 55;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        discardTelegraph();
    }

    private void discardTelegraph() {
        if (this.sightBeam != null) {
            this.sightBeam.discard();
            this.sightBeam = null;
        }
        if (this.chargeCircle != null) {
            this.chargeCircle.discard();
            this.chargeCircle = null;
        }
    }

    private void fireManaLaser(AlfheimEnforcerEntity mob, Vec3 dir) {
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0F, 1.4F);

        Vec3 start = mob.getEyePosition().add(dir.scale(1.4D));
        Vec3 end = start.add(dir.scale(30.0D));

        MagicCircleEntity muzzle = new MagicCircleEntity(
                ModEntities.MAGIC_CIRCLE.get(), mob.level(), mob, 2.4F,
                MagicCircleEntity.COLOR_BABYLON, 20, true, 0.0F, 0
        );
        muzzle.setPos(start.x, start.y, start.z);
        muzzle.setYRot(mob.getYRot());
        muzzle.setXRot(mob.getXRot() + 90.0F);
        mob.level().addFreshEntity(muzzle);

        mob.level().addFreshEntity(ManaBeamEntity.createDenseBeam(
                mob.level(), mob, start, end, 2.0F,
                ManaBeamEntity.COLOR_BABYLON, 18.0F, 18
        ));

        if (mob.level() instanceof ServerLevel serverLevel) {
            int hex = MagicCircleEntity.COLOR_BABYLON;
            float r = ((hex >> 16) & 0xFF) / 255.0F;
            float g = ((hex >> 8) & 0xFF) / 255.0F;
            float b = (hex & 0xFF) / 255.0F;

            WispParticleData wisp = WispParticleData.wisp(0.65F, r, g, b, 1.2F);
            SparkleParticleData sparkle = SparkleParticleData.sparkle(1.6F, r, g, b, 12);

            // Muzzle shockwave ring & dense beam trail
            spawnOrientedRing(serverLevel, start, dir, 2.5D, 36, wisp, sparkle);
            for (int step = 0; step <= 30; step += 2) {
                Vec3 pt = start.add(dir.scale(step));
                serverLevel.sendParticles(wisp, pt.x, pt.y, pt.z, 4, 0.45D, 0.45D, 0.45D, 0.03D);
                serverLevel.sendParticles(sparkle, pt.x, pt.y, pt.z, 3, 0.55D, 0.55D, 0.55D, 0.02D);
            }
        }
    }

    private static void spawnChargeParticles(ServerLevel level, Vec3 center, Vec3 dir, float radius, float progress, int tick) {
        int hex = MagicCircleEntity.COLOR_BABYLON;
        float r = ((hex >> 16) & 0xFF) / 255.0F;
        float g = ((hex >> 8) & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;

        WispParticleData wisp = WispParticleData.wisp(0.35F + 0.25F * progress, r, g, b, 0.9F);
        SparkleParticleData sparkle = SparkleParticleData.sparkle(1.1F + 0.5F * progress, r, g, b, 8);

        Vec3 up = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 orthoUp = right.cross(dir).normalize();

        int points = 8;
        double baseAngle = tick * 0.45D;
        for (int i = 0; i < points; i++) {
            double angle = baseAngle + (2.0D * Math.PI * i) / points;
            Vec3 offset = right.scale(Math.cos(angle) * radius).add(orthoUp.scale(Math.sin(angle) * radius));
            Vec3 pos = center.add(offset);
            level.sendParticles(wisp, pos.x, pos.y, pos.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(sparkle, pos.x, pos.y, pos.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }
    }

    private static void spawnOrientedRing(ServerLevel level, Vec3 center, Vec3 dir, double radius, int points, WispParticleData wisp, SparkleParticleData sparkle) {
        Vec3 up = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 orthoUp = right.cross(dir).normalize();

        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            Vec3 offset = right.scale(Math.cos(angle) * radius).add(orthoUp.scale(Math.sin(angle) * radius));
            Vec3 pos = center.add(offset);
            level.sendParticles(wisp, pos.x, pos.y, pos.z, 1, 0.03D, 0.03D, 0.03D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(sparkle, pos.x, pos.y, pos.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
            }
        }
    }
}