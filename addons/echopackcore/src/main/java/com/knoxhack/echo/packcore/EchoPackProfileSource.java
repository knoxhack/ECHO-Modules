package com.knoxhack.echo.packcore;

import java.nio.file.Path;

public record EchoPackProfileSource(
        Path path,
        String reportPath,
        boolean canonical,
        boolean legacyFallback,
        boolean localOnly
) {
    public EchoPackProfileSource {
        path = path == null ? null : path.toAbsolutePath().normalize();
        reportPath = PackContractGuards.requireText(reportPath, "pack profile source path");
    }

    public static EchoPackProfileSource repoRelative(Path workspaceRoot, Path path, boolean canonical, boolean legacyFallback) {
        Path normalizedRoot = workspaceRoot == null ? Path.of(".").toAbsolutePath().normalize() : workspaceRoot.toAbsolutePath().normalize();
        Path normalizedPath = path == null ? null : path.toAbsolutePath().normalize();
        String reportPath;
        boolean localOnly = false;
        if (normalizedPath == null) {
            reportPath = "built_in_default";
        } else {
            try {
                reportPath = normalizedRoot.relativize(normalizedPath).toString().replace('\\', '/');
            } catch (IllegalArgumentException ex) {
                reportPath = "localOnly:outside-workspace";
                localOnly = true;
            }
        }
        return new EchoPackProfileSource(normalizedPath, reportPath, canonical, legacyFallback, localOnly);
    }

    public static EchoPackProfileSource builtInDefault() {
        return new EchoPackProfileSource(null, "built_in_default", false, false, false);
    }
}
