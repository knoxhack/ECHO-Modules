package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Map;

public record EchoSupportBundleMetadata(
        String bundleId,
        long createdAtEpochMillis,
        EchoPackId packId,
        List<EchoModuleId> includedModules,
        List<String> includedReportKinds,
        boolean includesLogs,
        boolean includesCrashReports,
        boolean secretsRedacted,
        String privacyNote,
        Map<String, String> attributes
) {
    public EchoSupportBundleMetadata {
        bundleId = HealthContractGuards.requireText(bundleId, "support bundle id");
        createdAtEpochMillis = HealthContractGuards.nonNegative(createdAtEpochMillis, "support bundle timestamp");
        includedModules = HealthContractGuards.immutableList(includedModules);
        includedReportKinds = HealthContractGuards.immutableList(includedReportKinds);
        secretsRedacted = true;
        privacyNote = HealthContractGuards.optionalText(privacyNote);
        attributes = HealthContractGuards.immutableMap(attributes);
    }

    public static EchoSupportBundleMetadata none() {
        return new EchoSupportBundleMetadata("none", 0L, null, List.of(), List.of(), false, false, true, "", Map.of());
    }
}
