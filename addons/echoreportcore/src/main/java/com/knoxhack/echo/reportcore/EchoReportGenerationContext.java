package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoReportGenerationContext(
        EchoPackId packId,
        String addonSet,
        String minecraftVersion,
        String loaderName,
        String loaderVersion,
        long generatedAtEpochMillis,
        String sourceRevision,
        List<EchoModuleId> includedModules,
        Set<EchoReportAudience> audiences,
        EchoReportRedactionPolicy redactionPolicy,
        Map<String, String> attributes
) {
    public EchoReportGenerationContext {
        addonSet = ReportContractGuards.optionalText(addonSet);
        minecraftVersion = ReportContractGuards.optionalText(minecraftVersion);
        loaderName = ReportContractGuards.optionalText(loaderName);
        loaderVersion = ReportContractGuards.optionalText(loaderVersion);
        generatedAtEpochMillis = ReportContractGuards.nonNegative(generatedAtEpochMillis, "report generation timestamp");
        sourceRevision = ReportContractGuards.optionalText(sourceRevision);
        includedModules = ReportContractGuards.immutableList(includedModules);
        audiences = ReportContractGuards.immutableSet(audiences);
        redactionPolicy = redactionPolicy == null ? EchoReportRedactionPolicy.localDefault() : redactionPolicy;
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportGenerationContext empty() {
        return new EchoReportGenerationContext(null, "", "", "", "", 0L, "", List.of(), Set.of(), EchoReportRedactionPolicy.localDefault(), Map.of());
    }
}
