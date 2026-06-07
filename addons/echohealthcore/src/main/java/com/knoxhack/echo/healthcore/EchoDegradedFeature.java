package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.validationcore.EchoAffectedFeature;
import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoDiagnosticCode;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoDegradedFeature(
        EchoFeatureId featureId,
        EchoModuleId providerModule,
        EchoHealthStatus status,
        String reason,
        String fallbackMode,
        boolean optional,
        Set<EchoRuntimeSide> sides,
        List<EchoDiagnostic> diagnostics
) {
    public EchoDegradedFeature {
        Objects.requireNonNull(featureId, "featureId");
        status = status == null ? EchoHealthStatus.DEGRADED : status;
        reason = HealthContractGuards.optionalText(reason);
        fallbackMode = HealthContractGuards.optionalText(fallbackMode);
        sides = sides == null || sides.isEmpty() ? Set.of(EchoRuntimeSide.COMMON) : Set.copyOf(sides);
        diagnostics = HealthContractGuards.immutableList(diagnostics);
    }

    public EchoDiagnostic toDiagnostic() {
        EchoDiagnosticSeverity severity = optional ? EchoDiagnosticSeverity.NOTICE : EchoDiagnosticSeverity.WARNING;
        return EchoDiagnostic.builder(
                        EchoDiagnosticCode.of("health.feature." + featureId.value()),
                        severity,
                        "Feature degraded: " + featureId.value(),
                        reason.isBlank() ? "A feature is running in degraded mode." : reason
                )
                .moduleId(providerModule)
                .affectedFeature(new EchoAffectedFeature(featureId, featureId.value(), EchoValidationCategory.RUNTIME_HEALTH))
                .category(EchoValidationCategory.RUNTIME_HEALTH)
                .developerDetails(fallbackMode.isBlank() ? "" : "Fallback mode: " + fallbackMode)
                .build();
    }
}
