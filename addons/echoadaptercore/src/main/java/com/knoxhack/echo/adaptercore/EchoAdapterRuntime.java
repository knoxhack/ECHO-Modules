package com.knoxhack.echo.adaptercore;

public enum EchoAdapterRuntime {
    NEOFORGE("neoforge"),
    STANDALONE("standalone"),
    NATIVE_CLIENT("native_client"),
    ECHO_NATIVE("echo_native"),
    ECHO_RUNTIME_STANDALONE("echo_runtime_standalone"),
    TEST("test"),
    MOCK("mock"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoAdapterRuntime(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
