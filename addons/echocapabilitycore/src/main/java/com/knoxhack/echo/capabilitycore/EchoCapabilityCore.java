package com.knoxhack.echo.capabilitycore;

import java.util.List;

public final class EchoCapabilityCore {
    public static final String MODID = "echocapabilitycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echomodulegraph",
            "echoplatformcore"
        );
    public static final List<String> PROVIDES = List.of(
            "capability.registry",
            "capability.negotiation",
            "capability.missing_diagnostics",
            "capability.fallbacks"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "capability_registry",
            "missing_capability_diagnostics",
            "graceful_fallback_api"
        );

    public EchoCapabilityCore() {
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
