package net.sumomo.dontlitteralfheim.event;

import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sumomo.dontlitteralfheim.client.model.AlfheimEnforcerModel;
import net.sumomo.dontlitteralfheim.client.renderer.AlfheimEnforcerRenderer;
import net.sumomo.dontlitteralfheim.client.renderer.MagicCircleRenderer;
import net.sumomo.dontlitteralfheim.client.renderer.ManaBeamRenderer;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.registry.ModEntities;
import vazkii.botania.client.render.entity.BabylonWeaponRenderer;

import static net.sumomo.dontlitteralfheim.DontLitterInAlfheim.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ALFHEIM_ENFORCER.get(), AlfheimEnforcerEntity.createAttributes().build());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(AlfheimEnforcerModel.LAYER_LOCATION, AlfheimEnforcerModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.ALFHEIM_ENFORCER.get(), AlfheimEnforcerRenderer::new);
            event.registerEntityRenderer(ModEntities.MAGIC_CIRCLE.get(), MagicCircleRenderer::new);
            event.registerEntityRenderer(ModEntities.MANA_BEAM.get(), ManaBeamRenderer::new);
            event.registerEntityRenderer(ModEntities.ALFHEIM_LIGHTNING.get(), LightningBoltRenderer::new);
            event.registerEntityRenderer(ModEntities.ENFORCER_BLADE.get(), BabylonWeaponRenderer::new);
        }
    }
}