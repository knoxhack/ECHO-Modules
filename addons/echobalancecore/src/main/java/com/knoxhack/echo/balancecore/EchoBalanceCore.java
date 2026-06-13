package com.knoxhack.echo.balancecore;

import java.util.List;

public final class EchoBalanceCore {
    public static final String MODID = "echobalancecore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echorecipecore",
            "echolootcore",
            "echoeconomycore",
            "echoprogressioncore"
        );
    public static final List<String> PROVIDES = List.of(
            "balance.tables",
            "balance.audits",
            "balance.recommended_ranges"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "balance_report",
            "warning_ranges",
            "recommended_ranges"
        );

    public EchoBalanceCore() {
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
