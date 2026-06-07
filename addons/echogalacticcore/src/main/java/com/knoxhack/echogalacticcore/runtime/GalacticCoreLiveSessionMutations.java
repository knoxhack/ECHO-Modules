package com.knoxhack.echogalacticcore.runtime;

import com.knoxhack.echogalacticcore.GalacticCoreIds;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GalacticCoreLiveSessionMutations {
    private static final String SOURCE = "galacticraft_legacy_live_session_mutation_bridge";

    private final GalacticCorePlatformExecutors platformExecutors;
    private final HostMutationSink hostMutationSink;

    public GalacticCoreLiveSessionMutations(
            GalacticCorePlatformExecutors platformExecutors,
            HostMutationSink hostMutationSink
    ) {
        this.platformExecutors = Objects.requireNonNull(platformExecutors, "platformExecutors");
        this.hostMutationSink = Objects.requireNonNull(hostMutationSink, "hostMutationSink");
    }

    public LiveSessionMutationResult commitWorldTransfer(LiveSessionMutationContext context) {
        return commit("world_dimension_transfer", platformExecutors.executeWorldTransfer(platformContext(context)), context);
    }

    public LiveSessionMutationResult commitBossSpawn(LiveSessionMutationContext context) {
        return commit("entity_boss_spawn", platformExecutors.executeBossSpawn(platformContext(context)), context);
    }

    public LiveSessionMutationResult openScreenHost(String screenId, LiveSessionMutationContext context) {
        return commit("screen_" + requireText(screenId, "screenId"), platformExecutors.openScreenHost(screenId, platformContext(context)), context);
    }

    public List<LiveSessionMutationResult> releaseLiveSessionMutationSmokeResults() {
        return List.of(
                commitWorldTransfer(new LiveSessionMutationContext("session/world-smoke", "live/world-smoke", "player/smoke", "server")),
                commitBossSpawn(new LiveSessionMutationContext("session/entity-smoke", "live/entity-smoke", "boss/smoke", "server")),
                openScreenHost("holomap_routes", new LiveSessionMutationContext("session/holomap-smoke", "live/holomap-smoke", "player/smoke", "client")),
                openScreenHost("screencore_launch_checklist", new LiveSessionMutationContext("session/checklist-smoke", "live/checklist-smoke", "player/smoke", "client")),
                openScreenHost("treasure_chest", new LiveSessionMutationContext("session/treasure-smoke", "live/treasure-smoke", "player/smoke", "client"))
        );
    }

    public Map<String, Object> evidence() {
        return Map.of(
                "source", SOURCE,
                "typedReceiptsOnly", true,
                "hostOwnedMutationBoundary", true,
                "payloadContracts", "world_transfer, boss_spawn, screen_open",
                "sinkId", hostMutationSink.sinkId(),
                "mutations", "world_dimension_transfer, entity_boss_spawn, holomap_routes, screencore_launch_checklist, treasure_chest",
                "replaces", "legacy direct teleport, boss entity construction, and GUI opening mutation calls"
        );
    }

    public static HostMutationSink contractOnlyHostSink() {
        return new ContractOnlyHostMutationSink();
    }

    private LiveSessionMutationResult commit(
            String mutationKind,
            GalacticCorePlatformExecutors.PlatformExecutionResult platformResult,
            LiveSessionMutationContext context
    ) {
        if (platformResult.context().dryRun()) {
            throw new IllegalArgumentException("live session mutation requires a non-dry-run platform execution");
        }
        LiveSessionMutationRequest request = new LiveSessionMutationRequest(mutationKind, context, platformResult);
        HostMutationReceipt hostReceipt = hostMutationSink.commit(request);
        boolean accepted = platformResult.accepted() && hostReceipt.accepted();
        String status = accepted ? "live_session_mutation_accepted" : "live_session_mutation_blocked";
        return new LiveSessionMutationResult(
                accepted,
                status,
                request,
                hostReceipt,
                actionFor(request, hostReceipt, accepted, status)
        );
    }

    private static GalacticCorePlatformExecutors.PlatformExecutionContext platformContext(LiveSessionMutationContext context) {
        Objects.requireNonNull(context, "context");
        return new GalacticCorePlatformExecutors.PlatformExecutionContext(
                context.executorId(),
                context.subjectId(),
                context.hostLane(),
                false
        );
    }

    private static GalacticCoreRuntimeGateway.RuntimeAction actionFor(
            LiveSessionMutationRequest request,
            HostMutationReceipt hostReceipt,
            boolean accepted,
            String status
    ) {
        GalacticCoreRuntimeGateway.RuntimeAction platformAction = request.platformResult().action();
        String target = platformAction.target().replace("platform_executor/", "live_session/");
        return new GalacticCoreRuntimeGateway.RuntimeAction(
                target,
                platformAction.surface(),
                platformAction.action(),
                Map.ofEntries(
                        Map.entry("source", SOURCE),
                        Map.entry("typedReceiptsOnly", true),
                        Map.entry("hostOwnedMutationBoundary", true),
                        Map.entry("liveSessionId", request.context().sessionId()),
                        Map.entry("mutationKind", request.mutationKind()),
                        Map.entry("executorId", request.context().executorId()),
                        Map.entry("subjectId", request.context().subjectId()),
                        Map.entry("hostLane", request.context().hostLane()),
                        Map.entry("dryRun", false),
                        Map.entry("accepted", accepted),
                        Map.entry("status", status),
                        Map.entry("payloadKind", request.payload().kind()),
                        Map.entry("payloadOwnerService", request.payload().ownerService()),
                        Map.entry("payloadTarget", request.payload().target()),
                        Map.entry("payloadHostApi", request.payload().hostApi()),
                        Map.entry("payloadRequiredSteps", request.payload().requiredSteps()),
                        Map.entry("payloadStateKeys", request.payload().stateKeys()),
                        Map.entry("payloadSafetyChecks", request.payload().safetyChecks()),
                        Map.entry("platformTarget", platformAction.target()),
                        Map.entry("entrypointTarget", platformAction.evidence().get("entrypointTarget")),
                        Map.entry("adapterTarget", platformAction.evidence().get("adapterTarget")),
                        Map.entry("bindingTarget", platformAction.evidence().get("bindingTarget")),
                        Map.entry("bindingOwnerService", platformAction.evidence().get("bindingOwnerService")),
                        Map.entry("hostReceiptId", hostReceipt.receiptId()),
                        Map.entry("hostSinkId", hostReceipt.hostSinkId()),
                        Map.entry("hostCommittedSteps", hostReceipt.committedSteps()),
                        Map.entry("mutatesMinecraftObjects", hostReceipt.mutatesMinecraftObjects()),
                        Map.entry("saveDataTarget", platformAction.evidence().get("saveDataTarget")),
                        Map.entry("replacement", "ASDK live-session host mutation bridge for " + request.mutationKind())
                )
        );
    }

    public interface HostMutationSink {
        String sinkId();

        HostMutationReceipt commit(LiveSessionMutationRequest request);
    }

    private static final class ContractOnlyHostMutationSink implements HostMutationSink {
        @Override
        public String sinkId() {
            return GalacticCoreIds.id("contract_only_host_mutation_sink");
        }

        @Override
        public HostMutationReceipt commit(LiveSessionMutationRequest request) {
            return new HostMutationReceipt(
                    request.context().sessionId() + "/" + request.mutationKind(),
                    sinkId(),
                    request.platformResult().accepted(),
                    false,
                    request.platformResult().entrypointResult().completedSteps(),
                    Map.of(
                            "source", SOURCE,
                            "typedReceiptsOnly", true,
                            "contractOnly", true,
                            "hostRuntimeRequired", true,
                            "platformTarget", request.platformResult().action().target()
                    )
            );
        }
    }

    public record LiveSessionMutationContext(String sessionId, String executorId, String subjectId, String hostLane) {
        public LiveSessionMutationContext {
            sessionId = requireText(sessionId, "sessionId");
            executorId = requireText(executorId, "executorId");
            subjectId = requireText(subjectId, "subjectId");
            hostLane = requireText(hostLane, "hostLane");
        }
    }

    public record LiveSessionMutationRequest(
            String mutationKind,
            LiveSessionMutationContext context,
            GalacticCorePlatformExecutors.PlatformExecutionResult platformResult,
            HostMutationPayload payload
    ) {
        public LiveSessionMutationRequest(
                String mutationKind,
                LiveSessionMutationContext context,
                GalacticCorePlatformExecutors.PlatformExecutionResult platformResult
        ) {
            this(mutationKind, context, platformResult, HostMutationPayload.from(mutationKind, context, platformResult));
        }

        public LiveSessionMutationRequest {
            mutationKind = requireText(mutationKind, "mutationKind");
            context = Objects.requireNonNull(context, "context");
            platformResult = Objects.requireNonNull(platformResult, "platformResult");
            payload = Objects.requireNonNull(payload, "payload");
            if (!mutationKind.equals(payload.kind())) {
                throw new IllegalArgumentException("payload kind must match mutation kind");
            }
        }
    }

    public record HostMutationPayload(
            String kind,
            String ownerService,
            String hostApi,
            String target,
            String subjectId,
            String hostLane,
            List<String> requiredSteps,
            List<String> stateKeys,
            List<String> safetyChecks,
            Map<String, Object> evidence
    ) {
        public HostMutationPayload {
            kind = requireText(kind, "kind");
            ownerService = requireText(ownerService, "ownerService");
            hostApi = requireText(hostApi, "hostApi");
            target = requireText(target, "target");
            subjectId = requireText(subjectId, "subjectId");
            hostLane = requireText(hostLane, "hostLane");
            requiredSteps = List.copyOf(requiredSteps == null ? List.of() : requiredSteps);
            stateKeys = List.copyOf(stateKeys == null ? List.of() : stateKeys);
            safetyChecks = List.copyOf(safetyChecks == null ? List.of() : safetyChecks);
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }

        private static HostMutationPayload from(
                String mutationKind,
                LiveSessionMutationContext context,
                GalacticCorePlatformExecutors.PlatformExecutionResult platformResult
        ) {
            GalacticCoreRuntimeGateway.RuntimeAction platformAction = platformResult.action();
            String ownerService = String.valueOf(platformAction.evidence().get("bindingOwnerService"));
            String target = platformAction.target().replace("platform_executor/", "host_payload/");
            return new HostMutationPayload(
                    mutationKind,
                    ownerService,
                    hostApi(mutationKind),
                    target,
                    context.subjectId(),
                    context.hostLane(),
                    requiredSteps(mutationKind, platformResult),
                    stateKeys(mutationKind),
                    safetyChecks(mutationKind),
                    Map.ofEntries(
                            Map.entry("source", SOURCE),
                            Map.entry("typedReceiptsOnly", true),
                            Map.entry("hostOwnedMutationBoundary", true),
                            Map.entry("mutationKind", mutationKind),
                            Map.entry("ownerService", ownerService),
                            Map.entry("platformTarget", platformAction.target()),
                            Map.entry("entrypointTarget", platformAction.evidence().get("entrypointTarget")),
                            Map.entry("adapterTarget", platformAction.evidence().get("adapterTarget")),
                            Map.entry("bindingTarget", platformAction.evidence().get("bindingTarget")),
                            Map.entry("saveDataTarget", platformAction.evidence().get("saveDataTarget"))
                    )
            );
        }

        private static String hostApi(String mutationKind) {
            return switch (mutationKind) {
                case "world_dimension_transfer" -> "loadDestinationAndPlacePlayer";
                case "entity_boss_spawn" -> "spawnBossEntityAndAttachEncounter";
                case "screen_holomap_routes", "screen_screencore_launch_checklist", "screen_treasure_chest" -> "openScreenAndMountState";
                default -> throw new IllegalArgumentException("Unknown live-session mutation payload kind " + mutationKind);
            };
        }

        private static List<String> requiredSteps(
                String mutationKind,
                GalacticCorePlatformExecutors.PlatformExecutionResult platformResult
        ) {
            List<String> completedHostSteps = platformResult.entrypointResult().completedSteps();
            if (!completedHostSteps.isEmpty()) {
                return completedHostSteps;
            }
            return switch (mutationKind) {
                case "world_dimension_transfer" -> List.of("resolve_destination_level", "ticket_destination_chunk", "place_player_at_anchor", "sync_progression_attachment");
                case "entity_boss_spawn" -> List.of("resolve_boss_room", "instantiate_boss_entity", "attach_boss_state", "lock_treasure_room");
                case "screen_holomap_routes", "screen_screencore_launch_checklist", "screen_treasure_chest" -> List.of("resolve_screen_factory", "mount_renderer", "mount_widgets", "wire_actions", "sync_screen_state");
                default -> throw new IllegalArgumentException("Unknown live-session mutation payload kind " + mutationKind);
            };
        }

        private static List<String> stateKeys(String mutationKind) {
            return switch (mutationKind) {
                case "world_dimension_transfer" -> List.of("destination_dimension", "landing_anchor", "rocket_countdown", "progression_attachment");
                case "entity_boss_spawn" -> List.of("dungeon_id", "boss_id", "encounter_state", "treasure_room_lock");
                case "screen_holomap_routes" -> List.of("screen_id", "route_selection", "route_lock_state", "progression_attachment");
                case "screen_screencore_launch_checklist" -> List.of("screen_id", "pad_state", "fuel_state", "life_support_state", "countdown_state");
                case "screen_treasure_chest" -> List.of("screen_id", "treasure_room_state", "boss_defeat_state", "key_state", "reward_claims");
                default -> throw new IllegalArgumentException("Unknown live-session mutation payload kind " + mutationKind);
            };
        }

        private static List<String> safetyChecks(String mutationKind) {
            return switch (mutationKind) {
                case "world_dimension_transfer" -> List.of("server_lane_only", "destination_loaded", "chunk_ticketed", "landing_anchor_safe", "progression_receipt_required");
                case "entity_boss_spawn" -> List.of("server_lane_only", "boss_room_loaded", "single_encounter_instance", "attachment_receipt_required", "treasure_room_locked");
                case "screen_holomap_routes", "screen_screencore_launch_checklist", "screen_treasure_chest" -> List.of("client_lane_only", "screen_factory_registered", "renderer_bound", "widget_actions_bound", "state_sync_receipt_required");
                default -> throw new IllegalArgumentException("Unknown live-session mutation payload kind " + mutationKind);
            };
        }
    }

    public record HostMutationReceipt(
            String receiptId,
            String hostSinkId,
            boolean accepted,
            boolean mutatesMinecraftObjects,
            List<String> committedSteps,
            Map<String, Object> evidence
    ) {
        public HostMutationReceipt {
            receiptId = requireText(receiptId, "receiptId");
            hostSinkId = requireText(hostSinkId, "hostSinkId");
            committedSteps = List.copyOf(committedSteps == null ? List.of() : committedSteps);
            evidence = Map.copyOf(evidence == null ? Map.of() : evidence);
        }
    }

    public record LiveSessionMutationResult(
            boolean accepted,
            String status,
            LiveSessionMutationRequest request,
            HostMutationReceipt hostReceipt,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        public LiveSessionMutationResult {
            status = requireText(status, "status");
            request = Objects.requireNonNull(request, "request");
            hostReceipt = Objects.requireNonNull(hostReceipt, "hostReceipt");
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
