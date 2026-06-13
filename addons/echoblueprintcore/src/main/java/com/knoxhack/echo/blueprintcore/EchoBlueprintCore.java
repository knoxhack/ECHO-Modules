package com.knoxhack.echo.blueprintcore;

import java.util.List;

public final class EchoBlueprintCore {
    public static final String MODID = "echoblueprintcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoschemacore",
            "echocreatorcore",
            "echocontentcore"
        );
    public static final List<String> PROVIDES = List.of(
            "blueprint.schemas",
            "blueprint.templates",
            "blueprint.studio_generation"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "blueprint_schema",
            "studio_template_generation",
            "content_type_blueprints"
        );

    public EchoBlueprintCore() {
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
