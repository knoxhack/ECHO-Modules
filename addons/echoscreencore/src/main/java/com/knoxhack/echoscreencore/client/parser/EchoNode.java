package com.knoxhack.echoscreencore.client.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EchoNode {
    private final String tagName;
    private final String id;
    private final Set<String> classes;
    private final Map<String, String> attributes;
    private final String text;
    private final List<EchoNode> children;
    private final String source;

    public EchoNode(String tagName, Map<String, String> attributes, String text, List<EchoNode> children, String source) {
        this.tagName = normalize(tagName);
        this.attributes = attributes == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(attributes));
        this.id = this.attributes.getOrDefault("id", "");
        this.classes = parseClasses(this.attributes.get("class"));
        this.text = text == null ? "" : text.strip();
        this.children = children == null ? List.of() : List.copyOf(children);
        this.source = source == null ? "" : source;
    }

    public String tagName() {
        return tagName;
    }

    public String id() {
        return id;
    }

    public Set<String> classes() {
        return classes;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public String attribute(String name, String fallback) {
        return attributes.getOrDefault(name, fallback);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public String text() {
        return text;
    }

    public List<EchoNode> children() {
        return children;
    }

    public String source() {
        return source;
    }

    private static Set<String> parseClasses(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw.split("\\s+")) {
            if (!part.isBlank()) {
                values.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(values);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static Builder builder(String tagName) {
        return new Builder(tagName);
    }

    public static final class Builder {
        private final String tagName;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<EchoNode> children = new ArrayList<>();
        private String text = "";
        private String source = "";

        private Builder(String tagName) {
            this.tagName = tagName;
        }

        public Builder attribute(String key, String value) {
            if (key != null && value != null) {
                attributes.put(key.trim(), value.trim());
            }
            return this;
        }

        public Builder text(String text) {
            this.text = text == null ? "" : text;
            return this;
        }

        public Builder child(EchoNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Builder source(String source) {
            this.source = source == null ? "" : source;
            return this;
        }

        public EchoNode build() {
            return new EchoNode(tagName, attributes, text, children, source);
        }
    }
}
