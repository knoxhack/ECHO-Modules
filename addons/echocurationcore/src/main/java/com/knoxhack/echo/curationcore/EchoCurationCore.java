package com.knoxhack.echo.curationcore;

import java.util.List;

public final class EchoCurationCore {
    public static final String MODID = "echocurationcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echocapabilitycore",
            "echodependencydoctor",
            "echometadatacore"
        );
    public static final List<String> PROVIDES = List.of(
            "curation.recommendations",
            "curation.bundle_previews",
            "curation.readiness_badges"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "recommended_modules_report",
            "bundle_preview_contract",
            "readiness_badges"
        );

    public EchoCurationCore() {
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
