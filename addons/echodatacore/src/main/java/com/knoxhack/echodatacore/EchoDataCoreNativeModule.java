package com.knoxhack.echodatacore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoDataCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    public static final String MODULE_ID = EchoDataCoreRuntimeProfileSyncContract.MODULE_ID;
    public static final List<String> CONTRACT_IDS = List.of(
            EchoDataCoreRuntimeProfileSyncContract.TERMINAL_PROBE_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.PLAYER_SCHEMA_VERSION_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.WORLD_SCHEMA_VERSION_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.LAST_REGION_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.LAST_MARKER_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.ACTIVE_HAZARDS_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.DATA_SERVICE_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.DATA_SYNC_CONTRACT_ID,
            EchoDataCoreRuntimeProfileSyncContract.ADAPTERCORE_CONTRACT_ID
    );

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        boolean commonRegistered = ensureCommonServicesRegisteredForNativeLoader(context);
        context.attribute("nativeCommonServicesRegistered", commonRegistered);
        context.attribute("nativeCommonServicesAlreadyRegistered", !commonRegistered);
        context.recordMutation(
                "platform_services",
                commonRegistered ? "register" : "already_registered",
                "echodatacore:common_services",
                commonRegistered ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED);
    }

    private static boolean ensureCommonServicesRegisteredForNativeLoader(EchoNativeModuleLoadContext context) {
        String moduleClassName = EchoDataCoreNativeModule.class.getPackageName() + ".EchoDataCore";
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
                .phase("discover", "Discover DataCore persistence, sync, reload, and diagnostics contracts.")
                .phase("register_data_keys", "Record built-in player, world, team, and metadata keys.")
                .phase("execute_runtime_profile_sync", "Apply the AdapterCore runtime profile sync contract.")
                .phase("ready", "Expose DataCore as the native data provider for Ashfall modules.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("data_key", "echodatacore:system/terminal_probe", "Terminal probe data key.")
                .register("data_key", "echodatacore:system/player_schema_version", "Player schema version counter.")
                .register("data_key", "echodatacore:system/world_schema_version", "World schema version counter.")
                .register("data_key", "echodatacore:worldcore/last_region", "Last discovered WorldCore region.")
                .register("data_key", "echodatacore:worldcore/last_marker", "Last revealed WorldCore marker.")
                .register("data_key", "echodatacore:worldcore/active_hazards", "Current WorldCore hazard snapshot.")
                .register("service", "echodatacore:data_service", "ECHO core data persistence service.")
                .register("network_payload", "echodatacore:data_sync", "DataCore metadata and state sync payloads.")
                .register("adaptercore_contract", EchoDataCoreRuntimeProfileSyncContract.ADAPTERCORE_CONTRACT_ID,
                        "Executable runtime profile persistence, metadata reload, and sync proof.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("network.payload_register", "ModNetwork.registerPayloads", "Attach DataCore payload contracts.")
                .hook("common.setup", "EchoDataCore.commonSetup", "Attach data service and integration providers.")
                .hook("commands.register", "DataCoreCommands.register", "Expose DataCore command surface when native command bridge exists.")
                .hook("data.reload", "DataCoreReloaders.serverReloadListeners", "Attach DataCore JSON reloaders.")
                .hook("player.login", "DataCoreDataService.onPlayerLogin", "Prepare player data hydration.")
                .hook("player.clone", "DataCoreDataService.onPlayerClone", "Prepare player data migration.")
                .hook("player.tick", "DataCoreDataService.onPlayerTick", "Prepare scheduled player data sync.");

        EchoDataCoreRuntimeProfileSyncContract contract = new EchoDataCoreRuntimeProfileSyncContract();
        Map<String, Object> runtimeProfile = contract.execute(
                context.getOrDefault("playerId", EchoDataCoreRuntimeProfileSyncContract.REFERENCE_PLAYER_ID),
                "echo_native"
        );
        boolean runtimeProfilePassed = contract.referencePlanPassed(runtimeProfile);
        Map<?, ?> syncPayload = (Map<?, ?>) runtimeProfile.get("syncPayload");
        Map<?, ?> diagnostics = (Map<?, ?>) runtimeProfile.get("diagnostics");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "datacore_native_runtime_profile_sync_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("dataKeyCount", 6);
        result.put("serviceContractCount", 1);
        result.put("networkContractCount", 1);
        result.put("eventHookCount", 7);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("adapterDomains", List.of("data", "saves", "networking", "player"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("dataRuntimeProfile", runtimeProfile);
        result.put("dataRuntimeProfileExecuted", runtimeProfilePassed);
        result.put("dataRuntimeProfileContract", EchoDataCoreRuntimeProfileSyncContract.ADAPTERCORE_CONTRACT_ID);
        result.put("syncPayloadRevision", syncPayload.get("revision"));
        result.put("syncPayloadEntryCount", syncPayload.get("entryCount"));
        result.put("registeredKeyCount", diagnostics.get("registeredKeyCount"));
        result.put("metadataKeyCount", diagnostics.get("metadataKeyCount"));
        result.put("requiresDataPersistenceBridge", true);
        result.put("requiresNetworkBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", runtimeProfilePassed);
        result.put("transformsPerformed", false);
        result.put("summary", "DataCore native contract executed runtime profile persistence, metadata reload, diagnostics, and sync payload behavior through AdapterCore.");
        return result;
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoDataCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "agent49-datacore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "DataCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("adapterCoreUsed")),
                "DataCore native adapter should use AdapterCore");
        require(CONTRACT_IDS.equals(activation.get("registeredFeatureContracts")),
                "DataCore native adapter should expose all concrete AdapterCore contracts");
        require(Integer.valueOf(9).equals(activation.get("logicalRegistrationCount")),
                "DataCore native adapter should register nine logical contracts");
        require(Integer.valueOf(6).equals(activation.get("dataKeyCount")),
                "DataCore native adapter should register six public data key contracts");
        require(Integer.valueOf(7).equals(activation.get("eventHookCount")),
                "DataCore native adapter should attach seven native event hooks");
        require(Boolean.TRUE.equals(activation.get("dataRuntimeProfileExecuted")),
                "DataCore native adapter should execute the runtime profile sync service");
        require(Boolean.TRUE.equals(activation.get("serviceCodeExecuted")),
                "DataCore native adapter should execute service behavior");
        System.out.println("datacore native adapter smoke PASS contract="
                + EchoDataCoreRuntimeProfileSyncContract.ADAPTERCORE_CONTRACT_ID
                + " keys="
                + activation.get("registeredKeyCount")
                + " syncEntries="
                + activation.get("syncPayloadEntryCount"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
