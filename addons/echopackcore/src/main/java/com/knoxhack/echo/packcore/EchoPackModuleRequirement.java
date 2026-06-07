package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoPackModuleRequirement(
        EchoModuleId moduleId,
        boolean required,
        String versionRange,
        String reason,
        EchoPackVariantId variantId
) {
    public EchoPackModuleRequirement {
        Objects.requireNonNull(moduleId, "moduleId");
        versionRange = PackContractGuards.optionalText(versionRange);
        reason = PackContractGuards.optionalText(reason);
    }
}
