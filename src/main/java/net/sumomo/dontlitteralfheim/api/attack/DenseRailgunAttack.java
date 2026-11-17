package net.sumomo.dontlitteralfheim.api.attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.api.AbstractBotaniaAttack;
import net.sumomo.dontlitteralfheim.api.AttackContext;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;

public class DenseRailgunAttack extends AbstractBotaniaAttack {
    private final int color;
    private final float damage;
    private final float radius;

    public DenseRailgunAttack(int color, float damage, float radius) {
        super(0, 30);
        this.color = color;
        this.damage = damage;
        this.radius = radius;
    }

    public DenseRailgunAttack() {
        this(ManaBeamEntity.COLOR_BABYLON, 20.0F, 1.8F);
    }

    @Override
    protected void perform(AttackContext ctx) {
        Level level = ctx.level();
        if (level.isClientSide) {
            return;
        }

        LivingEntity caster = ctx.caster();
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(1.5));
        Vec3 dir = ctx.targetPos().subtract(start);
        if (dir.lengthSqr() < 0.0001) {
            dir = caster.getLookAngle();
        } else {
            dir = dir.normalize();
        }
        Vec3 end = start.add(dir.scale(32.0));

        MagicCircleEntity muzzle = new MagicCircleEntity(
                ModEntities.MAGIC_CIRCLE.get(), level, caster, this.radius * 1.4F, this.color, 20, true, 0.0F, 0
        );
        muzzle.setPos(start.x, start.y, start.z);
        muzzle.setYRot(caster.getYRot());
        muzzle.setXRot(caster.getXRot() + 90.0F);
        level.addFreshEntity(muzzle);

        level.addFreshEntity(ManaBeamEntity.createDenseBeam(
                level, caster, start, end, this.radius, this.color, this.damage, 18
        ));
    }
}