package com.knoxhack.echo.telemetrycore;

import java.util.List;

public final class EchoTelemetryCore {
    public static final String MODID = "echotelemetrycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echopolicycore",
            "echoreportcore",
            "echoplaytestcore"
        );
    public static final List<String> PROVIDES = List.of(
            "telemetry.local_bundle",
            "telemetry.privacy_policy",
            "telemetry.qa_metrics"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "opt_in_local_telemetry_bundle",
            "privacy_safe_metrics",
            "qa_support_export"
        );

    public EchoTelemetryCore() {
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
