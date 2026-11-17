/*
 * Tick/charge/launch logic adapted from vazkii.botania.common.entity.BabylonWeaponEntity (Botania Mod by Vazkii)
 * Distributed under the Botania License: http://botaniamod.net/license.php
 */

package net.sumomo.dontlitteralfheim.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import vazkii.botania.common.entity.BabylonWeaponEntity;

import static net.minecraft.core.BlockPos.getX;

public class EnforcerBladeEntity extends BabylonWeaponEntity {
    private static final int MAX_FLIGHT_TICKS = 200;

    @SuppressWarnings("unchecked")
    public EnforcerBladeEntity(EntityType<EnforcerBladeEntity> type, Level level) {
        super((EntityType<BabylonWeaponEntity>) (EntityType<?>) type, level);
    }

    /** レンダラーが見るのは rotation だけ: rotation = 180 - yaw */
    public void aimAt(Vec3 to) {
        double dx = to.x - getX();
        double dy = to.y - getY();
        double dz = to.z - getZ();
        float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        setRotation(Mth.wrapDegrees(180.0F - yaw));
        setYRot(yaw);
        setXRot(pitch); // 後述のレンダラー改造用
    }

    public void fire(Vec3 to, double speed) {
        aimAt(to);
        setDeltaMovement(to.subtract(position()).normalize().scale(speed));
        setCharging(false);
        setDelay(0);
        setLiveTicks(0);
        this.hurtMarked = true;
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (!level().isClientSide && (owner == null || !owner.isAlive())) {
            discard();
            return;
        }
        baseTick(); // super.tick() は呼ばない(Player以外のオーナーだとdiscardされる)

        if (isCharging()) { // 待機中
            setDeltaMovement(Vec3.ZERO);
            setChargeTicks(getChargeTicks() + 1);
            return;
        }

        Vec3 mot = getDeltaMovement();
        int live = getLiveTicks();

        if (!level().isClientSide) {
            setLiveTicks(live + 1);
            if (live > MAX_FLIGHT_TICKS) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit); // → onHitBlock/onHitEntity → 爆発して消える
                if (isRemoved()) return;
            }
        }
        setPos(getX() + mot.x, getY() + mot.y, getZ() + mot.z);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // 本家の「20ダメージ直撃」の代わり。不要なら消してOK
        if (hit.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity o && target != o) {
            target.hurt(damageSources().mobAttack(o), 20.0F);
        }
        super.onHitEntity(hit);
    }
}