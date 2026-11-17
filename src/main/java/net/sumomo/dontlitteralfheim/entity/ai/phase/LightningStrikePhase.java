package net.sumomo.dontlitteralfheim.entity.ai.phase;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.AlfheimLightningEntity;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;

import java.util.ArrayList;
import java.util.List;

public class LightningStrikePhase implements EnforcerAttackPhase {
    private static final int STRIKE_COUNT = 8;
    private static final int TELEGRAPH_DELAY_TICKS = 16;
    private static final int SPAWN_INTERVAL_TICKS = 3;

    private final List<Vec3> strikeLocations = new ArrayList<>(STRIKE_COUNT);

    @Override
    public void onStart(AlfheimEnforcerEntity mob, LivingEntity target) {
        this.strikeLocations.clear();
        Level level = mob.level();
        RandomSource random = mob.getRandom();
        Vec3 center = target.position();

        // First strike directly on target's current position
        this.strikeLocations.add(findGroundPos(level, center.x, center.y, center.z));

        // Remaining strikes at random positions within radius 9.0 around target
        for (int i = 1; i < STRIKE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = 1.8D + random.nextDouble() * 7.2D;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            this.strikeLocations.add(findGroundPos(level, x, center.y, z));
        }

        if (level instanceof ServerLevel serverLevel) {
            int hex = MagicCircleEntity.COLOR_WRATH;
            float r = ((hex >> 16) & 0xFF) / 255.0F;
            float g = ((hex >> 8) & 0xFF) / 255.0F;
            float b = (hex & 0xFF) / 255.0F;
            serverLevel.sendParticles(WispParticleData.wisp(0.55F, r, g, b, 1.1F),
                    mob.getX(), mob.getY() + 1.8D, mob.getZ(), 28, 0.6D, 0.5D, 0.6D, 0.05D);
            serverLevel.sendParticles(SparkleParticleData.sparkle(1.4F, r, g, b, 10),
                    mob.getX(), mob.getY() + 1.8D, mob.getZ(), 22, 0.7D, 0.5D, 0.7D, 0.04D);
        }

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.1F, 1.4F);
    }

    @Override
    public int tick(AlfheimEnforcerEntity mob, LivingEntity target, int actionTick) {
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Level level = mob.level();

        int hex = MagicCircleEntity.COLOR_WRATH;
        float r = ((hex >> 16) & 0xFF) / 255.0F;
        float g = ((hex >> 8) & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;

        // Phase A: Spawn warning magic circles staggered by SPAWN_INTERVAL_TICKS
        if ((actionTick - 1) % SPAWN_INTERVAL_TICKS == 0) {
            int circleIndex = (actionTick - 1) / SPAWN_INTERVAL_TICKS;
            if (circleIndex >= 0 && circleIndex < this.strikeLocations.size()) {
                Vec3 pos = this.strikeLocations.get(circleIndex);
                MagicCircleEntity warningCircle = new MagicCircleEntity(
                        ModEntities.MAGIC_CIRCLE.get(), level, mob, 2.2F,
                        MagicCircleEntity.COLOR_WRATH, TELEGRAPH_DELAY_TICKS + 10, false, 0.0F, 0
                );
                warningCircle.setPos(pos.x, pos.y, pos.z);
                level.addFreshEntity(warningCircle);

                if (level instanceof ServerLevel serverLevel) {
                    spawnGroundRing(serverLevel, pos.x, pos.y + 0.1D, pos.z, 2.2D, 24,
                            WispParticleData.wisp(0.40F, r, g, b, 0.9F),
                            SparkleParticleData.sparkle(1.1F, r, g, b, 8));
                }

                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.65F, 1.7F);
            }
        }

        // Phase B: Strike each circle location TELEGRAPH_DELAY_TICKS after its circle appeared
        int strikeTickOffset = actionTick - 1 - TELEGRAPH_DELAY_TICKS;
        if (strikeTickOffset >= 0 && strikeTickOffset % SPAWN_INTERVAL_TICKS == 0) {
            int strikeIndex = strikeTickOffset / SPAWN_INTERVAL_TICKS;
            if (strikeIndex >= 0 && strikeIndex < this.strikeLocations.size()) {
                Vec3 pos = this.strikeLocations.get(strikeIndex);
                AlfheimLightningEntity lightning = ModEntities.ALFHEIM_LIGHTNING.get().create(level);
                if (lightning != null) {
                    lightning.moveTo(pos.x, pos.y, pos.z);
                    lightning.setOwner(mob);
                    lightning.setDamage(13.0F);
                    level.addFreshEntity(lightning);
                }

                if (level instanceof ServerLevel serverLevel) {
                    WispParticleData strikeWisp = WispParticleData.wisp(0.60F, r, g, b, 1.2F);
                    SparkleParticleData strikeSparkle = SparkleParticleData.sparkle(1.5F, r, g, b, 12);
                    spawnGroundRing(serverLevel, pos.x, pos.y + 0.15D, pos.z, 2.4D, 28, strikeWisp, strikeSparkle);
                    serverLevel.sendParticles(strikeWisp, pos.x, pos.y + 1.5D, pos.z, 20, 0.45D, 1.5D, 0.45D, 0.06D);
                    serverLevel.sendParticles(strikeSparkle, pos.x, pos.y + 1.0D, pos.z, 18, 0.65D, 1.2D, 0.65D, 0.05D);
                }
            }
        }

        int totalDuration = (STRIKE_COUNT - 1) * SPAWN_INTERVAL_TICKS + TELEGRAPH_DELAY_TICKS + 10;
        if (actionTick >= totalDuration) {
            return 55;
        }
        return -1;
    }

    @Override
    public void onStop(AlfheimEnforcerEntity mob) {
        this.strikeLocations.clear();
    }

    private static void spawnGroundRing(ServerLevel level, double cx, double cy, double cz, double radius, int points, WispParticleData wisp, SparkleParticleData sparkle) {
        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(wisp, px, cy, pz, 1, 0.02D, 0.04D, 0.02D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(sparkle, px, cy, pz, 1, 0.03D, 0.04D, 0.03D, 0.01D);
            }
        }
    }

    private static Vec3 findGroundPos(Level level, double x, double refY, double z) {
        BlockPos basePos = BlockPos.containing(x, refY, z);
        for (int dy = 4; dy >= -6; dy--) {
            BlockPos check = basePos.offset(0, dy, 0);
            if (!level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir()) {
                return new Vec3(x, check.getY() + 1.0D, z);
            }
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return new Vec3(x, surfaceY, z);
    }
}