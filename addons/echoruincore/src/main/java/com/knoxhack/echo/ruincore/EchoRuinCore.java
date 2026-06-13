package com.knoxhack.echo.ruincore;

import java.util.List;

public final class EchoRuinCore {
    public static final String MODID = "echoruincore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echostructurecore",
            "echolootcore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "ruin.registry",
            "ruin.archaeology",
            "ruin.salvage_sites",
            "ruin.restoration_state"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "ruin_registry",
            "archaeology_contract",
            "salvage_site_contract",
            "restoration_state_contract"
        );

    public EchoRuinCore() {
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
