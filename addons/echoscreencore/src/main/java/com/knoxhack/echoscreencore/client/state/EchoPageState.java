package com.knoxhack.echoscreencore.client.state;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoPageState {
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    public Map<String, Object> values() {
        return values;
    }

    public Object get(String key) {
        return key == null ? null : values.get(key);
    }

    public String getString(String key, String fallback) {
        Object value = get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public void put(String key, Object value) {
        if (key != null && !key.isBlank()) {
            String clean = key.strip();
            values.put(clean, value);
            putNested(clean, value);
        }
    }

    @SuppressWarnings("unchecked")
    private void putNested(String key, Object value) {
        String[] parts = key.split("\\.");
        if (parts.length <= 1) {
            return;
        }
        Map<String, Object> cursor = values;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isBlank()) {
                return;
            }
            Object next = cursor.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                cursor.put(parts[i], next);
            }
            cursor = (Map<String, Object>) next;
        }
        cursor.put(parts[parts.length - 1], value);
    }

    public void clear() {
        values.clear();
    }
}
