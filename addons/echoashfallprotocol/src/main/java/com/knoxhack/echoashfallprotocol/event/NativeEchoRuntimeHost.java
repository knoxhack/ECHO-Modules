package com.knoxhack.echoashfallprotocol.event;

public final class NativeEchoRuntimeHost extends MinecraftEchoRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoashfallprotocol:native_runtime_host";

    public NativeEchoRuntimeHost(NativeRuntimeHostContext hostContext) {
        super(hostContext);
    }

    @Override
    public String compatibilityDelegateId() {
        return RUNTIME_HOST_ID.equals(runtimeHostId()) ? "" : RUNTIME_HOST_ID;
    }

    @Override
    public String runtimeLane() {
        return "Native";
    }
}
