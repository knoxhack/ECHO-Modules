package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoPlatformConstants;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.Map;
import java.util.Set;

public final class EchoNativeAdapterDescriptor {
    public static final EchoAdapterId ID = EchoAdapterId.of("echo_native");

    private EchoNativeAdapterDescriptor() {
    }

    public static EchoPlatformAdapter adapter() {
        EchoMinecraftVersionAdapter minecraft = new EchoMinecraftVersionAdapter(
                EchoAdapterConstants.MINECRAFT_TARGET,
                "echo_native",
                "0.1.0-alpha",
                "active_alpha",
                false
        );
        EchoAdapterContext context = new EchoAdapterContext(
                EchoAdapterConstants.MODULE_IDENTITY.id(),
                EchoRuntimeSide.COMMON,
                minecraft,
                "ECHO Native Platform",
                false,
                Map.of(
                        "native.module_artifacts", "active_alpha",
                        "native.runtime_host_bridge", "active_alpha",
                        "native.live_client_hooks", "gated"
                )
        );
        EchoAdapterCapabilities capabilities = new EchoAdapterCapabilities(
                Set.of(
                        capability("native.classpath"),
                        capability("native.transform_pipeline"),
                        capability("native.packos_bootstrap"),
                        capability("adapter.domains")
                ),
                Set.of(
                        EchoPlatformConstants.FEATURE_ADAPTER_NATIVE_PLANNED,
                        EchoAdapterConstants.FEATURE_ADAPTER_CONTRACTS,
                        EchoAdapterConstants.FEATURE_ADAPTER_DOMAINS
                ),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                false,
                false,
                false
        );
        EchoCompatibilityMatrix matrix = new EchoCompatibilityMatrix(
                ID,
                "[26.1.2,26.2)",
                "0.1.0-alpha",
                false,
                true,
                false,
                false,
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.DEV, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT),
                Set.of(EchoTrustLevel.OFFICIAL, EchoTrustLevel.VERIFIED, EchoTrustLevel.LOCAL, EchoTrustLevel.EXPERIMENTAL),
                Set.of()
        );
        return new EchoPlatformAdapter(
                ID,
                EchoAdapterKind.ECHO_NATIVE,
                EchoAdapterRuntime.ECHO_NATIVE,
                "ECHO Native",
                "Primary Native Loader adapter with active module-artifact loading and AdapterCore runtime-host bridge; live client hooks remain gated.",
                EchoAdapterStatus.ACTIVE_ALPHA,
                capabilities,
                context,
                matrix,
                true,
                Set.of(
                        new EchoAdapterDiagnostic(
                                "ECHO-ADAPTER-NATIVE-ACTIVE-ALPHA",
                                "ECHO Native adapter is active alpha",
                                "Native Loader module artifacts and the AdapterCore runtime-host bridge are implemented; live Minecraft client hook mutation remains gated until live-client proof passes.",
                                EchoAdapterStatus.ACTIVE_ALPHA,
                                EchoPlatformConstants.FEATURE_ADAPTER_NATIVE_PLANNED,
                                false,
                                "Route gameplay calls through AdapterCore and require NativeLoaderMutationLedger evidence before claiming gameplay readiness."
                        )
                )
        );
    }

    private static EchoCapabilityId capability(String value) {
        return EchoCapabilityId.of(value);
    }
}
