package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLockfileVerificationResult(
        EchoPackId packId,
        String lockfilePath,
        boolean present,
        String schema,
        EchoLockfileVerificationStatus status,
        int issueCount,
        int blockingCount,
        int repairableCount,
        Map<String, Integer> driftCounts,
        Map<String, String> expected,
        Map<String, String> actual,
        List<EchoLockfileVerificationIssue> issues
) {
    public EchoLockfileVerificationResult {
        Objects.requireNonNull(packId, "packId");
        lockfilePath = PackContractGuards.requireText(lockfilePath, "lockfile path");
        schema = PackContractGuards.optionalText(schema);
        status = status == null ? EchoLockfileVerificationStatus.UNKNOWN : status;
        issueCount = Math.max(0, issueCount);
        blockingCount = Math.max(0, blockingCount);
        repairableCount = Math.max(0, repairableCount);
        driftCounts = driftCounts == null ? Map.of() : Map.copyOf(driftCounts);
        expected = PackContractGuards.immutableStringMap(expected);
        actual = PackContractGuards.immutableStringMap(actual);
        issues = PackContractGuards.immutableList(issues);
    }
}
