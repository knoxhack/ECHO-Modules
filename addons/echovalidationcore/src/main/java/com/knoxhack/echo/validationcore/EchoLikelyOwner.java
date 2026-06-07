package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoLikelyOwner(
        EchoModuleId moduleId,
        String displayName,
        String contactHint,
        String suggestedAgentLane
) {
    public EchoLikelyOwner {
        Objects.requireNonNull(moduleId, "moduleId");
        displayName = displayName == null || displayName.isBlank() ? moduleId.value() : displayName.trim();
        contactHint = ValidationContractGuards.optionalText(contactHint);
        suggestedAgentLane = ValidationContractGuards.optionalText(suggestedAgentLane);
    }

    public static EchoLikelyOwner module(EchoModuleId moduleId) {
        return new EchoLikelyOwner(moduleId, "", "", "");
    }
}
