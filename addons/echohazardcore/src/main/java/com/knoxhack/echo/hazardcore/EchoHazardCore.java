package com.knoxhack.echo.hazardcore;

import java.util.List;

public final class EchoHazardCore {
    public static final String MODID = "echohazardcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echostatuscore",
            "echohealthcore",
            "echoweathercore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "hazard.registry",
            "hazard.exposure",
            "hazard.resistance",
            "hazard.world_hooks"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "hazard_registry",
            "exposure_contract",
            "resistance_contract",
            "world_hazard_hooks"
        );

    public EchoHazardCore() {
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
