package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class MultiblockProgressionRegistry {
    private static volatile Map<Identifier, MultiblockProgressionDefinition> progressions = Map.of();
    private static volatile Map<Identifier, MultiblockProgressionDefinition> byFacility = Map.of();

    private MultiblockProgressionRegistry() {
    }

    public static void replaceProgressions(Map<Identifier, MultiblockProgressionDefinition> loaded) {
        Map<Identifier, MultiblockProgressionDefinition> safe = Map.copyOf(loaded == null ? Map.of() : loaded);
        Map<Identifier, MultiblockProgressionDefinition> facilities = new LinkedHashMap<>();
        safe.values().stream()
                .sorted(Comparator.comparingInt(MultiblockProgressionDefinition::tier)
                        .thenComparing(definition -> definition.facilityId().toString()))
                .forEach(definition -> facilities.put(definition.facilityId(), definition));
        progressions = safe;
        byFacility = Map.copyOf(facilities);
        EchoMultiblockCore.LOGGER.info("ECHO MultiblockCore loaded {} progression definition(s).", progressions.size());
    }

    public static Optional<MultiblockProgressionDefinition> byId(Identifier id) {
        return Optional.ofNullable(id == null ? null : progressions.get(id));
    }

    public static Optional<MultiblockProgressionDefinition> byFacility(Identifier facilityId) {
        return Optional.ofNullable(facilityId == null ? null : byFacility.get(facilityId));
    }

    public static List<MultiblockProgressionDefinition> all() {
        return progressions.values().stream()
                .sorted(Comparator.comparingInt(MultiblockProgressionDefinition::tier)
                        .thenComparing(definition -> definition.facilityId().toString()))
                .toList();
    }

    public static Map<Identifier, MultiblockProgressionDefinition> snapshot() {
        return new LinkedHashMap<>(progressions);
    }
}
