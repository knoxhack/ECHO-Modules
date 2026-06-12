package com.echoplatform.echocore.api.config;

import java.util.List;

public record EchoConfigModule(String moduleId, EchoConfigSide side, List<EchoConfigCategory> categories) {
    public EchoConfigModule {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public EchoConfigModule(String moduleId, String displayName, List<EchoConfigCategory> categories) {
        this(moduleId, EchoConfigSide.COMMON, categories);
    }
}
