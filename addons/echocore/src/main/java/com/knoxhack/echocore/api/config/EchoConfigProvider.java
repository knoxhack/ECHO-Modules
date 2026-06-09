package com.knoxhack.echocore.api.config;

public interface EchoConfigProvider {
    EchoConfigModule describeConfig();

    default EchoConfigApplyResult apply(EchoConfigModule module) {
        return EchoConfigApplyResult.accepted();
    }
}
