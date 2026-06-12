package com.echoplatform.echocore.api.config;

import java.util.List;

public record EchoConfigEntrySnapshot(
        String moduleId,
        String categoryId,
        String entryId,
        String label,
        String description,
        EchoConfigSide side,
        EchoConfigValueKind kind,
        String value,
        String defaultValue,
        String minValue,
        String maxValue,
        List<String> options,
        boolean editable,
        boolean restartRequired,
        boolean newWorldOnly,
        String status) {
    public EchoConfigEntrySnapshot {
        moduleId = moduleId == null ? "" : moduleId;
        categoryId = categoryId == null ? "" : categoryId;
        entryId = entryId == null ? "" : entryId;
        label = label == null || label.isBlank() ? entryId : label;
        description = description == null ? "" : description;
        side = side == null ? EchoConfigSide.COMMON : side;
        kind = kind == null ? EchoConfigValueKind.STRING : kind;
        value = value == null ? "" : value;
        defaultValue = defaultValue == null ? "" : defaultValue;
        minValue = minValue == null ? "" : minValue;
        maxValue = maxValue == null ? "" : maxValue;
        options = options == null ? List.of() : List.copyOf(options);
        status = status == null ? "" : status;
    }
}
