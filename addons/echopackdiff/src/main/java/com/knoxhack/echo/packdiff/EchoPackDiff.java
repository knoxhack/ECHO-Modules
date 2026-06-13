package com.knoxhack.echo.packdiff;

import java.util.List;

public final class EchoPackDiff {
    public static final String MODID = "echopackdiff";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echomigrationcore",
            "echometadatacore",
            "echoreportcore"
        );
    public static final List<String> PROVIDES = List.of(
            "packdiff.json",
            "packdiff.markdown",
            "packdiff.changelog"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "markdown_changelog",
            "json_changelog",
            "dependency_diff",
            "migration_diff"
        );

    public EchoPackDiff() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
