package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record EchoRuntimeHealthReport(
        String reportId,
        long createdAtEpochMillis,
        EchoPackId packId,
        String runtimeName,
        String runtimeVersion,
        EchoHealthStatus status,
        EchoHealthSnapshot snapshot,
        List<EchoModuleHealth> modules,
        List<EchoDiagnostic> diagnostics,
        EchoSupportBundleMetadata supportBundle,
        boolean localOnly,
        Map<String, String> attributes
) {
    public EchoRuntimeHealthReport {
        reportId = HealthContractGuards.requireText(reportId, "runtime health report id");
        createdAtEpochMillis = HealthContractGuards.nonNegative(createdAtEpochMillis, "report timestamp");
        runtimeName = HealthContractGuards.optionalText(runtimeName);
        runtimeVersion = HealthContractGuards.optionalText(runtimeVersion);
        snapshot = snapshot == null ? EchoHealthSnapshot.empty("empty") : snapshot;
        modules = HealthContractGuards.immutableList(modules);
        diagnostics = HealthContractGuards.immutableList(diagnostics);
        supportBundle = supportBundle == null ? EchoSupportBundleMetadata.none() : supportBundle;
        localOnly = true;
        attributes = HealthContractGuards.immutableMap(attributes);
        status = status == null ? deriveStatus(snapshot, modules) : status;
    }

    public List<EchoDiagnostic> allDiagnostics() {
        List<EchoDiagnostic> all = new ArrayList<>(diagnostics);
        all.addAll(snapshot.allDiagnostics());
        modules.stream().flatMap(module -> module.allDiagnostics().stream()).forEach(all::add);
        return List.copyOf(all);
    }

    private static EchoHealthStatus deriveStatus(EchoHealthSnapshot snapshot, List<EchoModuleHealth> modules) {
        List<EchoHealthStatus> statuses = new ArrayList<>();
        statuses.add(snapshot.status());
        modules.stream().map(EchoModuleHealth::status).forEach(statuses::add);
        return EchoHealthStatus.worst(statuses);
    }
}
