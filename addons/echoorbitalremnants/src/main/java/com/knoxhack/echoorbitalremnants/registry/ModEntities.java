package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.entity.AbandonedCaptainEntity;
import com.knoxhack.echoorbitalremnants.entity.CorruptedDockingAiEntity;
import com.knoxhack.echoorbitalremnants.entity.BrokenAstronautEntity;
import com.knoxhack.echoorbitalremnants.entity.EchoDefenseDroneEntity;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.entity.EchoZeroEntity;
import com.knoxhack.echoorbitalremnants.entity.EuropaCryoWardenEntity;
import com.knoxhack.echoorbitalremnants.entity.LunarNexusHuskEntity;
import com.knoxhack.echoorbitalremnants.entity.NexusHuskEntity;
import com.knoxhack.echoorbitalremnants.entity.OrbitalFactionNpcEntity;
import com.knoxhack.echoorbitalremnants.entity.SaturnRelaySentinelEntity;
import com.knoxhack.echoorbitalremnants.entity.TitanMethaneStalkerEntity;
import com.knoxhack.echoorbitalremnants.entity.VacuumWraithEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<EntityType<EmergencyRocketEntity>> EMERGENCY_ROCKET_VEHICLE =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "emergency_rocket_vehicle", EmergencyRocketEntity::new, MobCategory.MISC,
                    builder -> builder.sized(1.4F, 3.5F).clientTrackingRange(12));

    public static final EchoBackendRegistryEntry<EntityType<EchoDefenseDroneEntity>> ECHO_DEFENSE_DRONE =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "echo_defense_drone", EchoDefenseDroneEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 0.6F).clientTrackingRange(8));

    public static final EchoBackendRegistryEntry<EntityType<VacuumWraithEntity>> VACUUM_WRAITH =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "vacuum_wraith", VacuumWraithEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.5F, 0.8F).clientTrackingRange(8));

    public static final EchoBackendRegistryEntry<EntityType<BrokenAstronautEntity>> BROKEN_ASTRONAUT =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "broken_astronaut", BrokenAstronautEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8));

    public static final EchoBackendRegistryEntry<EntityType<NexusHuskEntity>> NEXUS_HUSK =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "nexus_husk", NexusHuskEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8));

    public static final EchoBackendRegistryEntry<EntityType<CorruptedDockingAiEntity>> CORRUPTED_DOCKING_AI =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "corrupted_docking_ai", CorruptedDockingAiEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 0.9F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<LunarNexusHuskEntity>> LUNAR_NEXUS_HUSK =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "lunar_nexus_husk", LunarNexusHuskEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 2.2F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<AbandonedCaptainEntity>> ABANDONED_CAPTAIN =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "abandoned_captain", AbandonedCaptainEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.75F, 2.1F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<EchoZeroEntity>> ECHO_ZERO =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "echo_zero", EchoZeroEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 2.4F).clientTrackingRange(12));

    public static final EchoBackendRegistryEntry<EntityType<EuropaCryoWardenEntity>> EUROPA_CRYO_WARDEN =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "europa_cryo_warden", EuropaCryoWardenEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 1.0F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<SaturnRelaySentinelEntity>> SATURN_RELAY_SENTINEL =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "saturn_relay_sentinel", SaturnRelaySentinelEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.85F, 1.15F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<TitanMethaneStalkerEntity>> TITAN_METHANE_STALKER =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "titan_methane_stalker", TitanMethaneStalkerEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 2.05F).clientTrackingRange(10));

    public static final EchoBackendRegistryEntry<EntityType<OrbitalFactionNpcEntity>> ORBITAL_FACTION_NPC =
            EchoBackendEntityBridge.registerEntityType(ENTITIES, "orbital_faction_npc", OrbitalFactionNpcEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    private ModEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
    }

    public static void registerAttributes(Object event) {
        EchoBackendEntityBridge.putAttributes(event, ECHO_DEFENSE_DRONE, EchoDefenseDroneEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, VACUUM_WRAITH, VacuumWraithEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, BROKEN_ASTRONAUT, BrokenAstronautEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, NEXUS_HUSK, NexusHuskEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, CORRUPTED_DOCKING_AI, CorruptedDockingAiEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, LUNAR_NEXUS_HUSK, LunarNexusHuskEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ABANDONED_CAPTAIN, AbandonedCaptainEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ECHO_ZERO, EchoZeroEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, EUROPA_CRYO_WARDEN, EuropaCryoWardenEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, SATURN_RELAY_SENTINEL, SaturnRelaySentinelEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, TITAN_METHANE_STALKER, TitanMethaneStalkerEntity.createAttributes().build());
        EchoBackendEntityBridge.putAttributes(event, ORBITAL_FACTION_NPC, OrbitalFactionNpcEntity.createAttributes().build());
    }
}
