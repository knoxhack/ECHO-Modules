package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public record EchoAdapterCapabilities(
        Set<EchoCapabilityId> capabilities,
        Set<EchoFeatureId> features,
        boolean registryAdapter,
        boolean networkAdapter,
        boolean screenAdapter,
        boolean resourceAdapter,
        boolean commandAdapter,
        boolean lifecycleAdapter,
        boolean worldAdapter,
        boolean clientAdapter,
        boolean serverAdapter,
        boolean dataGeneration,
        boolean nativeClasspath,
        boolean nativeTransformPipeline,
        boolean nativePackOsBootstrap,
        boolean standaloneRuntime,
        boolean standaloneVoxelWorld,
        boolean standaloneDesktopWindow
) {
    public EchoAdapterCapabilities {
        capabilities = AdapterContractGuards.immutableSet(capabilities);
        features = AdapterContractGuards.immutableSet(features);
    }
}
