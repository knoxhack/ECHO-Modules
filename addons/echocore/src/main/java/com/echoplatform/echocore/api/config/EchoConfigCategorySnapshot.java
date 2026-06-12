package com.echoplatform.echocore.api.config;

import java.util.List;

public record EchoConfigCategorySnapshot(String categoryId, String title, List<EchoConfigEntrySnapshot> entries) {
    public EchoConfigCategorySnapshot {
        categoryId = categoryId == null ? "" : categoryId;
        title = title == null ? "" : title;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
