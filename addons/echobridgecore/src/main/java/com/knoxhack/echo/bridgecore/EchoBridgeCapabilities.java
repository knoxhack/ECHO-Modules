package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiAgentLane;
import com.knoxhack.echo.agentcore.EchoAiCommandRisk;

import java.util.Map;
import java.util.Set;

public record EchoBridgeCapabilities(
        String bridgeName,
        String bridgeVersion,
        boolean localOnly,
        boolean supportsStreamingLogs,
        boolean supportsJobCancellation,
        boolean supportsPromptSubmission,
        boolean supportsSafeActionRequests,
        boolean supportsConfirmationRequests,
        boolean supportsHeartbeat,
        boolean supportsDiagnostics,
        boolean supportsNextPhasePrompt,
        int maxInFlightJobs,
        Set<EchoBridgeTransportKind> supportedTransports,
        Set<EchoAiAgentLane> supportedAgentLanes,
        Set<EchoAiCommandRisk> acceptedCommandRisks,
        Map<String, String> attributes
) {
    public EchoBridgeCapabilities {
        bridgeName = BridgeContractGuards.optionalText(bridgeName);
        bridgeVersion = BridgeContractGuards.optionalText(bridgeVersion);
        localOnly = true;
        maxInFlightJobs = BridgeContractGuards.nonNegative(maxInFlightJobs, "max in-flight bridge jobs");
        supportedTransports = supportedTransports == null || supportedTransports.isEmpty()
                ? Set.of(EchoBridgeTransportKind.LOCAL_LOOPBACK)
                : BridgeContractGuards.immutableSet(supportedTransports);
        supportedAgentLanes = BridgeContractGuards.immutableSet(supportedAgentLanes);
        acceptedCommandRisks = acceptedCommandRisks == null || acceptedCommandRisks.isEmpty()
                ? Set.of(EchoAiCommandRisk.INFORMATIONAL, EchoAiCommandRisk.LOW)
                : BridgeContractGuards.immutableSet(acceptedCommandRisks);
        attributes = BridgeContractGuards.immutableMap(attributes);
    }

    public static EchoBridgeCapabilities contractOnly() {
        return new EchoBridgeCapabilities(
                "ECHO BridgeCore",
                EchoBridgeConstants.CONTRACT_VERSION,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                1,
                Set.of(EchoBridgeTransportKind.LOCAL_LOOPBACK, EchoBridgeTransportKind.STDIO),
                Set.of(),
                Set.of(EchoAiCommandRisk.INFORMATIONAL, EchoAiCommandRisk.LOW),
                Map.of()
        );
    }

    public boolean accepts(EchoBridgeCommand command) {
        return command != null
                && localOnly
                && command.localOnly()
                && acceptedCommandRisks.contains(command.risk())
                && supportedTransports.stream().anyMatch(kind -> kind != EchoBridgeTransportKind.UNKNOWN);
    }
}
