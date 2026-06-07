package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Set;

public final class EchoAdapterConstants {
    public static final String MOD_ID = "echoadaptercore";
    public static final String MOD_NAME = "ECHO: AdapterCore";
    public static final String MINECRAFT_TARGET = "26.1.2";
    public static final String NEOFORGE_TARGET = "26.1.2.29-beta";

    public static final EchoFeatureId FEATURE_ADAPTER_CONTRACTS = EchoFeatureId.of("adapter.contracts");
    public static final EchoFeatureId FEATURE_ADAPTER_NEOFORGE = EchoFeatureId.of("adapter.neoforge");
    public static final EchoFeatureId FEATURE_ADAPTER_NATIVE_PLANNED = EchoFeatureId.of("adapter.native_planned");
    public static final EchoFeatureId FEATURE_ADAPTER_RUNTIME_STANDALONE = EchoFeatureId.of("adapter.runtime_standalone");
    public static final EchoFeatureId FEATURE_ADAPTER_DOMAINS = EchoFeatureId.of("adapter.domains");

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.ADAPTER_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(
                    FEATURE_ADAPTER_CONTRACTS,
                    FEATURE_ADAPTER_NEOFORGE,
                    FEATURE_ADAPTER_NATIVE_PLANNED,
                    FEATURE_ADAPTER_RUNTIME_STANDALONE,
                    FEATURE_ADAPTER_DOMAINS
            ),
            Set.of(),
            EchoPermissionSet.empty()
    );

    public static final List<EchoPlatformAdapter> BUILTIN_ADAPTERS = List.of(
            EchoNeoForgeAdapterDescriptor.adapter(),
            EchoNativeAdapterDescriptor.adapter(),
            EchoRuntimeStandaloneAdapterDescriptor.adapter()
    );

    private EchoAdapterConstants() {
    }
}
