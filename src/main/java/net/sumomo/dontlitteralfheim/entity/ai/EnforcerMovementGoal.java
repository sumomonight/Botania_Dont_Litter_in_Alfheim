package net.sumomo.dontlitteralfheim.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerAttackState;

import java.util.EnumSet;

public class EnforcerMovementGoal extends Goal {
    private final AlfheimEnforcerEntity mob;
    private float targetStrafe = 0.0F;
    private float currentStrafe = 0.0F;
    private float currentForward = 0.0F;
    private int strafeTimer = 0;
    private int tpCooldown = 60;
    private boolean isApproaching = false;
    private boolean isRetreating = false;

    public EnforcerMovementGoal(AlfheimEnforcerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.currentStrafe = 0.0F;
        this.currentForward = 0.0F;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        EnforcerAttackState state = this.mob.getAttackState();
        boolean isStationaryAttack = state == EnforcerAttackState.BABYLON_BARRAGE
                || state == EnforcerAttackState.RADIAL_CHARGE_BEAM
                || state == EnforcerAttackState.REPULSION_NOVA;

        boolean isBabylon = state == EnforcerAttackState.BABYLON_BARRAGE;
        float maxTurnYaw = isBabylon ? 24.0F : (isStationaryAttack ? 8.0F : 16.0F);
        float maxTurnPitch = isBabylon ? 24.0F : (isStationaryAttack ? 8.0F : 14.0F);
        this.mob.getLookControl().setLookAt(target, maxTurnYaw, maxTurnPitch);

        float bodyTurnSpeed = isBabylon ? 18.0F : 7.5F;
        this.mob.yBodyRot = Mth.approachDegrees(this.mob.yBodyRot, this.mob.yHeadRot, bodyTurnSpeed);

        if (isStationaryAttack) {
            this.mob.getNavigation().stop();
            this.currentStrafe = Mth.lerp(0.25F, this.currentStrafe, 0.0F);
            this.currentForward = Mth.lerp(0.25F, this.currentForward, 0.0F);
            this.mob.getMoveControl().strafe(this.currentForward, this.currentStrafe);
            return;
        }

        double distSq = this.mob.distanceToSqr(target);

        if (this.tpCooldown > 0) this.tpCooldown--;

        if (--this.strafeTimer <= 0) {
            float roll = this.mob.getRandom().nextFloat();
            if (roll < 0.42F) {
                this.targetStrafe = 0.48F;
            } else if (roll < 0.84F) {
                this.targetStrafe = -0.48F;
            } else {
                this.targetStrafe = 0.0F;
            }
            this.strafeTimer = 50 + this.mob.getRandom().nextInt(45);
        }

        // Trigger safe teleport if too far (> 17 blocks) or cornered (< 2.5 blocks)
        if (this.tpCooldown <= 0 && (distSq > 289.0D || distSq < 6.25D)) {
            if (this.mob.safeTeleportAround(target, 6.5D, 9.5D)) {
                this.mob.getNavigation().stop();
                this.currentStrafe = 0.0F;
                this.currentForward = 0.0F;
                this.tpCooldown = 100;
                return;
            }
        }

        if (distSq > 132.25D) {
            this.isApproaching = true;
        } else if (distSq < 81.0D) {
            this.isApproaching = false;
        }

        if (distSq < 20.25D) {
            this.isRetreating = true;
        } else if (distSq > 36.0D) {
            this.isRetreating = false;
        }

        if (this.isApproaching) {
            this.mob.getNavigation().moveTo(target, 1.05D);
            this.currentStrafe = Mth.lerp(0.15F, this.currentStrafe, 0.0F);
            this.currentForward = Mth.lerp(0.15F, this.currentForward, 0.0F);
        } else {
            this.mob.getNavigation().stop();
            float desiredForward = this.isRetreating ? -0.36F : 0.0F;

            this.currentForward = Mth.lerp(0.12F, this.currentForward, desiredForward);
            this.currentStrafe = Mth.lerp(0.12F, this.currentStrafe, this.targetStrafe);

            this.mob.getMoveControl().strafe(this.currentForward, this.currentStrafe);
        }
    }
}