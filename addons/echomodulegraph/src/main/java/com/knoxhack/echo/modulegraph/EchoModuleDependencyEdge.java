package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoModuleDependencyEdge(
        EchoModuleId moduleId,
        EchoModuleId requiredModuleId,
        String versionRange,
        boolean present,
        String status,
        String reason
) {
    public EchoModuleDependencyEdge {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(requiredModuleId, "requiredModuleId");
        versionRange = ModuleGraphContractGuards.optionalText(versionRange);
        status = status == null || status.isBlank() ? "unknown" : status.trim();
        reason = ModuleGraphContractGuards.optionalText(reason);
    }

    public static EchoModuleDependencyEdge from(EchoModuleId moduleId, EchoMetadataDependency dependency, boolean present, String status) {
        Objects.requireNonNull(dependency, "dependency");
        return new EchoModuleDependencyEdge(moduleId, dependency.moduleId(), dependency.versionRange(), present, status, dependency.reason());
    }

    public EchoModuleGraphEdge toGraphEdge() {
        return new EchoModuleGraphEdge(
                moduleId,
                requiredModuleId,
                EchoModuleGraphEdgeKind.REQUIRES,
                null,
                versionRange,
                true,
                present,
                status,
                reason
        );
    }
}
