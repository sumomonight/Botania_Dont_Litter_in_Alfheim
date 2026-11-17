package net.sumomo.dontlitteralfheim.api.attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.api.AbstractBotaniaAttack;
import net.sumomo.dontlitteralfheim.api.AttackContext;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;

public class ContinuousSweepBeamAttack extends AbstractBotaniaAttack {
    private final int color;
    private final float damagePerHit;
    private final int duration;

    public ContinuousSweepBeamAttack(int color, float damagePerHit, int duration) {
        super(0, 50);
        this.color = color;
        this.damagePerHit = damagePerHit;
        this.duration = duration;
    }

    public ContinuousSweepBeamAttack() {
        this(ManaBeamEntity.COLOR_BABYLON, 5.0F, 40);
    }

    @Override
    protected void perform(AttackContext ctx) {
        Level level = ctx.level();
        if (level.isClientSide) {
            return;
        }

        LivingEntity caster = ctx.caster();
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(1.2));
        Vec3 dir = ctx.targetPos().subtract(start);
        if (dir.lengthSqr() < 0.0001) {
            dir = caster.getLookAngle();
        } else {
            dir = dir.normalize();
        }
        Vec3 end = start.add(dir.scale(28.0));

        level.addFreshEntity(ManaBeamEntity.createContinuousBeam(
                level, caster, start, end, 0.75F, this.color, this.damagePerHit, this.duration, 4
        ));
    }
}