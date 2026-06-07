package com.knoxhack.echo.creatorcore.ui.panels;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.codex.CodexBridgeStatus;
import com.knoxhack.echo.creatorcore.codex.CodexJobSnapshot;
import com.knoxhack.echo.creatorcore.codex.CodexJobProfile;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorPanel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class CodexStudioPanel implements CreatorPanel {
    @Override
    public Identifier id() {
        return EchoCreatorCore.id("codex_studio");
    }

    @Override
    public String title() {
        return "Codex Studio";
    }

    @Override
    public String summary() {
        return "Local Codex CLI bridge for repo-editing creator tasks.";
    }

    @Override
    public List<String> lines(CreatorDashboardModel model) {
        CodexBridgeStatus status = model.codexStatus();
        CodexJobSnapshot job = model.codexJob();
        List<String> lines = new ArrayList<>();
        lines.add("Bridge: " + (status.ok() ? "online" : "offline") + " - "
                + (status.message().isBlank() ? status.bridge() : status.message()));
        lines.add("Config: bridge=" + (model.codexBridgeLocked() ? "locked" : "allowed")
                + ", repo edits=" + (model.codexRepoEditsLocked() ? "locked" : "allowed"));
        lines.add("Bridge guards: auth=" + (status.authRequired() ? "required" : "off")
                + ", sidecar repo edits=" + (status.repoEditsAllowed() ? "allowed" : "locked")
                + ", template=" + (status.commandTemplateConfigured() ? "configured" : "default"));
        lines.add("Workspace: " + status.workspace());
        lines.add("Codex: " + (status.codexAvailable() ? "available" : "unavailable")
                + (status.codexError().isBlank() ? "" : " - " + status.codexError()));
        lines.add("Model: " + (status.defaultModel().isBlank() ? "Codex CLI default" : status.defaultModel()) + ", jobs=" + status.jobCount()
                + ", running=" + status.runningJobCount());
        lines.add("");
        lines.add("Commands:");
        lines.add("  /echo creatorcore codex status");
        lines.add("  /echo creatorcore codex run asset_repair describe the repair task");
        lines.add("  /echo creatorcore codex run mob_model create a RenderCore creature model");
        lines.add("  /echo creatorcore codex validate <job>");
        lines.add("  /echo creatorcore codex cancel <job>");
        lines.add("");
        lines.add("Profiles:");
        for (CodexJobProfile profile : CodexJobProfile.values()) {
            lines.add("  " + profile.id() + " - " + profile.title());
        }
        lines.add("");
        lines.add("Latest Job:");
        if (!job.hasJob()) {
            lines.add("  " + job.error());
            return lines;
        }
        lines.add("  " + job.id() + " [" + job.profile() + "] " + job.state()
                + " validation=" + job.validationStatus());
        if (!job.error().isBlank()) {
            lines.add("ERROR " + job.error());
        }
        if (!job.changedFiles().isEmpty()) {
            lines.add("Changed files:");
            job.changedFiles().stream().limit(12).forEach(path -> lines.add("  " + path));
        }
        if (!job.validationLines().isEmpty()) {
            lines.add("Validation tail:");
            job.validationLines().stream().skip(Math.max(0, job.validationLines().size() - 10))
                    .forEach(line -> lines.add("  " + line));
        }
        if (!job.stdoutSummary().isBlank()) {
            lines.add("Output tail:");
            List<String> output = job.stdoutSummary().lines().toList();
            for (String line : output.stream().skip(Math.max(0, output.size() - 8)).toList()) {
                lines.add("  " + line);
            }
        }
        return lines;
    }
}
