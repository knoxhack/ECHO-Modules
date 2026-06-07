package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.modulegraph.EchoModuleRoleIndex;
import com.knoxhack.echo.modulegraph.EchoRoleConflict;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoRoleGraphReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoModuleRoleIndex roleIndex,
        List<EchoRoleConflict> conflicts,
        Set<EchoModuleRole> exclusiveRolePolicy,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoRoleGraphReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.ROLE_GRAPH) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        roleIndex = roleIndex == null ? new EchoModuleRoleIndex(Map.of()) : roleIndex;
        conflicts = ReportContractGuards.immutableList(conflicts);
        exclusiveRolePolicy = exclusiveRolePolicy == null ? Set.of() : Set.copyOf(exclusiveRolePolicy);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean valid() {
        return conflicts.stream().noneMatch(EchoRoleConflict::blocking)
                && diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }
}
