package com.knoxhack.echolens.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoServiceRegistry;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.ILensInspectionService;
import com.knoxhack.echolens.registry.LensInspectionService;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public final class LensCoreIntegration {
    private static boolean registered;

    private LensCoreIntegration() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        EchoServiceRegistry.register(ILensInspectionService.class, LensInspectionService.INSTANCE);
        EchoCoreServices.registerLensService(LensCoreService.INSTANCE);
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return "lens";
            }

            @Override
            public String modId() {
                return EchoLens.MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: Lens";
            }

            @Override
            public String summary() {
                return "Smart scanner HUD with local inspection and server-assisted Deep Scan.";
            }

            @Override
            public String statusLine(Player player) {
                return "Lens online. Providers: " + LensProviderRegistry.count()
                        + " / server: " + LensProviderRegistry.serverProviders().size() + ".";
            }
        });
        EchoCoreServices.registerRouteRecordService(player -> List.of(new EchoRouteRecord(
                EchoLens.id("route/lens_online"),
                "lens",
                "Lens Scanner HUD",
                "Inspection",
                "Any dimension",
                "ONLINE",
                "Hold Shift for local details or the Deep Scan key for server-assisted public diagnostics.",
                true)));
        EchoCoreServices.registerDiagnosticService(player -> {
            if (LensProviderRegistry.count() > 0) {
                return List.of();
            }
            return List.of(new EchoDiagnosticBlocker(
                    EchoLens.id("diagnostics/no_providers"),
                    "lens",
                    EchoDiagnosticBlocker.Severity.WARNING,
                    "Lens has no providers",
                    "The scanner service is loaded but no information providers are registered.",
                    "Reload the pack or check addon startup logs."));
        });
        registered = true;
    }
}
