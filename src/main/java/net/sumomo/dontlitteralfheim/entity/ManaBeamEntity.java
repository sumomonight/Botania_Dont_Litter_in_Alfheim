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
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.registry.ModEntities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ManaBeamEntity extends Entity {
    public static final int COLOR_BABYLON = 0xFFFFFFFF;
    public static final int COLOR_EMERALD = 0xFF44FF88;
    public static final int COLOR_CALAMITY = 0xFFAA22FF;
    public static final int COLOR_WRATH = 0xFFFFCC22;
    public static final int COLOR_VOID = 0xFFFF3366;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_END_X = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Y = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_DENSE = SynchedEntityData.defineId(ManaBeamEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private float damage;
    private int hitInterval;

    public ManaBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ManaBeamEntity(EntityType<?> type, Level level, @Nullable LivingEntity owner, Vec3 start, Vec3 end, float radius, int color, float damage, int duration, int hitInterval, boolean dense) {
        this(type, level);
        this.setPos(start.x, start.y, start.z);
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
        this.setEndPos(end);
        this.setRadius(radius);
        this.setColor(color);
        this.damage = damage;
        this.setDuration(duration);
        this.hitInterval = hitInterval;
        this.setDense(dense);
    }

    public static ManaBeamEntity createContinuousBeam(Level level, @Nullable LivingEntity owner, Vec3 start, Vec3 end, float radius, int color, float damagePerHit, int duration, int hitInterval) {
        return new ManaBeamEntity(ModEntities.MANA_BEAM.get(), level, owner, start, end, radius, color, damagePerHit, duration, Math.max(1, hitInterval), false);
    }

    public static ManaBeamEntity createDenseBeam(Level level, @Nullable LivingEntity owner, Vec3 start, Vec3 end, float radius, int color, float burstDamage, int fadeDuration) {
        return new ManaBeamEntity(ModEntities.MANA_BEAM.get(), level, owner, start, end, radius, color, burstDamage, Math.max(1, fadeDuration), 0, true);
    }

    public static ManaBeamEntity createSightBeam(Level level, @Nullable LivingEntity owner, Vec3 start, Vec3 end, float radius, int color, int duration) {
        return new ManaBeamEntity(ModEntities.MANA_BEAM.get(), level, owner, start, end, radius, color, 0.0F, Math.max(1, duration), 0, false);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 0.6F);
        this.entityData.define(DATA_COLOR, COLOR_EMERALD);
        this.entityData.define(DATA_DURATION, 20);
        this.entityData.define(DATA_END_X, 0.0F);
        this.entityData.define(DATA_END_Y, 0.0F);
        this.entityData.define(DATA_END_Z, 0.0F);
        this.entityData.define(DATA_DENSE, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.damage > 0.0F) {
                if (this.isDense() || this.hitInterval <= 0) {
                    if (this.tickCount == 1) {
                        this.dealBeamDamage(true);
                    }
                } else {
                    if (this.tickCount % this.hitInterval == 0) {
                        this.dealBeamDamage(false);
                    }
                }
            }

            if (this.tickCount >= this.getDuration()) {
                this.discard();
            }
        }
    }

    private void dealBeamDamage(boolean knockback) {
        Vec3 start = this.position();
        Vec3 end = this.getEndPos();
        float r = this.getRadius();

        AABB searchBox = new AABB(start, end).inflate(r + 1.0);
        Vec3 dir = end.subtract(start);
        Vec3 flatKnockback = new Vec3(dir.x, 0.0, dir.z).normalize();

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
            if (!entity.isAlive() || entity.isSpectator()) {
                return false;
            }
            if (this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())) {
                return false;
            }
            AABB hitBox = entity.getBoundingBox().inflate(r);
            Optional<Vec3> hit = hitBox.clip(start, end);
            return hit.isPresent() || hitBox.contains(start) || hitBox.contains(end);
        });

        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            if (target.hurt(this.damageSources().magic(), this.damage) && knockback && flatKnockback.lengthSqr() > 0.001) {
                target.push(flatKnockback.x * 1.1, 0.35, flatKnockback.z * 1.1);
                target.hurtMarked = true;
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 start = this.position();
        Vec3 end = this.getEndPos();
        float r = this.getRadius() + 1.0F;
        return new AABB(start, end).inflate(r);
    }

    public Vec3 getEndPos() {
        return new Vec3(
                this.entityData.get(DATA_END_X),
                this.entityData.get(DATA_END_Y),
                this.entityData.get(DATA_END_Z)
        );
    }

    public void setEndPos(Vec3 end) {
        this.entityData.set(DATA_END_X, (float) end.x);
        this.entityData.set(DATA_END_Y, (float) end.y);
        this.entityData.set(DATA_END_Z, (float) end.z);
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

    public boolean isDense() {
        return this.entityData.get(DATA_DENSE);
    }

    public void setDense(boolean dense) {
        this.entityData.set(DATA_DENSE, dense);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.setRadius(tag.getFloat("Radius"));
        this.setColor(tag.getInt("Color"));
        this.setDuration(tag.getInt("Duration"));
        this.setEndPos(new Vec3(tag.getDouble("EndX"), tag.getDouble("EndY"), tag.getDouble("EndZ")));
        this.setDense(tag.getBoolean("Dense"));
        this.damage = tag.getFloat("Damage");
        this.hitInterval = tag.getInt("HitInterval");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Radius", this.getRadius());
        tag.putInt("Color", this.getColor());
        tag.putInt("Duration", this.getDuration());
        Vec3 end = this.getEndPos();
        tag.putDouble("EndX", end.x);
        tag.putDouble("EndY", end.y);
        tag.putDouble("EndZ", end.z);
        tag.putBoolean("Dense", this.isDense());
        tag.putFloat("Damage", this.damage);
        tag.putInt("AttackInterval", this.hitInterval);
    }
}