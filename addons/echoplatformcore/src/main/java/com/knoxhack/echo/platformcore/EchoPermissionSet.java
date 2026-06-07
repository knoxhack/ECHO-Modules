package com.knoxhack.echo.platformcore;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record EchoPermissionSet(Set<EchoPermission> permissions) {
    public EchoPermissionSet {
        permissions = EchoContractGuards.immutableSet(permissions);
    }

    public static EchoPermissionSet empty() {
        return new EchoPermissionSet(Set.of());
    }

    public static EchoPermissionSet of(EchoPermission... permissions) {
        return new EchoPermissionSet(Arrays.stream(permissions).collect(Collectors.toUnmodifiableSet()));
    }

    public boolean contains(EchoPermission permission) {
        return permissions.contains(permission);
    }
}
