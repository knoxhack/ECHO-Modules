package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoSoundDefinition(
        String id,
        String addon,
        String subtitle,
        List<String> sounds,
        String source
) {
    public EchoSoundDefinition {
        id = AdapterContractGuards.requireText(id, "sound definition id");
        addon = AdapterContractGuards.requireText(addon, "sound definition addon");
        subtitle = AdapterContractGuards.optionalText(subtitle);
        sounds = List.copyOf(Objects.requireNonNull(sounds, "sounds"));
        source = AdapterContractGuards.requireText(source, "sound definition source");
    }
}
