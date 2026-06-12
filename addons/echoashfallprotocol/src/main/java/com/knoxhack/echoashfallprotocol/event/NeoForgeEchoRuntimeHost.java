package com.knoxhack.echoashfallprotocol.event;

/**
 * Compatibility façade for callers that still use the old NeoForge runtime
 * host naming. New code should use {@link NativeEchoRuntimeHost}.
 */
@Deprecated(forRemoval = false)
public final class NeoForgeEchoRuntimeHost extends MinecraftEchoRuntimeHost {
    public static final String RUNTIME_HOST_ID = NativeEchoRuntimeHost.RUNTIME_HOST_ID;

    public NeoForgeEchoRuntimeHost(NativeRuntimeHostContext hostContext) {
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
