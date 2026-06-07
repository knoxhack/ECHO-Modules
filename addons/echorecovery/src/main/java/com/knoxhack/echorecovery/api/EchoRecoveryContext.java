package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.modulegraph.EchoModuleGraph;
import com.knoxhack.echo.packcore.EchoPackProfile;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoRecoveryContext(
        String id,
        long createdAtEpochMillis,
        EchoPackId packId,
        EchoPackProfile packProfile,
        EchoRuntimeHealthReport healthReport,
        EchoModuleGraph moduleGraph,
        List<EchoRecoveryTrigger> triggers,
        List<EchoModuleId> affectedModules,
        List<EchoFeatureId> affectedFeatures,
        List<EchoDiagnostic> diagnostics,
        List<String> recentSafeActions,
        List<String> likelyFiles,
        Map<String, String> attributes
) {
    public EchoRecoveryContext {
        id = RecoveryContractGuards.requireText(id, "recovery context id");
        createdAtEpochMillis = RecoveryContractGuards.nonNegative(createdAtEpochMillis, "recovery context timestamp");
        triggers = RecoveryContractGuards.immutableList(triggers);
        affectedModules = RecoveryContractGuards.immutableList(affectedModules);
        affectedFeatures = RecoveryContractGuards.immutableList(affectedFeatures);
        diagnostics = RecoveryContractGuards.immutableList(diagnostics);
        recentSafeActions = RecoveryContractGuards.immutableList(recentSafeActions);
        likelyFiles = RecoveryContractGuards.immutableList(likelyFiles);
        attributes = RecoveryContractGuards.immutableMap(attributes);
    }

    public static EchoRecoveryContext empty(String id) {
        return new EchoRecoveryContext(id, 0L, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }
}
