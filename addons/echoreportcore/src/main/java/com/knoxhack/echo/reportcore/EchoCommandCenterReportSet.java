package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCommandCenterReportSet(
        EchoReportGenerationContext context,
        EchoLauncherStatusReport launcherStatus,
        EchoPackReadinessReport packReadiness,
        EchoModuleGraphReport moduleGraph,
        EchoFeatureGraphReport featureGraph,
        EchoDiagnosticsReport diagnostics,
        EchoHealthReport health,
        EchoRepairPlanReport repairPlan,
        EchoAiTaskReport aiTasks,
        EchoMissingAssetsReport missingAssets,
        EchoSupportBundleManifest supportBundle,
        EchoCompatibilityMatrixReport compatibilityMatrix,
        EchoReleaseReadinessReport releaseReadiness,
        List<EchoReportDescriptor> descriptors,
        List<EchoReportArtifact> artifacts,
        List<EchoDiagnostic> reportDiagnostics,
        Map<String, String> attributes
) {
    public EchoCommandCenterReportSet {
        context = context == null ? EchoReportGenerationContext.empty() : context;
        descriptors = descriptors == null || descriptors.isEmpty()
                ? EchoReportConstants.DEFAULT_REPORT_DESCRIPTORS
                : ReportContractGuards.immutableList(descriptors);
        artifacts = artifacts == null || artifacts.isEmpty()
                ? EchoReportConstants.DEFAULT_REPORT_ARTIFACTS
                : ReportContractGuards.immutableList(artifacts);
        reportDiagnostics = ReportContractGuards.immutableList(reportDiagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return reportDiagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || releaseReadiness != null && !releaseReadiness.releasable();
    }
}
