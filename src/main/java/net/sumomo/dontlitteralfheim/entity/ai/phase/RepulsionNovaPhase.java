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
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

import javax.annotation.Nullable;
import java.util.List;

public class RepulsionNovaPhase implements EnforcerAttackPhase {
    private static final double BLAST_RADIUS = 6.5D;
    private static final float MAX_BLAST_DAMAGE = 26.0F;
    private static final float MIN_BLAST_DAMAGE = 14.0F;

    @Nullable
    private MagicCircleEntity warningCircle;

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        mob.getNavigation().stop();
        mob.setDeltaMovement(0.0D, Math.min(0.0D, mob.getDeltaMovement().y), 0.0D);

        this.warningCircle = new MagicCircleEntity(
                ModEntities.MAGIC_CIRCLE.get(), mob.level(), mob, (float) BLAST_RADIUS,
                MagicCircleEntity.COLOR_VOID, 22, false, 0.0F, 0
        );
        this.warningCircle.setPos(mob.getX(), mob.getY(), mob.getZ());
        mob.level().addFreshEntity(this.warningCircle);

        if (mob.level() instanceof ServerLevel serverLevel) {
            spawnVoidRing(serverLevel, mob.getX(), mob.getY() + 0.15D, mob.getZ(), BLAST_RADIUS, 48, 0.50F);
        }

        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 1.1F, 1.3F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        mob.getNavigation().stop();
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.warningCircle != null && this.warningCircle.isAlive()) {
            this.warningCircle.setPos(mob.getX(), mob.getY(), mob.getZ());
        }

        if (actionTick < 12 && mob.level() instanceof ServerLevel serverLevel) {
            double shrinkRadius = BLAST_RADIUS * (1.0D - (actionTick / 12.0D));
            spawnVoidRing(serverLevel, mob.getX(), mob.getY() + 0.25D, mob.getZ(), shrinkRadius, 24, 0.42F);
        }

        if (actionTick == 12) {
            triggerHighPowerNova(mob);
        }

        if (actionTick >= 22) {
            return 45;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        if (this.warningCircle != null) {
            this.warningCircle.discard();
            this.warningCircle = null;
        }
    }

    private void triggerHighPowerNova(AlfheimEnforcerEntity mob) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            int hex = MagicCircleEntity.COLOR_VOID;
            float r = ((hex >> 16) & 0xFF) / 255.0F;
            float g = ((hex >> 8) & 0xFF) / 255.0F;
            float b = (hex & 0xFF) / 255.0F;

            WispParticleData voidWisp = WispParticleData.wisp(0.70F, r, g, b, 1.4F);
            SparkleParticleData voidSparkle = SparkleParticleData.sparkle(1.7F, r, g, b, 14);

            spawnVoidRing(serverLevel, mob.getX(), mob.getY() + 0.2D, mob.getZ(), BLAST_RADIUS, 56, 0.65F);
            spawnVoidRing(serverLevel, mob.getX(), mob.getY() + 0.9D, mob.getZ(), BLAST_RADIUS * 0.65D, 36, 0.55F);

            serverLevel.sendParticles(voidWisp, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 65, 2.8D, 1.0D, 2.8D, 0.12D);
            serverLevel.sendParticles(voidSparkle, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 55, 3.0D, 1.2D, 3.0D, 0.10D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    mob.getX(), mob.getY() + 1.0D, mob.getZ(), 3, 1.5D, 0.5D, 1.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    mob.getX(), mob.getY() + 1.2D, mob.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.85F);

        AABB area = mob.getBoundingBox().inflate(BLAST_RADIUS);
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != mob && e.isAlive() && !e.isSpectator());

        for (LivingEntity entity : nearby) {
            double dist = Math.sqrt(entity.distanceToSqr(mob));
            if (dist > BLAST_RADIUS) {
                continue;
            }

            float falloff = 1.0F - Mth.clamp((float) (dist / BLAST_RADIUS), 0.0F, 1.0F);
            float damage = Mth.lerp(falloff, MIN_BLAST_DAMAGE, MAX_BLAST_DAMAGE);

            Vec3 delta = entity.position().subtract(mob.position());
            Vec3 horizontalDir = new Vec3(delta.x, 0.0D, delta.z);
            if (horizontalDir.lengthSqr() < 0.0001D) {
                horizontalDir = new Vec3(mob.getRandom().nextDouble() - 0.5D, 0.0D, mob.getRandom().nextDouble() - 0.5D);
            }
            horizontalDir = horizontalDir.normalize();

            entity.invulnerableTime = 0;
            entity.hurt(mob.damageSources().explosion(mob, mob), damage);
            entity.setDeltaMovement(horizontalDir.x * 2.4D, 0.85D, horizontalDir.z * 2.4D);
            entity.hurtMarked = true;
        }
    }

    private static void spawnVoidRing(ServerLevel level, double cx, double cy, double cz, double radius, int points, float size) {
        int hex = MagicCircleEntity.COLOR_VOID;
        float r = ((hex >> 16) & 0xFF) / 255.0F;
        float g = ((hex >> 8) & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;

        WispParticleData wisp = WispParticleData.wisp(size, r, g, b, 1.1F);
        SparkleParticleData sparkle = SparkleParticleData.sparkle(size * 2.2F, r, g, b, 10);

        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(wisp, px, cy, pz, 1, 0.03D, 0.05D, 0.03D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(sparkle, px, cy, pz, 1, 0.04D, 0.05D, 0.04D, 0.01D);
            }
        }
    }
}