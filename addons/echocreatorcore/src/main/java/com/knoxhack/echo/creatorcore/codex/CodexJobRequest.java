package com.knoxhack.echo.creatorcore.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public record CodexJobRequest(
        String profile,
        String prompt,
        String module,
        List<String> allowedFiles,
        List<String> captureIds,
        boolean useLatestCapture,
        String validationProfile,
        String model,
        String workspaceRoot,
        boolean allowRepoEdits) {
    public CodexJobRequest {
        profile = safe(profile, "asset_repair");
        prompt = safe(prompt, "");
        module = safe(module, "");
        allowedFiles = allowedFiles == null ? List.of() : List.copyOf(allowedFiles);
        captureIds = captureIds == null ? List.of() : List.copyOf(captureIds);
        validationProfile = safe(validationProfile, "focused");
        model = safe(model, "");
        workspaceRoot = safe(workspaceRoot, "");
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("profile", profile);
        json.addProperty("prompt", prompt);
        json.addProperty("module", module);
        json.addProperty("validationProfile", validationProfile);
        json.addProperty("model", model);
        json.addProperty("workspaceRoot", workspaceRoot);
        json.addProperty("allowRepoEdits", allowRepoEdits);
        json.addProperty("useLatestCapture", useLatestCapture);
        JsonArray allowed = new JsonArray();
        allowedFiles.forEach(allowed::add);
        json.add("allowedFiles", allowed);
        JsonArray captures = new JsonArray();
        captureIds.forEach(captures::add);
        json.add("captureIds", captures);
        return json;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
