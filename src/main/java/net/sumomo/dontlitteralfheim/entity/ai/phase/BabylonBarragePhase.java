
package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerBladeEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.common.item.BotaniaItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Gate of Babylon" barrage.
 * <p>
 * Three concentric golden fans (halos) open up behind the boss, one blade per tick,
 * each fan growing from its center outwards. After a short beat the blades are
 * fired one per tick in the same order, every blade re-aiming at the target's
 * current position (with partial lead and a little scatter), so it reads as a
 * continuous volley instead of a single simultaneous burst.
 */
public class BabylonBarragePhase implements EnforcerAttackPhase {
    // Key of the King's Law (Babylon) pure gold & royal amber palette
    private static final float GOLD_R = 1.00F;
    private static final float GOLD_G = 0.84F;
    private static final float GOLD_B = 0.05F;

    private static final float AMBER_R = 1.00F;
    private static final float AMBER_G = 0.62F;
    private static final float AMBER_B = 0.00F;

    // ---- Formation: concentric fans behind the boss (inner -> outer) ----
    private static final int[] ARC_COUNTS = {8, 10, 12};
    private static final double[] ARC_RADIUS = {3.5D, 5.0D, 6.5D};
    private static final double[] ARC_BACK = {1.8D, 2.6D, 3.4D};   // distance behind the boss
    private static final double HALO_CENTER_Y = 2.2D;              // fan center height above boss feet
    private static final double HALO_MIN_DEG = 10.0D;              // fan spans the upper half circle
    private static final double HALO_MAX_DEG = 170.0D;
    private static final float FORMATION_TURN_DEG = 8.0F;          // how fast the fan follows the boss' facing

    private record Slot(double radius, double back, double phi) {}

    private static final List<Slot> SLOTS = buildSlots();
    private static final int BLADE_COUNT = SLOTS.size();

    // ---- Timeline ----
    private static final int SPAWN_START_TICK = 10;
    private static final int SPAWN_END_TICK = SPAWN_START_TICK + BLADE_COUNT - 1;   // 1 blade / tick
    private static final int FIRE_START_TICK = SPAWN_END_TICK + 8;                  // short beat before the volley
    private static final int FIRE_END_TICK = FIRE_START_TICK + BLADE_COUNT - 1;     // 1 blade / tick
    private static final int DESCEND_TICK = FIRE_END_TICK + 8;

    // ---- Firing ----
    private static final double BLADE_SPEED = 2.4D;
    private static final double SCATTER_RADIUS = 3.0D;     // spreads explosions -> less knock-up
    private static final double SCATTER_VERTICAL = 0.5D;
    private static final double LEAD_FACTOR = 0.6D;        // 1.0 = perfect prediction
    private static final double MAX_LEAD_TICKS = 12.0D;

    private final List<EnforcerBladeEntity> blades = new ArrayList<>(BLADE_COUNT);
    private double hoverTargetY = 0.0D;
    private boolean landingBurstPlayed = false;
    private float formationYaw = 0.0F;

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        this.blades.clear();
        this.hoverTargetY = mob.getY() + 5.0D;
        this.landingBurstPlayed = false;
        this.formationYaw = mob.getYRot();
        mob.setNoGravity(true);

        if (mob.level() instanceof ServerLevel serverLevel) {
            double x = mob.getX();
            double y = mob.getY() + 0.2D;
            double z = mob.getZ();

            spawnBabylonRing(serverLevel, x, y, z, 2.0D, 12, 0.55F, true);

            serverLevel.sendParticles(
                    WispParticleData.wisp(0.60F, GOLD_R, GOLD_G, GOLD_B, 1.0F),
                    x, y + 0.6D, z, 14, 0.65D, 0.4D, 0.65D, 0.06D);
            serverLevel.sendParticles(
                    SparkleParticleData.sparkle(1.4F, AMBER_R, AMBER_G, AMBER_B, 10),
                    x, y + 0.8D, z, 12, 0.8D, 0.5D, 0.8D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y + 0.5D, z, 10, 0.4D, 0.2D, 0.4D, 0.06D);
        }
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.0F, 1.3F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        faceTargetSmoothly(mob, target, 24.0F);
        mob.getNavigation().stop();
        this.formationYaw = Mth.approachDegrees(this.formationYaw, mob.getYRot(), FORMATION_TURN_DEG);

