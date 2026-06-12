package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record DataKeyMetadata(
        Identifier id,
        DataScope scope,
        DataValueKind kind,
        boolean synced,
        String title,
        String description,
        String owner,
        String legacyRoot,
        String legacyField,
        String defaultValue,
        String source) {
    public DataKeyMetadata {
        scope = scope == null ? DataScope.PLAYER : scope;
        kind = kind == null ? DataValueKind.RECORD : kind;
        title = title == null ? "" : title;
        description = description == null ? "" : description;
        owner = owner == null || owner.isBlank() ? id == null ? "" : id.getNamespace() : owner;
        legacyRoot = legacyRoot == null ? "" : legacyRoot;
        legacyField = legacyField == null ? "" : legacyField;
        defaultValue = defaultValue == null ? "" : defaultValue;
        source = source == null ? "" : source;
    }

    public DataKeyMetadata(Identifier id, DataScope scope, String type, boolean synced) {
        this(id, scope, kindFromType(type), synced, "", "", "", "", "", "", "");
    }

    public static DataKeyMetadata of(IDataKey<?> key, String source) {
        if (key == null) {
            return new DataKeyMetadata(null, DataScope.PLAYER, DataValueKind.RECORD, false,
                    "", "", "", "", "", "", source);
        }
        String defaultValue = key.defaultValue() == null ? "" : String.valueOf(key.defaultValue());
        return new DataKeyMetadata(
                key.id(),
                key.scope(),
                key.kind(),
                key.synced(),
                titleFromId(key.id()),
                "",
                key.id() == null ? "" : key.id().getNamespace(),
                "",
                "",
                defaultValue,
                source);
    }

    public String type() {
        return kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    public DataKeyMetadata merge(DataKeyMetadata other) {
        if (other == null) {
            return this;
        }
        return new DataKeyMetadata(
                id == null ? other.id() : id,
                scope == null ? other.scope() : scope,
                kind == null ? other.kind() : kind,
                synced || other.synced(),
                choose(title, other.title()),
                choose(description, other.description()),
                choose(owner, other.owner()),
                choose(legacyRoot, other.legacyRoot()),
                choose(legacyField, other.legacyField()),
                choose(defaultValue, other.defaultValue()),
                choose(source, other.source()));
    }

    private static String choose(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback == null ? "" : fallback : preferred;
    }

    private static String titleFromId(Identifier id) {
        return id == null ? "" : id.getPath().replace('_', ' ').replace('/', ' ');
    }

    private static DataValueKind kindFromType(String type) {
        if (type == null || type.isBlank()) {
            return DataValueKind.RECORD;
        }
        String normalized = type.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return DataValueKind.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return switch (normalized) {
                case "BOOLEAN", "BOOL" -> DataValueKind.FLAG;
                case "INTEGER", "INT", "LONG", "NUMBER" -> DataValueKind.COUNTER;
                case "STRING" -> DataValueKind.STRING;
                default -> DataValueKind.RECORD;
            };
        }
    }
}
