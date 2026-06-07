package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EchoPackSnapshot(
        String id,
        EchoPackId packId,
        EchoSnapshotKind kind,
        Instant createdAt,
        String sourceProfile,
        String lockfileDigest,
        String saveId,
        List<String> includedPaths,
        String summary
) {
    public EchoPackSnapshot {
        id = PackContractGuards.requireText(id, "snapshot id");
        Objects.requireNonNull(packId, "packId");
        kind = kind == null ? EchoSnapshotKind.USER_BACKUP : kind;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        sourceProfile = PackContractGuards.optionalText(sourceProfile);
        lockfileDigest = PackContractGuards.optionalText(lockfileDigest);
        saveId = PackContractGuards.optionalText(saveId);
        includedPaths = PackContractGuards.immutableList(includedPaths);
        summary = PackContractGuards.optionalText(summary);
    }
}
