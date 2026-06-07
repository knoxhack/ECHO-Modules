package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EchoNativeRuntimeGapAudit {
    private final String moduleId;

    public EchoNativeRuntimeGapAudit(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> audit(String id, List<Map<String, Object>> reports) {
        List<Map<String, Object>> gaps = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        ConsumedRuntimeEvidence consumedEvidence = ConsumedRuntimeEvidence.from(reports);
        if (reports != null) {
            int index = 0;
            for (Map<String, Object> report : reports) {
                scan(report, "reports[" + index++ + "]", gaps, seen, consumedEvidence);
            }
        }
        gaps.sort(Comparator
                .comparingInt((Map<String, Object> gap) -> priorityRank(String.valueOf(gap.get("priority"))))
                .thenComparing(gap -> String.valueOf(gap.get("category")))
                .thenComparing(gap -> String.valueOf(gap.get("operationId"))));

        Map<String, Integer> byCategory = countBy(gaps, "category");
        Map<String, Integer> byPhase = countBy(gaps, "phase");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime gap audit id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_runtime_gap_audit");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 1 AdapterCore runtime gap audit for queued operations that do not yet mutate real native state");
        report.put("executionMode", "adaptercore_jdk_runtime_gap_audit");
        report.put("apiVersion", EchoNativeRuntimeHost.API_VERSION);
        report.put("prioritizedCategories", List.of(
                "inventory",
                "block_placement",
                "structure_placement",
                "respawn",
                "advancements",
                "packets",
                "hud",
                "player_state",
                "world_state",
                "persistence"));
        report.put("queuedGapCount", gaps.size());
        report.put("p0GapCount", countPriority(gaps, "P0"));
        report.put("p1GapCount", countPriority(gaps, "P1"));
        report.put("consumedSourceOperationCount", consumedEvidence.sourceOperationIds().size());
        report.put("consumedHostCallAdapterCount", consumedEvidence.hostCallAdapterIds().size());
        report.put("consumedSourceOperationIds", consumedEvidence.sourceOperationIds());
        report.put("consumedHostCallAdapterIds", consumedEvidence.hostCallAdapterIds());
        report.put("gapsByCategory", byCategory);
        report.put("gapsByPhase", byPhase);
        report.put("gaps", List.copyOf(gaps));
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("runtimeGapStatus", gaps.isEmpty() ? "CLOSED" : "OPEN");
        report.put("status", "PASS");
        report.put("summary", gaps.isEmpty()
                ? "No queued AdapterCore operations are waiting on real native-state mutation."
                : "AdapterCore runtime gap audit listed every discovered queued operation and host call that remains outside real native-state mutation.");
        return Map.copyOf(report);
    }

    private void scan(
            Object value,
            String path,
            List<Map<String, Object>> gaps,
            Set<String> seen,
            ConsumedRuntimeEvidence consumedEvidence) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = copyStringObjectMap(rawMap);
            addOperations(path, map, gaps, seen, consumedEvidence);
            addCommands(path, map, gaps, seen, consumedEvidence);
            addHostCalls(path, map, gaps, seen, consumedEvidence);
            addHooks(path, map, gaps, seen);
            addLiveMutationTruthClaim(path, map, gaps, seen);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object child = entry.getValue();
                if (child instanceof Map<?, ?> || child instanceof List<?>) {
                    scan(child, path + "." + entry.getKey(), gaps, seen, consumedEvidence);
                }
            }
        } else if (value instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                Object child = list.get(index);
                if (child instanceof Map<?, ?> || child instanceof List<?>) {
                    scan(child, path + "[" + index + "]", gaps, seen, consumedEvidence);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addOperations(String path, Map<String, Object> report, List<Map<String, Object>> gaps,
                                      Set<String> seen, ConsumedRuntimeEvidence consumedEvidence) {
        Object rawOperations = report.get("operations");
        if (!(rawOperations instanceof List<?> operations)
                || !String.valueOf(report.getOrDefault("liveBridgeStatus", "")).contains("pending")) {
            return;
        }
        String sourceId = sourceId(path, report);
        for (Object rawOperation : operations) {
            if (rawOperation instanceof Map<?, ?> map) {
                Map<String, Object> operation = (Map<String, Object>) map;
                String operationId = value(operation, "id");
                if (operationId.isBlank()) {
                    continue;
                }
                if (consumedEvidence.consumesOperation(operationId)) {
                    continue;
                }
                addGap(seen, gaps, "operation|" + sourceId + "|" + operationId,
                        gap(path, sourceId, "adaptercore_operation_rehearsal", operationId,
                                value(operation, "bridge"), "", value(operation, "status"),
                                payloadCount(operation), "operation is still marked pending_live_bridge"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addCommands(String path, Map<String, Object> report, List<Map<String, Object>> gaps,
                                    Set<String> seen, ConsumedRuntimeEvidence consumedEvidence) {
        Object rawCommands = report.get("commands");
        if (!(rawCommands instanceof List<?> commands)) {
            return;
        }
        String executionMode = value(report, "executionMode");
        if (!executionMode.contains("command_queue") && !"adaptercore.native_command".equals(value(report, "bridge"))) {
            return;
        }
        String sourceId = sourceId(path, report);
        for (Object rawCommand : commands) {
            if (rawCommand instanceof Map<?, ?> map) {
                Map<String, Object> command = (Map<String, Object>) map;
                String operationId = value(command, "operationId");
                if (operationId.isBlank() || Boolean.TRUE.equals(command.get("liveRuntimeMutation"))) {
                    continue;
                }
                if (consumedEvidence.consumesOperation(operationId)) {
                    continue;
                }
                addGap(seen, gaps, "command|" + sourceId + "|" + operationId,
                        gap(path, sourceId, "adaptercore_command_queue", operationId,
                                value(command, "targetBridge"), "", value(command, "status"),
                                payloadCount(command), "command queue records intent but does not mutate real native state"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addHostCalls(String path, Map<String, Object> report, List<Map<String, Object>> gaps,
                                     Set<String> seen, ConsumedRuntimeEvidence consumedEvidence) {
        Object rawHostCalls = report.get("hostCalls");
        if (!(rawHostCalls instanceof List<?> hostCalls)) {
            return;
        }
        String sourceId = sourceId(path, report);
        for (Object rawHostCall : hostCalls) {
            if (rawHostCall instanceof Map<?, ?> map) {
                Map<String, Object> hostCall = (Map<String, Object>) map;
                String operationId = firstNonBlank(
                        value(hostCall, "adapterId"),
                        value(hostCall, "invocationId"),
                        value(hostCall, "id"));
                if (operationId.isBlank()) {
                    continue;
                }
                if (consumedEvidence.consumesHostCall(operationId)) {
                    continue;
                }
                boolean pending = Boolean.TRUE.equals(hostCall.get("liveRuntimeMutationPending"))
                        || Boolean.TRUE.equals(hostCall.get("hostAdapterImplementationRequired"))
                        || !Boolean.TRUE.equals(hostCall.get("realNativeStateMutated"));
                if (!pending) {
                    continue;
                }
                String hostApi = firstNonBlank(value(hostCall, "hostRuntimeApi"), value(hostCall, "hostSurfaceApi"));
                Map<String, Object> gap = gap(path, sourceId, "native_host_call_queue", operationId,
                        value(hostCall, "targetBridgeId"), hostApi, value(hostCall, "status"),
                        numberValue(hostCall, "payloadCount"),
                        "host call is queued for a native loader adapter and still has liveRuntimeMutationPending=true");
                addGap(seen, gaps, "host|" + sourceId + "|" + operationId, gap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addHooks(String path, Map<String, Object> report, List<Map<String, Object>> gaps,
                                 Set<String> seen) {
        Object rawHooks = report.get("hooks");
        if (!(rawHooks instanceof List<?> hooks) || !"adaptercore.native_event".equals(value(report, "bridge"))) {
            return;
        }
        String sourceId = sourceId(path, report);
        for (Object rawHook : hooks) {
            if (rawHook instanceof Map<?, ?> map) {
                Map<String, Object> hook = (Map<String, Object>) map;
                String eventId = value(hook, "event");
                if (eventId.isBlank() || Boolean.TRUE.equals(hook.get("handlerExecuted"))) {
                    continue;
                }
                addGap(seen, gaps, "hook|" + sourceId + "|" + eventId + "|" + value(hook, "handler"),
                        gap(path, sourceId, "adaptercore_event_hook_queue", eventId,
                                value(hook, "handler"), "events.publish", "planned_handler_not_executed",
                                1, "event hook is planned but no live native event has invoked its handler"));
            }
        }
    }

    private static void addLiveMutationTruthClaim(String path, Map<String, Object> report,
                                                  List<Map<String, Object>> gaps, Set<String> seen) {
        if (!Boolean.TRUE.equals(report.get("liveRuntimeMutation"))) {
            return;
        }
        boolean runtimeMutated = Boolean.TRUE.equals(report.get("minecraftRuntimeMutated"))
                || Boolean.TRUE.equals(report.get("realNativeStateMutated"));
        if (runtimeMutated) {
            return;
        }
        String sourceId = sourceId(path, report);
        String operationId = firstNonBlank(
                value(report, "operationId"),
                value(report, "event"),
                value(report, "id"),
                sourceId);
        String targetBridge = firstNonBlank(
                value(report, "targetBridgeId"),
                value(report, "targetBridge"),
                value(report, "bridge"));
        String hostApi = firstNonBlank(value(report, "hostRuntimeApi"), value(report, "hostSurfaceApi"));
        addGap(seen, gaps, "truth-claim|" + path + "|" + sourceId + "|" + operationId,
                gap(path, sourceId, "adaptercore_live_mutation_truth_claim", operationId,
                        targetBridge, hostApi, value(report, "status"),
                        payloadCount(report),
                        "report claims liveRuntimeMutation=true but does not include real runtime mutation evidence"));
    }

    private static Map<String, Object> gap(String path, String sourceId, String queueKind, String operationId,
                                           String targetBridge, String hostApi, String status, int payloadCount,
                                           String reason) {
        String category = category(operationId, targetBridge, hostApi);
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("priority", priority(category));
        gap.put("phase", phase(category, operationId, targetBridge, hostApi));
        gap.put("category", category);
        gap.put("sourcePath", path);
        gap.put("sourceReportId", sourceId);
        gap.put("queueKind", queueKind);
        gap.put("operationId", operationId);
        gap.put("targetBridgeId", targetBridge);
        gap.put("hostApi", hostApi);
        gap.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(firstNonBlank(hostApi, operationId, targetBridge)));
        gap.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(firstNonBlank(hostApi, operationId)));
        gap.put("payloadCount", payloadCount);
        gap.put("currentStatus", status);
        gap.put("mutatesRealNativeState", false);
        gap.put("reason", reason);
        return Map.copyOf(gap);
    }

    private static String category(String operationId, String targetBridge, String hostApi) {
        String text = (operationId + " " + targetBridge + " " + hostApi).toLowerCase(Locale.ROOT);
        if (text.contains("inventory")) {
            return "inventory";
        }
        if (text.contains("consume_item")) {
            return "item_consume";
        }
        if (text.contains("craft_item")) {
            return "recipe_crafted";
        }
        if (text.contains("sleep")) {
            return "shelter_slept";
        }
        if (text.contains("special_marker")) {
            return "special_marker";
        }
        if (text.contains("place_block")) {
            return "block_placement";
        }
        if (text.contains("structure") || text.contains("drop_pod")) {
            return "structure_placement";
        }
        if (text.contains("respawn")) {
            return "respawn";
        }
        if (text.contains("advancement")) {
            return "advancements";
        }
        if (text.contains("packet") || text.contains("network") || text.contains("screen")) {
            return "packets";
        }
        if (text.contains("hud") || text.contains("hazard_readout") || text.contains("mission_tracker")) {
            return "hud";
        }
        if (text.contains("player_state") || text.contains("teleport") || text.contains("positioner")
                || text.contains("position")) {
            return "player_state";
        }
        if (text.contains("persistent") || text.contains("save") || text.contains("field_cache")
                || text.contains("recovery")) {
            return "persistence";
        }
        if (text.contains("world") || text.contains("weather") || text.contains("atmosphere")
                || text.contains("holomap") || text.contains("route")) {
            return "world_state";
        }
        if (text.contains("block_entity") || text.contains("machine")) {
            return "block_entities";
        }
        if (text.contains("capability") || text.contains("energy")) {
            return "capabilities";
        }
        if (text.contains("event")) {
            return "events";
        }
        return "surface";
    }

    private static String priority(String category) {
        return switch (category) {
            case "inventory", "block_placement", "structure_placement", "respawn", "player_state" -> "P0";
            case "advancements", "packets", "hud", "world_state", "persistence" -> "P1";
            case "block_entities", "capabilities", "events", "item_consume",
                    "recipe_crafted", "shelter_slept", "special_marker" -> "P2";
            default -> "P3";
        };
    }

    private static String phase(String category, String operationId, String targetBridge, String hostApi) {
        String text = (operationId + " " + targetBridge + " " + hostApi).toLowerCase(Locale.ROOT);
        if (List.of("inventory", "structure_placement", "respawn", "advancements",
                "packets", "hud", "player_state").contains(category)) {
            return "Phase 3 First-Spawn Runtime";
        }
        if (category.equals("events") || category.equals("item_consume")
                || category.equals("recipe_crafted") || category.equals("shelter_slept")
                || category.equals("special_marker") || text.contains("player.")) {
            return "Phase 4 Early Event Bridges";
        }
        if (category.equals("block_entities") || category.equals("capabilities")) {
            return "Phase 5 Machine Runtime";
        }
        if (text.contains("holomap") || text.contains("lens") || text.contains("codex")
                || text.contains("terminal") || text.contains("wiki")) {
            return "Phase 6 Exploration Runtime";
        }
        if (text.contains("hazard") || text.contains("weather") || text.contains("atmosphere")) {
            return "Phase 7 Hazard Runtime";
        }
        if (category.equals("persistence")) {
            return "Phase 9 Hardening";
        }
        return "Phase 10 API Freeze";
    }

    private static int priorityRank(String priority) {
        return switch (priority) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            default -> 3;
        };
    }

    private static void addGap(Set<String> seen, List<Map<String, Object>> gaps, String key,
                               Map<String, Object> gap) {
        if (seen.add(key)) {
            gaps.add(gap);
        }
    }

    private static Map<String, Integer> countBy(List<Map<String, Object>> gaps, String key) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> gap : gaps) {
            String value = String.valueOf(gap.get(key));
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }
        return Map.copyOf(counts);
    }

    private static int countPriority(List<Map<String, Object>> gaps, String priority) {
        int count = 0;
        for (Map<String, Object> gap : gaps) {
            if (priority.equals(gap.get("priority"))) {
                count++;
            }
        }
        return count;
    }

    private static int payloadCount(Map<String, Object> operation) {
        Object payload = operation.get("payload");
        if (payload instanceof Map<?, ?> map) {
            return map.isEmpty() ? 0 : 1;
        }
        if (payload instanceof List<?> list) {
            return list.size();
        }
        return payload == null ? 0 : 1;
    }

    private static int numberValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String sourceId(String path, Map<String, Object> report) {
        return firstNonBlank(value(report, "id"), value(report, "bridge"), path);
    }

    private static Map<String, Object> copyStringObjectMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private record ConsumedRuntimeEvidence(
            Set<String> sourceOperationIds,
            Set<String> hostCallAdapterIds) {
        private ConsumedRuntimeEvidence {
            sourceOperationIds = sourceOperationIds == null ? Set.of() : Set.copyOf(sourceOperationIds);
            hostCallAdapterIds = hostCallAdapterIds == null ? Set.of() : Set.copyOf(hostCallAdapterIds);
        }

        static ConsumedRuntimeEvidence from(List<Map<String, Object>> reports) {
            Set<String> sourceOperationIds = new LinkedHashSet<>();
            Set<String> hostCallAdapterIds = new LinkedHashSet<>();
            if (reports != null) {
                for (Map<String, Object> report : reports) {
                    collect(report, sourceOperationIds, hostCallAdapterIds);
                }
            }
            return new ConsumedRuntimeEvidence(sourceOperationIds, hostCallAdapterIds);
        }

        boolean consumesOperation(String operationId) {
            return sourceOperationIds.contains(normalize(operationId));
        }

        boolean consumesHostCall(String hostCallAdapterId) {
            return hostCallAdapterIds.contains(normalize(hostCallAdapterId));
        }

        private static void collect(
                Object value,
                Set<String> sourceOperationIds,
                Set<String> hostCallAdapterIds) {
            if (value instanceof Map<?, ?> rawMap) {
                Map<String, Object> map = copyStringObjectMap(rawMap);
                collectNativeResult(map, sourceOperationIds, hostCallAdapterIds);
                if (Boolean.TRUE.equals(map.get("realNativeStateMutated"))) {
                    collectList(map.get("consumedSourceOperationIds"), sourceOperationIds);
                    collectList(map.get("consumedHostCallAdapterIds"), hostCallAdapterIds);
                }
                for (Object child : map.values()) {
                    if (child instanceof Map<?, ?> || child instanceof List<?>) {
                        collect(child, sourceOperationIds, hostCallAdapterIds);
                    }
                }
            } else if (value instanceof List<?> list) {
                for (Object child : list) {
                    if (child instanceof Map<?, ?> || child instanceof List<?>) {
                        collect(child, sourceOperationIds, hostCallAdapterIds);
                    }
                }
            }
        }

        private static void collectNativeResult(
                Map<String, Object> nativeResult,
                Set<String> sourceOperationIds,
                Set<String> hostCallAdapterIds) {
            if (!Boolean.TRUE.equals(nativeResult.get("mutated"))) {
                return;
            }
            Object snapshot = nativeResult.get("snapshot");
            if (!(snapshot instanceof Map<?, ?> rawSnapshot)) {
                return;
            }
            Map<String, Object> map = copyStringObjectMap(rawSnapshot);
            if (!Boolean.TRUE.equals(map.get("adapterCoreActionDispatched"))
                    || !Boolean.TRUE.equals(map.get("runtimeHostResolved"))) {
                return;
            }
            collectString(map.get("adapterCoreActionId"), sourceOperationIds);
            collectString(map.get("adapterCoreSourceOperationId"), sourceOperationIds);
            collectList(map.get("adapterCoreSourceOperationIds"), sourceOperationIds);
            collectString(map.get("adapterCoreHostCallAdapterId"), hostCallAdapterIds);
            collectList(map.get("adapterCoreHostCallAdapterIds"), hostCallAdapterIds);
            collectList(map.get("consumedSourceOperationIds"), sourceOperationIds);
            collectList(map.get("consumedHostCallAdapterIds"), hostCallAdapterIds);
        }

        private static void collectList(Object value, Set<String> target) {
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    collectString(item, target);
                }
            }
        }

        private static void collectString(Object value, Set<String> target) {
            if (value instanceof String text) {
                String normalized = normalize(text);
                if (!normalized.isBlank()) {
                    target.add(normalized);
                }
            }
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
