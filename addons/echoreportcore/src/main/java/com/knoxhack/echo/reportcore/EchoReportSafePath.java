package com.knoxhack.echo.reportcore;

import java.nio.file.Path;
import java.util.Map;

public record EchoReportSafePath(
        String path,
        boolean localOnly,
        String privacyNote,
        Map<String, String> attributes
) {
    public EchoReportSafePath {
        path = ReportContractGuards.normalizedPath(path, "report path");
        privacyNote = ReportContractGuards.optionalText(privacyNote);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportSafePath repoRelative(String path) {
        return new EchoReportSafePath(path, false, "Repository-relative path.", Map.of());
    }

    public static EchoReportSafePath localOnly(Path path, String privacyNote) {
        return new EchoReportSafePath(path.toAbsolutePath().normalize().toString(), true, privacyNote, Map.of());
    }
}
