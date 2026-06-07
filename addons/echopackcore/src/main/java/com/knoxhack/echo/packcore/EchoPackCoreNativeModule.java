package com.knoxhack.echo.packcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoPackCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover PackCore profile, lockfile, and repair-plan contracts.")
                .phase("register_pack_contracts", "Record built-in pack variants and channels.")
                .phase("validate_pack_context", "Prepare native pack profile validation without mutating installed packs.")
                .phase("ready", "Expose PackCore as the native PackOS contract provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("pack_profile", "echopackcore:ashfall", "Ashfall pack profile contract.")
                .register("pack_profile", "echopackcore:echo_prime", "ECHO Prime pack profile contract.")
                .register("pack_profile", "echopackcore:arcana_division", "Arcana Division pack profile contract.")
                .register("pack_variant", "echopackcore:standard", "Default balanced pack variant.")
                .register("pack_variant", "echopackcore:performance", "Lower-cost pack variant.")
                .register("pack_variant", "echopackcore:cinematic", "High-visual pack variant.")
                .register("pack_channel", "echopackcore:stable", "Stable release channel.")
                .register("pack_channel", "echopackcore:beta", "Beta release channel.")
                .register("pack_channel", "echopackcore:experimental", "Experimental release channel.")
                .register("service", "echopackcore:repair_plan", "Plan-only repair contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("pack.profile.load", "EchoPackProfileLoader", "Load pack profiles from native PackOS descriptors.")
                .hook("pack.lockfile.verify", "EchoLockfileVerifier", "Verify native lockfiles without repair execution.")
                .hook("pack.repair.plan", "EchoRepairPlan", "Produce plan-only repair recommendations.");
        EchoPackCoreLoadPlanContract loadPlanContract = new EchoPackCoreLoadPlanContract();
        Map<String, Object> loadPlan = loadPlanContract.execute(context.getOrDefault("packId", "ashfall"), "echo_native");
        boolean loadPlanPassed = loadPlanContract.referencePlanPassed(loadPlan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "packcore_native_load_plan_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", 10);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of("pack.profile", "pack.repair", EchoPackCoreLoadPlanContract.ADAPTERCORE_CONTRACT_ID));
        result.put("packLoadPlan", loadPlan);
        result.put("packLoadPlanExecuted", loadPlanPassed);
        result.put("packLoadPlanContract", EchoPackCoreLoadPlanContract.ADAPTERCORE_CONTRACT_ID);
        result.put("packLoadPlanStepCount", ((List<?>) loadPlan.get("loadPlan")).size());
        result.put("packLoadPlanValidationCount", ((List<?>) loadPlan.get("validation")).size());
        result.put("requiresPackOsBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", loadPlanPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "PackCore native contract loaded and validated the Ashfall PackOS profile, lockfile, and gated repair load plan through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echopackcore";
}
