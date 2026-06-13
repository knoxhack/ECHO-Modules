package com.knoxhack.echo.serveropscore;

import java.util.List;

public final class EchoServerOpsCore {
    public static final String MODID = "echoserveropscore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echopolicycore",
            "echoreportcore",
            "echotelemetrycore",
            "echonetcore"
        );
    public static final List<String> PROVIDES = List.of(
            "serverops.moderation",
            "serverops.backups",
            "serverops.announcements",
            "serverops.player_reports"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "moderation_contract",
            "backup_contract",
            "announcement_contract",
            "player_report_contract"
        );

    public EchoServerOpsCore() {
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
