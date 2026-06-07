package com.knoxhack.echotextureforge.api.report;

import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TextureAuditReport(
        Instant generatedAt,
        Path workspaceRoot,
        Path outputRoot,
        int totalScannedAddons,
        int totalRegisteredItems,
        int totalRegisteredBlocks,
        int totalSpecs,
        int totalTextures,
        int totalItemModels,
        int totalBlockModels,
        int totalBlockstates,
        int missingTextures,
        int missingModels,
        int missingBlockstates,
        int missingLangKeys,
        int wrongSizeTextures,
        int unusedTextures,
        Map<TextureAuditSeverity, Integer> severitySummary,
        List<TextureAuditIssue> issues,
        List<TextureSpec> specs,
        List<Path> promptFiles,
        List<Path> reportFiles) {
    public TextureAuditReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        severitySummary = severitySummary == null ? Map.of() : Map.copyOf(severitySummary);
        issues = issues == null ? List.of() : List.copyOf(issues);
        specs = specs == null ? List.of() : List.copyOf(specs);
        promptFiles = promptFiles == null ? List.of() : List.copyOf(promptFiles);
        reportFiles = reportFiles == null ? List.of() : List.copyOf(reportFiles);
    }

    public List<TextureAuditIssue> issues(TextureAuditSeverity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).toList();
    }
}
