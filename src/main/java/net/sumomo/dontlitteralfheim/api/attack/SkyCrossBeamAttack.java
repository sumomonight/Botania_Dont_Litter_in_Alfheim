package net.sumomo.dontlitteralfheim.api.attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.api.AbstractBotaniaAttack;
import net.sumomo.dontlitteralfheim.api.AttackContext;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;

public class SkyCrossBeamAttack extends AbstractBotaniaAttack {
    private final int color;
    private final float damagePerBeam;
    private final int totalSteps;

    public SkyCrossBeamAttack(int color, float damagePerBeam, int totalSteps) {
        super(0, 40);
        this.color = color;
        this.damagePerBeam = damagePerBeam;
        this.totalSteps = Math.max(2, totalSteps);
    }

    public SkyCrossBeamAttack() {
        this(MagicCircleEntity.COLOR_BABYLON, 12.0F, 6);
    }

    @Override
    protected void perform(AttackContext ctx) {
        for (int step = 0; step < this.totalSteps; step++) {
            this.fireStep(ctx, step);
        }
    }

    public void fireStep(AttackContext ctx, int step) {
        Level level = ctx.level();
        if (level.isClientSide) {
            return;
        }

        LivingEntity caster = ctx.caster();
        Vec3 targetGround = ctx.targetPos();

        Vec3 look = caster.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        if (flatLook.lengthSqr() < 0.0001) {
            flatLook = new Vec3(0.0, 0.0, 1.0);
        } else {
            flatLook = flatLook.normalize();
        }
        Vec3 right = flatLook.cross(new Vec3(0.0, 1.0, 0.0)).normalize();

        Vec3 leftSkyStart = targetGround.add(right.scale(-5.5)).add(0.0, 10.5, 0.0).add(flatLook.scale(-2.0));
        Vec3 rightSkyStart = targetGround.add(right.scale(5.5)).add(0.0, 10.5, 0.0).add(flatLook.scale(-2.0));

        if (step == 0) {
            MagicCircleEntity leftCircle = new MagicCircleEntity(
                    ModEntities.MAGIC_CIRCLE.get(), level, caster, 2.2F, this.color, 35, false, 0.0F, 0
            );
            leftCircle.setPos(leftSkyStart.x, leftSkyStart.y, leftSkyStart.z);
            level.addFreshEntity(leftCircle);

            MagicCircleEntity rightCircle = new MagicCircleEntity(
                    ModEntities.MAGIC_CIRCLE.get(), level, caster, 2.2F, this.color, 35, false, 0.0F, 0
            );
            rightCircle.setPos(rightSkyStart.x, rightSkyStart.y, rightSkyStart.z);
            level.addFreshEntity(rightCircle);
        }

        double sweep = ((double) step / (this.totalSteps - 1)) * 2.0 - 1.0;
        double depthSpan = 6.5;

        Vec3 leftGroundEnd = targetGround
                .add(right.scale(2.8))
                .add(flatLook.scale(sweep * depthSpan));

        Vec3 rightGroundEnd = targetGround
                .add(right.scale(-2.8))
                .add(flatLook.scale(-sweep * depthSpan));

        level.addFreshEntity(ManaBeamEntity.createDenseBeam(
                level, caster, leftSkyStart, leftGroundEnd, 0.85F, this.color, this.damagePerBeam, 16
        ));
        level.addFreshEntity(ManaBeamEntity.createDenseBeam(
                level, caster, rightSkyStart, rightGroundEnd, 0.85F, this.color, this.damagePerBeam, 16
        ));
    }

    public int getTotalSteps() {
        return this.totalSteps;
    }
}