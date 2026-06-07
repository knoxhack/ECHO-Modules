package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class AshfallNativeRuntimeMutationEvidence {
    private static final List<Map<String, Object>> RECORDS = new CopyOnWriteArrayList<>();

    private AshfallNativeRuntimeMutationEvidence() {
    }

    static void record(
            String sourceHook,
            String runtimeHostId,
            String branch,
            String status,
            boolean realNativeStateMutated,
            int mutationCount,
            List<NativeResult> nativeResults,
            Map<String, Object> snapshot) {
        Map<String, Object> record = Map.of(
                "sourceHook", sourceHook == null ? "" : sourceHook,
                "runtimeHostId", runtimeHostId == null ? "" : runtimeHostId,
                "branch", branch == null ? "" : branch,
                "status", status == null ? "" : status,
                "realNativeStateMutated", realNativeStateMutated,
                "mutationCount", mutationCount,
                "nativeResults", resultSummaries(nativeResults),
                "snapshot", snapshot == null ? Map.of() : snapshot);
        RECORDS.add(record);
        EchoAshfallProtocol.LOGGER.debug("Recorded Ashfall native runtime mutation evidence: {}", record);
    }

    static List<Map<String, Object>> records() {
        return List.copyOf(RECORDS);
    }

    static void clearForTests() {
        RECORDS.clear();
    }

    private static List<Map<String, Object>> resultSummaries(List<NativeResult> nativeResults) {
        if (nativeResults == null || nativeResults.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (NativeResult result : nativeResults) {
            if (result == null) {
                continue;
            }
            summaries.add(Map.of(
                    "status", result.status(),
                    "mutated", result.mutated(),
                    "message", result.message(),
                    "snapshot", result.snapshot()));
        }
        return List.copyOf(summaries);
    }
}
