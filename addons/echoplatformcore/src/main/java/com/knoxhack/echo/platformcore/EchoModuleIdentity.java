package com.knoxhack.echo.platformcore;

import java.util.Objects;
import java.util.Set;

public record EchoModuleIdentity(
        EchoModuleId id,
        EchoModuleName name,
        EchoModuleVersion version,
        EchoModuleKind kind,
        EchoModuleRole role,
        EchoRuntimeSide side,
        EchoApiStability apiStability,
        EchoTrustLevel trustLevel,
        boolean official,
        boolean standalone,
        Set<EchoFeatureId> providedFeatures,
        Set<EchoFeatureRequirement> consumedFeatures,
        EchoPermissionSet permissions
) {
    public EchoModuleIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        kind = kind == null ? EchoModuleKind.ADDON : kind;
        role = role == null ? EchoModuleRole.CONTENT_EXPANSION : role;
        side = side == null ? EchoRuntimeSide.COMMON : side;
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        trustLevel = trustLevel == null ? EchoTrustLevel.EXPERIMENTAL : trustLevel;
        providedFeatures = EchoContractGuards.immutableSet(providedFeatures);
        consumedFeatures = EchoContractGuards.immutableSet(consumedFeatures);
        permissions = permissions == null ? EchoPermissionSet.empty() : permissions;
    }
}
