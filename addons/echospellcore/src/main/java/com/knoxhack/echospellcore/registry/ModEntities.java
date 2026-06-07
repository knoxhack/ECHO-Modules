package com.knoxhack.echospellcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoSpellCore.MODID);

    public static final EchoBackendRegistryEntry<EntityType<SpellProjectileEntity>> SPELL_PROJECTILE =
            EchoBackendEntityBridge.registerEntityType(ENTITIES,
                    "spell_projectile",
                    SpellProjectileEntity::new,
                    MobCategory.MISC,
                    builder -> builder.sized(0.28F, 0.28F).clientTrackingRange(10));

    private ModEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }
}
