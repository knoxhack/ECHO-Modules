package com.knoxhack.echo.accessibilitycore;

import java.util.List;

public final class EchoAccessibilityCore {
    public static final String MODID = "echoaccessibilitycore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echothemecore",
            "echoscreencore",
            "echoinputcore",
            "echosoundcore"
        );
    public static final List<String> PROVIDES = List.of(
            "accessibility.settings",
            "accessibility.validation",
            "accessibility.narration_metadata"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "accessibility_settings_contract",
            "validation_checks",
            "caption_metadata",
            "prompt_remaps"
        );

    public EchoAccessibilityCore() {
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
