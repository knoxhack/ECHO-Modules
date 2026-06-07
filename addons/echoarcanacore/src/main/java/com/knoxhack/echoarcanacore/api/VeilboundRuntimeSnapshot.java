package com.knoxhack.echoarcanacore.api;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record VeilboundRuntimeSnapshot(
        boolean arcanaLoaded,
        boolean playerDataAvailable,
        int scanCount,
        Set<String> scannedTargets,
        Set<String> knownResonances,
        Set<String> unlockedResearch,
        Set<String> theories,
        Set<String> usedMachines,
        Set<String> events,
        Map<String, Integer> observations,
        Map<String, ScanCoordinate> scanCoordinates,
        String activeResearch,
        String endgamePath,
        int lastVeilPressure,
        int lastFracturePressure,
        String lastFieldState,
        boolean worldDataAvailable,
        int localVeilPressure,
        int localFracturePressure,
        String localFieldState,
        boolean veilboundGuardianDefeated,
        boolean unwrittenOneDefeated,
        boolean fractureHeartDefeated,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z) {
    public VeilboundRuntimeSnapshot {
        scanCount = Math.max(0, scanCount);
        scannedTargets = cleanSet(scannedTargets);
        knownResonances = cleanSet(knownResonances);
        unlockedResearch = cleanSet(unlockedResearch);
        theories = cleanSet(theories);
        usedMachines = cleanSet(usedMachines);
        events = cleanSet(events);
        observations = cleanMap(observations);
        scanCoordinates = cleanCoordinateMap(scanCoordinates);
        activeResearch = clean(activeResearch);
        endgamePath = clean(endgamePath);
        lastFieldState = clean(lastFieldState);
        localFieldState = clean(localFieldState);
        dimension = dimension == null ? Level.OVERWORLD : dimension;
    }

    public static VeilboundRuntimeSnapshot unavailable(boolean arcanaLoaded) {
        return new VeilboundRuntimeSnapshot(
                arcanaLoaded,
                false,
                0,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                "",
                "",
                -1,
                -1,
                "",
                false,
                -1,
                -1,
                "",
                false,
                false,
                false,
                Level.OVERWORLD,
                0.0D,
                0.0D,
                0.0D);
    }

    public boolean available() {
        return arcanaLoaded && playerDataAvailable;
    }

    public boolean hasAnyFieldReading() {
        return localVeilPressure >= 0 || localFracturePressure >= 0 || lastVeilPressure >= 0 || lastFracturePressure >= 0;
    }

    public int effectiveVeilPressure() {
        return localVeilPressure >= 0 ? localVeilPressure : lastVeilPressure;
    }

    public int effectiveFracturePressure() {
        return localFracturePressure >= 0 ? localFracturePressure : lastFracturePressure;
    }

    public String effectiveFieldState() {
        return !localFieldState.isBlank() ? localFieldState : lastFieldState;
    }

    public boolean hasScan(String scanId) {
        return scannedTargets.contains(clean(scanId));
    }

    public boolean hasResearch(String researchId) {
        return unlockedResearch.contains(clean(researchId));
    }

    public boolean hasEvent(String eventId) {
        return events.contains(clean(eventId));
    }

    public boolean hasObservation(String observationId) {
        return observations.containsKey(clean(observationId));
    }

    public int observationCount(String observationId) {
        return observations.getOrDefault(clean(observationId), 0);
    }

    public Optional<ScanCoordinate> scanCoordinate(String id) {
        return Optional.ofNullable(scanCoordinates.get(clean(id)));
    }

    public boolean hasAnyObservationPrefix(String prefix) {
        String cleanPrefix = clean(prefix);
        return observations.keySet().stream().anyMatch(key -> key.startsWith(cleanPrefix));
    }

    public boolean hasAnyEntityObservation() {
        return hasAnyObservationPrefix("entity/");
    }

    public boolean hasAnyStructureObservation() {
        return hasAnyObservationPrefix("structure/");
    }

    public boolean hasAnyProgress() {
        return scanCount > 0
                || !scannedTargets.isEmpty()
                || !observations.isEmpty()
                || !unlockedResearch.isEmpty()
                || !events.isEmpty();
    }

    public String pressureSummary() {
        if (!hasAnyFieldReading()) {
            return "No saved pressure reading";
        }
        String state = effectiveFieldState().isBlank() ? "unknown" : effectiveFieldState();
        return "Veil " + effectiveVeilPressure() + " / Fracture " + effectiveFracturePressure() + " / " + state;
    }

    private static Set<String> cleanSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(values.stream().map(VeilboundRuntimeSnapshot::clean).filter(value -> !value.isBlank()).toList());
    }

    private static Map<String, Integer> cleanMap(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            String key = clean(entry.getKey());
            if (!key.isBlank()) {
                clean.put(key, Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
            }
        }
        return Map.copyOf(clean);
    }

    private static Map<String, ScanCoordinate> cleanCoordinateMap(Map<String, ScanCoordinate> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, ScanCoordinate> clean = new LinkedHashMap<>();
        for (Map.Entry<String, ScanCoordinate> entry : values.entrySet()) {
            String key = clean(entry.getKey());
            if (!key.isBlank() && entry.getValue() != null) {
                clean.put(key, entry.getValue());
            }
        }
        return Map.copyOf(clean);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ScanCoordinate(ResourceKey<Level> dimension, double x, double y, double z, long gameTime) {
        public ScanCoordinate {
            dimension = dimension == null ? Level.OVERWORLD : dimension;
        }
    }
}
