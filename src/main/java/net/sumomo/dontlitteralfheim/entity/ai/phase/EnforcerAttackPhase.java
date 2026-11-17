package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.world.entity.LivingEntity;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;

public interface EnforcerAttackPhase {
    default void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {}

    // Returns next cooldown (>= 0) when complete, or -1 while still executing
    int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick);

    default void onStop(AlfheimEnforcerEntity mob) {}
}