package com.knoxhack.echotextureforge.common.export;

import java.time.Instant;
import java.util.List;

public record TextureApplyResult(
        Instant generatedAt,
        boolean dryRun,
        boolean overwriteApproved,
        String modidFilter,
        int copied,
        int skipped,
        int conflicts,
        List<String> backupFiles,
        List<TextureApplyAction> actions) {
    public TextureApplyResult {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        modidFilter = modidFilter == null ? "" : modidFilter;
        backupFiles = backupFiles == null ? List.of() : List.copyOf(backupFiles);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
