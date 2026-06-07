package com.knoxhack.echo.reportcore;

import java.nio.file.Path;

public final class EchoReportPaths {
    public static final String REPORT_ROOT = "reports/echo";

    private EchoReportPaths() {
    }

    public static EchoReportSafePath defaultPath(EchoReportKind kind) {
        return EchoReportSafePath.repoRelative(kind.defaultOutputPath());
    }

    public static Path resolve(Path workspaceRoot, EchoReportKind kind) {
        Path root = workspaceRoot == null ? Path.of(".") : workspaceRoot;
        return root.resolve(kind.defaultOutputPath()).normalize();
    }
}
