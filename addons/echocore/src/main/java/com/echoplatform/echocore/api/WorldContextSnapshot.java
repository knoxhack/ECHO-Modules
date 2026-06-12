package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record WorldContextSnapshot(
        WorldRegionInstance currentRegion,
        List<WorldRegionInstance> activeRegions,
        List<WorldMarker> markers,
        WorldHazardSnapshot hazard,
        Set<Identifier> discoveredRegions) {
    public WorldContextSnapshot {
        activeRegions = activeRegions == null ? List.of() : List.copyOf(activeRegions);
        markers = markers == null ? List.of() : List.copyOf(markers);
        hazard = hazard == null ? WorldHazardSnapshot.nominal() : hazard;
        discoveredRegions = discoveredRegions == null ? Set.of() : Set.copyOf(discoveredRegions);
    }

    public WorldContextSnapshot(
            Optional<WorldRegionInstance> currentRegionOptional,
            WorldHazardSnapshot hazard,
            List<IMapMarker> nearbyMarkers,
            List<WorldRegionInstance> activeRegions) {
        this(currentRegionOptional == null ? null : currentRegionOptional.orElse(null),
                activeRegions,
                List.of(),
                hazard,
                Set.of());
    }

    public static WorldContextSnapshot empty() {
        return new WorldContextSnapshot(null, List.of(), List.of(), WorldHazardSnapshot.nominal(), Set.of());
    }

    public Optional<WorldRegionInstance> currentRegionOptional() {
        return Optional.ofNullable(currentRegion);
    }

    public Set<Identifier> discoveredRegionIds() {
        return discoveredRegions;
    }

    public List<WorldMarker> nearbyMarkers() {
        return markers;
    }
}
