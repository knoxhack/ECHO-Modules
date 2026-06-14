package com.knoxhack.echo.equipmentcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.equipmentcore.EchoEquipmentCore;
import com.knoxhack.echo.equipmentcore.data.InstalledUpgrades;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    private static final Object DATA_COMPONENT_TYPES =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoEquipmentCore.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<InstalledUpgrades>> INSTALLED_UPGRADES =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "installed_upgrades", () -> DataComponentType.<InstalledUpgrades>builder()
                    .persistent(InstalledUpgrades.CODEC)
                    .networkSynchronized(InstalledUpgrades.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
    }
}
