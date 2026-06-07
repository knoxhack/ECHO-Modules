package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreLiveHostEntrypoints {
    private final GalacticCoreLiveHostAdapters liveHostAdapters;

    public GalacticCoreLiveHostEntrypoints(GalacticCoreLiveHostAdapters liveHostAdapters) {
        this.liveHostAdapters = Objects.requireNonNull(liveHostAdapters, "liveHostAdapters");
    }

    public LiveHostInvocationResult executeWorldTransfer(LiveHostInvocation invocation) {
        return invoke("world", invocation);
    }

    public LiveHostInvocationResult executeBossSpawn(LiveHostInvocation invocation) {
        return invoke("entity", invocation);
    }

    public LiveHostInvocationResult openScreenHost(String screenId, LiveHostInvocation invocation) {
        return invoke("screen_" + requireText(screenId, "screenId"), invocation);
    }

    public List<LiveHostInvocationResult> releaseLiveHostEntrypointSmokeResults() {
        return List.of(
                executeWorldTransfer(new LiveHostInvocation("world/smoke", "player/smoke", "server")),
                executeBossSpawn(new LiveHostInvocation("entity/smoke", "boss/smoke", "server")),
                openScreenHost("holomap_routes", new LiveHostInvocation("screen/holomap-smoke", "player/smoke", "client")),
                openScreenHost("screencore_launch_checklist", new LiveHostInvocation("screen/checklist-smoke", "player/smoke", "client")),
                openScreenHost("treasure_chest", new LiveHostInvocation("screen/treasure-smoke", "player/smoke", "client"))
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_live_host_entrypoints",
                "typedReceiptsOnly", true,
                "entrypoints", "world_transfer, boss_spawn, holomap_routes, screencore_launch_checklist, treasure_chest",
                "replaces", "WorldProvider transfer callback entry, EntityBoss spawn callback entry, Gui* open callback entry"
        );
    }

    private LiveHostInvocationResult invoke(String selector, LiveHostInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        GalacticCoreLiveHostAdapters.LiveHostAdapterPlan plan = plan(selector);
        boolean accepted = !plan.executorSteps().isEmpty();
        String status = accepted ? "host_entrypoint_ready" : "host_entrypoint_blocked";
        return new LiveHostInvocationResult(
                accepted,
                status,
                invocation,
                plan,
                plan.executorSteps(),
                actionFor(plan, invocation, accepted, status)
        );
    }

    private GalacticCoreLiveHostAdapters.LiveHostAdapterPlan plan(String selector) {
        return liveHostAdapters.releaseLiveHostAdapterSmokePlans().stream()
                .filter(candidate -> matches(selector, candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No live host adapter plan for " + selector));
    }

    private static boolean matches(String selector, GalacticCoreLiveHostAdapters.LiveHostAdapterPlan plan) {
        if ("world".equals(selector)) {
            return "world".equals(plan.evidence().get("hostSurface"));
        }
        if ("entity".equals(selector)) {
            return "entity".equals(plan.evidence().get("hostSurface"));
        }
        return selector.equals(plan.binding().bindingKind());
    }

    private static GalacticCoreRuntimeGateway.RuntimeAction actionFor(
            GalacticCoreLiveHostAdapters.LiveHostAdapterPlan plan,
            LiveHostInvocation invocation,
            boolean accepted,
            String status
    ) {
        String target = plan.target().replace("live_host/", "live_callback/");
        return new GalacticCoreRuntimeGateway.RuntimeAction(
                target,
                plan.surface(),
                plan.action(),
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_live_host_entrypoints"),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("adapterSource", plan.evidence().get("source")),
                        Map.entry("adapterId", plan.adapterId()),
                        Map.entry("adapterTarget", plan.target()),
                        Map.entry("bindingTarget", plan.binding().target()),
                        Map.entry("bindingOwnerService", plan.binding().serviceId()),
                        Map.entry("hostEntrypoint", invocation.entrypointId()),
                        Map.entry("subjectId", invocation.subjectId()),
                        Map.entry("hostLane", invocation.hostLane()),
                        Map.entry("accepted", accepted),
                        Map.entry("status", status),
                        Map.entry("completedSteps", plan.executorSteps()),
                        Map.entry("saveDataTarget", plan.saveDataTarget()),
                        Map.entry("replacement", "ASDK live host callback entrypoint for " + plan.binding().bindingKind())
                )
        );
    }

    public record LiveHostInvocation(String entrypointId, String subjectId, String hostLane) {
        public LiveHostInvocation {
            entrypointId = requireText(entrypointId, "entrypointId");
            subjectId = requireText(subjectId, "subjectId");
            hostLane = requireText(hostLane, "hostLane");
        }
    }

    public record LiveHostInvocationResult(
            boolean accepted,
            String status,
            LiveHostInvocation invocation,
            GalacticCoreLiveHostAdapters.LiveHostAdapterPlan plan,
            List<String> completedSteps,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        public LiveHostInvocationResult {
            status = requireText(status, "status");
            invocation = Objects.requireNonNull(invocation, "invocation");
            plan = Objects.requireNonNull(plan, "plan");
            completedSteps = List.copyOf(completedSteps == null ? List.of() : completedSteps);
            action = Objects.requireNonNull(action, "action");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
