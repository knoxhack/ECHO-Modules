package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class MultiblockUpgradeRegistry {
    private static volatile Map<Identifier, MultiblockUpgradeDefinition> upgrades = Map.of();

    private MultiblockUpgradeRegistry() {
    }

    public static void replaceUpgrades(Map<Identifier, MultiblockUpgradeDefinition> loaded) {
        upgrades = Map.copyOf(loaded == null ? Map.of() : loaded);
        EchoMultiblockCore.LOGGER.info("ECHO MultiblockCore loaded {} upgrade definition(s).", upgrades.size());
    }

    public static Optional<MultiblockUpgradeDefinition> byId(Identifier id) {
        return Optional.ofNullable(id == null ? null : upgrades.get(id));
    }

    public static List<MultiblockUpgradeDefinition> all() {
        return upgrades.values().stream()
                .sorted(Comparator.comparing(upgrade -> upgrade.id().toString()))
                .toList();
    }

    public static Map<Identifier, MultiblockUpgradeDefinition> snapshot() {
        return new LinkedHashMap<>(upgrades);
    }
}
