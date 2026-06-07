package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoRepairRollbackAction(
        String id,
        EchoRepairActionKind kind,
        String summary,
        EchoRepairActionRisk risk,
        boolean requiresConfirmation,
        List<String> affectedFiles,
        List<String> safeCommands
) {
    public EchoRepairRollbackAction {
        id = PackContractGuards.requireText(id, "repair rollback action id");
        kind = kind == null ? EchoRepairActionKind.MANUAL_REVIEW : kind;
        summary = PackContractGuards.optionalText(summary);
        risk = risk == null ? EchoRepairActionRisk.UNKNOWN : risk;
        affectedFiles = PackContractGuards.immutableList(affectedFiles);
        safeCommands = PackContractGuards.immutableList(safeCommands);
    }
}
