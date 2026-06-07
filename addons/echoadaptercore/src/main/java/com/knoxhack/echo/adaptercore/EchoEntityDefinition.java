package com.knoxhack.echo.adaptercore;

public record EchoEntityDefinition(
        String id,
        String addon,
        String model,
        String texture,
        String langKey,
        String lang
) {
    public EchoEntityDefinition(
            String id,
            String addon,
            String model,
            String texture,
            String lang
    ) {
        this(id, addon, model, texture, "", lang);
    }

    public EchoEntityDefinition {
        id = AdapterContractGuards.requireText(id, "entity definition id");
        addon = AdapterContractGuards.requireText(addon, "entity definition addon");
        model = AdapterContractGuards.optionalText(model);
        texture = AdapterContractGuards.optionalText(texture);
        langKey = AdapterContractGuards.optionalText(langKey);
        lang = AdapterContractGuards.optionalText(lang);
    }
}
