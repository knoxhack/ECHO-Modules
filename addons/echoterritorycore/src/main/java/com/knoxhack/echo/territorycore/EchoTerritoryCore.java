package com.knoxhack.echo.territorycore;

import java.util.List;

public final class EchoTerritoryCore {
    public static final String MODID = "echoterritorycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echofactioncore",
            "echoholomap",
            "echoworldcore",
            "echopolicycore"
        );
    public static final List<String> PROVIDES = List.of(
            "territory.claims",
            "territory.control",
            "territory.map_overlays",
            "territory.server_rules"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "region_control_contract",
            "claim_contract",
            "contested_zone_contract",
            "map_overlay_contract"
        );

    public EchoTerritoryCore() {
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
