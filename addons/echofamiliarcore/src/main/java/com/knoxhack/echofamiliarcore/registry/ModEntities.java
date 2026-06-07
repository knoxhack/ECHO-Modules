package com.knoxhack.echofamiliarcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.entity.AetherWispEntity;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import com.knoxhack.echofamiliarcore.entity.SpiritDroneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoFamiliarCore.MODID);

    public static final EchoBackendRegistryEntry<EntityType<AetherWispEntity>> AETHER_WISP =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "aether_wisp", AetherWispEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.42F, 0.42F).clientTrackingRange(10).fireImmune());

    public static final EchoBackendRegistryEntry<EntityType<SpiritDroneEntity>> SPIRIT_DRONE =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "spirit_drone", SpiritDroneEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.55F, 0.45F).clientTrackingRange(10).fireImmune());

    private ModEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, AETHER_WISP, ArcanaFamiliarEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SPIRIT_DRONE, ArcanaFamiliarEntity.createAttributes().build());
    }
}
