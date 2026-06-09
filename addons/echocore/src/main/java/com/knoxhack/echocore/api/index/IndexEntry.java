package com.knoxhack.echocore.api.index;

import java.util.Map;

public record IndexEntry(String id, String title, String category, Map<String, String> fields) {
    public IndexEntry {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
