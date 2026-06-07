package com.knoxhack.echo.creatorcore.codex;

import java.util.List;

public record CodexJobSnapshot(
        String id,
        String profile,
        String prompt,
        String module,
        String model,
        String state,
        String stdoutSummary,
        String error,
        List<String> changedFiles,
        List<String> repoStatusBefore,
        List<String> repoStatusAfter,
        String validationProfile,
        String validationStatus,
        List<String> validationLines,
        String commandLine) {
    public CodexJobSnapshot {
        id = safe(id);
        profile = safe(profile);
        prompt = safe(prompt);
        module = safe(module);
        model = safe(model);
        state = state == null || state.isBlank() ? "unknown" : state;
        stdoutSummary = safe(stdoutSummary);
        error = safe(error);
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
        repoStatusBefore = repoStatusBefore == null ? List.of() : List.copyOf(repoStatusBefore);
        repoStatusAfter = repoStatusAfter == null ? List.of() : List.copyOf(repoStatusAfter);
        validationProfile = safe(validationProfile);
        validationStatus = validationStatus == null || validationStatus.isBlank() ? "unknown" : validationStatus;
        validationLines = validationLines == null ? List.of() : List.copyOf(validationLines);
        commandLine = safe(commandLine);
    }

    public static CodexJobSnapshot empty(String message) {
        return new CodexJobSnapshot("", "", "", "", "", "unavailable", "", message,
                List.of(), List.of(), List.of(), "", "unavailable", List.of(), "");
    }

    public boolean hasJob() {
        return !id.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
