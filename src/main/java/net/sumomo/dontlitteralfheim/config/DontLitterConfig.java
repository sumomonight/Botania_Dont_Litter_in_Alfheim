package net.sumomo.dontlitteralfheim.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class DontLitterConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Littering & Anger
    public static final ForgeConfigSpec.DoubleValue ANGER_INCREASE_PER_TRASH;
    public static final ForgeConfigSpec.DoubleValue MAX_ANGER_CHANCE;
    public static final ForgeConfigSpec.IntValue CALAMITY_TRASH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue CREATIVE_TRASH_THRESHOLD;

    // Souvenir Gacha
    public static final ForgeConfigSpec.DoubleValue SOUVENIR_DROP_CHANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_SOUVENIR_ITEMS;

    // Entities & Boss
    public static final ForgeConfigSpec.IntValue ANGRY_ELF_MIN_COUNT;
    public static final ForgeConfigSpec.IntValue ANGRY_ELF_MAX_COUNT;
    public static final ForgeConfigSpec.DoubleValue CALAMITY_ELF_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue CALAMITY_ELF_DAMAGE_CAP_RATIO;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PORTAL_BLOCK_DESTRUCTION;

    static {
        BUILDER.push("Littering and Anger Settings");
        ANGER_INCREASE_PER_TRASH = BUILDER
                .comment("Probability increase per #dont_litter_in_alfheim:trash item thrown into the portal (0.001 = 0.1%).")
                .defineInRange("angerIncreasePerTrash", 0.001, 0.0, 1.0);
        MAX_ANGER_CHANCE = BUILDER
                .comment("Maximum cumulative anger chance (1.0 = 100%).")
                .defineInRange("maxAngerChance", 1.0, 0.0, 1.0);
        CALAMITY_TRASH_THRESHOLD = BUILDER
                .comment("Total trash count required to trigger the Calamity Elf boss encounter.")
                .defineInRange("calamityTrashThreshold", 10000, 1, 1000000);
        CREATIVE_TRASH_THRESHOLD = BUILDER
                .comment("Total #dont_litter_in_alfheim:trash/creative items required to trigger the Ultimate Wrath boss encounter (no random chance).")
                .defineInRange("creativeTrashThreshold", 10000, 1, 1000000);
        BUILDER.pop();

        BUILDER.push("Souvenir Gacha Settings");
        SOUVENIR_DROP_CHANCE = BUILDER
                .comment("Chance to roll the Alfheim Souvenir loot table when an eligible item is thrown in (0.25 = 25%).")
                .defineInRange("souvenirDropChance", 0.25, 0.0, 1.0);
        EXTRA_SOUVENIR_ITEMS = BUILDER
                .comment("Additional item IDs (e.g., 'minecraft:amethyst_shard') or item tags prefixed with '#' (e.g., '#forge:nuggets') allowed alongside #dont_litter_in_alfheim:souvenir_trade.")
                .defineListAllowEmpty(
                        List.of("extraSouvenirTradeItems"),
                        List::of,
                        obj -> obj instanceof String s && !s.isBlank()
                );
        BUILDER.pop();

        BUILDER.push("Elf Raid and Boss Settings");
        ANGRY_ELF_MIN_COUNT = BUILDER
                .comment("Minimum number of Angry Elves spawned when a raid triggers.")
                .defineInRange("angryElfMinCount", 2, 1, 20);
        ANGRY_ELF_MAX_COUNT = BUILDER
                .comment("Maximum number of Angry Elves spawned when a raid triggers.")
                .defineInRange("angryElfMaxCount", 4, 1, 20);
        CALAMITY_ELF_MAX_HEALTH = BUILDER
                .comment("Base maximum health of the Calamity Elf boss.")
                .defineInRange("calamityElfMaxHealth", 600.0, 20.0, 100000.0);
        CALAMITY_ELF_DAMAGE_CAP_RATIO = BUILDER
                .comment("Max damage taken per hit as a ratio of max health (0.05 = 5%, 1.0 = disabled).")
                .defineInRange("calamityElfDamageCapRatio", 0.05, 0.001, 1.0);
        ENABLE_PORTAL_BLOCK_DESTRUCTION = BUILDER
                .comment("Whether the Calamity Elf can break blocks around the portal.")
                .define("enablePortalBlockDestruction", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}