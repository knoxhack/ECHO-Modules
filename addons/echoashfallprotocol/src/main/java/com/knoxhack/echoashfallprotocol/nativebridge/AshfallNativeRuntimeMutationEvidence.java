package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores facts from completed live host mutations for native activation reports.
 */
public final class AshfallNativeRuntimeMutationEvidence {
    private static final AtomicReference<Map<String, Object>> LAST_EVIDENCE =
            new AtomicReference<>(emptyEvidence());

    private AshfallNativeRuntimeMutationEvidence() {
    }

    public static void record(
            String sourceAction,
            String runtimeHostId,
            String branch,
            String status,
            boolean realNativeStateMutated,
            int mutationCount,
            List<Map<String, Object>> nativeResults,
            Map<String, Object> snapshot) {
        List<Map<String, Object>> combinedNativeResults = combinedNativeResults(nativeResults);
        int dispatchedActionCount = dispatchedActionCount(combinedNativeResults);
        int mutatingDispatchedActionCount = mutatingDispatchedActionCount(combinedNativeResults, runtimeHostId);
        boolean realHostMutation = mutatingDispatchedActionCount > 0;
        String statusText = status == null ? "" : status;
        List<String> diagnostics = diagnostics(
                realNativeStateMutated,
                mutationCount,
                dispatchedActionCount,
                mutatingDispatchedActionCount);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("id", "echoashfallprotocol:live_runtime_mutation_evidence");
        evidence.put("moduleId", "echoashfallprotocol");
        evidence.put("sourceAction", sourceAction == null ? "" : sourceAction);
        evidence.put("runtimeHostId", runtimeHostId == null ? "" : runtimeHostId);
        evidence.put("branch", branch == null ? "" : branch);
        evidence.put("sourceStatus", statusText);
        evidence.put("status", realHostMutation ? statusText : "NO_RUNTIME_MUTATION_RECORDED");
        evidence.put("adapterCoreActionDispatched", dispatchedActionCount > 0);
        evidence.put("dispatchedActionCount", dispatchedActionCount);
        evidence.put("nativeResultCount", combinedNativeResults.size());
        evidence.put("callerReportedMutation", realNativeStateMutated);
        evidence.put("callerReportedMutationCount", Math.max(0, mutationCount));
        evidence.put("mutationCount", mutatingDispatchedActionCount);
        evidence.put("mutatingDispatchedActionCount", mutatingDispatchedActionCount);
        evidence.put("minecraftRuntimeAccessed", realHostMutation);
        evidence.put("minecraftRuntimeMutated", realHostMutation);
        evidence.put("minecraftRegistryMutated", false);
        evidence.put("realNativeStateMutated", realHostMutation);
        evidence.put("saveUpdated", saveTouchedResult(combinedNativeResults));
        evidence.put("visibleFeedbackEmitted", hudOrPacketResult(combinedNativeResults)
                || hudOrEventResult(combinedNativeResults));
        evidence.put("activationReportFactsAfterMutation", realHostMutation);
        evidence.put("diagnostics", diagnostics);
        evidence.put("nativeResults", combinedNativeResults);
        evidence.put("snapshot", snapshot == null ? Map.of() : Map.copyOf(snapshot));
        LAST_EVIDENCE.set(Map.copyOf(evidence));
    }

    public static Map<String, Object> snapshot() {
        return LAST_EVIDENCE.get();
    }

