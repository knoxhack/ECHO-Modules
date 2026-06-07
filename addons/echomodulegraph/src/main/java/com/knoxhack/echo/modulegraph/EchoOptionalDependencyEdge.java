package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoOptionalDependencyEdge(
        EchoModuleId moduleId,
        EchoModuleId optionalModuleId,
        String versionRange,
        boolean present,
        String status,
        String reason
) {
    public EchoOptionalDependencyEdge {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(optionalModuleId, "optionalModuleId");
        versionRange = ModuleGraphContractGuards.optionalText(versionRange);
        status = status == null || status.isBlank() ? "unknown" : status.trim();
        reason = ModuleGraphContractGuards.optionalText(reason);
    }

    public static EchoOptionalDependencyEdge from(EchoModuleId moduleId, EchoMetadataDependency dependency, boolean present, String status) {
        Objects.requireNonNull(dependency, "dependency");
        return new EchoOptionalDependencyEdge(moduleId, dependency.moduleId(), dependency.versionRange(), present, status, dependency.reason());
    }

    public EchoModuleGraphEdge toGraphEdge() {
        return new EchoModuleGraphEdge(
                moduleId,
                optionalModuleId,
                EchoModuleGraphEdgeKind.OPTIONAL,
                null,
                versionRange,
                false,
                present,
                status,
                reason
        );
    }
}
