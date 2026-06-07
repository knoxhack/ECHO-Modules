package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoPlatformConstants;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.Map;
import java.util.Set;

public final class EchoRuntimeStandaloneAdapterDescriptor {
    public static final EchoAdapterId ID = EchoAdapterId.of("echo_runtime_standalone");

    private EchoRuntimeStandaloneAdapterDescriptor() {
    }

    public static EchoPlatformAdapter adapter() {
        EchoMinecraftVersionAdapter minecraft = new EchoMinecraftVersionAdapter(
                "none",
                "echo_runtime_standalone",
                "none",
                "0.1.0",
                false
        );
        EchoAdapterContext context = new EchoAdapterContext(
                EchoAdapterConstants.MODULE_IDENTITY.id(),
                EchoRuntimeSide.COMMON,
                minecraft,
                "ECHO Runtime Standalone",
                false,
                Map.of(
                        "standalone.runtime", "active_alpha",
                        "standalone.voxel_world", "active_alpha",
                        "standalone.desktop_window", "active_alpha",
                        "standalone.renderer.target", "opengl",
                        "standalone.renderer.opengl", "active_alpha",
                        "adapter.module_loading", "contract_required"
                )
        );
        EchoAdapterCapabilities capabilities = new EchoAdapterCapabilities(
                Set.of(
                        capability("standalone.runtime"),
                        capability("standalone.desktop_window"),
                        capability("standalone.voxel_world"),
                        capability("renderer.opengl"),
                        capability("adapter.module_loading"),
                        capability("adapter.domains"),
                        capability("registry.blocks"),
                        capability("registry.items"),
                        capability("registry.entities"),
                        capability("resources.assets"),
                        capability("resources.data"),
                        capability("worldgen.features"),
                        capability("commands.runtime"),
                        capability("lifecycle.runtime_boot")
                ),
                Set.of(
                        EchoAdapterConstants.FEATURE_ADAPTER_RUNTIME_STANDALONE,
                        EchoAdapterConstants.FEATURE_ADAPTER_CONTRACTS,
                        EchoAdapterConstants.FEATURE_ADAPTER_DOMAINS
                ),
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                true
        );
        EchoCompatibilityMatrix matrix = new EchoCompatibilityMatrix(
                ID,
                "[0.1.0,)",
                "standalone-alpha",
                false,
                true,
                false,
                false,
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.CLIENT, EchoRuntimeSide.DEV, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.AI_AGENT),
                Set.of(EchoTrustLevel.OFFICIAL, EchoTrustLevel.VERIFIED, EchoTrustLevel.LOCAL, EchoTrustLevel.EXPERIMENTAL),
                Set.of()
        );
        return new EchoPlatformAdapter(
                ID,
                EchoAdapterKind.ECHO_RUNTIME_STANDALONE,
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE,
                "ECHO Runtime Standalone",
                "Standalone desktop runtime adapter for the ECHO block/voxel beta path without Minecraft or NeoForge.",
                EchoAdapterStatus.ACTIVE_ALPHA,
                capabilities,
                context,
                matrix,
                true,
                Set.of(
                        new EchoAdapterDiagnostic(
                                "ECHO-ADAPTER-RUNTIME-STANDALONE-ALPHA",
                                "ECHO Runtime Standalone adapter is active alpha",
                                "The descriptor exposes standalone runtime, desktop window, voxel-world, and OpenGL renderer target contracts while module adapters are brought online.",
                                EchoAdapterStatus.ACTIVE_ALPHA,
                                EchoAdapterConstants.FEATURE_ADAPTER_RUNTIME_STANDALONE,
                                false,
                                "Use this adapter for standalone beta work; keep NeoForge and Native Loader adapters separate."
                        )
                )
        );
    }

    private static EchoCapabilityId capability(String value) {
        return EchoCapabilityId.of(value);
    }
}
