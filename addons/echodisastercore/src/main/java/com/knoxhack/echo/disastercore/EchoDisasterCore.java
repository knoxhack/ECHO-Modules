package com.knoxhack.echo.disastercore;

import java.util.List;

public final class EchoDisasterCore {
    public static final String MODID = "echodisastercore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echohazardcore",
            "echoweathercore",
            "echosessioncore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "disaster.events",
            "disaster.recovery",
            "disaster.station_failures",
            "disaster.world_impacts"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "disaster_event_contract",
            "recovery_event_contract",
            "station_failure_contract",
            "storm_disaster_contract"
        );

    public EchoDisasterCore() {
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
