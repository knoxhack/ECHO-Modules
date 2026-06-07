package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record EchoRuntimeHostCapabilities(
        String runtimeHostId,
        Set<String> nativeInterfaces,
        Set<String> actionIds,
        Set<String> canonicalContentIds,
        boolean missionUpdates,
        boolean saveWrites,
        boolean hudEvents) {
    public EchoRuntimeHostCapabilities {
        runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
        nativeInterfaces = normalizeSet(nativeInterfaces);
        actionIds = normalizeSet(actionIds);
        canonicalContentIds = normalizeSet(canonicalContentIds);
    }

    public static EchoRuntimeHostCapabilities empty(String runtimeHostId) {
        return new EchoRuntimeHostCapabilities(runtimeHostId, Set.of(), Set.of(), Set.of(), false, false, false);
    }

    public static EchoRuntimeHostCapabilities of(
            String runtimeHostId,
            Set<String> nativeInterfaces,
            Set<String> actionIds,
            Set<String> canonicalContentIds) {
        return new EchoRuntimeHostCapabilities(
                runtimeHostId,
                nativeInterfaces,
                actionIds,
                canonicalContentIds,
                false,
                false,
                false);
    }

    public boolean supportsNativeInterface(String nativeInterface) {
        return nativeInterfaces.contains(normalize(nativeInterface));
    }

    public boolean supportsAction(String actionId) {
        return actionIds.contains(normalize(actionId));
    }

    public boolean supportsCanonicalContent(String canonicalContentId) {
        return canonicalContentIds.contains(normalize(canonicalContentId));
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("runtimeHostId", runtimeHostId);
        snapshot.put("nativeInterfaces", nativeInterfaces);
        snapshot.put("actionIds", actionIds);
        snapshot.put("canonicalContentIds", canonicalContentIds);
        snapshot.put("missionUpdates", missionUpdates);
        snapshot.put("saveWrites", saveWrites);
        snapshot.put("hudEvents", hudEvents);
        return Map.copyOf(snapshot);
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(EchoRuntimeHostCapabilities::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
