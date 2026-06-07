package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiCommandEnvironmentPolicy;
import com.knoxhack.echo.agentcore.EchoAiCommandRisk;
import com.knoxhack.echo.healthcore.EchoHealthStatus;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoBridgeCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoBridgeConstants.MOD_ID;
    public static final String SESSION_DATA_CONTRACT_ID = "echobridgecore:data/session_state_contract";
    public static final String SAFE_ACTION_DIAGNOSTIC_CONTRACT_ID = "echobridgecore:diagnostic/safe_action_gate";
    public static final String LOCAL_TRANSPORT_CONTRACT_ID = "echobridgecore:networking/local_transport_heartbeat";
    public static final List<String> CONTRACT_IDS = List.of(
            SESSION_DATA_CONTRACT_ID,
            SAFE_ACTION_DIAGNOSTIC_CONTRACT_ID,
            LOCAL_TRANSPORT_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "bridgecore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics", "networking"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("sessionDataRoundTrip", referenceProbe.get("sessionDataRoundTrip"));
        result.put("safeActionGateRoundTrip", referenceProbe.get("safeActionGateRoundTrip"));
        result.put("localTransportRoundTrip", referenceProbe.get("localTransportRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "BridgeCore native contract exercised session state, safe-action confirmation gates, and local transport heartbeat behavior.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoBridgeCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "bridgecore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "BridgeCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("sessionDataRoundTrip")),
                "BridgeCore native adapter should exercise session data behavior");
        require(Boolean.TRUE.equals(activation.get("safeActionGateRoundTrip")),
                "BridgeCore native adapter should exercise safe action diagnostic behavior");
        require(Boolean.TRUE.equals(activation.get("localTransportRoundTrip")),
                "BridgeCore native adapter should exercise local transport behavior");
        System.out.println("bridgecore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoBridgeSessionId sessionId = EchoBridgeSessionId.of("Dev-Bridge-01");
        EchoBridgeCapabilities capabilities = EchoBridgeCapabilities.contractOnly();
        EchoBridgeAgentStatus agentStatus = new EchoBridgeAgentStatus(
                sessionId,
                EchoHealthStatus.WARNING,
                null,
                null,
                EchoBridgeJobStatus.NEEDS_CONFIRMATION,
                0,
                1,
                true,
                false,
                " waiting ",
                null,
                42L
        );
        EchoBridgeSession session = new EchoBridgeSession(
                sessionId,
                " Dev Bridge ",
                null,
                capabilities,
                agentStatus,
                null,
                null,
                null,
                null,
                1L,
                2L,
                Map.of("mode", "local")
        );

        EchoBridgeCommand command = new EchoBridgeCommand(
                "inspect",
                "Inspect Workspace",
                List.of("git", "status"),
                " . ",
                EchoAiCommandRisk.MEDIUM,
                false,
                List.of("--short"),
                Duration.ofSeconds(30L),
                null,
                EchoAiCommandEnvironmentPolicy.CONFIRM_WRITE,
                false,
                " inspect ",
                " no execution "
        );
        EchoBridgeSafeActionRequest safeRequest = new EchoBridgeSafeActionRequest(
                "safe-1",
                sessionId,
                null,
                null,
                command,
                null,
                false,
                "codex",
                " confirm ",
                " details ",
                null,
                10L,
                20L
        );
        EchoBridgeSafeActionResult safeResult = new EchoBridgeSafeActionResult(
                "safe-1",
                null,
                EchoBridgeSafeActionStatus.BLOCKED,
                false,
                false,
                " blocked ",
                " confirmation missing ",
                null,
                21L
        );
        EchoBridgeEvent event = new EchoBridgeEvent(
                "event-1",
                EchoBridgeEventKind.LOG_CHUNK,
                sessionId,
                null,
                null,
                " event ",
                "",
                null,
                safeRequest,
                safeResult,
                null,
                null,
                22L
        );
        EchoBridgeControlResult controlResult = new EchoBridgeControlResult(
                "request-1",
                EchoBridgeControlAction.REQUEST_CONFIRMATION,
                EchoBridgeControlStatus.NEEDS_CONFIRMATION,
                session,
                null,
                List.of(event),
                null,
                null,
                null,
                null,
                null,
                " summary ",
                "",
                false,
                false,
                23L,
                24L,
                null
        );

        EchoBridgeTransportHint transport = new EchoBridgeTransportHint(
                EchoBridgeTransportKind.WEBSOCKET,
                " 0.0.0.0:9911 ",
                false,
                false,
                true,
                Duration.ofSeconds(2L),
                " setup ",
                Map.of("scope", "local")
        );
        EchoBridgeHeartbeat heartbeat = new EchoBridgeHeartbeat(
                sessionId,
                7L,
                50L,
                agentStatus,
                null,
                null,
                false,
                " alive ",
                null
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionDataRoundTrip", session.id().value().equals("dev-bridge-01")
                && session.side() == EchoRuntimeSide.DEV
                && session.capabilities().localOnly()
                && session.capabilities().maxInFlightJobs() == 1
                && session.displayName().equals("Dev Bridge")
                && session.requiresAttention()
                && session.activeJobs().isEmpty());
        result.put("safeActionGateRoundTrip", command.localOnly()
                && command.requiresConfirmation()
                && !command.executionBlockedByPolicy()
                && safeRequest.requiresConfirmation()
                && !safeRequest.expiredAt(19L)
                && safeRequest.expiredAt(20L)
                && safeResult.denied()
                && event.carriesConfirmationRequest()
                && controlResult.localOnly()
                && controlResult.redacted()
                && controlResult.requiresAttention());
        result.put("localTransportRoundTrip", transport.localOnly()
                && transport.kind() == EchoBridgeTransportKind.WEBSOCKET
                && transport.endpoint().equals("0.0.0.0:9911")
                && heartbeat.localOnly()
                && heartbeat.healthStatus() == EchoHealthStatus.WARNING
                && heartbeat.latestCursor().streamId().equals("dev-bridge-01.events")
                && heartbeat.latestCursor().offset() == 0L);
        result.put("normalizedSessionId", session.id().value());
        result.put("requiresConfirmation", command.requiresConfirmation());
        result.put("safeActionExpiredAt20", safeRequest.expiredAt(20L));
        result.put("controlRedacted", controlResult.redacted());
        result.put("heartbeatCursor", heartbeat.latestCursor().streamId());
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
