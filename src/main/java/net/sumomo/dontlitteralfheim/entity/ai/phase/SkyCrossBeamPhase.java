package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.api.AlfheimAttackHelper;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

public class SkyCrossBeamPhase implements EnforcerAttackPhase {
    // Match the circle color used in AlfheimAttackHelper.SKY_CROSS_BEAM
    private static final int SKY_CIRCLE_COLOR = 0xFF38E8FF;

    private Vec3 lockedGroundPos = Vec3.ZERO;

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        this.lockedGroundPos = target.position();

        if (mob.level() instanceof ServerLevel serverLevel) {
            spawnSkyCrossParticles(serverLevel, this.lockedGroundPos, 2.5D, 24);
        }

        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.5F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        int totalSteps = AlfheimAttackHelper.SKY_CROSS_BEAM.getTotalSteps();
        if (actionTick % 4 == 0) {
            int step = (actionTick / 4) - 1;
            if (step >= 0 && step < totalSteps) {
                AlfheimAttackHelper.executeSkyCrossStep(mob, target, this.lockedGroundPos, step);

                if (mob.level() instanceof ServerLevel serverLevel) {
                    spawnSkyCrossParticles(serverLevel, this.lockedGroundPos, 2.0D + step * 0.45D, 28);
                }

                mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.7F, 1.6F + step * 0.08F);
            }
        }

        if (actionTick >= totalSteps * 4 + 8) {
            return 50;
        }
        return -1;
    }

    private static void spawnSkyCrossParticles(ServerLevel level, Vec3 center, double radius, int points) {
        float r = ((SKY_CIRCLE_COLOR >> 16) & 0xFF) / 255.0F;
        float g = ((SKY_CIRCLE_COLOR >> 8) & 0xFF) / 255.0F;
        float b = (SKY_CIRCLE_COLOR & 0xFF) / 255.0F;

        WispParticleData wisp = WispParticleData.wisp(0.50F, r, g, b, 1.1F);
        SparkleParticleData sparkle = SparkleParticleData.sparkle(1.4F, r, g, b, 10);

        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(wisp, px, center.y + 0.15D, pz, 1, 0.03D, 0.06D, 0.03D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(sparkle, px, center.y + 0.2D, pz, 1, 0.04D, 0.08D, 0.04D, 0.01D);
            }
        }
        level.sendParticles(wisp, center.x, center.y + 3.5D, center.z, 12, 1.2D, 2.5D, 1.2D, 0.04D);
    }
}