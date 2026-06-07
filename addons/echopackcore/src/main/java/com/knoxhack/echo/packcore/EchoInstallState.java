package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Objects;

public record EchoInstallState(
        EchoPackId packId,
        EchoInstallStateStatus status,
        EchoManagedTarget managedTarget,
        List<EchoLockedModule> expectedModules,
        EchoJarInventory jarInventory,
        EchoInstallDrift drift,
        List<EchoInstallStateIssue> issues
) {
    public EchoInstallState {
        Objects.requireNonNull(packId, "packId");
        status = status == null ? EchoInstallStateStatus.UNKNOWN : status;
        expectedModules = PackContractGuards.immutableList(expectedModules);
        issues = PackContractGuards.immutableList(issues);
    }
}
