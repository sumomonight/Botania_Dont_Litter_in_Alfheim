package net.sumomo.dontlitteralfheim.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class MagicCircleEntity extends Entity {
    public static final int COLOR_BABYLON = 0xFFFFFFFF;
    public static final int COLOR_EMERALD = 0xFF44FF88;
    public static final int COLOR_CALAMITY = 0xFFAA22FF;
    public static final int COLOR_WRATH = 0xFFFFCC22;
    public static final int COLOR_VOID = 0xFFFF3366;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_VERTICAL = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private float damage;
    private int attackInterval;

    public MagicCircleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MagicCircleEntity(EntityType<?> type, Level level, @Nullable LivingEntity owner, float radius, int color, int duration, boolean vertical, float damage, int attackInterval) {
        this(type, level);
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
        this.setRadius(radius);
        this.setColor(color);
        this.setDuration(duration);
        this.setVertical(vertical);
        this.damage = damage;
        this.attackInterval = attackInterval;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 2.0F);
        this.entityData.define(DATA_COLOR, COLOR_BABYLON);
        this.entityData.define(DATA_DURATION, 40);
        this.entityData.define(DATA_VERTICAL, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.damage > 0.0F && this.attackInterval > 0 && this.tickCount % this.attackInterval == 0) {
                this.dealAreaDamage();
            }

            if (this.tickCount >= this.getDuration()) {
                this.discard();
            }
        }
    }

    private void dealAreaDamage() {
        float r = this.getRadius();
        AABB area = new AABB(
                this.getX() - r, this.getY() - 1.0, this.getZ() - r,
                this.getX() + r, this.getY() + 3.0, this.getZ() + r
        );

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (!entity.isAlive() || entity.isSpectator()) {
                return false;
            }
            if (this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())) {
                return false;
            }
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            return (dx * dx + dz * dz) <= (r * r);
        });

        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            target.hurt(this.damageSources().magic(), this.damage);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float r = this.getRadius();
        return new AABB(
                this.getX() - r, this.getY() - r, this.getZ() - r,
                this.getX() + r, this.getY() + r, this.getZ() + r
        );
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(DATA_RADIUS, radius);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    public void setDuration(int duration) {
        this.entityData.set(DATA_DURATION, duration);
    }

    public boolean isVertical() {
        return this.entityData.get(DATA_VERTICAL);
    }

    public void setVertical(boolean vertical) {
        this.entityData.set(DATA_VERTICAL, vertical);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.setRadius(tag.getFloat("Radius"));
        this.setColor(tag.getInt("Color"));
        this.setDuration(tag.getInt("Duration"));
        this.setVertical(tag.getBoolean("Vertical"));
        this.damage = tag.getFloat("Damage");
        this.attackInterval = tag.getInt("AttackInterval");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Radius", this.getRadius());
        tag.putInt("Color", this.getColor());
        tag.putInt("Duration", this.getDuration());
        tag.putBoolean("Vertical", this.isVertical());
        tag.putFloat("Damage", this.damage);
        tag.putInt("AttackInterval", this.attackInterval);
    }
}