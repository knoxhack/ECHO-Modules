package com.knoxhack.echo.scriptcore.model;

import java.util.Map;

public record EchoFactionReputationEvent(String id, String title, int amount, Map<String, Object> metadata) {
    public EchoFactionReputationEvent {
        id = id == null || id.isBlank() ? "event" : id;
        title = title == null ? id : title;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
