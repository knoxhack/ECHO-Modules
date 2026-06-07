package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoRepairAction(
        String id,
        EchoRepairActionKind kind,
        String label,
        String target,
        String moduleId,
        String packId,
        String summary,
        String reason,
        EchoRepairActionRisk risk,
        boolean requiresConfirmation,
        boolean requiresBackup,
        boolean safeToAutomate,
        String estimatedImpact,
        List<String> affectedPaths,
        List<String> safeCommandRefs,
        List<String> relatedDiagnostics,
        List<EchoRepairActionRequirement> requirements,
        List<EchoRepairCommandPreview> commandPreviews,
        EchoRepairRollbackAction rollbackAction
) {
    public EchoRepairAction {
        id = PackContractGuards.requireText(id, "repair action id");
        kind = kind == null ? EchoRepairActionKind.EXPORT_SUPPORT_BUNDLE : kind;
        label = PackContractGuards.requireText(label, "repair action label");
        target = PackContractGuards.optionalText(target);
        moduleId = PackContractGuards.optionalText(moduleId);
        packId = PackContractGuards.optionalText(packId);
        summary = PackContractGuards.optionalText(summary);
        reason = PackContractGuards.optionalText(reason);
        risk = risk == null ? EchoRepairActionRisk.UNKNOWN : risk;
        estimatedImpact = PackContractGuards.optionalText(estimatedImpact);
        affectedPaths = PackContractGuards.immutableList(affectedPaths);
        safeCommandRefs = PackContractGuards.immutableList(safeCommandRefs);
        relatedDiagnostics = PackContractGuards.immutableList(relatedDiagnostics);
        requirements = PackContractGuards.immutableList(requirements);
        commandPreviews = PackContractGuards.immutableList(commandPreviews);
        requiresConfirmation = requiresConfirmation || risk.requiresReview();
        safeToAutomate = safeToAutomate && !requiresConfirmation && risk == EchoRepairActionRisk.NONE;
    }

    public EchoRepairAction(
            String id,
            EchoRepairActionKind kind,
            String label,
            String summary,
            String risk,
            boolean requiresConfirmation,
            List<String> affectedPaths,
            List<String> safeCommandRefs
    ) {
        this(
                id,
                kind,
                label,
                "",
                "",
                "",
                summary,
                "",
                parseRisk(risk),
                requiresConfirmation,
                false,
                false,
                "",
                affectedPaths,
                safeCommandRefs,
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    private static EchoRepairActionRisk parseRisk(String value) {
        if (value == null || value.isBlank()) {
            return EchoRepairActionRisk.UNKNOWN;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        try {
            return EchoRepairActionRisk.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return EchoRepairActionRisk.UNKNOWN;
        }
    }
}
