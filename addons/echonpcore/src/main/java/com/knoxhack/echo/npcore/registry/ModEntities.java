package com.knoxhack.echo.npcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoNpcCore.MODID);

    public static final EchoBackendRegistryEntry<EntityType<EchoNpcEntity>> ECHO_NPC =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "echo_npc", EchoNpcEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64));

    private ModEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, ECHO_NPC, EchoNpcEntity.createAttributes().build());
    }
}
