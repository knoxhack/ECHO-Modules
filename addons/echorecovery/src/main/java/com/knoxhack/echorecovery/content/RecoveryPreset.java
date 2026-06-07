package com.knoxhack.echorecovery.content;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record RecoveryPreset(
        Identifier id,
        String displayName,
        Map<String, String> values) {
    public RecoveryPreset {
        displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName.strip();
        values = Map.copyOf(values == null ? Map.of() : values);
    }
}
