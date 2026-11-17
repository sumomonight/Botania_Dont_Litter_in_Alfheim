package net.sumomo.dontlitteralfheim.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerAttackState;
import net.sumomo.dontlitteralfheim.entity.ai.phase.*;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class EnforcerAttackGoal extends Goal {
    private final AlfheimEnforcerEntity mob;
    private final Map<EnforcerAttackState, EnforcerAttackPhase> phases = new EnumMap<>(EnforcerAttackState.class);

    @Nullable
    private EnforcerAttackPhase activePhase;
    private int cooldown = 30;
    private int actionTick = 0;
    private boolean health75Triggered = false;
    private boolean health50Triggered = false;
    private boolean health25Triggered = false;

    public EnforcerAttackGoal(AlfheimEnforcerEntity mob) {
        this.mob = mob;
        this.phases.put(EnforcerAttackState.BABYLON_BARRAGE, new BabylonBarragePhase());
        this.phases.put(EnforcerAttackState.TRASH_BARRAGE, new SkyCrossBeamPhase());
        this.phases.put(EnforcerAttackState.MANA_LASER, new DenseRailgunPhase());
        this.phases.put(EnforcerAttackState.RADIAL_CHARGE_BEAM, new RadialChargeBeamPhase());
        this.phases.put(EnforcerAttackState.LIGHTNING_STRIKE, new LightningStrikePhase());
        this.phases.put(EnforcerAttackState.REPULSION_NOVA, new RepulsionNovaPhase());
        this.phases.put(EnforcerAttackState.SUMMON_PIXIES, new SummonPixiesPhase());
        this.phases.put(EnforcerAttackState.UNLIMITED_BLADE_BARRAGE, new UnlimitedBladeBarragePhase());
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public void stop() {
        if (this.activePhase != null) {
            this.activePhase.onStop(this.mob);
            this.activePhase = null;
        }
        this.mob.setAttackState(EnforcerAttackState.IDLE);
        this.actionTick = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        EnforcerAttackState state = this.mob.getAttackState();

        if (state == EnforcerAttackState.IDLE) {
            if (--this.cooldown <= 0) {
                EnforcerAttackState nextState = selectNextAttack(target);
                this.mob.setAttackState(nextState);
                this.actionTick = 0;
                this.activePhase = this.phases.get(nextState);
                if (this.activePhase != null) {
                    this.activePhase.onStart(this.mob, target);
                }
            }
            return;
        }

        if (this.activePhase == null) {
            this.activePhase = this.phases.get(state);
            if (this.activePhase == null) {
                finishAttack(30);
                return;
            }
        }

        this.actionTick++;
        int resultCooldown = this.activePhase.tick(this.mob, target, this.actionTick);
        if (resultCooldown >= 0) {
            finishAttack(resultCooldown);
        }
    }

    private EnforcerAttackState selectNextAttack(LivingEntity target) {
        float healthRatio = this.mob.getHealth() / this.mob.getMaxHealth();
        boolean isHealth25 = healthRatio <= 0.25F;
        boolean isHealth50 = healthRatio <= 0.50F;
        boolean isHealth75 = healthRatio <= 0.75F;

        // 25%以下になった最初の攻撃は必ずUnlimited Blade Barrage
        if (isHealth25 && !this.health25Triggered) {
            this.health25Triggered = true;
            this.health50Triggered = true;
            this.health75Triggered = true;
            return EnforcerAttackState.UNLIMITED_BLADE_BARRAGE;
        }

        // 50%以下になった最初の攻撃は必ずRadial Charge Beam
        if (isHealth50 && !this.health50Triggered) {
            this.health50Triggered = true;
            this.health75Triggered = true;
            return EnforcerAttackState.RADIAL_CHARGE_BEAM;
        }

        // 75%以下になった最初の攻撃は必ずBabylon Barrage
        if (isHealth75 && !this.health75Triggered) {
            this.health75Triggered = true;
            return EnforcerAttackState.BABYLON_BARRAGE;
        }

        double distSq = this.mob.distanceToSqr(target);
        if (distSq < 25.0D) {
            return EnforcerAttackState.REPULSION_NOVA;
        }

        float roll = this.mob.getRandom().nextFloat();

        // HP25%以下 (25% -> 20% -> 18% -> 15% -> 10% -> 7% -> 5%)
        if (isHealth25) {
            if (roll < 0.25F) {
                return EnforcerAttackState.BABYLON_BARRAGE;
            } else if (roll < 0.45F) {
                return EnforcerAttackState.TRASH_BARRAGE;
            } else if (roll < 0.63F) {
                return EnforcerAttackState.MANA_LASER;
            } else if (roll < 0.78F) {
                return EnforcerAttackState.RADIAL_CHARGE_BEAM;
            } else if (roll < 0.88F) {
                return EnforcerAttackState.LIGHTNING_STRIKE;
            } else if (roll < 0.95F) {
                return EnforcerAttackState.SUMMON_PIXIES;
            } else {
                return EnforcerAttackState.UNLIMITED_BLADE_BARRAGE;
            }
        }

        // HP50%以下 (25% -> 23% -> 20% -> 17% -> 9% -> 6%)
        if (isHealth50) {
            if (roll < 0.25F) {
                return EnforcerAttackState.BABYLON_BARRAGE;
            } else if (roll < 0.48F) {
                return EnforcerAttackState.TRASH_BARRAGE;
            } else if (roll < 0.68F) {
                return EnforcerAttackState.MANA_LASER;
            } else if (roll < 0.85F) {
                return EnforcerAttackState.RADIAL_CHARGE_BEAM;
            } else if (roll < 0.94F) {
                return EnforcerAttackState.LIGHTNING_STRIKE;
            } else {
                return EnforcerAttackState.SUMMON_PIXIES;
            }
        }

        // HP75%以下 (30% -> 26% -> 22% -> 14% -> 8%)
        if (isHealth75) {
            if (roll < 0.30F) {
                return EnforcerAttackState.BABYLON_BARRAGE;
            } else if (roll < 0.56F) {
                return EnforcerAttackState.TRASH_BARRAGE;
            } else if (roll < 0.78F) {
                return EnforcerAttackState.MANA_LASER;
            } else if (roll < 0.92F) {
                return EnforcerAttackState.LIGHTNING_STRIKE;
            } else {
                return EnforcerAttackState.SUMMON_PIXIES;
            }
        }

        // HP75%超 (45% -> 28% -> 17% -> 10%)
        if (roll < 0.45F) {
            return EnforcerAttackState.TRASH_BARRAGE;
        } else if (roll < 0.73F) {
            return EnforcerAttackState.MANA_LASER;
        } else if (roll < 0.90F) {
            return EnforcerAttackState.LIGHTNING_STRIKE;
        } else {
            return EnforcerAttackState.SUMMON_PIXIES;
        }
    }

    private void finishAttack(int nextCooldown) {
        if (this.activePhase != null) {
            this.activePhase.onStop(this.mob);
            this.activePhase = null;
        }
        this.mob.setAttackState(EnforcerAttackState.IDLE);
        this.actionTick = 0;
        this.cooldown = nextCooldown;
    }
}