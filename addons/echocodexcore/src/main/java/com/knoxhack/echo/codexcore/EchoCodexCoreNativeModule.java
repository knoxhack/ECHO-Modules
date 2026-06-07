package com.knoxhack.echo.codexcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCodexCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Codex archive, category, entry, and discovery-state contracts.")
                .phase("register_codex_contracts", "Record Codex entries and search metadata before native UI execution.")
                .phase("attach_codex_events", "Record packet-consumer and discovery hooks.")
                .phase("ready", "Expose CodexCore as the native Ashfall opening-route entry surface.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("codex_archive", "echocodexcore:archives", "Codex archive contract.")
                .register("codex_category", "echocodexcore:categories", "Codex category contract.")
                .register("codex_entry", "echocodexcore:entries", "Codex entry contract.")
                .register("codex_discovery", "echocodexcore:discovery_state", "Codex discovery state contract.")
                .register("service", "echocodexcore:codex_service", "Codex runtime service contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("codex.packet", "EchoCodexCoreNativeModule.consumeAshfallRuntimePackets", "Consume AdapterCore Codex runtime packets.")
                .hook("codex.discovery", "EchoCodexDiscoveryState", "Record discovered Codex entries.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("holomap_lens_codex_wiki", "echocodexcore:codex_service", "codex_runtime",
                        "Keeps Codex opening entries ready for AdapterCore packets.",
                        "codex.entries", "codex.discovery");
        Map<String, Object> codexLookup = EchoCodexEntryLookupContract.executeReferenceLookup(
                EchoCodexEntryLookupContract.REFERENCE_QUERY);
        boolean lookupPassed = EchoCodexEntryLookupContract.referenceLookupPassed(codexLookup);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "codexcore_native_entry_lookup_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("logicalRegistrationCount", 5);
        result.put("eventHookCount", 2);
        result.put("approvedNativeServiceCount", 1);
        result.put("registeredFeatureContracts", List.of(
                "codex.discovery",
                "codex.entries",
                EchoCodexEntryLookupContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("codexLookup", codexLookup);
        result.put("codexLookupExecuted", lookupPassed);
        result.put("requiresCodexBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", lookupPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "CodexCore native contract registered opening-entry/discovery contracts and executed the AdapterCore codex entry lookup service.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echocodexcore:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echocodexcore:opening_entries"));
    }

    private static final String MODULE_ID = "echocodexcore";
}
