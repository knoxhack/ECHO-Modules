package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.entity.ArchiveSeekerEntity;
import com.knoxhack.echonexusprotocol.entity.CoreSoldierEntity;
import com.knoxhack.echonexusprotocol.entity.CorruptionWardenEntity;
import com.knoxhack.echonexusprotocol.entity.DataWraithEntity;
import com.knoxhack.echonexusprotocol.entity.NexusGuardianEntity;
import com.knoxhack.echonexusprotocol.entity.NexusHuskEntity;
import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.entity.StaticCrawlerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<EntityType<NexusHuskEntity>> NEXUS_HUSK = EchoBackendEntityBridge.registerEntityType(ENTITIES, "nexus_husk", NexusHuskEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<DataWraithEntity>> DATA_WRAITH = EchoBackendEntityBridge.registerEntityType(ENTITIES, "data_wraith", DataWraithEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.55F, 1.3F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<StaticCrawlerEntity>> STATIC_CRAWLER = EchoBackendEntityBridge.registerEntityType(ENTITIES, "static_crawler", StaticCrawlerEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.7F, 0.7F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<CoreSoldierEntity>> CORE_SOLDIER = EchoBackendEntityBridge.registerEntityType(ENTITIES, "core_soldier", CoreSoldierEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.7F, 2.05F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<ArchiveSeekerEntity>> ARCHIVE_SEEKER = EchoBackendEntityBridge.registerEntityType(ENTITIES, "archive_seeker", ArchiveSeekerEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.6F, 2.4F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<CorruptionWardenEntity>> CORRUPTION_WARDEN = EchoBackendEntityBridge.registerEntityType(ENTITIES, "corruption_warden", CorruptionWardenEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.9F, 2.6F).clientTrackingRange(10));
   public static final EchoBackendRegistryEntry<EntityType<NexusGuardianEntity>> NEXUS_GUARDIAN = EchoBackendEntityBridge.registerEntityType(ENTITIES, "nexus_guardian", NexusGuardianEntity::new, MobCategory.MONSTER, builder -> builder.sized(1.35F, 3.2F).clientTrackingRange(10));
   private ModEntities() {}
   public static void register(Object eventBus) { EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus); }
   public static void registerAttributes(Object event) { EchoBackendEntityBridge.putAttributes(event, NEXUS_HUSK, NexusMobEntity.createAttributes().build()); EchoBackendEntityBridge.putAttributes(event, DATA_WRAITH, NexusMobEntity.createAttributes().add(Attributes.MAX_HEALTH, 24.0).build()); EchoBackendEntityBridge.putAttributes(event, STATIC_CRAWLER, NexusMobEntity.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.42).build()); EchoBackendEntityBridge.putAttributes(event, CORE_SOLDIER, NexusMobEntity.createAttributes().add(Attributes.ARMOR, 8.0).build()); EchoBackendEntityBridge.putAttributes(event, ARCHIVE_SEEKER, NexusMobEntity.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).build()); EchoBackendEntityBridge.putAttributes(event, CORRUPTION_WARDEN, NexusMobEntity.createAttributes().add(Attributes.MAX_HEALTH, 110.0).add(Attributes.ATTACK_DAMAGE, 9.0).build()); EchoBackendEntityBridge.putAttributes(event, NEXUS_GUARDIAN, NexusMobEntity.createAttributes().add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ATTACK_DAMAGE, 13.0).add(Attributes.ARMOR, 12.0).build()); }
   public static void registerSpawnPlacements(Object event) { registerMonsterSpawn(event, NEXUS_HUSK); registerMonsterSpawn(event, DATA_WRAITH); registerMonsterSpawn(event, STATIC_CRAWLER); registerMonsterSpawn(event, CORE_SOLDIER); registerMonsterSpawn(event, ARCHIVE_SEEKER); registerBossSpawn(event, CORRUPTION_WARDEN); registerBossSpawn(event, NEXUS_GUARDIAN); }
   private static <T extends Monster> void registerMonsterSpawn(Object event, EchoBackendRegistryEntry<EntityType<T>> entity) { EchoBackendEntityBridge.registerSpawnPlacement(event, entity, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random)); }
   private static <T extends Monster> void registerBossSpawn(Object event, EchoBackendRegistryEntry<EntityType<T>> entity) { EchoBackendEntityBridge.registerSpawnPlacement(event, entity, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> reason != net.minecraft.world.entity.EntitySpawnReason.NATURAL && reason != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION && Monster.checkMonsterSpawnRules(type, level, reason, pos, random)); }
}
