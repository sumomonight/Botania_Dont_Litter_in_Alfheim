package net.sumomo.dontlitteralfheim.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "dont_litter_in_alfheim");

    public static final RegistryObject<CreativeModeTab> ALFHEIM_TAB = CREATIVE_MODE_TABS.register(
            "dont_litter_in_alfheim_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.ALFHEIM_SOUVENIR.get()))
                    .title(Component.translatable("creativetab.dont_litter_in_alfheim"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ALFHEIM_SOUVENIR.get());
                        output.accept(ModItems.PRESENT.get());
                        output.accept(ModItems.TRASH.get());
                        output.accept(ModItems.CREATIVE_TRASH.get());
                        output.accept(ModItems.APOLOGY_LETTER.get());
                        output.accept(ModItems.CREATIVE_APOLOGY_LETTER.get());
                        output.accept(ModItems.ALFHEIM_ENFORCER_SPAWN_EGG.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}