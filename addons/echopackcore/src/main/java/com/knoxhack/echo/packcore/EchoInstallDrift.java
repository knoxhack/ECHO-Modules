package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoInstallDrift(
        List<String> missingModules,
        List<String> extraFiles,
        List<String> duplicateModules,
        List<String> staleFiles,
        List<String> checksumMismatchFiles,
        List<String> wrongVersionFiles,
        List<String> unknownFiles,
        List<String> nonEchoFiles,
        List<String> unreadableFiles
) {
    public EchoInstallDrift {
        missingModules = PackContractGuards.immutableList(missingModules);
        extraFiles = PackContractGuards.immutableList(extraFiles);
        duplicateModules = PackContractGuards.immutableList(duplicateModules);
        staleFiles = PackContractGuards.immutableList(staleFiles);
        checksumMismatchFiles = PackContractGuards.immutableList(checksumMismatchFiles);
        wrongVersionFiles = PackContractGuards.immutableList(wrongVersionFiles);
        unknownFiles = PackContractGuards.immutableList(unknownFiles);
        nonEchoFiles = PackContractGuards.immutableList(nonEchoFiles);
        unreadableFiles = PackContractGuards.immutableList(unreadableFiles);
    }
}
