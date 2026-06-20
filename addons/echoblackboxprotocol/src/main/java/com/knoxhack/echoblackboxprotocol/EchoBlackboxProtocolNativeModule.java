package com.knoxhack.echoblackboxprotocol;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoBlackboxProtocolNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        boolean commonRegistered = ensureCommonServicesRegisteredForNativeLoader(context);
        context.attribute("nativeCommonServicesRegistered", commonRegistered);
        context.attribute("nativeCommonServicesAlreadyRegistered", !commonRegistered);
        context.recordMutation(
                "platform_services",
                commonRegistered ? "register" : "already_registered",
                "echoblackboxprotocol:common_services",
                commonRegistered ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED);
        EchoNativeModuleAdapter.super.ready(context);
    }

    private static boolean ensureCommonServicesRegisteredForNativeLoader(EchoNativeModuleLoadContext context) {
        String moduleClassName = EchoBlackboxProtocolNativeModule.class.getPackageName() + ".EchoBlackboxProtocol";
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

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Blackbox archive and Prime route memory contracts.")
                .phase("register_story_contracts", "Record core memory archive and lore handoff contracts.")
                .phase("attach_story_events", "Record archive unlock and Prime route handoff hooks.")
                .phase("ready", "Expose Blackbox Protocol as a native Prime-route archive provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("archive_entry", "echoblackboxprotocol:archive/core_memory", "Blackbox core memory archive contract.")
                .register("lore_surface", "echoblackboxprotocol:lore/core_memory", "Core memory Index/Wiki/Lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("archive.unlock", "BlackboxTerminalIntegration.unlockCoreMemory", "Unlock the Blackbox core memory archive.")
                .hook("story.route.handoff", "BlackboxPrimeIntegration.publishPrimeRoute", "Hand off the recovered memory to Prime Core.")
                .hook("lore.index.update", "BlackboxIndexProvider.publishCoreMemory", "Publish Blackbox memory records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .readDataDrive(
                        "echoorbitalremnants:data_drive/orbital_blackbox",
                        "echoblackboxprotocol:archive/core_memory",
                        "echoprimecore:story_flag/prime_route_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "blackbox_prime_route_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntime.report());
        result.put("logicalRegistrationCount", 2);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of(
                "blackbox.archive.core_memory",
                "blackbox.prime_route_handoff",
                "blackbox.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Blackbox Protocol native contract registered the core memory archive and Prime route handoff through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoblackboxprotocol";
}
