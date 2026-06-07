package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.NetworkSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.ValidationQueueSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public final class RuntimeGuardDiagnostics {
    private RuntimeGuardDiagnostics() {
    }

    public static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        List<EchoDiagnosticBlocker> diagnostics = new ArrayList<>();
        RuntimeMetricsSnapshot metrics = RuntimeProfilerService.INSTANCE.lastSnapshot();
        if (!RuntimeGuardConfig.enabled()) {
            diagnostics.add(info("disabled", "RuntimeGuard disabled",
                    "RuntimeGuard is loaded but its master switch is off.",
                    "Enable RuntimeGuard if shared budget diagnostics are expected."));
            return diagnostics;
        }
        if (RuntimeModeService.INSTANCE.isEmergency()) {
            diagnostics.add(warning("emergency_mode", "RuntimeGuard emergency mode active",
                    "RuntimeGuard is applying emergency-mode budgets. Source: " + RuntimeModeService.INSTANCE.modeSource() + ".",
                    "Inspect /echo_perf status and /echo_perf dump for current pressure."));
        }
        if (metrics.averageTps() <= RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.WARNING_TPS, 18.0D)
                || metrics.averageMspt() >= 55.0D) {
            diagnostics.add(warning("server_pressure", "Server tick pressure",
                    "Average TPS " + one(metrics.averageTps()) + ", average MSPT " + one(metrics.averageMspt()) + ".",
                    "Use /echo_perf top and /echo_perf dump to identify profiled expensive work."));
        }
        if (metrics.lagSpikesLastMinute() > 0) {
            diagnostics.add(warning("lag_spikes", "Recent lag spikes",
                    metrics.lagSpikesLastMinute() + " spike(s) were recorded in the current sample window.",
                    "Review RuntimeGuard report Top Issues for recent spike context."));
        }
        ValidationQueueSnapshot validations = MultiblockValidationScheduler.INSTANCE.getSnapshot();
        int validationLimit = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.MAX_VALIDATIONS_PER_TICK, 2) * 20;
        if (validations.queued() > validationLimit) {
            diagnostics.add(warning("validation_backlog", "Validation queue backlog",
                    validations.queued() + " multiblock validation request(s) are queued.",
                    "Reduce validation frequency or raise the RuntimeGuard validation budget."));
        }
        NetworkSnapshot network = NetworkBudgetService.INSTANCE.getSnapshot();
        if (network.warnings() > 0) {
            diagnostics.add(warning("network_pressure", "RuntimeGuard network pressure",
                    network.warnings() + " network budget warning(s), " + network.duplicateDrops() + " duplicate drop(s).",
                    "Inspect /echo_perf network and batch non-critical sync traffic."));
        }
        return diagnostics;
    }

    private static EchoDiagnosticBlocker info(String path, String title, String detail, String nextAction) {
        return blocker(path, EchoDiagnosticBlocker.Severity.INFO, title, detail, nextAction);
    }

    private static EchoDiagnosticBlocker warning(String path, String title, String detail, String nextAction) {
        return blocker(path, EchoDiagnosticBlocker.Severity.WARNING, title, detail, nextAction);
    }

    private static EchoDiagnosticBlocker blocker(String path, EchoDiagnosticBlocker.Severity severity,
            String title, String detail, String nextAction) {
        return new EchoDiagnosticBlocker(EchoRuntimeGuard.id("diagnostics/" + path), EchoRuntimeGuard.CHAPTER_ID,
                severity, title, detail, nextAction);
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
