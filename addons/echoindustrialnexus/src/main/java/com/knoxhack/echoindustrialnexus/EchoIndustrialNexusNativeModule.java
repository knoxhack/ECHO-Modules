package com.knoxhack.echoindustrialnexus;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoIndustrialNexusNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        boolean commonRegistered = ensureCommonServicesRegisteredForNativeLoader(context);
        context.attribute("nativeCommonServicesRegistered", commonRegistered);
        context.attribute("nativeCommonServicesAlreadyRegistered", !commonRegistered);
        context.recordMutation(
                "platform_services",
                commonRegistered ? "register" : "already_registered",
                "echoindustrialnexus:common_services",
                commonRegistered ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED);
        EchoNativeSurfaceModuleEntrypoint.super.ready(context);
    }

    private static boolean ensureCommonServicesRegisteredForNativeLoader(EchoNativeModuleLoadContext context) {
        String moduleClassName = EchoIndustrialNexusNativeModule.class.getPackageName() + ".EchoIndustrialNexus";
        try {
            Object result = Class.forName(moduleClassName)
                    .getMethod("ensureCommonServicesRegisteredForNativeLoader")
                    .invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeCommonServicesDeferred", true);
            context.attribute("nativeCommonServicesDeferredReason", exception.getClass().getSimpleName());
            return false;
        }
    }

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> nativeHost = Agent9IndustrialNexusRuntimeAdapter.activateNativeHostEntrypoint();
        Map<String, Object> result = new LinkedHashMap<>(nativeHost);
        boolean passed = "PASS".equals(nativeHost.get("status"));
        result.put("activated", passed);
        result.put("activationStage", "industrialnexus_agent9_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", List.of(String.valueOf(nativeHost.get("adapterCoreContract"))));
        result.put("runtimeTargets", List.of("adapter_backend", "echo_native", "echo_runtime_standalone"));
        result.put("summary", "IndustrialNexus native module executed the Agent9 industrial runtime adapter through AdapterCore.");
        return Map.copyOf(result);
    }
}
