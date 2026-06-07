package com.knoxhack.echoworldcore.integration;

import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echocore.api.WorldCoreValidationIssue;
import com.knoxhack.echocore.api.WorldCoreValidationReport;
import com.knoxhack.echoworldcore.Config;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum WorldCoreDiagnosticProvider implements EchoDiagnosticService {
    INSTANCE;

    @Override
    public List<EchoDiagnosticBlocker> diagnostics(Player player) {
        WorldCoreValidationReport report = WorldRegionService.INSTANCE.validationReport(player == null ? null : player.level());
        List<EchoDiagnosticBlocker> diagnostics = new ArrayList<>();
        for (WorldCoreValidationIssue issue : report.issues()) {
            diagnostics.add(new EchoDiagnosticBlocker(
                    id("validation/" + sanitize(issue.category()) + "/" + sanitize(issue.id().getPath())),
                    EchoWorldCore.CHAPTER_ID,
                    severity(issue.severity()),
                    "WorldCore " + readable(issue.category()),
                    issue.message(),
                    "Run /echoworld validate and fix the owning datapack or marker producer."));
        }
        diagnostics.add(new EchoDiagnosticBlocker(
                id("runtime/scan_interval"),
                EchoWorldCore.CHAPTER_ID,
                EchoDiagnosticBlocker.Severity.INFO,
                "WorldCore Runtime",
                "scanInterval=" + Config.playerScanInterval()
                        + ", activeRadius=" + Config.activeRegionRadius()
                        + ", markers=" + report.markerCount()
                        + ", warnings=" + report.warningCount(),
                "Tune WorldCore config only if marker or region scans become noisy."));
        return List.copyOf(diagnostics);
    }

    private static EchoDiagnosticBlocker.Severity severity(WorldCoreValidationIssue.Severity severity) {
        return switch (severity == null ? WorldCoreValidationIssue.Severity.WARNING : severity) {
            case ERROR -> EchoDiagnosticBlocker.Severity.BLOCKED;
            case WARNING -> EchoDiagnosticBlocker.Severity.WARNING;
            case INFO -> EchoDiagnosticBlocker.Severity.INFO;
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, sanitize(path));
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.toLowerCase(java.util.Locale.ROOT)
                .replace('\\', '/')
                .replace(':', '/')
                .replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }

    private static String readable(String value) {
        String clean = value == null ? "issue" : value.replace('_', ' ');
        return clean.isBlank() ? "Issue" : Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }
}
