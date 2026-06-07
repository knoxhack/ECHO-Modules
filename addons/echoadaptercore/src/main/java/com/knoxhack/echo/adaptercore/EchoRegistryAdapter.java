package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoRegistryAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet registryCapabilities() {
        return EchoCapabilitySet.of(
                EchoCapabilityId.of("registry.blocks"),
                EchoCapabilityId.of("registry.items"),
                EchoCapabilityId.of("registry.entities"),
                EchoCapabilityId.of("registry.menus"),
                EchoCapabilityId.of("registry.sounds"),
                EchoCapabilityId.of("registry.recipes"),
                EchoCapabilityId.of("registry.loot"),
                EchoCapabilityId.of("registry.structures"),
                EchoCapabilityId.of("registry.tags"),
                EchoCapabilityId.of("registry.creative_groups")
        );
    }
}
