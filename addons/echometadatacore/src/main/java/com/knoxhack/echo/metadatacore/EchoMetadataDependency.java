package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoMetadataDependency(
        EchoModuleId moduleId,
        boolean required,
        String versionRange,
        String reason,
        EchoApiStability minimumStability
) {
    public EchoMetadataDependency {
        Objects.requireNonNull(moduleId, "moduleId");
        versionRange = MetadataContractGuards.optionalText(versionRange);
        reason = MetadataContractGuards.optionalText(reason);
        minimumStability = minimumStability == null ? EchoApiStability.EXPERIMENTAL : minimumStability;
    }

    public static EchoMetadataDependency required(EchoModuleId moduleId, String versionRange, String reason) {
        return new EchoMetadataDependency(moduleId, true, versionRange, reason, EchoApiStability.EXPERIMENTAL);
    }

    public static EchoMetadataDependency optional(EchoModuleId moduleId, String versionRange, String reason) {
        return new EchoMetadataDependency(moduleId, false, versionRange, reason, EchoApiStability.EXPERIMENTAL);
    }
}
