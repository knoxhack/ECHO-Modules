package com.knoxhack.echoworldcore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoDiscoveryProvider;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.WorldRegionDefinition;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class WorldCoreDiscoveryProvider implements EchoDiscoveryProvider {
    private final WorldRegionService service;

    public WorldCoreDiscoveryProvider(WorldRegionService service) {
        this.service = service;
    }

    @Override
    public List<EchoDiscoveryEntry> entries(Player player) {
        return service.regionDefinitions().stream()
                .map(WorldCoreDiscoveryProvider::entry)
                .toList();
    }

    @Override
    public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
        if (player != null && entry != null && EchoCoreServices.hasDiscoveredFeature(player, entry.id())) {
            return EchoDiscoveryState.DISCOVERED;
        }
        return EchoDiscoveryState.LOCKED;
    }

    private static EchoDiscoveryEntry entry(WorldRegionDefinition definition) {
        return new EchoDiscoveryEntry(
                definition.discoveryId(),
                Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, "world_core"),
                EchoDiscoveryCategory.REGION,
                definition.displayName(),
                "Unknown Region",
                "Enter or scan this region to map it.",
                definition.summary(),
                null,
                null,
                accent(definition.type()),
                null,
                definition.sortOrder());
    }

    private static int accent(com.echoplatform.echocore.api.WorldRegionType type) {
        return switch (type) {
            case CRASH_ZONE -> 0xFFFFB35C;
            case RUINED_CITY -> 0xFF9DA7B3;
            case TOXIC_SWAMP -> 0xFF70D67A;
            case RADIATION_ZONE -> 0xFFECE35A;
            case CRYOGENIC_RUINS -> 0xFF8FD8FF;
            case NEXUS_SCAR, ANOMALY_ZONE -> 0xFFC09BFF;
            case ORBITAL_DEBRIS_FIELD -> 0xFF66E8FF;
            case CONVOY_ROUTE -> 0xFFFFD166;
            case SECURE_OUTPOST -> 0xFF92F7A6;
            case CUSTOM -> 0xFF9DA7B3;
        };
    }
}
