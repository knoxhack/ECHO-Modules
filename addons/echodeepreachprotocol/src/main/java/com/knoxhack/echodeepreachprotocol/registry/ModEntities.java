package com.knoxhack.echodeepreachprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import com.knoxhack.echodeepreachprotocol.entity.AbyssalLeviathanEntity;
import com.knoxhack.echodeepreachprotocol.entity.BloaterEntity;
import com.knoxhack.echodeepreachprotocol.entity.HadalWraithEntity;
import com.knoxhack.echodeepreachprotocol.entity.LatticeBoltEntity;
import com.knoxhack.echodeepreachprotocol.entity.LatticeSentinelEntity;
import com.knoxhack.echodeepreachprotocol.entity.RemoraSubmersibleEntity;
import com.knoxhack.echodeepreachprotocol.entity.TwilightStalkerEntity;
import com.knoxhack.echodeepreachprotocol.entity.VentCrabEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Entity registry for ECHO: Deep Reach Protocol creatures.
 */
public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(BuiltInRegistries.ENTITY_TYPE, EchoDeepReachProtocol.MODID);

    public static final EchoBackendRegistryEntry<EntityType<TwilightStalkerEntity>> TWILIGHT_STALKER =
            register("twilight_stalker", TwilightStalkerEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 2.1F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<VentCrabEntity>> VENT_CRAB =
            register("vent_crab", VentCrabEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.5F, 0.45F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<AbyssalLeviathanEntity>> ABYSSAL_LEVIATHAN =
            register("abyssal_leviathan", AbyssalLeviathanEntity::new, MobCategory.WATER_CREATURE,
                    builder -> builder.sized(2.4F, 1.2F).clientTrackingRange(12));

    public static final EchoBackendRegistryEntry<EntityType<LatticeSentinelEntity>> LATTICE_SENTINEL =
            register("lattice_sentinel", LatticeSentinelEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<BloaterEntity>> BLOATER =
            register("bloater", BloaterEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(8));

    public static final EchoBackendRegistryEntry<EntityType<HadalWraithEntity>> HADAL_WRAITH =
            register("hadal_wraith", HadalWraithEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.55F, 1.9F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<RemoraSubmersibleEntity>> REMORA_SUBMERSIBLE =
            register("remora_submersible", RemoraSubmersibleEntity::new, MobCategory.MISC,
                    builder -> builder.sized(1.5F, 0.6F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<LatticeBoltEntity>> LATTICE_BOLT =
            register("lattice_bolt", LatticeBoltEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(10));

    private ModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> EchoBackendRegistryEntry<EntityType<T>> register(
            String id, EntityType.EntityFactory<T> factory, MobCategory category,
            java.util.function.Function<EntityType.Builder<T>, EntityType.Builder<T>> builderCustomizer) {
        return EchoBackendEntityBridge.registerEntityType(ENTITIES, id, factory, category, builderCustomizer);
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, TWILIGHT_STALKER,
                TwilightStalkerEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, VENT_CRAB,
                VentCrabEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ABYSSAL_LEVIATHAN,
                AbyssalLeviathanEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, LATTICE_SENTINEL,
                LatticeSentinelEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, BLOATER,
                BloaterEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, HADAL_WRAITH,
                HadalWraithEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(Object event) {
        registerMonsterSpawn(event, TWILIGHT_STALKER);
        registerCreatureSpawn(event, VENT_CRAB);
        registerWaterSpawn(event, ABYSSAL_LEVIATHAN);
        registerMonsterSpawn(event, LATTICE_SENTINEL);
        registerCreatureSpawn(event, BLOATER);
        registerMonsterSpawn(event, HADAL_WRAITH);
        registerWaterSpawnForEntity(event, REMORA_SUBMERSIBLE);
    }

    private static void registerWaterSpawnForEntity(Object event, EchoBackendRegistryEntry<EntityType<RemoraSubmersibleEntity>> entity) {
        if (event instanceof net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent spawnEvent) {
            spawnEvent.register(entity.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> level.getFluidState(pos).is(FluidTags.WATER),
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private static <T extends Monster> void registerMonsterSpawn(Object event, EchoBackendRegistryEntry<EntityType<T>> entity) {
        EchoBackendEntityBridge.registerSpawnPlacement(event, entity, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
    }

    private static <T extends Animal> void registerCreatureSpawn(Object event, EchoBackendRegistryEntry<EntityType<T>> entity) {
        EchoBackendEntityBridge.registerSpawnPlacement(event, entity, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
    }

    private static <T extends Mob> void registerWaterSpawn(Object event, EchoBackendRegistryEntry<EntityType<T>> entity) {
        EchoBackendEntityBridge.registerSpawnPlacement(event, entity, SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(FluidTags.WATER));
    }
}
