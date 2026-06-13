package com.knoxhack.echo.playtestcore;

import java.util.List;

public final class EchoPlaytestCore {
    public static final String MODID = "echoplaytestcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoreportcore",
            "echovalidationcore",
            "echomodulegraph",
            "echoruntimeguard"
        );
    public static final List<String> PROVIDES = List.of(
            "playtest.scenarios",
            "playtest.evidence_runner",
            "playtest.release_readiness",
            "playtest.session_proofs"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "json_scenario_definitions",
            "first_30_minutes_run",
            "two_hour_run",
            "completion_path",
            "save_load_proof",
            "crash_free_session",
            "install_update_repair_rollback_proof",
            "release_readiness_report"
        );

    public EchoPlaytestCore() {
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
