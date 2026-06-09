package com.knoxhack.echocore.api.config;

import java.util.List;

public record EchoConfigApplyResult(boolean accepted, List<String> messages) {
    public EchoConfigApplyResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static EchoConfigApplyResult accepted() {
        return new EchoConfigApplyResult(true, List.of());
    }

    public static EchoConfigApplyResult rejected(String message) {
        return new EchoConfigApplyResult(false, List.of(message));
    }
}
