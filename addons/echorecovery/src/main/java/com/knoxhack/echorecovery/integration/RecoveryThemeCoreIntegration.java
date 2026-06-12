package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echorecovery.EchoRecovery;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class RecoveryThemeCoreIntegration {
    private static boolean registered;

    private RecoveryThemeCoreIntegration() {
    }

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDiagnosticService(RecoveryThemeCoreIntegration::diagnostics);
    }

    private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        int panel = EchoCoreServices.themeService().resolveColor("panel.primary", 0xFF161820);
        int accent = EchoCoreServices.themeService().resolveColor("accent.primary", 0xFF66D9EF);
        return List.of(new EchoDiagnosticBlocker(
                id("diagnostic/theme_tokens"),
                "recovery",
                EchoDiagnosticBlocker.Severity.INFO,
                "Recovery ThemeCore Styling",
                "Grave UI is consuming ThemeCore colors from " + EchoCoreServices.themeService().currentThemeName()
                        + " (panel=" + Integer.toHexString(panel) + ", accent=" + Integer.toHexString(accent) + ").",
                "Open a grave screen to review themed chrome."));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
