package com.echoplatform.echocore.api.config;

import java.util.function.Supplier;

public interface EchoConfigProvider {
    EchoConfigModule describeConfig();

    default EchoConfigApplyResult apply(EchoConfigModule module) {
        return EchoConfigApplyResult.acceptedResult();
    }

    static EchoConfigProvider of(String moduleId, Supplier<EchoConfigModule> supplier) {
        return new EchoConfigProvider() {
            @Override
            public EchoConfigModule describeConfig() {
                return supplier.get();
            }
        };
    }
}
