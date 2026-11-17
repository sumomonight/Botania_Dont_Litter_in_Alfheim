package net.sumomo.dontlitteralfheim.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLLoader;
import net.sumomo.dontlitteralfheim.api.IAlfheimPortalAnger;
import net.sumomo.dontlitteralfheim.config.DontLitterConfig;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.common.block.block_entity.AlfheimPortalBlockEntity;
import vazkii.botania.common.entity.BotaniaEntities;
import vazkii.botania.common.entity.PixieEntity;

import java.util.List;

public class PortalAngerManager {

    public static void onTrashLittered(ServerLevel level, AlfheimPortalBlockEntity portal, IAlfheimPortalAnger angerData, int count) {
        BlockPos pos = portal.getBlockPos();
        double increasePerItem = DontLitterConfig.ANGER_INCREASE_PER_TRASH.get();
        double maxChance = DontLitterConfig.MAX_ANGER_CHANCE.get();
        int calamityThreshold = DontLitterConfig.CALAMITY_TRASH_THRESHOLD.get();
        int wrathThreshold = DontLitterConfig.CREATIVE_TRASH_THRESHOLD.get();

        double currentChance = angerData.dontLitter$getAngerChance();
        int currentEyeCount = angerData.dontLitter$getTrashCount();
        int currentWrathCount = angerData.dontLitter$getCreativeTrashCount();

        boolean triggeredNormalRaid = false;
        boolean triggeredCalamityBoss = false;
        boolean triggeredWrathBoss = false;

        for (int i = 0; i < count; i++) {
            currentEyeCount++;
            currentWrathCount++;
            currentChance = Math.min(maxChance, currentChance + increasePerItem);

            if (currentWrathCount >= wrathThreshold) {
                triggeredWrathBoss = true;
                currentWrathCount = 0;
            }

            if (currentEyeCount >= calamityThreshold) {
                triggeredCalamityBoss = true;
                currentEyeCount = 0;
                currentChance = 0.0;
            } else if (level.random.nextDouble() < currentChance) {
                triggeredNormalRaid = true;
                currentChance = 0.0;
            }
        }

        angerData.dontLitter$setTrashCount(currentEyeCount);
        angerData.dontLitter$setCreativeTrashCount(currentWrathCount);
        angerData.dontLitter$setAngerChance(currentChance);

        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 0.8F);

        String formattedChance = String.format("%.2f", currentChance * 100.0);
        sendChatLog(level, pos, Component.translatable(
                "message.dont_litter_in_alfheim.trash_littered",
                count, currentEyeCount, calamityThreshold, formattedChance, currentWrathCount, wrathThreshold
        ).withStyle(ChatFormatting.YELLOW));

