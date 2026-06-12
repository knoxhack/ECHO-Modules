package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class RecoveryRuntimeGuardIntegration {
    private static boolean registered;

    private RecoveryRuntimeGuardIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDiagnosticService(RecoveryRuntimeGuardIntegration::diagnostics);
        EchoRecovery.LOGGER.info("Recovery RuntimeGuard diagnostics registered.");
    }

    private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return List.of();
        }
        List<EchoDiagnosticBlocker> diagnostics = new ArrayList<>();
        int active = 0;
        if (level.getServer() != null) {
            for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                active += RecoveryWorldData.getOrCreate(serverLevel).getActiveGraves(player.getUUID()).size();
            }
        } else {
            active = RecoveryWorldData.getOrCreate(level).getActiveGraves(player.getUUID()).size();
        }
        if (active > 0) {
            diagnostics.add(new EchoDiagnosticBlocker(
                    id("diagnostic/active_graves"),
                    "recovery",
                    EchoDiagnosticBlocker.Severity.INFO,
                    "Active Recovery Graves",
                    active + " active grave/cache record(s) are tracked for this player.",
                    "Use /graves list or a Recovery Compass to locate the nearest cache."));
        }
        if (!RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()) {
            diagnostics.add(new EchoDiagnosticBlocker(
                    id("diagnostic/remote_recovery_disabled"),
                    "recovery",
                    EchoDiagnosticBlocker.Severity.INFO,
                    "Remote Recovery Disabled",
                    "Remote recovery is disabled by default for the forgiving beta lane.",
                    "Recover in person, or enable remote_recovery_enabled for command/service restores."));
        }
        return List.copyOf(diagnostics);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
