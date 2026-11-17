package net.sumomo.dontlitteralfheim.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.sumomo.dontlitteralfheim.api.IAlfheimPortalAnger;
import net.sumomo.dontlitteralfheim.config.DontLitterConfig;
import vazkii.botania.common.block.block_entity.AlfheimPortalBlockEntity;
import vazkii.botania.xplat.XplatAbstractions;

import java.util.List;

public class PortalItemHandler {

    public static final TagKey<Item> TRASH_TAG = ItemTags.create(new ResourceLocation("dont_litter_in_alfheim", "trash"));
    public static final TagKey<Item> CREATIVE_TRASH_TAG = ItemTags.create(new ResourceLocation("dont_litter_in_alfheim", "trash/creative"));
    public static final TagKey<Item> RESET_ITEM_TAG = ItemTags.create(new ResourceLocation("dont_litter_in_alfheim", "anger_reset"));
    public static final TagKey<Item> CREATIVE_RESET_ITEM_TAG = ItemTags.create(new ResourceLocation("dont_litter_in_alfheim", "anger_reset/creative"));
    public static final TagKey<Item> SOUVENIR_TRADE_TAG = ItemTags.create(new ResourceLocation("dont_litter_in_alfheim", "souvenir_trade"));

    public static final ResourceLocation SOUVENIR_LOOT_DEFAULT = new ResourceLocation("dont_litter_in_alfheim", "gameplay/alfheim_souvenir");
    public static final ResourceLocation SOUVENIR_LOOT_MYTHIC = new ResourceLocation("dont_litter_in_alfheim", "gameplay/alfheim_souvenir_mythic");

    public static boolean handleBeforeTradeCheck(ServerLevel level, AlfheimPortalBlockEntity portal, IAlfheimPortalAnger angerData, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.is(CREATIVE_RESET_ITEM_TAG)) {
            PortalAngerManager.resetAllAnger(level, portal, angerData);
            return true;
        }

        if (stack.is(RESET_ITEM_TAG)) {
            PortalAngerManager.resetEyeAnger(level, portal, angerData);
            return true;
        }

        if (stack.is(CREATIVE_TRASH_TAG)) {
            PortalAngerManager.onCreativeTrashLittered(level, portal, angerData);
            return true;
        }

        if (stack.is(TRASH_TAG)) {
            PortalAngerManager.onTrashLittered(level, portal, angerData, stack.getCount());
            return true;
        }

        return false;
    }

    public static void handleNonRecipeItem(ServerLevel level, AlfheimPortalBlockEntity portal, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        if (stack.is(CREATIVE_RESET_ITEM_TAG) || stack.is(RESET_ITEM_TAG)
                || stack.is(CREATIVE_TRASH_TAG) || stack.is(TRASH_TAG)
                || stack.is(Items.BREAD)) {
            return;
        }

        if (!isEligibleForSouvenir(stack)) {
            ejectRejectedItem(level, portal.getBlockPos(), stack);
            return;
        }

        int count = stack.getCount();
        double dropChance = DontLitterConfig.SOUVENIR_DROP_CHANCE.get();
        boolean droppedAny = false;

        for (int i = 0; i < count; i++) {
            if (level.random.nextDouble() < dropChance) {
                spawnSouvenirLoot(level, portal.getBlockPos());
                droppedAny = true;
            }
        }

        if (droppedAny) {
            level.playSound(null, portal.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            PortalAngerManager.sendPlayerMessage(level, portal.getBlockPos(),
                    Component.translatable("message.dont_litter_in_alfheim.souvenir_received", stack.getHoverName())
                            .withStyle(ChatFormatting.GREEN));
        }
    }

    private static boolean isEligibleForSouvenir(ItemStack stack) {
        // Reject any item with NBT tags, durability (weapons/armor/tools), or max stack size <= 1
        if (stack.hasTag() || stack.isDamageableItem() || stack.getMaxStackSize() <= 1) {
            return false;
        }

        if (stack.is(SOUVENIR_TRADE_TAG)) {
            return true;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (String entry : DontLitterConfig.EXTRA_SOUVENIR_ITEMS.get()) {
            String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                ResourceLocation tagLoc = ResourceLocation.tryParse(trimmed.substring(1));
                if (tagLoc != null && stack.is(ItemTags.create(tagLoc))) {
                    return true;
                }
            } else if (itemId != null && itemId.toString().equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static void ejectRejectedItem(ServerLevel level, BlockPos portalPos, ItemStack stack) {
        Vec3 spawnPos = new Vec3(portalPos.getX() + 0.5, portalPos.getY() + 1.5, portalPos.getZ() + 0.5);
        ItemEntity ejected = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, stack.copy());
        XplatAbstractions.INSTANCE.itemFlagsComponent(ejected).elvenPortalSpawned = true;
        ejected.setDeltaMovement(
                (level.random.nextDouble() - 0.5) * 0.35,
                0.25,
                (level.random.nextDouble() - 0.5) * 0.35
        );
        ejected.setPickUpDelay(10);
        level.addFreshEntity(ejected);
        level.playSound(null, portalPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.7F);

        PortalAngerManager.sendPlayerMessage(level, portalPos,
                Component.translatable("message.dont_litter_in_alfheim.item_rejected", stack.getHoverName())
                        .withStyle(ChatFormatting.GRAY));
    }

    private static ResourceLocation getActiveSouvenirLootTable() {
        return ModList.get().isLoaded("mythicbotany") ? SOUVENIR_LOOT_MYTHIC : SOUVENIR_LOOT_DEFAULT;
    }

    private static void spawnSouvenirLoot(ServerLevel level, BlockPos portalPos) {
        LootTable table = level.getServer().getLootData().getLootTable(getActiveSouvenirLootTable());
        Vec3 spawnPos = new Vec3(portalPos.getX() + 0.5, portalPos.getY() + 1.5, portalPos.getZ() + 0.5);

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, spawnPos)
                .create(LootContextParamSets.CHEST);

        List<ItemStack> rewards = table.getRandomItems(params);
        for (ItemStack reward : rewards) {
            if (!reward.isEmpty()) {
                ItemEntity dropped = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, reward.copy());
                XplatAbstractions.INSTANCE.itemFlagsComponent(dropped).elvenPortalSpawned = true;
                dropped.setDeltaMovement(
                        (level.random.nextDouble() - 0.5) * 0.25,
                        0.2,
                        (level.random.nextDouble() - 0.5) * 0.25
                );
                level.addFreshEntity(dropped);
            }
        }
    }
}