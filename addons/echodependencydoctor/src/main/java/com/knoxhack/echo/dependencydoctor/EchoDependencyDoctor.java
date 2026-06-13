package com.knoxhack.echo.dependencydoctor;

import java.util.List;

public final class EchoDependencyDoctor {
    public static final String MODID = "echodependencydoctor";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echomodulegraph",
            "echocapabilitycore",
            "echoreportcore"
        );
    public static final List<String> PROVIDES = List.of(
            "dependency.explanations",
            "dependency.launch_report",
            "dependency.conflict_diagnostics",
            "dependency.artifact_diagnostics"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "why_pack_wont_launch_report",
            "conflict_explanations",
            "missing_artifact_report",
            "optional_integration_diagnostics"
        );

    public EchoDependencyDoctor() {
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
