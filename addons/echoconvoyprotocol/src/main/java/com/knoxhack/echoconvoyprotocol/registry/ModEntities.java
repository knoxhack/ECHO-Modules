package com.knoxhack.echoconvoyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEntityBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final Object ENTITIES = EchoBackendRegistryBridge.create(Registries.ENTITY_TYPE, EchoConvoyProtocol.MODID);

   public static final EchoBackendRegistryEntry<EntityType<ConvoyVehicleEntity>> SCRAP_BIKE =
      vehicle(ConvoyVehicleKind.SCRAP_BIKE, 0.95F, 1.35F);
   public static final EchoBackendRegistryEntry<EntityType<ConvoyVehicleEntity>> WASTELAND_ROVER =
      vehicle(ConvoyVehicleKind.WASTELAND_ROVER, 2.25F, 1.75F);
   public static final EchoBackendRegistryEntry<EntityType<ConvoyVehicleEntity>> CARGO_CRAWLER =
      vehicle(ConvoyVehicleKind.CARGO_CRAWLER, 3.05F, 1.85F);
   public static final EchoBackendRegistryEntry<EntityType<ConvoyVehicleEntity>> ARMORED_RELAY_TRUCK =
      vehicle(ConvoyVehicleKind.ARMORED_RELAY_TRUCK, 3.10F, 2.15F);

   private ModEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ENTITIES, eventBus);
   }

   public static EntityType<ConvoyVehicleEntity> typeFor(ConvoyVehicleKind kind) {
      if (kind == ConvoyVehicleKind.SCRAP_BIKE) {
         return SCRAP_BIKE.get();
      }
      if (kind == ConvoyVehicleKind.WASTELAND_ROVER) {
         return WASTELAND_ROVER.get();
      }
      if (kind == ConvoyVehicleKind.CARGO_CRAWLER) {
         return CARGO_CRAWLER.get();
      }
      return ARMORED_RELAY_TRUCK.get();
   }

   private static EchoBackendRegistryEntry<EntityType<ConvoyVehicleEntity>> vehicle(ConvoyVehicleKind kind, float width, float height) {
      return EchoBackendEntityBridge.registerEntityType(ENTITIES, kind.getSerializedName(),
         (type, level) -> ConvoyVehicleEntity.create(type, level, kind),
         MobCategory.MISC,
         builder -> builder.sized(width, height).clientTrackingRange(12));
   }
}
