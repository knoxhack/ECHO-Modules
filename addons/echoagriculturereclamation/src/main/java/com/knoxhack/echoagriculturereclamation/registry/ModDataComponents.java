package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
   public static final Object DATA_COMPONENT_TYPES =
      EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoAgricultureReclamation.MODID);

   public static final EchoBackendRegistryEntry<DataComponentType<SeedProfile>> SEED_PROFILE =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES,
         "seed_profile",
         () -> DataComponentType.<SeedProfile>builder()
            .persistent(SeedProfile.CODEC)
            .networkSynchronized(SeedProfile.STREAM_CODEC)
            .build()
      );

   private ModDataComponents() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
   }
}
