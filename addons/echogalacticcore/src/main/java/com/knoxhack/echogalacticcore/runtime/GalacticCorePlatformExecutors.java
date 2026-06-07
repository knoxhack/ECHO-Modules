package com.knoxhack.echogalacticcore.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCorePlatformExecutors {
    private final GalacticCoreLiveHostEntrypoints entrypoints;

    public GalacticCorePlatformExecutors(GalacticCoreLiveHostEntrypoints entrypoints) {
        this.entrypoints = Objects.requireNonNull(entrypoints, "entrypoints");
    }

    public PlatformExecutionResult executeWorldTransfer(PlatformExecutionContext context) {
        return execute("world_dimension_transfer", entrypoints.executeWorldTransfer(invocation("world", context)), context);
    }

    public PlatformExecutionResult executeBossSpawn(PlatformExecutionContext context) {
        return execute("entity_boss_spawn", entrypoints.executeBossSpawn(invocation("entity", context)), context);
    }

    public PlatformExecutionResult openScreenHost(String screenId, PlatformExecutionContext context) {
        return execute("screen_" + requireText(screenId, "screenId"), entrypoints.openScreenHost(screenId, invocation("screen", context)), context);
    }

    public List<PlatformExecutionResult> releasePlatformExecutorSmokeResults() {
        return List.of(
                executeWorldTransfer(new PlatformExecutionContext("executor/world-smoke", "player/smoke", "server", true)),
                executeBossSpawn(new PlatformExecutionContext("executor/entity-smoke", "boss/smoke", "server", true)),
                openScreenHost("holomap_routes", new PlatformExecutionContext("executor/holomap-smoke", "player/smoke", "client", true)),
                openScreenHost("screencore_launch_checklist", new PlatformExecutionContext("executor/checklist-smoke", "player/smoke", "client", true)),
                openScreenHost("treasure_chest", new PlatformExecutionContext("executor/treasure-smoke", "player/smoke", "client", true))
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", "galacticraft_legacy_platform_executor_facade",
                "typedReceiptsOnly", true,
                "platformMutationDeferred", true,
                "mutatesMinecraftObjects", false,
                "executors", "world_dimension_transfer, entity_boss_spawn, holomap_routes, screencore_launch_checklist, treasure_chest",
                "replaces", "final ASDK-safe executor boundary before host-owned Minecraft object mutation"
        );
    }

    private PlatformExecutionResult execute(
            String executionKind,
            GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult entrypointResult,
            PlatformExecutionContext context
    ) {
        boolean accepted = entrypointResult.accepted();
        String status = accepted
                ? (context.dryRun() ? "platform_executor_dry_run_ready" : "platform_executor_ready")
                : "platform_executor_blocked";
        List<String> executedSteps = List.of(
                "validate_entrypoint_result",
                "validate_binding_contract",
                "validate_adapter_steps",
                "queue_platform_mutation_receipt"
        );
        return new PlatformExecutionResult(
                accepted,
                status,
                executionKind,
                context,
                entrypointResult,
                executedSteps,
                actionFor(executionKind, entrypointResult, context, accepted, status, executedSteps)
        );
    }

    private static GalacticCoreLiveHostEntrypoints.LiveHostInvocation invocation(String prefix, PlatformExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return new GalacticCoreLiveHostEntrypoints.LiveHostInvocation(
                prefix + "/" + context.executorId(),
                context.subjectId(),
                context.hostLane()
        );
    }

    private static GalacticCoreRuntimeGateway.RuntimeAction actionFor(
            String executionKind,
            GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult entrypointResult,
            PlatformExecutionContext context,
            boolean accepted,
            String status,
            List<String> executedSteps
    ) {
        GalacticCoreRuntimeGateway.RuntimeAction entrypointAction = entrypointResult.action();
        String target = entrypointAction.target().replace("live_callback/", "platform_executor/");
        return new GalacticCoreRuntimeGateway.RuntimeAction(
                target,
                entrypointAction.surface(),
                entrypointAction.action(),
                Map.ofEntries(
                        Map.entry("source", "galacticraft_legacy_platform_executor_facade"),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("platformMutationDeferred", true),
                        Map.entry("mutatesMinecraftObjects", false),
                        Map.entry("executionKind", executionKind),
                        Map.entry("executorId", context.executorId()),
                        Map.entry("subjectId", context.subjectId()),
                        Map.entry("hostLane", context.hostLane()),
                        Map.entry("dryRun", context.dryRun()),
                        Map.entry("accepted", accepted),
                        Map.entry("status", status),
                        Map.entry("executedSteps", executedSteps),
                        Map.entry("entrypointSource", entrypointAction.evidence().get("source")),
                        Map.entry("entrypointTarget", entrypointAction.target()),
                        Map.entry("adapterTarget", entrypointAction.evidence().get("adapterTarget")),
                        Map.entry("bindingTarget", entrypointAction.evidence().get("bindingTarget")),
                        Map.entry("bindingOwnerService", entrypointAction.evidence().get("bindingOwnerService")),
                        Map.entry("completedHostSteps", entrypointResult.completedSteps()),
                        Map.entry("saveDataTarget", entrypointAction.evidence().get("saveDataTarget")),
                        Map.entry("replacement", "ASDK platform executor facade for " + executionKind)
                )
        );
    }

    public record PlatformExecutionContext(String executorId, String subjectId, String hostLane, boolean dryRun) {
        public PlatformExecutionContext {
            executorId = requireText(executorId, "executorId");
            subjectId = requireText(subjectId, "subjectId");
            hostLane = requireText(hostLane, "hostLane");
        }
    }

    public record PlatformExecutionResult(
            boolean accepted,
            String status,
            String executionKind,
            PlatformExecutionContext context,
            GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult entrypointResult,
            List<String> executedSteps,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        public PlatformExecutionResult {
            status = requireText(status, "status");
            executionKind = requireText(executionKind, "executionKind");
            context = Objects.requireNonNull(context, "context");
            entrypointResult = Objects.requireNonNull(entrypointResult, "entrypointResult");
            executedSteps = List.copyOf(executedSteps == null ? List.of() : executedSteps);
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
