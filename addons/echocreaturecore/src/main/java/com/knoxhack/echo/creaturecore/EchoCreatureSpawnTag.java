package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Locale;
import java.util.Map;

public record EchoCreatureSpawnTag(
        String tag,
        EchoContentReference biomeReference,
        EchoContentReference structureReference,
        EchoContentReference hazardReference,
        Map<String, String> attributes
) {
    public EchoCreatureSpawnTag {
        tag = CreatureContractGuards.requireText(tag, "creature spawn tag").toLowerCase(Locale.ROOT);
        attributes = CreatureContractGuards.immutableMap(attributes);
    }
}
