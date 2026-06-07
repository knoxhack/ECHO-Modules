package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.entity.PollinatorDroneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoAgricultureReclamation.MODID);

   public static final EchoBackendRegistryEntry<EntityType<PollinatorDroneEntity>> POLLINATOR_DRONE =
      EchoBackendEntityBridge.registerEntityType(ENTITIES, "pollinator_drone", PollinatorDroneEntity::new, MobCategory.MISC,
         builder -> builder.sized(0.6F, 0.6F).clientTrackingRange(10));

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   public static void registerAttributes(Object event) {
      EchoBackendEntityBridge.putAttributes(event, POLLINATOR_DRONE.get(), PollinatorDroneEntity.createAttributes().build());
   }
}
