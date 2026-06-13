package com.knoxhack.echo.factioncore;

import java.util.List;

public final class EchoFactionCore {
    public static final String MODID = "echofactioncore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echomissioncore",
            "echoeconomycore",
            "echosocialcore"
        );
    public static final List<String> PROVIDES = List.of(
            "faction.registry",
            "faction.reputation",
            "faction.standings",
            "faction.mission_consequences"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "faction_registry",
            "reputation_state",
            "standing_rules",
            "mission_consequence_hooks"
        );

    public EchoFactionCore() {
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
