package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoRepairPlan(
        String id,
        EchoPackId packId,
        EchoPackVariantId variantId,
        EchoPackChannelId channelId,
        EchoRepairPlanStatus status,
        String summary,
        boolean requiresConfirmation,
        List<EchoRepairAction> actions,
        List<EchoRepairAction> automaticActions,
        List<EchoRepairAction> manualRecommendations,
        List<EchoDiagnostic> diagnostics,
        List<EchoRepairPlanIssue> issues,
        EchoRepairSafetyPolicy safetyPolicy,
        Map<String, String> attributes
) {
    public EchoRepairPlan {
        id = PackContractGuards.requireText(id, "repair plan id");
        Objects.requireNonNull(packId, "packId");
        status = status == null ? EchoRepairPlanStatus.UNKNOWN : status;
        summary = PackContractGuards.optionalText(summary);
        actions = PackContractGuards.immutableList(actions);
        automaticActions = PackContractGuards.immutableList(automaticActions);
        manualRecommendations = PackContractGuards.immutableList(manualRecommendations);
        diagnostics = PackContractGuards.immutableList(diagnostics);
        issues = PackContractGuards.immutableList(issues);
        safetyPolicy = safetyPolicy == null ? EchoRepairSafetyPolicy.planningOnly() : safetyPolicy;
        attributes = PackContractGuards.immutableStringMap(attributes);
        requiresConfirmation = requiresConfirmation
                || actions.stream().anyMatch(EchoRepairAction::requiresConfirmation)
                || automaticActions.stream().anyMatch(EchoRepairAction::requiresConfirmation)
                || manualRecommendations.stream().anyMatch(EchoRepairAction::requiresConfirmation);
    }

    public EchoRepairPlan(
            String id,
            EchoPackId packId,
            EchoPackVariantId variantId,
            EchoPackChannelId channelId,
            String summary,
            boolean requiresConfirmation,
            List<EchoRepairAction> actions,
            List<EchoDiagnostic> diagnostics
    ) {
        this(
                id,
                packId,
                variantId,
                channelId,
                EchoRepairPlanStatus.UNKNOWN,
                summary,
                requiresConfirmation,
                actions,
                List.of(),
                actions,
                diagnostics,
                List.of(),
                EchoRepairSafetyPolicy.planningOnly(),
                Map.of()
        );
    }
}
