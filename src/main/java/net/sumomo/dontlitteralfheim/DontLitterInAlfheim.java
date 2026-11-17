package net.sumomo.dontlitteralfheim;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sumomo.dontlitteralfheim.config.DontLitterConfig;
import net.sumomo.dontlitteralfheim.registry.ModCreativeTabs;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import net.sumomo.dontlitteralfheim.registry.ModItems;

@Mod(DontLitterInAlfheim.MODID)
public class DontLitterInAlfheim {
    public static final String MODID = "dont_litter_in_alfheim";

    public DontLitterInAlfheim() {
        IEventBus bus= FMLJavaModLoadingContext.get().getModEventBus();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DontLitterConfig.SPEC);
        ModEntities.register(bus);
        ModItems.register(bus);
        ModCreativeTabs.register(bus);
    }

    private void clientSetup(final FMLClientSetupEvent event) {

    }
}
