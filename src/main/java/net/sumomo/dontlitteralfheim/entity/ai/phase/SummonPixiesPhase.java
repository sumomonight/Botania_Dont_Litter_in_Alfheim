package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import vazkii.botania.common.entity.PixieEntity;

public class SummonPixiesPhase implements EnforcerAttackPhase {
    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        if (actionTick < 15) {
            if (mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WITCH,
                        mob.getX(), mob.getY() + 1.5D, mob.getZ(),
                        4, 0.8D, 0.8D, 0.8D, 0.05D);
            }
        } else if (actionTick == 15) {
            int count = 3 + mob.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0D / count) * i;
                double spawnX = mob.getX() + Math.cos(angle) * 1.5D;
                double spawnY = mob.getY() + 1.5D;
                double spawnZ = mob.getZ() + Math.sin(angle) * 1.5D;

                PixieEntity pixie = new PixieEntity(mob.level());
                pixie.setPos(spawnX, spawnY, spawnZ);
                pixie.setProps(target, mob, 1, 6.0F);
                pixie.setTarget(target);
                mob.level().addFreshEntity(pixie);
            }

            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.3F);
        }

        if (actionTick >= 25) {
            return 65;
        }
        return -1;
    }
}