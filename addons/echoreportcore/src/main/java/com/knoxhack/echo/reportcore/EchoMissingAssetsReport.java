package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.assetcore.EchoMissingAssetDiagnostic;
import com.knoxhack.echo.assetcore.EchoNeededAssetList;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoMissingAssetsReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoNeededAssetList> neededAssetLists,
        List<EchoMissingAssetDiagnostic> missingAssets,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoMissingAssetsReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.MISSING_ASSETS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        neededAssetLists = ReportContractGuards.immutableList(neededAssetLists);
        missingAssets = ReportContractGuards.immutableList(missingAssets);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return missingAssets.stream().anyMatch(EchoMissingAssetDiagnostic::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
