package com.knoxhack.echo.scriptcore.model;

import java.util.Map;
import java.util.Optional;

public record EchoFactionRank(String name, int min, Optional<Integer> max, Optional<String> color, Map<String, Object> metadata) {
    public EchoFactionRank {
        name = name == null || name.isBlank() ? "rank" : name;
        max = max == null ? Optional.empty() : max;
        color = color == null ? Optional.empty() : color;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
