package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.entity.FurnaceDroneEntity;
import com.knoxhack.echoindustrialnexus.entity.FurnaceWardenEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Function;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(BuiltInRegistries.ENTITY_TYPE, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<EntityType<FurnaceWardenEntity>> FURNACE_WARDEN = registerEntityType(
      "furnace_warden", FurnaceWardenEntity::new, MobCategory.MONSTER, builder -> builder.sized(1.2F, 2.8F).clientTrackingRange(12)
   );
   public static final EchoBackendRegistryEntry<EntityType<FurnaceDroneEntity>> FURNACE_DRONE = registerEntityType(
      "furnace_drone", FurnaceDroneEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.75F, 1.65F).clientTrackingRange(8)
   );

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   public static void registerAttributes(Object event) {
      EchoBackendEntityBridge.putAttributes(event, FURNACE_WARDEN.get(), FurnaceWardenEntity.createAttributes().build());
      EchoBackendEntityBridge.putAttributes(event, FURNACE_DRONE.get(), FurnaceDroneEntity.createAttributes().build());
   }

   private static <T extends Entity> EchoBackendRegistryEntry<EntityType<T>> registerEntityType(
      String id,
      EntityType.EntityFactory<T> factory,
      MobCategory category,
      Function<EntityType.Builder<T>, EntityType.Builder<T>> builderCustomizer
   ) {
      return EchoBackendEntityBridge.registerEntityType(ENTITIES, id, factory, category, builderCustomizer);
   }
}