        // Hover until the volley is over
        if (actionTick < DESCEND_TICK) {
            double diffY = this.hoverTargetY - mob.getY();
            if (Math.abs(diffY) > 0.1D) {
                double vy = Math.max(-0.5D, Math.min(0.65D, diffY * 0.35D));
                mob.setDeltaMovement(0.0D, vy, 0.0D);
            } else {
                mob.setDeltaMovement(Vec3.ZERO);
            }
            mob.hurtMarked = true;
        }

        if (mob.level() instanceof ServerLevel serverLevel) {
            renderBarrageParticles(serverLevel, mob, actionTick);
        }

        // Hold the Key of the King's Law for show (the real item is NOT used:
        // its onUseTick spawns BabylonWeaponEntity with the mob as owner, which Botania discards at once)
        if (actionTick == SPAWN_START_TICK) {
            mob.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BotaniaItems.kingKey));
        }

        // Phase 1: open the fans, one blade per tick, each fan growing from its center outwards
        if (actionTick >= SPAWN_START_TICK && actionTick <= SPAWN_END_TICK) {
            spawnBlade(mob, target, actionTick - SPAWN_START_TICK);
        }

        // Keep unfired blades attached to the formation and aimed at the target
        if (actionTick >= SPAWN_START_TICK && actionTick <= FIRE_END_TICK) {
            updateHeldBlades(mob, target);
        }

        // Phase 2: the beat before the volley
        if (actionTick == FIRE_START_TICK) {
            mob.level().playSound(null, mob.getX(), mob.getY() + 2.0D, mob.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.7F);
        }

        // Phase 3: volley, one blade per tick
        if (actionTick >= FIRE_START_TICK && actionTick <= FIRE_END_TICK) {
            fireBlade(mob, target, actionTick - FIRE_START_TICK);
        }

        if (actionTick == FIRE_END_TICK + 1) {
            mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        // Phase 4: descend
        if (actionTick == DESCEND_TICK) {
            mob.setNoGravity(false);
            mob.setDeltaMovement(0.0D, -0.25D, 0.0D);
            mob.hurtMarked = true;
        } else if (actionTick > DESCEND_TICK && !mob.onGround()) {
            Vec3 motion = mob.getDeltaMovement();
            if (motion.y < -0.35D) {
                mob.setDeltaMovement(motion.x, -0.35D, motion.z);
            }
        }

        if (actionTick >= DESCEND_TICK + 2 && mob.onGround() && !this.landingBurstPlayed) {
            this.landingBurstPlayed = true;
            if (mob.level() instanceof ServerLevel serverLevel) {
                spawnBabylonRing(serverLevel, mob.getX(), mob.getY() + 0.15D, mob.getZ(), 2.4D, 14, 0.50F, false);
            }
        }

        if (actionTick >= DESCEND_TICK + 2 && (mob.onGround() || actionTick >= DESCEND_TICK + 15)) {
            return 75;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        // Only discard blades that were never launched
        for (EnforcerBladeEntity blade : this.blades) {
            if (blade != null && blade.isAlive() && blade.isCharging()) {
                blade.discard();
            }
        }
        this.blades.clear();
        mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        mob.setNoGravity(false);
    }

    // ------------------------------------------------------------------
    // Formation
    // ------------------------------------------------------------------

    private static List<Slot> buildSlots() {
        List<Slot> slots = new ArrayList<>();
        for (int arc = 0; arc < ARC_COUNTS.length; arc++) {
            int n = ARC_COUNTS[arc];
            List<Slot> arcSlots = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double deg = HALO_MIN_DEG + (HALO_MAX_DEG - HALO_MIN_DEG) * i / (n - 1.0D);
                arcSlots.add(new Slot(ARC_RADIUS[arc], ARC_BACK[arc], Math.toRadians(deg)));
            }
            // center-out: the fan opens symmetrically from the top
            arcSlots.sort(Comparator.comparingDouble(s -> Math.abs(s.phi() - Math.PI / 2.0D)));
            slots.addAll(arcSlots);
        }
        return List.copyOf(slots);
    }

    private Vec3 slotPos(AlfheimEnforcerEntity mob, Slot slot) {
        double yaw = Math.toRadians(this.formationYaw);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -fz;
        double rz = fx;

        double lateral = Math.cos(slot.phi()) * slot.radius();
        double up = Math.sin(slot.phi()) * slot.radius();

        return new Vec3(
                mob.getX() - fx * slot.back() + rx * lateral,
                mob.getY() + HALO_CENTER_Y + up,
                mob.getZ() - fz * slot.back() + rz * lateral);
    }

    private void spawnBlade(AlfheimEnforcerEntity mob, LivingEntity target, int index) {
        if (index < 0 || index >= BLADE_COUNT) {
            return;
        }
        Vec3 pos = slotPos(mob, SLOTS.get(index));

        EnforcerBladeEntity blade = new EnforcerBladeEntity(ModEntities.ENFORCER_BLADE.get(), mob.level());
        blade.setOwner(mob);
        blade.setPos(pos.x, pos.y, pos.z);
        blade.setCharging(true);
        blade.setVariety(index % 12);
        blade.aimAt(getTargetAimPos(target));

        mob.level().addFreshEntity(blade);
        this.blades.add(blade);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    SparkleParticleData.sparkle(1.2F, GOLD_R, GOLD_G, GOLD_B, 8),
                    pos.x, pos.y, pos.z, 3, 0.12D, 0.12D, 0.12D, 0.01D);
        }
        float pitch = 0.9F + 0.8F * index / (float) BLADE_COUNT;
        mob.level().playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, pitch);
    }

    private void updateHeldBlades(AlfheimEnforcerEntity mob, LivingEntity target) {
        Vec3 aimTarget = getTargetAimPos(target);
        for (int i = 0; i < this.blades.size(); i++) {
            EnforcerBladeEntity blade = this.blades.get(i);
            if (blade == null || !blade.isAlive() || !blade.isCharging()) {
                continue;
            }
            Vec3 pos = slotPos(mob, SLOTS.get(i));
            blade.setPos(pos.x, pos.y, pos.z);
            blade.aimAt(aimTarget);
        }
    }

    // ------------------------------------------------------------------
    // Firing
    // ------------------------------------------------------------------

    private void fireBlade(AlfheimEnforcerEntity mob, LivingEntity target, int index) {
        if (index < 0 || index >= this.blades.size()) {
            return;
        }
        EnforcerBladeEntity blade = this.blades.get(index);
        if (blade == null || !blade.isAlive() || !blade.isCharging()) {
            return;
        }

        Vec3 from = blade.position();
        Vec3 aim = computeAimPoint(target, from, mob.getRandom());
        blade.fire(aim, BLADE_SPEED);

        if (index % 3 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    WispParticleData.wisp(0.5F, GOLD_R, GOLD_G, GOLD_B, 0.9F),
                    from.x, from.y, from.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
        }
        if (index % 2 == 0) {
            mob.level().playSound(null, from.x, from.y, from.z,
                    SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.9F, 1.2F + 0.4F * mob.getRandom().nextFloat());
        }
    }

    /** Target center + partial horizontal lead + random scatter. */
    private static Vec3 computeAimPoint(LivingEntity target, Vec3 from, RandomSource random) {
        Vec3 center = getTargetAimPos(target);

        // per-tick horizontal velocity (vertical ignored so falling targets don't pull shots into the ground)
        double vx = target.getX() - target.xOld;
        double vz = target.getZ() - target.zOld;
        double flightTicks = Math.min(MAX_LEAD_TICKS, from.distanceTo(center) / BLADE_SPEED);
        Vec3 lead = new Vec3(vx, 0.0D, vz).scale(flightTicks * LEAD_FACTOR);

        return center.add(lead).add(randomScatter(random));
    }

    private static Vec3 randomScatter(RandomSource random) {
        double r = SCATTER_RADIUS * Math.sqrt(random.nextDouble());
        double a = random.nextDouble() * Math.PI * 2.0D;
        double dy = (random.nextDouble() * 2.0D - 1.0D) * SCATTER_VERTICAL;
        return new Vec3(Math.cos(a) * r, dy, Math.sin(a) * r);
    }

    private static Vec3 getTargetAimPos(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    private static void renderBarrageParticles(ServerLevel serverLevel, AlfheimEnforcerEntity mob, int actionTick) {
        double cx = mob.getX();
        double cy = mob.getY() + mob.getBbHeight() * 0.5D;
        double cz = mob.getZ();

        // Double-helix swirling golden aura while the gate is open (every 2 ticks)
        if (actionTick <= FIRE_END_TICK && actionTick % 2 == 0) {
            double baseAngle = actionTick * 0.35D;
            double radius = 1.35D + Math.sin(actionTick * 0.15D) * 0.25D;

            WispParticleData goldOrb = WispParticleData.wisp(0.45F, GOLD_R, GOLD_G, GOLD_B, 0.85F);
            WispParticleData amberOrb = WispParticleData.wisp(0.45F, AMBER_R, AMBER_G, AMBER_B, 0.85F);

            for (int arm = 0; arm < 2; arm++) {
                double angle = baseAngle + arm * Math.PI;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                double py = cy + Math.sin(actionTick * 0.25D + arm * Math.PI) * 0.65D;

                serverLevel.sendParticles(arm == 0 ? goldOrb : amberOrb,
                        px, py, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            }

            serverLevel.sendParticles(
                    SparkleParticleData.sparkle(1.15F, GOLD_R, GOLD_G, GOLD_B, 8),
                    cx, cy, cz, 4, 0.9D, 0.8D, 0.9D, 0.015D);
        }

        // Gate opens
        if (actionTick == SPAWN_START_TICK) {
            spawnBabylonRing(serverLevel, cx, cy, cz, 2.2D, 14, 0.55F, true);
            serverLevel.sendParticles(
                    SparkleParticleData.sparkle(1.5F, GOLD_R, GOLD_G, GOLD_B, 10),
                    cx, cy, cz, 16, 1.1D, 1.0D, 1.1D, 0.03D);
        }

        // Volley begins
        if (actionTick == FIRE_START_TICK) {
            spawnBabylonRing(serverLevel, cx, cy, cz, 2.8D, 16, 0.60F, false);
            serverLevel.sendParticles(
                    WispParticleData.wisp(0.60F, GOLD_R, GOLD_G, GOLD_B, 1.1F),
                    cx, cy, cz, 18, 1.2D, 0.8D, 1.2D, 0.05D);
        }

        // Descent trail
        if (actionTick >= DESCEND_TICK && !mob.onGround() && actionTick % 2 == 0) {
            serverLevel.sendParticles(
                    WispParticleData.wisp(0.38F, GOLD_R, GOLD_G, GOLD_B, 0.7F),
                    cx, mob.getY() + 0.3D, cz, 3, 0.35D, 0.2D, 0.35D, 0.015D);
        }
    }

    private static void spawnBabylonRing(ServerLevel level, double cx, double cy, double cz, double radius, int points, float size, boolean primaryGold) {
        float r = primaryGold ? GOLD_R : AMBER_R;
        float g = primaryGold ? GOLD_G : AMBER_G;
        float b = primaryGold ? GOLD_B : AMBER_B;

        WispParticleData wisp = WispParticleData.wisp(size, r, g, b, 0.95F);

        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(wisp, px, cy, pz, 1, 0.02D, 0.02D, 0.02D, 0.005D);
        }
    }

    private static void faceTargetSmoothly(AlfheimEnforcerEntity mob, LivingEntity target, float maxTurnDeg) {
        mob.getLookControl().setLookAt(target, maxTurnDeg, maxTurnDeg);

        double dx = target.getX() - mob.getX();
        double dy = (target.getY() + target.getBbHeight() * 0.5D) - mob.getEyeY();
        double dz = target.getZ() - mob.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG));

        float nextYaw = Mth.approachDegrees(mob.getYRot(), targetYaw, maxTurnDeg);
        float nextPitch = Mth.approachDegrees(mob.getXRot(), targetPitch, maxTurnDeg);

        mob.setYRot(nextYaw);
        mob.setXRot(nextPitch);
        mob.yHeadRot = nextYaw;
        mob.yBodyRot = Mth.approachDegrees(mob.yBodyRot, nextYaw, maxTurnDeg * 0.75F);
    }
}