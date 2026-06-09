package com.knoxhack.echocore.api.config;

public record EchoConfigEntry(String key, EchoConfigValueKind kind, String defaultValue, String description) {
}