    private static int dispatchedActionCount(List<Map<String, Object>> nativeResults) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> nativeResult : nativeResults) {
            Object snapshot = nativeResult.get("snapshot");
            if (snapshot instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("adapterCoreActionDispatched"))) {
                count++;
            }
        }
        return count;
    }

    private static int mutatingDispatchedActionCount(List<Map<String, Object>> nativeResults, String runtimeHostId) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> nativeResult : nativeResults) {
            Object snapshot = nativeResult.get("snapshot");
            if (!(snapshot instanceof Map<?, ?> map)
                    || !Boolean.TRUE.equals(nativeResult.get("mutated"))
                    || !Boolean.TRUE.equals(map.get("adapterCoreActionDispatched"))
                    || !Boolean.TRUE.equals(map.get("runtimeHostResolved"))) {
                continue;
            }
            Object resultRuntimeHostId = map.get("runtimeHostId");
            if (runtimeHostId == null || runtimeHostId.isBlank()) {
                if (resultRuntimeHostId instanceof String text && !text.isBlank()) {
                    count++;
                }
            } else if (runtimeHostId.equals(resultRuntimeHostId)) {
                count++;
            }
        }
        return count;
    }

    private static boolean saveTouchedResult(List<Map<String, Object>> nativeResults) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return false;
        }
        for (Map<String, Object> nativeResult : nativeResults) {
            Object snapshot = nativeResult.get("snapshot");
            if (snapshot instanceof Map<?, ?> map
                    && Boolean.TRUE.equals(nativeResult.get("mutated"))
                    && (Boolean.TRUE.equals(map.get("saveTouched"))
                    || Boolean.TRUE.equals(map.get("hostSaveTouched")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hudOrPacketResult(List<Map<String, Object>> nativeResults) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return false;
        }
        for (Map<String, Object> nativeResult : nativeResults) {
            Object snapshot = nativeResult.get("snapshot");
            if (!(snapshot instanceof Map<?, ?> map) || !Boolean.TRUE.equals(nativeResult.get("mutated"))) {
                continue;
            }
            Object nativeInterface = map.get("nativeInterface");
            if ("EchoNativeRuntimeHost.Hud".equals(nativeInterface)
                    || "EchoNativeRuntimeHost.Packets".equals(nativeInterface)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hudOrEventResult(List<Map<String, Object>> nativeResults) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return false;
        }
        for (Map<String, Object> nativeResult : nativeResults) {
            Object snapshot = nativeResult.get("snapshot");
            if (snapshot instanceof Map<?, ?> map
                    && Boolean.TRUE.equals(nativeResult.get("mutated"))
                    && Boolean.TRUE.equals(map.get("hudOrEventEmitted"))) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, Object>> combinedNativeResults(List<Map<String, Object>> nativeResults) {
        List<Map<String, Object>> combined = new ArrayList<>();
        Object existing = LAST_EVIDENCE.get().get("nativeResults");
        if (existing instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    combined.add(copyMap(map));
                }
            }
        }
        if (nativeResults != null) {
            for (Map<String, Object> nativeResult : nativeResults) {
                if (nativeResult != null) {
                    combined.add(Map.copyOf(nativeResult));
                }
            }
        }
        return List.copyOf(combined);
    }

    private static Map<String, Object> copyMap(Map<?, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(copy);
    }

    private static List<String> diagnostics(
            boolean callerReportedMutation,
            int callerReportedMutationCount,
            int dispatchedActionCount,
            int mutatingDispatchedActionCount) {
        List<String> diagnostics = new ArrayList<>();
        if (callerReportedMutation && mutatingDispatchedActionCount == 0) {
            diagnostics.add("Caller reported mutation, but no dispatched and resolved mutating NativeResult was recorded.");
        }
        if (callerReportedMutationCount > mutatingDispatchedActionCount) {
            diagnostics.add("Caller mutation count exceeded dispatched mutating NativeResult evidence.");
        }
        if (dispatchedActionCount == 0) {
            diagnostics.add("No AdapterCore-dispatched runtime action was recorded.");
        }
        return List.copyOf(diagnostics);
    }

    private static Map<String, Object> emptyEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("id", "echoashfallprotocol:live_runtime_mutation_evidence");
        evidence.put("moduleId", "echoashfallprotocol");
        evidence.put("sourceAction", "");
        evidence.put("runtimeHostId", "");
        evidence.put("branch", "");
        evidence.put("sourceStatus", "");
        evidence.put("status", "NO_RUNTIME_MUTATION_RECORDED");
        evidence.put("adapterCoreActionDispatched", false);
        evidence.put("dispatchedActionCount", 0);
        evidence.put("nativeResultCount", 0);
        evidence.put("callerReportedMutation", false);
        evidence.put("callerReportedMutationCount", 0);
        evidence.put("mutationCount", 0);
        evidence.put("mutatingDispatchedActionCount", 0);
        evidence.put("minecraftRuntimeAccessed", false);
        evidence.put("minecraftRuntimeMutated", false);
        evidence.put("minecraftRegistryMutated", false);
        evidence.put("realNativeStateMutated", false);
        evidence.put("saveUpdated", false);
        evidence.put("visibleFeedbackEmitted", false);
        evidence.put("activationReportFactsAfterMutation", false);
        evidence.put("diagnostics", List.of());
        evidence.put("nativeResults", List.of());
        evidence.put("snapshot", Map.of());
        return Map.copyOf(evidence);
    }
}
