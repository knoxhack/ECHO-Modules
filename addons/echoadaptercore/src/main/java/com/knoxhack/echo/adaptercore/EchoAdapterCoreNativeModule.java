package com.knoxhack.echo.adaptercore;

import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoAdapterCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoPlatformAdapter adapter = EchoNativeAdapterDescriptor.adapter();
        EchoPlatformAdapter standalone = EchoRuntimeStandaloneAdapterDescriptor.adapter();
        EchoAdapterCapabilities capabilities = adapter.capabilities();
        EchoAdapterCapabilities standaloneCapabilities = standalone.capabilities();
        EchoRegistryParityResult registryParity = EchoRegistryParityVerifier.verifySampleContract();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "adaptercore_runtime_backends_parity_active");
        result.put("adapterCoreUsed", true);
        result.put("moduleId", EchoAdapterConstants.MOD_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("adapterId", adapter.id().value());
        result.put("adapterKind", adapter.kind().serializedName());
        result.put("adapterRuntime", adapter.runtime().serializedName());
        result.put("adapterStatus", adapter.status().serializedName());
        result.put("nativeClasspath", capabilities.nativeClasspath());
        result.put("nativePackOsBootstrap", capabilities.nativePackOsBootstrap());
        result.put("nativeTransformPipeline", capabilities.nativeTransformPipeline());
        result.put("nativeLoaderSupported", adapter.nativeLoaderSupported());
        result.put("standaloneRuntimeAdapterId", standalone.id().value());
        result.put("standaloneRuntime", standaloneCapabilities.standaloneRuntime());
        result.put("standaloneVoxelWorld", standaloneCapabilities.standaloneVoxelWorld());
        result.put("standaloneDesktopWindow", standaloneCapabilities.standaloneDesktopWindow());
        result.put("nativeAdapterCodeExecuted", true);
        result.put("supportedRuntimes", Map.of(
                EchoAdapterRuntime.NEOFORGE.serializedName(), EchoNeoForgeAdapterDescriptor.adapter().status().serializedName(),
                EchoAdapterRuntime.NATIVE_CLIENT.serializedName(), adapter.status().serializedName(),
                EchoAdapterRuntime.STANDALONE.serializedName(), standalone.status().serializedName(),
                EchoAdapterRuntime.ECHO_NATIVE.serializedName(), adapter.status().serializedName(),
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName(), standalone.status().serializedName()
        ));
        result.put("bridgeContracts", Map.of(
                "lifecycle", "adaptercore.native_lifecycle",
                "registry", "adaptercore.native_registry",
                "events", "adaptercore.native_event",
                "standaloneRuntime", "adaptercore.echo_runtime_standalone",
                "voxelWorld", "adaptercore.standalone_voxel_world"
        ));
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "adaptercore_runtime_backend_projection");
        result.put("serviceCodeExecuted", registryParity.passed());
        result.put("runtimeBackendParityPassed", registryParity.passed());
        result.put("runtimeBackendParityChecks", registryParity.passedChecks());
        result.put("runtimeBackendParityFailures", registryParity.failedChecks());
        result.put("summary",
                "AdapterCore native module resolved registry contracts through Echo Native Loader "
                        + "and Echo Standalone Runtime backends, then verified parity.");
        return result;
    }
}
