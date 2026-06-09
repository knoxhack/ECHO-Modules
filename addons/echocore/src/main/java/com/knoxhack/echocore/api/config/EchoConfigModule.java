package com.knoxhack.echocore.api.config;

import java.util.List;

public record EchoConfigModule(String moduleId, EchoConfigSide side, List<EchoConfigCategory> categories) {
    public EchoConfigModule {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
