package com.knoxhack.echogalacticcore.integration;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.asdk.GalacticCoreNativeMutations;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;
import dev.echo.nativeplatform.contracts.EchoNativeScreenService;

import java.util.List;
import java.util.Map;

public final class GalacticCoreEchoIntegrations {
    private static final List<Integration> INTEGRATIONS = List.of(
            new Integration("echopackcore", "packos_profile", "PackOS module profile and dependency diagnostics"),
            new Integration("echoindex", "index_catalog", "Index entries for planets, oxygen, rockets, machines, schematics, fuels, mobs, and dungeons"),
            new Integration("echolens", "lens_scan", "Lens scans for oxygen, pressure, thermal risk, gravity, radiation, machine state, and rocket readiness"),
            new Integration("echoholomap", "holomap_routes", "HoloMap celestial routes for Orbit, Moon, Mars, Asteroids, Venus, and space stations"),
            new Integration("echoscreencore", "screencore_surfaces", "ScreenCore launch checklist, machine screens, rocket inventory, and celestial selection"),
            new Integration("echoashfallprotocol", "ashfall_milestones", "Optional Ashfall milestone mirroring for launch and planet progression")
    );

    private GalacticCoreEchoIntegrations() {
    }

    public static void register(
            EchoNativeModuleLoadContext context,
            EchoNativeCapabilityService capabilities,
            EchoNativeResourceService resources,
            EchoNativeScreenService screens
    ) {
        for (Integration integration : INTEGRATIONS) {
            String target = GalacticCoreIds.id("integration/" + integration.path());
            Map<String, Object> evidence = Map.of(
                    "optionalModule", integration.moduleId(),
                    "description", integration.description(),
                    "required", false,
                    "typedReceiptsOnly", true
            );
            GalacticCoreNativeMutations.record(
                    context,
                    capabilities.registerIntegration(GalacticCoreNativeMutations.common(
                            "capabilities",
                            "registerIntegration",
                            target,
                            evidence
                    ))
            );
            GalacticCoreNativeMutations.record(
                    context,
                    resources.registerReloadListener(GalacticCoreNativeMutations.common(
                            "resources",
                            "registerReloadListener",
                            target,
                            evidence
                    ))
            );
            if ("echoscreencore".equals(integration.moduleId()) || "echoholomap".equals(integration.moduleId())) {
                GalacticCoreNativeMutations.record(
                        context,
                        screens.registerSurface(GalacticCoreNativeMutations.client(
                                "screens",
                                "registerSurface",
                                target,
                                evidence
                        ))
                );
            }
        }
    }

    private record Integration(String moduleId, String path, String description) {
    }
}
