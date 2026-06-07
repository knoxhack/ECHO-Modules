package com.knoxhack.echo.validationcore;

import java.util.List;

public record EchoRepairSuggestion(
        String id,
        String label,
        String summary,
        String risk,
        boolean requiresConfirmation,
        List<String> actions,
        List<String> relatedDocs
) {
    public EchoRepairSuggestion {
        id = ValidationContractGuards.requireText(id, "repair suggestion id");
        label = ValidationContractGuards.requireText(label, "repair suggestion label");
        summary = ValidationContractGuards.optionalText(summary);
        risk = ValidationContractGuards.optionalText(risk);
        actions = ValidationContractGuards.immutableList(actions);
        relatedDocs = ValidationContractGuards.immutableList(relatedDocs);
    }

    public static EchoRepairSuggestion manual(String id, String label, String summary) {
        return new EchoRepairSuggestion(id, label, summary, "manual_review", true, List.of(), List.of());
    }
}
