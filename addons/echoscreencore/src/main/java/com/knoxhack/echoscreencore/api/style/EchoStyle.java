package com.knoxhack.echoscreencore.api.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EchoStyle {
    public static final EchoStyle EMPTY = new EchoStyle(Map.of());

    private final Map<String, String> properties;

    public EchoStyle(Map<String, String> properties) {
        this.properties = properties == null || properties.isEmpty()
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public Map<String, String> properties() {
        return properties;
    }

    public Optional<String> value(String property) {
        return Optional.ofNullable(property == null ? null : properties.get(normalize(property)));
    }

    public String value(String property, String fallback) {
        return value(property).orElse(fallback);
    }

    public boolean bool(String property, boolean fallback) {
        return value(property)
            .map(value -> switch (value.toLowerCase(Locale.ROOT)) {
                case "true", "yes", "1", "on" -> true;
                case "false", "no", "0", "off" -> false;
                default -> fallback;
            })
            .orElse(fallback);
    }

    public EchoStyle with(String property, String value) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(properties);
        if (property != null && value != null) {
            copy.put(normalize(property), value.trim());
        }
        return new EchoStyle(copy);
    }

    public EchoStyle merge(EchoStyle other) {
        if (other == null || other.properties.isEmpty()) {
            return this;
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(properties);
        copy.putAll(other.properties);
        return new EchoStyle(copy);
    }

    public static String normalize(String property) {
        return property.trim().toLowerCase(Locale.ROOT);
    }
}
