package net.sumomo.dontlitteralfheim.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.sumomo.dontlitteralfheim.entity.ai.EnforcerAttackGoal;
import net.sumomo.dontlitteralfheim.entity.ai.EnforcerMovementGoal;

public class AlfheimEnforcerEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(AlfheimEnforcerEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_ANGRY =
            SynchedEntityData.defineId(AlfheimEnforcerEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean clientMusicPlaying = false;

    private int consecutiveDamageHits = 0;

    public int getConsecutiveDamageHits() {
        return this.consecutiveDamageHits;
    }

    public void resetConsecutiveDamageHits() {
        this.consecutiveDamageHits = 0;
    }

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    public AlfheimEnforcerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACK_STATE, EnforcerAttackState.IDLE.id);
        this.entityData.define(DATA_ANGRY, false);
    }

    public EnforcerAttackState getAttackState() {
        return EnforcerAttackState.byId(this.entityData.get(DATA_ATTACK_STATE));
    }

    public void setAttackState(EnforcerAttackState state) {
        this.entityData.set(DATA_ATTACK_STATE, state.id);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EnforcerAttackGoal(this));
        this.goalSelector.addGoal(3, new EnforcerMovementGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        boolean isAngry = this.getTarget() != null && this.getTarget().isAlive();
        BossEvent.BossBarColor targetColor = isAngry ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN;
        if (this.bossEvent.getColor() != targetColor) {
            this.bossEvent.setColor(targetColor);
        }
    }

    public boolean safeTeleportAround(LivingEntity center, double minRadius, double maxRadius) {
        if (this.level().isClientSide) return false;

        for (int attempt = 0; attempt < 25; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = minRadius + this.random.nextDouble() * (maxRadius - minRadius);

            int targetX = Mth.floor(center.getX() + Math.cos(angle) * dist);
            int targetZ = Mth.floor(center.getZ() + Math.sin(angle) * dist);
            int startY = Mth.floor(center.getY()) + 4;

            for (int yOffset = 0; yOffset <= 8; yOffset++) {
                BlockPos groundPos = new BlockPos(targetX, startY - yOffset, targetZ);
                BlockState groundState = this.level().getBlockState(groundPos);

                if (groundState.blocksMotion()) {
                    BlockPos feetPos = groundPos.above();
                    if (isClearSpace(feetPos)) {
                        double destX = feetPos.getX() + 0.5D;
                        double destY = feetPos.getY();
                        double destZ = feetPos.getZ() + 0.5D;

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.1D);
                        }
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);

                        this.teleportTo(destX, destY, destZ);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, destX, destY + 1.0D, destZ, 25, 0.5D, 1.0D, 0.5D, 0.05D);
                        }
                        this.level().playSound(null, destX, destY, destZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean isClearSpace(BlockPos feetPos) {
        for (int i = 0; i < 3; i++) {
            BlockPos checkPos = feetPos.above(i);
            BlockState state = this.level().getBlockState(checkPos);
            if (state.blocksMotion() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        AABB targetBox = this.getType().getDimensions().makeBoundingBox(
                feetPos.getX() + 0.5D,
                feetPos.getY(),
                feetPos.getZ() + 0.5D
        );
        return this.level().noCollision(this, targetBox);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // Field in AlfheimEnforcerEntity
    private float damageWarpChance = 0.0F;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        float maxDamage = this.getMaxHealth() * 0.05F;
        boolean hurt = super.hurt(source, Math.min(amount, maxDamage));

        if (hurt && !this.level().isClientSide && amount > 0.0F) {
            this.damageWarpChance = Math.min(1.0F, this.damageWarpChance + 0.10F);

            EnforcerAttackState state = this.getAttackState();
            boolean isStationaryAttack = state == EnforcerAttackState.BABYLON_BARRAGE
                    || state == EnforcerAttackState.RADIAL_CHARGE_BEAM
                    || state == EnforcerAttackState.REPULSION_NOVA
                    || state == EnforcerAttackState.UNLIMITED_BLADE_BARRAGE;

            if (!isStationaryAttack && this.random.nextFloat() < this.damageWarpChance) {
                LivingEntity warpTarget = (source.getEntity() instanceof LivingEntity attacker)
                        ? attacker
                        : this.getTarget();
                if (warpTarget != null && this.safeTeleportAround(warpTarget, 6.5D, 9.5D)) {
                    this.getNavigation().stop();
                    this.damageWarpChance = 0.0F;
                }
            }
        }
        return hurt;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            super.actuallyHurt(source, amount);
            return;
        }
        float maxDamage = this.getMaxHealth() * 0.05F;
        super.actuallyHurt(source, Math.min(amount, maxDamage));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public boolean isAngry() {
        return this.entityData.get(DATA_ANGRY);
    }
}