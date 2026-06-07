package com.knoxhack.echo.creatorcore.codex;

import java.util.List;

public record CodexBridgeStatus(
        boolean ok,
        String message,
        String bridge,
        String workspace,
        String codexPath,
        boolean codexAvailable,
        String codexError,
        boolean dryRun,
        String defaultModel,
        boolean authRequired,
        boolean repoEditsAllowed,
        int maxJobs,
        boolean commandTemplateConfigured,
        String defaultValidationProfile,
        List<String> diagnostics,
        int jobCount,
        int runningJobCount,
        List<String> profiles,
        List<String> validationProfiles,
        CodexJobSnapshot latestJob) {
    public CodexBridgeStatus {
        message = safe(message);
        bridge = safe(bridge);
        workspace = safe(workspace);
        codexPath = safe(codexPath);
        codexError = safe(codexError);
        defaultModel = safe(defaultModel);
        defaultValidationProfile = safe(defaultValidationProfile);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        validationProfiles = validationProfiles == null ? List.of() : List.copyOf(validationProfiles);
        latestJob = latestJob == null ? CodexJobSnapshot.empty("") : latestJob;
    }

    public static CodexBridgeStatus unavailable(String message) {
        return new CodexBridgeStatus(false, message, "Echo Codex Bridge", "", "", false, message,
                false, "", false, false, 0, false, "focused", List.of(safe(message)), 0, 0,
                CodexJobProfile.ids(), List.of("none", "mob_assets", "creatorcore",
                "rendercore", "focused", "full"), CodexJobSnapshot.empty(message));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
