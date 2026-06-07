package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoItemDefinition(
        String id,
        String addon,
        String model,
        String texture,
        String langKey,
        String lang,
        List<String> recipes,
        List<String> lootTables,
        boolean searchVisible
) {
    public EchoItemDefinition(
            String id,
            String addon,
            String model,
            String texture,
            String lang,
            List<String> recipes,
            List<String> lootTables,
            boolean searchVisible
    ) {
        this(id, addon, model, texture, "", lang, recipes, lootTables, searchVisible);
    }

    public EchoItemDefinition {
        id = AdapterContractGuards.requireText(id, "item definition id");
        addon = AdapterContractGuards.requireText(addon, "item definition addon");
        model = AdapterContractGuards.optionalText(model);
        texture = AdapterContractGuards.optionalText(texture);
        langKey = AdapterContractGuards.optionalText(langKey);
        lang = AdapterContractGuards.optionalText(lang);
        recipes = List.copyOf(Objects.requireNonNull(recipes, "recipes"));
        lootTables = List.copyOf(Objects.requireNonNull(lootTables, "lootTables"));
    }
}
