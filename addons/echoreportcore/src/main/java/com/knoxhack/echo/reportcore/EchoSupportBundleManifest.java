package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.healthcore.EchoSupportBundleMetadata;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoSupportBundleManifest(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoSupportBundleMetadata healthMetadata,
        List<EchoReportArtifact> includedArtifacts,
        List<EchoDiagnostic> diagnostics,
        boolean localOnly,
        boolean secretsRedacted,
        Map<String, String> attributes
) {
    public EchoSupportBundleManifest {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.SUPPORT_BUNDLE) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        healthMetadata = healthMetadata == null ? EchoSupportBundleMetadata.none() : healthMetadata;
        includedArtifacts = ReportContractGuards.immutableList(includedArtifacts);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        localOnly = true;
        secretsRedacted = true;
        attributes = ReportContractGuards.immutableMap(attributes);
    }
}
