package net.sumomo.dontlitteralfheim.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.sumomo.dontlitteralfheim.entity.*;

import static net.sumomo.dontlitteralfheim.DontLitterInAlfheim.MODID;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<AlfheimEnforcerEntity>> ALFHEIM_ENFORCER =
            ENTITY_TYPES.register("alfheim_enforcer",
                    () -> EntityType.Builder.of(AlfheimEnforcerEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.4F)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .build("alfheim_enforcer"));

    public static final RegistryObject<EntityType<MagicCircleEntity>> MAGIC_CIRCLE =
            ENTITY_TYPES.register("magic_circle",
                    () -> EntityType.Builder.<MagicCircleEntity>of(MagicCircleEntity::new, MobCategory.MISC)
                            .sized(1.0F, 0.1F)
                            .noSummon()
                            .fireImmune()
                            .clientTrackingRange(128)
                            .updateInterval(2)
                            .build("magic_circle"));

    public static final RegistryObject<EntityType<ManaBeamEntity>> MANA_BEAM =
            ENTITY_TYPES.register("mana_beam",
                    () -> EntityType.Builder.<ManaBeamEntity>of(ManaBeamEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .noSummon()
                            .fireImmune()
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("mana_beam"));

    public static final RegistryObject<EntityType<AlfheimLightningEntity>> ALFHEIM_LIGHTNING =
            ENTITY_TYPES.register("alfheim_lightning",
                    () -> EntityType.Builder.<AlfheimLightningEntity>of(AlfheimLightningEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .noSave()
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("alfheim_lightning"));

    public static final RegistryObject<EntityType<EnforcerBladeEntity>> ENFORCER_BLADE =
            ENTITY_TYPES.register("enforcer_blade",
                    () -> EntityType.Builder.<EnforcerBladeEntity>of(EnforcerBladeEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .noSummon()
                            .noSave()
                            .fireImmune()
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("enforcer_blade"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}