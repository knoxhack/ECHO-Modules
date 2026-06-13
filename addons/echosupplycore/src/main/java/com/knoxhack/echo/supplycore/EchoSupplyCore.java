package com.knoxhack.echo.supplycore;

import java.util.List;

public final class EchoSupplyCore {
    public static final String MODID = "echosupplycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echologisticscore",
            "echoeconomycore",
            "echolootcore"
        );
    public static final List<String> PROVIDES = List.of(
            "supply.scarcity",
            "supply.stockpiles",
            "supply.rationing",
            "supply.pressure"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "scarcity_contract",
            "stockpile_contract",
            "rationing_contract",
            "supply_pressure_contract"
        );

    public EchoSupplyCore() {
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
