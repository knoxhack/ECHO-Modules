package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.content.LoadoutCardSelection;
import com.knoxhack.echologisticsnetwork.content.RemoteRequestSelection;
import com.knoxhack.echologisticsnetwork.content.RouteManifestSelection;
import com.knoxhack.echologisticsnetwork.content.SupplyTagSelection;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
   public static final Object DATA_COMPONENT_TYPES =
      EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoLogisticsNetwork.MODID);

   public static final EchoBackendRegistryEntry<DataComponentType<SupplyTagSelection>> SUPPLY_TAG_SELECTION =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "supply_tag_selection", () -> DataComponentType.<SupplyTagSelection>builder()
         .persistent(SupplyTagSelection.CODEC)
         .networkSynchronized(SupplyTagSelection.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<LoadoutCardSelection>> LOADOUT_CARD_SELECTION =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "loadout_card_selection", () -> DataComponentType.<LoadoutCardSelection>builder()
         .persistent(LoadoutCardSelection.CODEC)
         .networkSynchronized(LoadoutCardSelection.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<RouteManifestSelection>> ROUTE_MANIFEST_SELECTION =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "route_manifest_selection", () -> DataComponentType.<RouteManifestSelection>builder()
         .persistent(RouteManifestSelection.CODEC)
         .networkSynchronized(RouteManifestSelection.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<RemoteRequestSelection>> REMOTE_REQUEST_SELECTION =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "remote_request_selection", () -> DataComponentType.<RemoteRequestSelection>builder()
         .persistent(RemoteRequestSelection.CODEC)
         .networkSynchronized(RemoteRequestSelection.STREAM_CODEC)
         .build());

   private ModDataComponents() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
   }
}
