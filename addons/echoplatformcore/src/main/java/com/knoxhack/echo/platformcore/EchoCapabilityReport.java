package com.knoxhack.echo.platformcore;

import java.util.Objects;
import java.util.Set;

public record EchoCapabilityReport(
        EchoModuleId moduleId,
        EchoCapabilitySet declaredCapabilities,
        EchoCapabilitySet availableCapabilities,
        Set<EchoCapabilityId> missingCapabilities,
        Set<EchoPlatformDiagnostic> diagnostics
) {
    public EchoCapabilityReport {
        Objects.requireNonNull(moduleId, "moduleId");
        declaredCapabilities = declaredCapabilities == null ? EchoCapabilitySet.empty() : declaredCapabilities;
        availableCapabilities = availableCapabilities == null ? EchoCapabilitySet.empty() : availableCapabilities;
        missingCapabilities = EchoContractGuards.immutableSet(missingCapabilities);
        diagnostics = EchoContractGuards.immutableSet(diagnostics);
    }
}
