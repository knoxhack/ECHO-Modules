package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationProofKind;

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
        boolean hudEvents,
        String runtimeLane,
        boolean releaseProofCapable,
        Set<NativeMutationProofKind> proofKinds) {
    public EchoRuntimeHostCapabilities {
        runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
        nativeInterfaces = normalizeSet(nativeInterfaces);
        actionIds = normalizeSet(actionIds);
        canonicalContentIds = normalizeSet(canonicalContentIds);
        runtimeLane = AdapterContractGuards.optionalText(runtimeLane);
        if (runtimeLane.isBlank()) {
            runtimeLane = "runtime";
        }
        proofKinds = normalizeProofKinds(proofKinds);
        if (releaseProofCapable && proofKinds.isEmpty()) {
            proofKinds = defaultProofKinds(saveWrites, hudEvents);
        }
    }

    public EchoRuntimeHostCapabilities(
            String runtimeHostId,
            Set<String> nativeInterfaces,
            Set<String> actionIds,
            Set<String> canonicalContentIds,
            boolean missionUpdates,
            boolean saveWrites,
            boolean hudEvents) {
        this(
                runtimeHostId,
                nativeInterfaces,
                actionIds,
                canonicalContentIds,
                missionUpdates,
                saveWrites,
                hudEvents,
                "runtime",
                missionUpdates || saveWrites || hudEvents,
                defaultProofKinds(saveWrites, hudEvents));
    }

    public static EchoRuntimeHostCapabilities empty(String runtimeHostId) {
        return new EchoRuntimeHostCapabilities(runtimeHostId, Set.of(), Set.of(), Set.of(), false, false, false,
                "unsupported", false, Set.of());
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
                false,
                "runtime",
                false,
                Set.of());
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

    public boolean supportsProofKind(NativeMutationProofKind proofKind) {
        return proofKind != null && proofKinds.contains(proofKind);
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
        snapshot.put("runtimeLane", runtimeLane);
        snapshot.put("releaseProofCapable", releaseProofCapable);
        snapshot.put("proofKinds", proofKinds.stream()
                .map(NativeMutationProofKind::name)
                .collect(Collectors.toUnmodifiableSet()));
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

    private static Set<NativeMutationProofKind> normalizeProofKinds(Set<NativeMutationProofKind> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && value.releaseProof())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<NativeMutationProofKind> defaultProofKinds(boolean saveWrites, boolean hudEvents) {
        java.util.LinkedHashSet<NativeMutationProofKind> kinds = new java.util.LinkedHashSet<>();
        kinds.add(NativeMutationProofKind.HOST_STATE);
        if (saveWrites) {
            kinds.add(NativeMutationProofKind.SAVE_WRITE);
        }
        if (hudEvents) {
            kinds.add(NativeMutationProofKind.HUD_EVENT);
            kinds.add(NativeMutationProofKind.PACKET_EVENT);
        }
        return Set.copyOf(kinds);
    }
}
