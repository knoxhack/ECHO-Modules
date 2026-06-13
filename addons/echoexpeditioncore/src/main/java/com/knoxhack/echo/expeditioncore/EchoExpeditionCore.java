package com.knoxhack.echo.expeditioncore;

import java.util.List;

public final class EchoExpeditionCore {
    public static final String MODID = "echoexpeditioncore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echosessioncore",
            "echohazardcore",
            "echomissioncore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "expedition.routes",
            "expedition.risk_budget",
            "expedition.extraction",
            "expedition.travel_contracts"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "route_prep_contract",
            "risk_budget_contract",
            "extraction_loop_contract",
            "travel_contract"
        );

    public EchoExpeditionCore() {
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
