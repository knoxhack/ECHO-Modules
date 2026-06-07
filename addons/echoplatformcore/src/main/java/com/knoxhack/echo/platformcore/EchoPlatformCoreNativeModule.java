package com.knoxhack.echo.platformcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoPlatformCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echoplatformcore";
    public static final String MODULE_IDENTITY_CONTRACT_ID = "echoplatformcore:data/module_identity";
    public static final String CAPABILITY_REPORT_CONTRACT_ID = "echoplatformcore:diagnostic/capability_report";
    public static final String TRUST_POLICY_CONTRACT_ID = "echoplatformcore:data/trust_policy";
    public static final List<String> CONTRACT_IDS = List.of(
            MODULE_IDENTITY_CONTRACT_ID,
            CAPABILITY_REPORT_CONTRACT_ID,
            TRUST_POLICY_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "platformcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics"));
        result.put("runtimeTargets", List.of("echo_native"));
        result.put("platformFeatureCount", EchoPlatformConstants.PLATFORM_FEATURES.size());
        result.put("platformPermissionCount", EchoPlatformConstants.PLATFORM_PERMISSIONS.size());
        result.put("requiresModuleIdentityBridge", true);
        result.put("requiresCapabilityBridge", true);
        result.put("requiresTrustPolicyBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "PlatformCore native contract exposed module identity, capability report, and trust policy behavior through AdapterCore.");
        return Map.copyOf(result);
    }
}
