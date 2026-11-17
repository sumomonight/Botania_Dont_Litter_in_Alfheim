package net.sumomo.dontlitteralfheim.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class AlfheimLightningEntity extends LightningBolt {
    @Nullable
    private UUID ownerUuid;
    private float damage = 12.0F;
    private boolean dealtDamage = false;

    public AlfheimLightningEntity(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
        // Prevent vanilla fire spawning and item/mob conversions
        this.setVisualOnly(true);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUuid = (owner != null) ? owner.getUUID() : null;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !this.dealtDamage) {
            this.dealtDamage = true;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.5F, 0.9F + this.random.nextFloat() * 0.2F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.5F, 0.8F + this.random.nextFloat() * 0.2F);

            if (this.damage > 0.0F) {
                AABB hitArea = new AABB(
                        this.getX() - 2.5, this.getY() - 1.0, this.getZ() - 2.5,
                        this.getX() + 2.5, this.getY() + 4.0, this.getZ() + 2.5
                );
                List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitArea, entity -> {
                    if (!entity.isAlive() || entity.isSpectator()) {
                        return false;
                    }
                    return this.ownerUuid == null || !this.ownerUuid.equals(entity.getUUID());
                });

                for (LivingEntity target : targets) {
                    target.invulnerableTime = 0;
                    target.hurt(this.damageSources().lightningBolt(), this.damage);
                    target.setRemainingFireTicks(0);
                }
            }
        }
    }
}