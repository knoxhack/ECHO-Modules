package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Map;

public record WorldCoreValidationReport(
        int regionDefinitions,
        int dataRegionDefinitions,
        int hazardDefinitions,
        int dataHazardDefinitions,
        int markerCount,
        Map<String, Integer> regionSourceCounts,
        Map<String, Integer> hazardSourceCounts,
        List<WorldCoreValidationIssue> issues,
        List<String> warnings) {
    public WorldCoreValidationReport {
        regionSourceCounts = regionSourceCounts == null ? Map.of() : Map.copyOf(regionSourceCounts);
        hazardSourceCounts = hazardSourceCounts == null ? Map.of() : Map.copyOf(hazardSourceCounts);
        issues = issues == null ? List.of() : List.copyOf(issues);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public List<String> messages() {
        if (!warnings.isEmpty()) {
            return warnings;
        }
        return issues.stream().map(WorldCoreValidationIssue::message).toList();
    }

    public boolean valid() {
        return errorCount() == 0;
    }

    public int regionDefinitionCount() {
        return regionDefinitions;
    }

    public int dataRegionDefinitionCount() {
        return dataRegionDefinitions;
    }

    public int hazardDefinitionCount() {
        return hazardDefinitions;
    }

    public int dataHazardDefinitionCount() {
        return dataHazardDefinitions;
    }

    public int warningCount() {
        return warnings.size() + (int) issues.stream()
                .filter(issue -> issue.severity() == WorldCoreValidationIssue.Severity.WARNING)
                .count();
    }

    public int errorCount() {
        return (int) issues.stream()
                .filter(issue -> issue.severity() == WorldCoreValidationIssue.Severity.ERROR)
                .count();
    }

    public List<String> reloadWarnings() {
        return warnings;
    }
}
