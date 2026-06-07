package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoInstalledModule(
        EchoJarFingerprint fingerprint,
        String moduleId,
        String version,
        String expectedVersion,
        String matchKind,
        String status,
        List<EchoInstallStateIssue> issues
) {
    public EchoInstalledModule {
        moduleId = PackContractGuards.optionalText(moduleId);
        version = PackContractGuards.optionalText(version);
        expectedVersion = PackContractGuards.optionalText(expectedVersion);
        matchKind = PackContractGuards.optionalText(matchKind);
        status = PackContractGuards.requireText(status, "installed module status");
        issues = PackContractGuards.immutableList(issues);
    }
}
