package com.knoxhack.echocore.api.config;

import java.util.List;

public record EchoConfigCategory(String id, String displayName, List<EchoConfigEntry> entries) {
    public EchoConfigCategory {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