        if (triggeredWrathBoss) {
            spawnUltimateWrathBoss(level, pos);
        }
        if (triggeredCalamityBoss) {
            spawnCalamityBoss(level, pos);
        } else if (triggeredNormalRaid) {
            spawnAngryElfSquad(level, pos);
        }
    }

    public static void onCreativeTrashLittered(ServerLevel level, AlfheimPortalBlockEntity portal, IAlfheimPortalAnger angerData) {
        BlockPos pos = portal.getBlockPos();
        int wrathThreshold = DontLitterConfig.CREATIVE_TRASH_THRESHOLD.get();

        angerData.dontLitter$setCreativeTrashCount(wrathThreshold);

        sendChatLog(level, pos, Component.translatable(
                "message.dont_litter_in_alfheim.creative_trash_littered",
                wrathThreshold, wrathThreshold
        ).withStyle(ChatFormatting.LIGHT_PURPLE));

        angerData.dontLitter$setCreativeTrashCount(0);
        spawnUltimateWrathBoss(level, pos);
    }

    public static void resetEyeAnger(ServerLevel level, AlfheimPortalBlockEntity portal, IAlfheimPortalAnger angerData) {
        BlockPos pos = portal.getBlockPos();
        angerData.dontLitter$setAngerChance(0.0);
        angerData.dontLitter$setTrashCount(0);

        level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.4F);

        sendChatLog(level, pos, Component.translatable("message.dont_litter_in_alfheim.reset_eye")
                .withStyle(ChatFormatting.GREEN));
    }

    public static void resetAllAnger(ServerLevel level, AlfheimPortalBlockEntity portal, IAlfheimPortalAnger angerData) {
        BlockPos pos = portal.getBlockPos();
        angerData.dontLitter$setAngerChance(0.0);
        angerData.dontLitter$setTrashCount(0);
        angerData.dontLitter$setCreativeTrashCount(0);

        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 16, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.5F);

        sendChatLog(level, pos, Component.translatable("message.dont_litter_in_alfheim.reset_all")
                .withStyle(ChatFormatting.AQUA));
    }

    private static void spawnAngryElfSquad(ServerLevel level, BlockPos portalPos) {
        int min = DontLitterConfig.ANGRY_ELF_MIN_COUNT.get();
        int max = Math.max(min, DontLitterConfig.ANGRY_ELF_MAX_COUNT.get());
        int spawnCount = min + level.random.nextInt(max - min + 1);

        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, portalPos.getX() + 0.5, portalPos.getY() + 1.5, portalPos.getZ() + 0.5, 15, 0.5, 0.6, 0.5, 0.05);
        level.playSound(null, portalPos, SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 1.2F, 1.3F);

        sendChatLog(level, portalPos, Component.translatable("message.dont_litter_in_alfheim.raid_triggered", spawnCount)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        Player nearestPlayer = level.getNearestPlayer(portalPos.getX() + 0.5, portalPos.getY() + 1.2, portalPos.getZ() + 0.5, 24.0, false);

        for (int i = 0; i < spawnCount; i++) {
            spawnHostilePixie(level, portalPos, nearestPlayer, 4.0F);
        }
    }

    private static void spawnCalamityBoss(ServerLevel level, BlockPos portalPos) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, portalPos.getX() + 0.5, portalPos.getY() + 1.5, portalPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.0);
        level.playSound(null, portalPos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 0.85F);

        sendChatLog(level, portalPos, Component.translatable("message.dont_litter_in_alfheim.calamity_triggered")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));

        Player nearestPlayer = level.getNearestPlayer(portalPos.getX() + 0.5, portalPos.getY() + 1.2, portalPos.getZ() + 0.5, 24.0, false);
        spawnHostilePixie(level, portalPos, nearestPlayer, 8.0F);
    }

    private static void spawnHostilePixie(ServerLevel level, BlockPos portalPos, Player target, float damage) {
        PixieEntity pixie = BotaniaEntities.PIXIE.create(level);
        if (pixie != null) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
            pixie.moveTo(portalPos.getX() + 0.5 + offsetX, portalPos.getY() + 1.2, portalPos.getZ() + 0.5 + offsetZ, level.random.nextFloat() * 360.0F, 0.0F);
            pixie.setProps(target, null, 1, damage);
            pixie.finalizeSpawn(level, level.getCurrentDifficultyAt(portalPos), MobSpawnType.TRIGGERED, null, null);
            level.addFreshEntity(pixie);
        }
    }

    private static void spawnUltimateWrathBoss(ServerLevel level, BlockPos portalPos) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, portalPos.getX() + 0.5, portalPos.getY() + 1.5, portalPos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, portalPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.9F);

        sendChatLog(level, portalPos, Component.translatable("message.dont_litter_in_alfheim.wrath_triggered")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        AlfheimEnforcerEntity enforcer = ModEntities.ALFHEIM_ENFORCER.get().create(level);
        if (enforcer != null) {
            enforcer.moveTo(portalPos.getX() + 0.5, portalPos.getY() + 1.2, portalPos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            enforcer.finalizeSpawn(level, level.getCurrentDifficultyAt(portalPos), MobSpawnType.TRIGGERED, null, null);
            level.addFreshEntity(enforcer);
        }
    }

    // Debug logs are restricted to development environments only
    public static void sendChatLog(ServerLevel level, BlockPos pos, Component message) {
        if (FMLLoader.isProduction()) {
            return;
        }
        sendPlayerMessage(level, pos, message);
    }

    // Player-facing portal feedback sent in both development and production environments
    public static void sendPlayerMessage(ServerLevel level, BlockPos pos, Component message) {
        AABB searchBox = new AABB(pos).inflate(24.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchBox);
        for (ServerPlayer player : players) {
            player.sendSystemMessage(message);
        }
    }
}