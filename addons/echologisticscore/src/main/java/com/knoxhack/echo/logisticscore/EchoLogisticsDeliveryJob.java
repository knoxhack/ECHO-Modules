package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLogisticsDeliveryJob(
        EchoLogisticsDeliveryJobId id,
        EchoLogisticsDeliveryState state,
        EchoLogisticsNodeId fromNode,
        EchoLogisticsNodeId toNode,
        List<EchoContentReference> payloadReferences,
        EchoConvoyIntegrationRef convoyIntegration,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoLogisticsDeliveryJob {
        Objects.requireNonNull(id, "id");
        state = state == null ? EchoLogisticsDeliveryState.UNKNOWN : state;
        payloadReferences = LogisticsContractGuards.immutableList(payloadReferences);
        diagnostics = LogisticsContractGuards.immutableList(diagnostics);
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }

    public boolean blocked() {
        return state == EchoLogisticsDeliveryState.FAILED || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
