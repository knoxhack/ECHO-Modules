package com.knoxhack.echo.seasoncore;

import java.util.List;

public final class EchoSeasonCore {
    public static final String MODID = "echoseasoncore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echomissioncore",
            "echolootcore",
            "echopolicycore"
        );
    public static final List<String> PROVIDES = List.of(
            "season.objectives",
            "season.loot",
            "season.timed_modifiers",
            "season.live_events"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "rotating_objectives_contract",
            "seasonal_loot_contract",
            "timed_modifier_contract",
            "live_event_contract"
        );

    public EchoSeasonCore() {
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
