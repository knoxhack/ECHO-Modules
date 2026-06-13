package com.knoxhack.echo.localizationcore;

import java.util.List;

public final class EchoLocalizationCore {
    public static final String MODID = "echolocalizationcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoschemacore",
            "echoreportcore"
        );
    public static final List<String> PROVIDES = List.of(
            "localization.validation",
            "localization.fallbacks",
            "localization.language_pack_overlay"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "missing_key_report",
            "fallback_text_contract",
            "language_pack_overlay_support"
        );

    public EchoLocalizationCore() {
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
