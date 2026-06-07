package com.knoxhack.echobasegrid;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoBaseGridNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> nativeHost = Agent9BaseGridRuntimeAdapter.activateNativeHostEntrypoint();
        Map<String, Object> result = new LinkedHashMap<>(nativeHost);
        boolean passed = "PASS".equals(nativeHost.get("status"));
        result.put("activated", passed);
        result.put("activationStage", "basegrid_agent9_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", List.of(String.valueOf(nativeHost.get("adapterCoreContract"))));
        result.put("runtimeTargets", List.of("echo_backend", "echo_native", "echo_runtime_standalone"));
        result.put("summary", "BaseGrid native module executed the Agent9 base grid runtime adapter through AdapterCore.");
        return Map.copyOf(result);
    }
}
