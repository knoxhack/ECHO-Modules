package com.knoxhack.echo.creatorcore.codex;

import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.io.IOException;
import java.util.List;

public final class CodexBridgeService {
    private static final long STATUS_CACHE_MILLIS = 1500L;
    private volatile CodexBridgeStatus lastStatus = CodexBridgeStatus.unavailable("Codex bridge has not been checked yet.");
    private volatile CodexJobSnapshot lastJob = CodexJobSnapshot.empty("No Codex job has run yet.");
    private volatile long lastStatusCheckedMillis;

    public CodexBridgeStatus status() {
        return status(false);
    }

    public CodexBridgeStatus refreshStatus() {
        return status(true);
    }

    private CodexBridgeStatus status(boolean force) {
        if (!bridgeAllowed()) {
            lastStatus = CodexBridgeStatus.unavailable("Codex bridge is locked by config (allow_codex_bridge=false).");
            lastStatusCheckedMillis = System.currentTimeMillis();
            return lastStatus;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastStatusCheckedMillis < STATUS_CACHE_MILLIS) {
            return lastStatus;
        }
        try {
            lastStatus = client().status();
            lastJob = lastStatus.latestJob().hasJob() ? lastStatus.latestJob() : lastJob;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastStatus = CodexBridgeStatus.unavailable("Codex bridge unavailable: " + exception.getMessage());
        }
        lastStatusCheckedMillis = now;
        return lastStatus;
    }

    public CodexJobSnapshot startJob(String rawProfile, String rawPrompt) {
        return startJob(rawProfile, rawPrompt, false);
    }

    public CodexJobSnapshot startJobWithLatestCapture(String rawProfile, String rawPrompt) {
        return startJob(rawProfile, rawPrompt, true);
    }

    private CodexJobSnapshot startJob(String rawProfile, String rawPrompt, boolean useLatestCapture) {
        if (!bridgeAllowed()) {
            return remember(CodexJobSnapshot.empty("Codex bridge is locked by config (allow_codex_bridge=false)."));
        }
        if (!repoEditsAllowed()) {
            return remember(CodexJobSnapshot.empty("Codex repo edits are locked by config (allow_codex_repo_edits=false)."));
        }
        CodexJobProfile profile = CodexJobProfile.byId(rawProfile).orElse(CodexJobProfile.ASSET_REPAIR);
        String prompt = rawPrompt == null || rawPrompt.isBlank() ? profile.defaultPrompt() : rawPrompt.trim();
        CodexJobRequest request = new CodexJobRequest(profile.id(), prompt, "", List.of(), List.of(), useLatestCapture,
                "focused", CreatorCoreConfig.string(CreatorCoreConfig.CODEX_MODEL, ""),
                CreatorCoreConfig.string(CreatorCoreConfig.CODEX_WORKSPACE_ROOT, ""), true);
        try {
            lastStatusCheckedMillis = 0L;
            return remember(client().startJob(request));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return remember(CodexJobSnapshot.empty("Codex job start failed: " + exception.getMessage()));
        }
    }

    public CodexJobSnapshot getJob(String id) {
        if (!bridgeAllowed()) {
            return remember(CodexJobSnapshot.empty("Codex bridge is locked by config (allow_codex_bridge=false)."));
        }
        if (id == null || id.isBlank()) {
            return lastJob;
        }
        try {
            lastStatusCheckedMillis = 0L;
            return remember(client().getJob(id.trim()));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return remember(CodexJobSnapshot.empty("Codex job refresh failed: " + exception.getMessage()));
        }
    }

    public CodexJobSnapshot cancelJob(String id) {
        if (!bridgeAllowed()) {
            return remember(CodexJobSnapshot.empty("Codex bridge is locked by config (allow_codex_bridge=false)."));
        }
        try {
            lastStatusCheckedMillis = 0L;
            return remember(client().cancelJob(id));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return remember(CodexJobSnapshot.empty("Codex job cancel failed: " + exception.getMessage()));
        }
    }

    public CodexJobSnapshot validateJob(String id) {
        if (!bridgeAllowed()) {
            return remember(CodexJobSnapshot.empty("Codex bridge is locked by config (allow_codex_bridge=false)."));
        }
        try {
            lastStatusCheckedMillis = 0L;
            return remember(client().validateJob(id));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return remember(CodexJobSnapshot.empty("Codex validation request failed: " + exception.getMessage()));
        }
    }

    public CodexJobSnapshot lastJob() {
        return lastJob;
    }

    public List<String> profileIds() {
        return CodexJobProfile.ids();
    }

    public boolean bridgeAllowed() {
        return CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false);
    }

    public boolean repoEditsAllowed() {
        return CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS, false);
    }

    private CodexJobSnapshot remember(CodexJobSnapshot job) {
        lastJob = job;
        return job;
    }

    private CodexBridgeClient client() {
        return new CodexBridgeClient(
                CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_URL, "http://127.0.0.1:47321"),
                CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_TOKEN, ""));
    }
}
