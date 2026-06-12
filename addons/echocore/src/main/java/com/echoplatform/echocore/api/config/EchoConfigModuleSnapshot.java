package com.echoplatform.echocore.api.config;

import java.util.List;

public record EchoConfigModuleSnapshot(String moduleId, String displayName, List<EchoConfigCategorySnapshot> categories) {
    public EchoConfigModuleSnapshot {
        moduleId = moduleId == null ? "" : moduleId;
        displayName = displayName == null || displayName.isBlank() ? moduleId : displayName;
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public boolean hasEntries() {
        return categories.stream().anyMatch(category -> !category.entries().isEmpty());
    }
}
