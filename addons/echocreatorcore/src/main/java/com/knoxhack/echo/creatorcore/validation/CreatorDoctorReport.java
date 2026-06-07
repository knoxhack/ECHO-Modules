package com.knoxhack.echo.creatorcore.validation;

import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CreatorDoctorReport(
        Instant timestamp,
        int adaptersAvailable,
        int adaptersTotal,
        int definitionCount,
        int draftCount,
        long errors,
        long warnings,
        long info,
        boolean scriptCoreAvailable,
        boolean writeModeLocked,
        boolean exportsLocked,
        String draftRoot,
        String exportRoot,
        boolean pathSafetyOk,
        List<CreatorDiagnostic> diagnostics) {
    public CreatorDoctorReport {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        draftRoot = draftRoot == null ? "" : draftRoot;
        exportRoot = exportRoot == null ? "" : exportRoot;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public List<String> compactLines() {
        List<String> lines = new ArrayList<>();
        lines.add("CreatorCore Doctor @ " + timestamp);
        lines.add("Adapters: " + adaptersAvailable + "/" + adaptersTotal + " available");
        lines.add("Definitions: " + definitionCount + ", drafts: " + draftCount);
        lines.add("Diagnostics: " + errors + " errors, " + warnings + " warnings, " + info + " info");
        lines.add("ScriptCore: " + (scriptCoreAvailable ? "available" : "missing or API unavailable"));
        lines.add("Writes: " + (writeModeLocked ? "locked" : "allowed") + ", exports: " + (exportsLocked ? "locked" : "allowed"));
        lines.add("Draft root: " + draftRoot);
        lines.add("Export root: " + exportRoot);
        lines.add("Path safety: " + (pathSafetyOk ? "ok" : "check configuration"));
        return List.copyOf(lines);
    }
}
