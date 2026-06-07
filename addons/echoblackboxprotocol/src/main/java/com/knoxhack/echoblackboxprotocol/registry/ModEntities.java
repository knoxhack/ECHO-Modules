package com.knoxhack.echoblackboxprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.entity.BlackboxBossEntity;
import com.knoxhack.echoblackboxprotocol.entity.BlackboxMobEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, "echoblackboxprotocol");
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> ARCHIVE_HUSK = mob("archive_husk", 0.6F, 1.95F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> SECURITY_ECHO = mob("security_echo", 0.6F, 1.95F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> MEMORY_PARASITE = mob("memory_parasite", 0.55F, 0.9F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> FALSE_ECHO_MINION = mob("false_echo_minion", 0.6F, 1.95F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> COMMAND_REMNANT_MINION = mob("command_remnant_minion", 0.6F, 1.95F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> BLACKBOX_SENTINEL = mob("blackbox_sentinel", 0.9F, 2.4F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxBossEntity>> FALSE_ECHO = boss("false_echo", 0.8F, 2.2F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxBossEntity>> COMMAND_REMNANT = boss("command_remnant", 0.9F, 2.4F);
   public static final EchoBackendRegistryEntry<EntityType<BlackboxBossEntity>> NEXUS_GUARDIAN = boss("nexus_guardian", 1.1F, 2.8F);

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   public static void registerAttributes(Object event) {
      EchoBackendEntityBridge.putAttributes(event, (EntityType)ARCHIVE_HUSK.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)SECURITY_ECHO.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)MEMORY_PARASITE.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)FALSE_ECHO_MINION.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)COMMAND_REMNANT_MINION.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)BLACKBOX_SENTINEL.get(), BlackboxMobEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)FALSE_ECHO.get(), BlackboxBossEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)COMMAND_REMNANT.get(), BlackboxBossEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, (EntityType)NEXUS_GUARDIAN.get(), BlackboxBossEntity.createAttributes().build());
   }

   private static EchoBackendRegistryEntry<EntityType<BlackboxMobEntity>> mob(String name, float width, float height) {
      return EchoBackendEntityBridge.registerEntityType(ENTITIES, name, BlackboxMobEntity::new, MobCategory.MONSTER, builder -> builder.sized(width, height).clientTrackingRange(8));
   }

   private static EchoBackendRegistryEntry<EntityType<BlackboxBossEntity>> boss(String name, float width, float height) {
      return EchoBackendEntityBridge.registerEntityType(ENTITIES, name, BlackboxBossEntity::new, MobCategory.MONSTER, builder -> builder.sized(width, height).clientTrackingRange(12));
   }
}
