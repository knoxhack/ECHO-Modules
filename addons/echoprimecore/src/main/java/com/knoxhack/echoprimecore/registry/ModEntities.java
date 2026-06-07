package com.knoxhack.echoprimecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.entity.PrimeMobEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoPrimeCore.MODID);

    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> SCRAP_CRAWLER =
            monster("scrap_crawler", PrimeMobEntity::new, 0.75F, 0.65F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> BROKEN_SURVEY_DRONE =
            monster("broken_survey_drone", PrimeMobEntity::new, 0.8F, 1.2F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> SIGNAL_WISP =
            monster("signal_wisp", PrimeMobEntity::new, 0.55F, 1.7F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> FERAL_REPAIR_BOT =
            monster("feral_repair_bot", PrimeMobEntity::new, 0.8F, 1.8F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> RELAY_SENTRY =
            monster("relay_sentry", PrimeMobEntity::new, 0.8F, 1.9F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> STATIC_PHANTOM =
            monster("static_phantom", PrimeMobEntity::new, 0.9F, 0.5F);
    public static final EchoBackendRegistryEntry<EntityType<PrimeMobEntity>> PRIME_GUARDIAN =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "prime_guardian", PrimeMobEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.4F, 2.7F).clientTrackingRange(12));

    private ModEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, SCRAP_CRAWLER, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, BROKEN_SURVEY_DRONE, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SIGNAL_WISP, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, FERAL_REPAIR_BOT, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, RELAY_SENTRY, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, STATIC_PHANTOM, PrimeMobEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, PRIME_GUARDIAN, PrimeMobEntity.createGuardianAttributes().build());
    }

    private static <T extends Mob> EchoBackendRegistryEntry<EntityType<T>> monster(
            String name,
            EntityType.EntityFactory<T> factory,
            float width,
            float height) {
        return EchoBackendEntityBridge.registerEntityType(ENTITIES, name, factory, MobCategory.MONSTER,
                builder -> builder.sized(width, height).clientTrackingRange(8));
    }
}
