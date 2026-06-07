package com.knoxhack.echo.adaptercore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AdapterCore backend bridge for entity registration lifecycle surfaces.
 */
public final class EchoBackendEntityBridge {
    private EchoBackendEntityBridge() {
    }

    public static <T extends Entity> EchoBackendRegistryEntry<EntityType<T>> registerEntityType(
            Object registry,
            String id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            Function<EntityType.Builder<T>, EntityType.Builder<T>> builderCustomizer) {
        return EchoBackendRegistryBridge.registerWithId(registry, id, key -> builderCustomizer
                .apply(EntityType.Builder.of(factory, category))
                .build(ResourceKey.create(Registries.ENTITY_TYPE, key)));
    }

    public static void putAttributes(Object event,
            Supplier<? extends EntityType<? extends LivingEntity>> entityType,
            AttributeSupplier attributes) {
        putAttributes(event, entityType.get(), attributes);
    }

    public static void putAttributes(Object event,
            EntityType<? extends LivingEntity> entityType,
            AttributeSupplier attributes) {
        if (event instanceof EntityAttributeCreationEvent attributeEvent) {
            attributeEvent.put(entityType, attributes);
        }
    }

    public static <T extends Mob> void registerSpawnPlacement(
            Object event,
            Supplier<? extends EntityType<T>> entityType,
            SpawnPlacementType placementType,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate) {
        if (event instanceof RegisterSpawnPlacementsEvent spawnEvent) {
            spawnEvent.register(entityType.get(), placementType, heightmap, predicate,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
