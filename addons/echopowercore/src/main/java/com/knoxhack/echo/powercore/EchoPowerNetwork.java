package com.knoxhack.echo.powercore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoPowerNetwork(
        EchoPowerNetworkId id,
        EchoModuleId ownerModule,
        List<EchoPowerNode> nodes,
        List<EchoPowerInstability> instability,
        EchoPowerIntegrationRefs integrationRefs,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoPowerNetwork {
        Objects.requireNonNull(id, "id");
        nodes = PowerContractGuards.immutableList(nodes);
        instability = PowerContractGuards.immutableList(instability);
        diagnostics = PowerContractGuards.immutableList(diagnostics);
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return instability.stream().anyMatch(EchoPowerInstability::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
