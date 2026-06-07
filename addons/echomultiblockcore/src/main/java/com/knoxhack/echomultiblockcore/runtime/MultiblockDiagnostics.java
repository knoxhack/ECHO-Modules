package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import java.util.ArrayList;
import java.util.List;

public final class MultiblockDiagnostics {
    private MultiblockDiagnostics() {
    }

    public static List<String> lines(String state, MultiblockDefinition definition, ValidationResult result,
            float integrity, int robotCount, String taskStatusLine, String taskBlockedReason) {
        List<String> lines = new ArrayList<>();
        lines.add("STRUCTURE STATUS: " + (result.valid() ? state : "INCOMPLETE"));
        lines.add("Definition: " + (definition == null ? String.valueOf(result.definitionId()) : definition.displayName()));
        lines.add("Completion: " + Math.round(result.completion() * 100.0D) + "%");
        if (result.valid()) {
            lines.add("Frame Integrity: " + Math.round(integrity <= 0.0F ? 100.0F : integrity) + "%");
            lines.add("Robotics: " + robotCount + " arm(s)");
            lines.add("Task Queue: " + taskStatusLine);
        }
        if (!result.groupedIssues().isEmpty()) {
            lines.add("Issues:");
            result.groupedIssues().stream().limit(10).forEach(issue ->
                    lines.add("- " + issue.count() + "x " + issue.kind() + " expected " + issue.expected()
                            + " found " + issue.found() + " near " + issue.firstPos().toShortString()));
        }
        if (taskBlockedReason != null && !taskBlockedReason.isBlank()) {
            lines.add("Task Diagnostic: " + taskBlockedReason);
        }
        result.unloadedAreaWarnings().forEach(warning -> lines.add("Unloaded: " + warning));
        result.warnings().forEach(warning -> lines.add("Warning: " + warning));
        result.errors().forEach(error -> lines.add("Error: " + error));
        return lines;
    }
}
