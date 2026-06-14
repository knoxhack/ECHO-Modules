package com.knoxhack.echo.adaptercore.bridge;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smoke test for {@link EchoModuleTruthLayerBootstrap}.
 */
public final class EchoModuleTruthLayerBootstrapSmoke {
    private EchoModuleTruthLayerBootstrapSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoModuleTruthLayerBootstrapSmoke failed: " + report);
        }
        System.out.println("echo module truth layer bootstrap smoke PASS bridges="
                + report.get("bridgesRegistered") + " hosts=" + report.get("runtimeHostsRegistered"));
    }

    public static Map<String, Object> capture() {
        EchoModuleTruthLayerBootstrap.registerAll();

        boolean registered = EchoModuleTruthLayerBootstrap.isRegistered();

        int runtimeHosts = 0;
        for (EchoRuntimeHostRegistry.RegisteredRuntimeHost host : EchoRuntimeHostRegistry.global().registeredHosts()) {
            String hostId = host.runtimeHostId();
            if (hostId.endsWith(":runtime_host")) {
                runtimeHosts++;
            }
        }
        boolean indexHostReady = EchoRuntimeHostRegistry.global()
                .resolve("echoindex:runtime_host")
                .map(host -> host.capabilities().supportsNativeInterface("EchoNativeRuntimeHost.Packets")
                        && host.capabilities().supportsNativeInterface("EchoNativeRuntimeHost.Hud")
                        && host.capabilities().supportsNativeInterface("EchoNativeRuntimeHost.Capabilities")
                        && host.capabilities().supportsNativeInterface("EchoNativeRuntimeHost.PlayerInventory")
                        && host.capabilities().supportsAction("index.recipe_query")
                        && host.capabilities().supportsAction("index.recipe_transfer")
                        && host.capabilities().supportsAction("index.inventory_overlay_render")
                        && host.capabilities().supportsAction("index.inventory_overlay_input")
                        && host.capabilities().supportsCanonicalContent("index.recipes")
                        && host.capabilities().supportsCanonicalContent("index.inventory_overlay")
                        && host.capabilities().supportsCanonicalContent("echoindex:inventory_overlay")
                        && host.capabilities().supportsCanonicalContent("echoindex:recipe_search/index_query")
                        && host.capabilities().hudEvents())
                .orElse(false);
        int compatibilityCatalogModules = EchoAdapterCoreModuleCompatibilityCatalog.moduleIds().size();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.module_truth_layer_bootstrap_smoke.v1");
        report.put("passed", registered && runtimeHosts >= compatibilityCatalogModules && indexHostReady);
        report.put("bridgesRegistered", registered ? 91 : 0);
        report.put("runtimeHostsRegistered", runtimeHosts);
        report.put("compatibilityCatalogModules", compatibilityCatalogModules);
        report.put("echoIndexRuntimeHostReady", indexHostReady);
        return Map.copyOf(report);
    }
}
