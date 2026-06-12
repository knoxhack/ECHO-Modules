package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echorendercore.profile.CreatorAddonShowcaseRegistry;
import com.knoxhack.echorecovery.EchoRecovery;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class RecoveryRenderCoreIntegration {
    public static final Identifier GRAVE_SCREEN_PROFILE =
            Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "screen/recovery_grave");

    private static boolean registered;

    private RecoveryRenderCoreIntegration() {
    }

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDiagnosticService(RecoveryRenderCoreIntegration::diagnostics);
    }

    private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        var showcase = CreatorAddonShowcaseRegistry.integrated(
                EchoRecovery.MODID,
                "ECHO: Recovery",
                "screen",
                "grave_screen",
                GRAVE_SCREEN_PROFILE,
                "dark_fallback",
                "Verify grave/cache screen chrome remains readable with 54 slots and deterministic Recover All feedback.",
                "recovery UI consumes ThemeCore colors and exposes a RenderCore screen profile id.");
        return List.of(new EchoDiagnosticBlocker(
                id("diagnostic/rendercore_surface"),
                "recovery",
                EchoDiagnosticBlocker.Severity.INFO,
                "Recovery RenderCore Surface",
                "Declared " + showcase.surface() + " surface using " + showcase.showcaseProfile() + ".",
                showcase.qaNotes()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
