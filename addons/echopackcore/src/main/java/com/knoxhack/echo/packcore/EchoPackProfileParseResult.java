package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoPackProfileParseResult(
        EchoPackId requestedPackId,
        EchoPackProfileSource source,
        Map<String, Object> normalizedProfile,
        EchoPackProfile profile,
        EchoPackProfileStatus status,
        List<EchoPackProfileIssue> issues
) {
    public EchoPackProfileParseResult {
        Objects.requireNonNull(requestedPackId, "requestedPackId");
        source = source == null ? EchoPackProfileSource.builtInDefault() : source;
        normalizedProfile = PackContractGuards.immutableMap(normalizedProfile);
        status = status == null ? EchoPackProfileStatus.UNKNOWN : status;
        issues = PackContractGuards.immutableList(issues);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(EchoPackProfileIssue::blocking);
    }
}
