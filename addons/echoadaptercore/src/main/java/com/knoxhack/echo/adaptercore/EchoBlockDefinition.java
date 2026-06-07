package com.knoxhack.echo.adaptercore;

public record EchoBlockDefinition(
        String id,
        String addon,
        String blockstate,
        String model,
        String texture,
        String langKey,
        String lang
) {
    public EchoBlockDefinition(
            String id,
            String addon,
            String blockstate,
            String model,
            String texture,
            String lang
    ) {
        this(id, addon, blockstate, model, texture, "", lang);
    }

    public EchoBlockDefinition {
        id = AdapterContractGuards.requireText(id, "block definition id");
        addon = AdapterContractGuards.requireText(addon, "block definition addon");
        blockstate = AdapterContractGuards.optionalText(blockstate);
        model = AdapterContractGuards.optionalText(model);
        texture = AdapterContractGuards.optionalText(texture);
        langKey = AdapterContractGuards.optionalText(langKey);
        lang = AdapterContractGuards.optionalText(lang);
    }
}
