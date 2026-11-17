package net.sumomo.dontlitteralfheim.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sumomo.dontlitteralfheim.registry.ModEntities;

public class ModItems {

    public static class ShinyItem extends Item {
        public ShinyItem(Item.Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "dont_litter_in_alfheim");

    // Normal Items (素材・ドロップ品用)
    public static final RegistryObject<Item> ALFHEIM_SOUVENIR = ITEMS.register(
            "alfheim_souvenir",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> PRESENT = ITEMS.register(
            "present",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
    );

    public static final RegistryObject<Item> TRASH = ITEMS.register(
            "trash",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> CREATIVE_TRASH = ITEMS.register(
            "creative_trash",
            () -> new ShinyItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> APOLOGY_LETTER = ITEMS.register(
            "apology_letter",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> CREATIVE_APOLOGY_LETTER = ITEMS.register(
            "creative_apology_letter",
            () -> new ShinyItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    // Spawn Egg
    public static final RegistryObject<Item> ALFHEIM_ENFORCER_SPAWN_EGG = ITEMS.register(
            "alfheim_enforcer_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ALFHEIM_ENFORCER, 0xC5FCB2, 0xF5FFA8, new Item.Properties())
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}