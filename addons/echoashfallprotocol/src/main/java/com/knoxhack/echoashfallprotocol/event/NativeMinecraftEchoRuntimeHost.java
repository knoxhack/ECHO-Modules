package com.knoxhack.echoashfallprotocol.event;

public final class NativeMinecraftEchoRuntimeHost extends MinecraftEchoRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoashfallprotocol:native_minecraft_runtime_host";

    public NativeMinecraftEchoRuntimeHost(NativeLoaderRuntimeHostContext hostContext) {
        super(hostContext);
    }

    @Override
    public String compatibilityDelegateId() {
        return "";
    }

    @Override
    public String runtimeLane() {
        return "Native Loader";
    }
}
