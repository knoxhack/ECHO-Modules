package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.Map;
import java.util.Set;

public final class EchoNeoForgeAdapterDescriptor {
    public static final EchoAdapterId ID = EchoAdapterId.of("neoforge");

    private EchoNeoForgeAdapterDescriptor() {
    }

    public static EchoPlatformAdapter adapter() {
        EchoMinecraftVersionAdapter minecraft = new EchoMinecraftVersionAdapter(
                EchoAdapterConstants.MINECRAFT_TARGET,
                "neoforge",
                EchoAdapterConstants.NEOFORGE_TARGET,
                "1.0.0",
                true
        );
        EchoAdapterContext context = new EchoAdapterContext(
                EchoAdapterConstants.MODULE_IDENTITY.id(),
                EchoRuntimeSide.COMMON,
                minecraft,
                "NeoForge ModDev",
                true,
                Map.of(
                        "minecraft.target", EchoAdapterConstants.MINECRAFT_TARGET,
                        "neoforge.target", EchoAdapterConstants.NEOFORGE_TARGET
                )
        );
        EchoAdapterCapabilities capabilities = new EchoAdapterCapabilities(
                Set.of(
                        capability("registry.blocks"),
                        capability("registry.items"),
                        capability("registry.entities"),
                        capability("registry.menus"),
                        capability("registry.sounds"),
                        capability("screen.custom"),
                        capability("network.custom_payload"),
                        capability("resources.assets"),
                        capability("resources.data"),
                        capability("worldgen.features"),
                        capability("commands.literal"),
                        capability("adapter.domains"),
                        capability("lifecycle.common_setup"),
                        capability("lifecycle.client_setup"),
                        capability("lifecycle.server_setup"),
                        capability("datagen")
                ),
                Set.of(
                        EchoAdapterConstants.FEATURE_ADAPTER_NEOFORGE,
                        EchoAdapterConstants.FEATURE_ADAPTER_CONTRACTS,
                        EchoAdapterConstants.FEATURE_ADAPTER_DOMAINS
                ),
                true,
                true,
                true,
                true,
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
                false
        );
        EchoCompatibilityMatrix matrix = new EchoCompatibilityMatrix(
                ID,
                "[26.1.2,26.2)",
                "[26.1.2.29-beta,)",
                true,
                false,
                true,
                true,
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.CLIENT, EchoRuntimeSide.SERVER, EchoRuntimeSide.DATA, EchoRuntimeSide.DEV),
                Set.of(EchoTrustLevel.OFFICIAL, EchoTrustLevel.VERIFIED, EchoTrustLevel.COMMUNITY, EchoTrustLevel.LOCAL, EchoTrustLevel.EXPERIMENTAL),
                Set.of()
        );
        return new EchoPlatformAdapter(
                ID,
                EchoAdapterKind.NEOFORGE,
                EchoAdapterRuntime.NEOFORGE,
                "NeoForge",
                "Current ECHO runtime adapter for Minecraft 26.1.2 on NeoForge 26.1.2.29-beta.",
                EchoAdapterStatus.ACTIVE_CURRENT,
                capabilities,
                context,
                matrix,
                false,
                Set.of()
        );
    }

    private static EchoCapabilityId capability(String value) {
        return EchoCapabilityId.of(value);
    }
}
