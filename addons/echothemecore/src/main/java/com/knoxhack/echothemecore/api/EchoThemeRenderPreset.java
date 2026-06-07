package com.knoxhack.echothemecore.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoThemeRenderPreset(
    Identifier id,
    Identifier theme,
    String type,
    Map<String, String> colors,
    List<Effect> effects,
    Map<String, String> metadata
) {
    public EchoThemeRenderPreset {
        colors = colors == null ? Map.of() : Map.copyOf(colors);
        effects = effects == null ? List.of() : List.copyOf(effects);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Optional<String> colorToken(String key) {
        return Optional.ofNullable(colors.get(key));
    }

    public record Effect(
        String type,
        int durationTicks,
        String strength,
        String color,
        String particleStyle,
        int count,
        Map<String, String> data
    ) {
        public Effect {
            type = type == null || type.isBlank() ? "unknown" : type;
            durationTicks = Math.max(0, durationTicks);
            count = Math.max(0, count);
            data = data == null ? Map.of() : Map.copyOf(data);
        }
    }
}
