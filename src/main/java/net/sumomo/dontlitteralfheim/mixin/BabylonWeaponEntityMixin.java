/*
 * Adapted from vazkii.botania.common.entity.BabylonWeaponEntity (Botania Mod by Vazkii).
 * This file has been modified from the original.
 * Distributed under the Botania License: http://botaniamod.net/license.php
 */
package net.sumomo.dontlitteralfheim.mixin;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.common.entity.BabylonWeaponEntity;
import vazkii.botania.common.entity.ThrowableCopyEntity;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.helper.VecHelper;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import vazkii.botania.common.item.relic.KeyOfTheKingsLawItem;

import java.util.List;

@Mixin(value = BabylonWeaponEntity.class, remap = false)
public abstract class BabylonWeaponEntityMixin extends ThrowableCopyEntity {

    protected BabylonWeaponEntityMixin(EntityType<? extends ThrowableCopyEntity> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract boolean isCharging();
    @Shadow public abstract void setCharging(boolean charging);
    @Shadow public abstract int getChargeTicks();
    @Shadow public abstract void setChargeTicks(int ticks);
    @Shadow public abstract int getLiveTicks();
    @Shadow public abstract void setLiveTicks(int ticks);
    @Shadow public abstract int getDelay();

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"), cancellable = true)
    private void dontLitter$onTickEnforcer(CallbackInfo ci) {
        Entity thrower = this.getOwner();
        if (!(thrower instanceof AlfheimEnforcerEntity enforcer)) {
            return;
        }

        ci.cancel();

        if (!enforcer.isAlive()) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide) {
            ItemStack mainHand = enforcer.getMainHandItem();
            boolean newCharging = mainHand.is(BotaniaItems.kingKey) && KeyOfTheKingsLawItem.isCharging(mainHand);
            if (this.isCharging() != newCharging) {
                this.setCharging(newCharging);
            }
        }

        Vec3 mot = this.getDeltaMovement();
        int liveTime = this.getLiveTicks();
        int delay = this.getDelay();
        boolean charging = this.isCharging() && liveTime == 0;

        if (charging) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setChargeTicks(this.getChargeTicks() + 1);

            if (this.level().random.nextInt(20) == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        BotaniaSounds.babylonSpawn, SoundSource.HOSTILE, 0.1F, 1.0F + this.level().random.nextFloat() * 3.0F);
            }
        } else {
            if (liveTime < delay) {
                this.setDeltaMovement(Vec3.ZERO);
            } else if (liveTime == delay) {
                Vec3 targetPos;
                LivingEntity target = enforcer.getTarget();
                if (target != null && target.isAlive()) {
                    targetPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                } else {
                    BlockHitResult rtr = ToolCommons.raytraceFromEntity(enforcer, 64, true);
                    if (rtr.getType() != HitResult.Type.BLOCK) {
                        targetPos = enforcer.getLookAngle().scale(64).add(enforcer.position());
                    } else {
                        targetPos = Vec3.atCenterOf(rtr.getBlockPos());
                    }
                }

                Vec3 thisVec = VecHelper.fromEntityCenter(this);
                mot = targetPos.subtract(thisVec.x, thisVec.y, thisVec.z).normalize().scale(2.0D);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        BotaniaSounds.babylonAttack, SoundSource.HOSTILE, 2.0F, 0.1F + this.level().random.nextFloat() * 3.0F);
            }

            if (!this.level().isClientSide) {
                this.setLiveTicks(liveTime + 1);
                AABB axis = new AABB(this.getX(), this.getY(), this.getZ(), this.xOld, this.yOld, this.zOld).inflate(2.0D);
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, axis);
                for (LivingEntity living : entities) {
                    if (living == enforcer) {
                        continue;
                    }

                    if (living.hurtTime == 0) {
                        living.hurt(this.level().damageSources().mobAttack(enforcer), 14.0F);
                        this.onHit(new EntityHitResult(living));
                        return;
                    }
                }
            }
        }

        super.tick();
        this.setDeltaMovement(mot);

        if (this.level().isClientSide && liveTime > delay) {
            WispParticleData data = WispParticleData.wisp(0.3F, 1.0F, 1.0F, 0.0F, 1);
            this.level().addParticle(data, this.getX(), this.getY(), this.getZ(), 0, 0.0D, 0);
        }

        if (!this.level().isClientSide && liveTime > 200 + delay) {
            this.discard();
        }
    }
}