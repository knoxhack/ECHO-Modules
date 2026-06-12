package com.echoplatform.echocore.api.config;

import java.util.List;

public record EchoConfigApplyResult(boolean accepted, List<String> messages) {
    public EchoConfigApplyResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static EchoConfigApplyResult acceptedResult() {
        return new EchoConfigApplyResult(true, List.of());
    }

    public static EchoConfigApplyResult rejected(String message) {
        return new EchoConfigApplyResult(false, List.of(message));
    }

    public boolean success() {
        return accepted;
    }

    public String message() {
        return String.join("; ", messages);
    }

    public String moduleId() {
        return "";
    }

    public String entryId() {
        return "";
    }
}
