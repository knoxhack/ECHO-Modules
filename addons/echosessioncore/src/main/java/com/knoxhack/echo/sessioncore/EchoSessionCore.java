package com.knoxhack.echo.sessioncore;

import java.util.List;

public final class EchoSessionCore {
    public static final String MODID = "echosessioncore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echodatacore",
            "echomissioncore",
            "echoplayercore"
        );
    public static final List<String> PROVIDES = List.of(
            "session.snapshot",
            "session.objective_state",
            "session.route_history",
            "session.pack_phase"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "session_snapshot_api",
            "current_objective_state",
            "recent_death_state",
            "active_hazard_state"
        );

    public EchoSessionCore() {
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
