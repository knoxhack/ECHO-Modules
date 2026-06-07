package com.knoxhack.echonetcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNetCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> networkService = EchoNetCorePacketServiceContract.executeReferenceService(
                context.getOrDefault("packId", "unknown")
        );
        boolean networkServicePassed = EchoNetCorePacketServiceContract.referenceServicePassed(networkService);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover NetCore packet and guard contracts.")
                .phase("register_payload_contracts", "Record ECHO packet channels before a native network bridge mutates Minecraft networking.")
                .phase("attach_network_events", "Record logout and shutdown hooks for native rate-limit cleanup.")
                .phase("execute_network_service", "Execute packet service routing and rate-limit reference behavior.")
                .phase("ready", "Expose NetCore as the native packet bridge provider for downstream Ashfall modules.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("network_payload", "echonetcore:faction_sync", "Clientbound faction sync payload.")
                .register("network_payload", "echonetcore:discovery_toast", "Clientbound discovery toast payload.")
                .register("network_payload", "echonetcore:echo_sync", "Generic ECHO sync payload.")
                .register("network_payload", "echonetcore:debug_command", "Serverbound debug command payload gated by policy.")
                .register("service", "echonetcore:network_service", "ECHO core network service provider.")
                .register("policy", "echonetcore:rate_limiter", "Native-visible network action rate limit policy.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("network.payload_register", "EchoNetCorePackets.register", "Attach packet contracts to the future native network bridge.")
                .hook("player.logout", "EchoRateLimiter.clearPlayer", "Clear per-player rate-limit state.")
                .hook("server.stopping", "EchoRateLimiter.onServerStopping", "Clear server network state.")
                .hook("config.common", "EchoNetCoreConfig.registerEchoConfig", "Expose NetCore common config contract.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "netcore_native_packet_service_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("networkService", networkService);
        result.put("networkServiceExecuted", networkServicePassed);
        result.put("logicalRegistrationCount", 6);
        result.put("eventHookCount", 4);
        result.put("registeredFeatureContracts", List.of(
                "echo.net",
                EchoNetCorePacketServiceContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresNetworkBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", networkServicePassed);
        result.put("transformsPerformed", false);
        result.put("summary", "NetCore native contract registered packet hooks and executed the AdapterCore packet service routing behavior.");
        return result;
    }

    private static final String MODULE_ID = "echonetcore";
}
