package net.sumomo.dontlitteralfheim.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.api.attack.ContinuousSweepBeamAttack;
import net.sumomo.dontlitteralfheim.api.attack.DenseRailgunAttack;
import net.sumomo.dontlitteralfheim.api.attack.SkyCrossBeamAttack;

import javax.annotation.Nullable;

public final class AlfheimAttackHelper {
    public static final SkyCrossBeamAttack SKY_CROSS_BEAM = new SkyCrossBeamAttack();
    public static final DenseRailgunAttack DENSE_RAILGUN = new DenseRailgunAttack();
    public static final ContinuousSweepBeamAttack CONTINUOUS_BEAM = new ContinuousSweepBeamAttack();

    private AlfheimAttackHelper() {}

    // Entry point for Item#use (Player)
    public static boolean executeFromItem(AbstractBotaniaAttack attack, Level level, Player player, ItemStack stack) {
        Vec3 targetPos = raycastTargetPos(level, player, 32.0);
        AttackContext ctx = new AttackContext(level, player, null, targetPos, stack);
        return attack.playerExecute(ctx);
    }

    // Entry point for Boss AI (Mob)
    public static void executeFromAi(AbstractBotaniaAttack attack, LivingEntity boss, @Nullable LivingEntity target) {
        Vec3 targetPos = (target != null)
                ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                : boss.getEyePosition().add(boss.getLookAngle().scale(16.0));
        AttackContext ctx = new AttackContext(boss.level(), boss, target, targetPos, ItemStack.EMPTY);
        attack.aiExecute(ctx);
    }

    // Step-by-step execution for staggered volley attacks (AI or channeled tick)
    public static void executeSkyCrossStep(LivingEntity caster, @Nullable LivingEntity target, Vec3 lockedTargetGround, int step) {
        AttackContext ctx = new AttackContext(caster.level(), caster, target, lockedTargetGround, ItemStack.EMPTY);
        SKY_CROSS_BEAM.fireStep(ctx, step);
    }

    public static Vec3 raycastTargetPos(Level level, LivingEntity caster, double maxRange) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endVec = eyePos.add(lookVec.scale(maxRange));

        AABB searchBox = caster.getBoundingBox().expandTowards(lookVec.scale(maxRange)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                caster, eyePos, endVec, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(),
                maxRange * maxRange
        );
        if (entityHit != null) {
            return entityHit.getLocation();
        }

        HitResult blockHit = level.clip(new ClipContext(
                eyePos, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster
        ));
        return blockHit.getLocation();
    }
}