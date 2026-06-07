package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.data.ArmoryLoadout;
import com.knoxhack.echoarmory.data.ArmoryStance;
import com.knoxhack.echoarmory.data.CosmeticTrim;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.EquipmentTier;
import com.knoxhack.echoarmory.data.InstalledModules;
import com.knoxhack.echoarmory.data.InstabilityState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
   private static final Object DATA_COMPONENT_TYPES =
      EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoArmory.MODID);

   public static final EchoBackendRegistryEntry<DataComponentType<ArmoryLoadout>> ARMORY_LOADOUT =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "armory_loadout", () -> DataComponentType.<ArmoryLoadout>builder()
         .persistent(ArmoryLoadout.CODEC)
         .networkSynchronized(ArmoryLoadout.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<InstalledModules>> INSTALLED_MODULES =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "installed_modules", () -> DataComponentType.<InstalledModules>builder()
         .persistent(InstalledModules.CODEC)
         .networkSynchronized(InstalledModules.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<EnergyState>> ENERGY_STATE =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "energy_state", () -> DataComponentType.<EnergyState>builder()
         .persistent(EnergyState.CODEC)
         .networkSynchronized(EnergyState.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<EquipmentTier>> EQUIPMENT_TIER =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "equipment_tier", () -> DataComponentType.<EquipmentTier>builder()
         .persistent(EquipmentTier.CODEC)
         .networkSynchronized(EquipmentTier.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<ArmoryStance>> STANCE =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "stance", () -> DataComponentType.<ArmoryStance>builder()
         .persistent(ArmoryStance.CODEC)
         .networkSynchronized(ArmoryStance.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<CosmeticTrim>> COSMETIC_TRIM =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "cosmetic_trim", () -> DataComponentType.<CosmeticTrim>builder()
         .persistent(CosmeticTrim.CODEC)
         .networkSynchronized(CosmeticTrim.STREAM_CODEC)
         .build());

   public static final EchoBackendRegistryEntry<DataComponentType<InstabilityState>> INSTABILITY_STATE =
      EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "instability_state", () -> DataComponentType.<InstabilityState>builder()
         .persistent(InstabilityState.CODEC)
         .networkSynchronized(InstabilityState.STREAM_CODEC)
         .build());

   private ModDataComponents() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
   }
}
