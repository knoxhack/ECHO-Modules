package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, EchoLogisticsNetwork.MODID);

   public static final EchoBackendRegistryEntry<EntityType<CourierDroneEntity>> COURIER_DRONE =
      EchoBackendEntityBridge.registerEntityType(ENTITIES, "courier_drone", CourierDroneEntity::new, MobCategory.MISC,
         builder -> builder.sized(0.6F, 0.6F).clientTrackingRange(10));

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   public static void registerAttributes(Object event) {
      EchoBackendEntityBridge.putAttributes(event, COURIER_DRONE.get(), CourierDroneEntity.createAttributes().build());
   }
}
