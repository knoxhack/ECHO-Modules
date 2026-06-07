package dev.echo.api.block;

import java.util.Map;

public record EchoBlockStateView(EchoBlockId blockId, Map<String, String> properties) {
    public EchoBlockStateView {
        properties = Map.copyOf(properties);
    }
}
