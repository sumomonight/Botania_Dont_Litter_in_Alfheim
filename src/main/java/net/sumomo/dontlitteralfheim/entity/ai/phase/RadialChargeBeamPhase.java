package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RadialChargeBeamPhase implements EnforcerAttackPhase {
    private static final int RADIAL_BEAM_COUNT = 12;
    private static final int CHARGE_DURATION = 44;
    private static final double NOVA_RADIUS = 5.5D;
    private static final double BLAST_HEIGHT_OFFSET = 2.4D;
    private static final float MAX_NOVA_DAMAGE = 22.0F;
    private static final float MIN_NOVA_DAMAGE = 12.0F;

    private final List<ManaBeamEntity> radialChargeBeams = new ArrayList<>(RADIAL_BEAM_COUNT);
    @Nullable
    private MagicCircleEntity groundMagicCircle;
    private float radialAngleDeg = 0.0F;

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        mob.getNavigation().stop();
        mob.setDeltaMovement(0.0D, Math.min(0.0D, mob.getDeltaMovement().y), 0.0D);

        this.radialAngleDeg = mob.getYRot();
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        mob.getNavigation().stop();
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        mob.getLookControl().setLookAt(target, 15.0F, 15.0F);

        if (actionTick <= CHARGE_DURATION) {
            float progress = Math.min(1.0F, (actionTick - 1) / (float) (CHARGE_DURATION - 1));
            float angularSpeed = 2.5F + 22.0F * (progress * progress);
            this.radialAngleDeg = (this.radialAngleDeg + angularSpeed) % 360.0F;

            int r = 255;
            int g = 153 - (int) (133 * progress);
            int b = 153 - (int) (133 * progress);
            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
            float beamRadius = 0.08F + (0.24F * progress);

            if (this.groundMagicCircle == null || !this.groundMagicCircle.isAlive()) {
                this.groundMagicCircle = new MagicCircleEntity(
                        ModEntities.MAGIC_CIRCLE.get(), mob.level(), mob, (float) NOVA_RADIUS,
                        color, CHARGE_DURATION + 18, false, 0.0F, 0
                );
                this.groundMagicCircle.setPos(mob.getX(), mob.getY(), mob.getZ());
                mob.level().addFreshEntity(this.groundMagicCircle);
            } else {
                this.groundMagicCircle.setPos(mob.getX(), mob.getY(), mob.getZ());
                this.groundMagicCircle.setColor(color);
            }

            if (mob.level() instanceof ServerLevel serverLevel) {
                float rf = r / 255.0F;
                float gf = g / 255.0F;
                float bf = b / 255.0F;
                WispParticleData wisp = WispParticleData.wisp(0.38F + 0.22F * progress, rf, gf, bf, 0.9F);
                SparkleParticleData sparkle = SparkleParticleData.sparkle(1.1F + 0.4F * progress, rf, gf, bf, 8);

                for (int i = 0; i < 6; i++) {
                    double angle = (this.radialAngleDeg * Mth.DEG_TO_RAD) + (2.0D * Math.PI * i) / 6.0D;
                    double px = mob.getX() + Math.cos(angle) * NOVA_RADIUS;
                    double pz = mob.getZ() + Math.sin(angle) * NOVA_RADIUS;
                    serverLevel.sendParticles(wisp, px, mob.getY() + 0.2D, pz, 1, 0.06D, 0.12D, 0.06D, 0.02D);
                    if (i % 2 == 0) {
                        serverLevel.sendParticles(sparkle, px, mob.getY() + 0.2D, pz, 1, 0.08D, 0.15D, 0.08D, 0.02D);
                    }
                }
            }

            if (actionTick % 8 == 0) {
                mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.9F, 0.8F + progress * 1.1F);
            }

            double originY = mob.getY() + 1.1D;
            double beamLength = 24.0D;

            for (int i = 0; i < RADIAL_BEAM_COUNT; i++) {
                float angleRad = (this.radialAngleDeg + (360.0F / RADIAL_BEAM_COUNT) * i) * Mth.DEG_TO_RAD;
                double dirX = -Mth.sin(angleRad);
                double dirZ = Mth.cos(angleRad);

                Vec3 start = new Vec3(mob.getX() + dirX * 1.1D, originY, mob.getZ() + dirZ * 1.1D);
                Vec3 end = new Vec3(mob.getX() + dirX * beamLength, originY, mob.getZ() + dirZ * beamLength);

                if (i >= this.radialChargeBeams.size()) {
                    ManaBeamEntity beam = ManaBeamEntity.createSightBeam(
                            mob.level(), mob, start, end, beamRadius, color, CHARGE_DURATION + 5
                    );
                    this.radialChargeBeams.add(beam);
                    mob.level().addFreshEntity(beam);
                } else {
                    ManaBeamEntity beam = this.radialChargeBeams.get(i);
                    if (beam != null && beam.isAlive()) {
                        beam.setPos(start.x, start.y, start.z);
                        beam.setEndPos(end);
                        beam.setColor(color);
                        beam.setRadius(beamRadius);
                    }
                }
            }
        } else if (actionTick == CHARGE_DURATION + 1) {
            for (ManaBeamEntity beam : this.radialChargeBeams) {
                if (beam != null) {
                    beam.discard();
                }
            }
            this.radialChargeBeams.clear();

            triggerCenterBlast(mob);

            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 1.2F);

            double originY = mob.getY() + 1.1D;
            double burstLength = 26.0D;
            int crimsonColor = 0xFFFF1414;

            for (int i = 0; i < RADIAL_BEAM_COUNT; i++) {
                float angleRad = (this.radialAngleDeg + (360.0F / RADIAL_BEAM_COUNT) * i) * Mth.DEG_TO_RAD;
                double dirX = -Mth.sin(angleRad);
                double dirZ = Mth.cos(angleRad);

                Vec3 start = new Vec3(mob.getX() + dirX * 1.1D, originY, mob.getZ() + dirZ * 1.1D);
                Vec3 end = new Vec3(mob.getX() + dirX * burstLength, originY, mob.getZ() + dirZ * burstLength);

                mob.level().addFreshEntity(ManaBeamEntity.createDenseBeam(
                        mob.level(), mob, start, end, 0.95F, crimsonColor, 15.0F, 16
                ));
            }
        }

        if (actionTick >= CHARGE_DURATION + 16) {
            return 65;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        for (ManaBeamEntity beam : this.radialChargeBeams) {
            if (beam != null) {
                beam.discard();
            }
        }
        this.radialChargeBeams.clear();

        if (this.groundMagicCircle != null) {
            this.groundMagicCircle.discard();
            this.groundMagicCircle = null;
        }
    }

    private void triggerCenterBlast(AlfheimEnforcerEntity mob) {
        Vec3 blastCenter = new Vec3(mob.getX(), mob.getY() + BLAST_HEIGHT_OFFSET, mob.getZ());

        if (mob.level() instanceof ServerLevel serverLevel) {
            float rf = 1.0F;
            float gf = 0x14 / 255.0F;
            float bf = 0x14 / 255.0F;
            WispParticleData crimsonWisp = WispParticleData.wisp(0.65F, rf, gf, bf, 1.3F);
            SparkleParticleData crimsonSparkle = SparkleParticleData.sparkle(1.6F, rf, gf, bf, 14);

            for (int i = 0; i < 48; i++) {
                double angle = (2.0D * Math.PI * i) / 48.0D;
                double px = mob.getX() + Math.cos(angle) * NOVA_RADIUS;
                double pz = mob.getZ() + Math.sin(angle) * NOVA_RADIUS;
                serverLevel.sendParticles(crimsonWisp, px, mob.getY() + 0.3D, pz, 1, 0.08D, 0.25D, 0.08D, 0.04D);
                serverLevel.sendParticles(crimsonSparkle, px, mob.getY() + 0.5D, pz, 1, 0.1D, 0.3D, 0.1D, 0.04D);
            }
            serverLevel.sendParticles(crimsonWisp, blastCenter.x, blastCenter.y, blastCenter.z, 50, 2.2D, 0.8D, 2.2D, 0.08D);
            serverLevel.sendParticles(crimsonSparkle, blastCenter.x, blastCenter.y, blastCenter.z, 45, 2.5D, 1.0D, 2.5D, 0.06D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    blastCenter.x, blastCenter.y, blastCenter.z, 2, 1.2D, 0.4D, 1.2D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    blastCenter.x, blastCenter.y, blastCenter.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        mob.level().playSound(null, blastCenter.x, blastCenter.y, blastCenter.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.3F, 0.9F);

        AABB area = mob.getBoundingBox().inflate(NOVA_RADIUS);
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != mob && e.isAlive() && !e.isSpectator());

        for (LivingEntity entity : nearby) {
            double dist = Math.sqrt(entity.distanceToSqr(mob));
            if (dist > NOVA_RADIUS) {
                continue;
            }

            float falloff = 1.0F - Mth.clamp((float) (dist / NOVA_RADIUS), 0.0F, 1.0F);
            float damage = Mth.lerp(falloff, MIN_NOVA_DAMAGE, MAX_NOVA_DAMAGE);

            Vec3 entityCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 pushVec = entityCenter.subtract(blastCenter);
            Vec3 horizontalDir = new Vec3(pushVec.x, 0.0D, pushVec.z);
            if (horizontalDir.lengthSqr() < 0.0001D) {
                horizontalDir = new Vec3(mob.getRandom().nextDouble() - 0.5D, 0.0D, mob.getRandom().nextDouble() - 0.5D);
            }
            horizontalDir = horizontalDir.normalize();

            entity.invulnerableTime = 0;
            entity.hurt(mob.damageSources().explosion(mob, mob), damage);
            entity.setDeltaMovement(horizontalDir.x * 2.3D, 0.20D, horizontalDir.z * 2.3D);
            entity.hurtMarked = true;
        }
    }
}