package com.knoxhack.echo.settlementcore;

import java.util.List;

public final class EchoSettlementCore {
    public static final String MODID = "echosettlementcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echobasegrid",
            "echonpcore",
            "echologisticscore",
            "echoworldcore"
        );
    public static final List<String> PROVIDES = List.of(
            "settlement.registry",
            "settlement.jobs",
            "settlement.defense_score",
            "settlement.logistics_requests"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "settlement_snapshot",
            "npc_job_contract",
            "defense_score_contract",
            "logistics_request_contract"
        );

    public EchoSettlementCore() {
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
