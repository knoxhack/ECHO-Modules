package com.knoxhack.echoruntimeguard.report;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.NetworkSnapshot;
import com.knoxhack.echoruntimeguard.api.ParticleBudgetSnapshot;
import com.knoxhack.echoruntimeguard.api.ProfilerEntry;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import com.knoxhack.echoruntimeguard.api.ValidationQueueSnapshot;
import com.knoxhack.echoruntimeguard.runtime.BlockEntitySleepService;
import com.knoxhack.echoruntimeguard.runtime.EntityAiGuardService;
import com.knoxhack.echoruntimeguard.runtime.IntegrationThrottleService;
import com.knoxhack.echoruntimeguard.runtime.LagSpikeReporter;
import com.knoxhack.echoruntimeguard.runtime.MultiblockValidationScheduler;
import com.knoxhack.echoruntimeguard.runtime.NetworkBudgetService;
import com.knoxhack.echoruntimeguard.runtime.ParticleBudgetService;
import com.knoxhack.echoruntimeguard.runtime.PerformanceBudgetService;
import com.knoxhack.echoruntimeguard.runtime.RuntimeModeService;
import com.knoxhack.echoruntimeguard.runtime.RuntimeProfilerService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class RuntimeGuardReportWriter {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private RuntimeGuardReportWriter() {
    }

    public static Path write(MinecraftServer server) throws IOException {
        RuntimeMetricsSnapshot metrics = RuntimeProfilerService.INSTANCE.snapshot(server);
        ParticleBudgetSnapshot particles = ParticleBudgetService.INSTANCE.getSnapshot();
        ValidationQueueSnapshot validations = MultiblockValidationScheduler.INSTANCE.getSnapshot();
        Path root = reportRoot(server);
        Files.createDirectories(root);
        String stamp = LocalDateTime.now().format(FILE_STAMP);
        Path report = root.resolve("runtimeguard-report-" + stamp + ".txt");
        Path json = root.resolve("runtimeguard-report-" + stamp + ".json");
        Files.writeString(report, content(metrics, particles, validations), StandardCharsets.UTF_8);
        Files.writeString(json, jsonContent(metrics, particles, validations), StandardCharsets.UTF_8);
        return report;
    }

    private static String content(RuntimeMetricsSnapshot metrics, ParticleBudgetSnapshot particles,
            ValidationQueueSnapshot validations) {
        StringBuilder builder = new StringBuilder();
        builder.append("ECHO RuntimeGuard Performance Report\n\n");
        builder.append("General:\n");
        builder.append("- Runtime mode: ").append(metrics.mode().displayName()).append('\n');
        builder.append("- Emergency mode: ").append(metrics.emergency() ? "on" : "off").append('\n');
        builder.append("- Mode source: ").append(RuntimeModeService.INSTANCE.modeSource()).append('\n');
        builder.append("- RuntimeGuard enabled: ").append(RuntimeGuardConfig.enabled()).append('\n');
        builder.append("- Average TPS: ").append(format(metrics.averageTps())).append('\n');
        builder.append("- Average MSPT: ").append(format(metrics.averageMspt())).append('\n');
        builder.append("- Worst MSPT: ").append(format(metrics.worstMsptLastMinute())).append('\n');
        builder.append("- Lag spikes: ").append(metrics.lagSpikesLastMinute()).append('\n');
        builder.append("- Uptime sample window: ").append(metrics.sampledTicks()).append(" tick(s)\n\n");

        builder.append("World:\n");
        builder.append("- Loaded chunks: ").append(metrics.loadedChunks()).append('\n');
        builder.append("- Entity count: ").append(metrics.entityCount()).append('\n');
        builder.append("- Block entity count: ").append(metrics.blockEntityCount()).append('\n');
        builder.append("- Players: ").append(metrics.players()).append("\n\n");

        builder.append("Budgets:\n");
        builder.append("- Particle budget/current usage: ").append(particles.budget()).append('/')
                .append(particles.used()).append(" denied ").append(particles.denied()).append('\n');
        builder.append("- Multiblock validation queue size: ").append(validations.queued())
                .append(" merged ").append(validations.mergedRequests()).append('\n');
        builder.append("- Lens scan budget: ").append(IntegrationThrottleService.INSTANCE.getDeepScanBudgetPerTick(null))
                .append(" deep units/tick\n");
        builder.append("- HoloMap refresh interval: ").append(IntegrationThrottleService.INSTANCE.getHoloMapRefreshIntervalTicks())
                .append(" ticks\n");
        builder.append("- Network packet warnings: ").append(NetworkBudgetService.INSTANCE.getSnapshot().warnings()).append("\n\n");

        builder.append("Config:\n");
        builder.append("- TPS guard: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.TPS_GUARD_ENABLED, true)).append('\n');
        builder.append("- Smart ticking: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.SMART_TICK_ENABLED, true)).append('\n');
        builder.append("- Particle guard: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.PARTICLE_BUDGET_ENABLED, true)).append('\n');
        builder.append("- Network guard: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.NETWORK_GUARD_ENABLED, true)).append('\n');
        builder.append("- Multiblock scheduler: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.MULTIBLOCK_SCHEDULER_ENABLED, true)).append('\n');
        builder.append("- Lens guard: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.LENS_GUARD_ENABLED, true)).append('\n');
        builder.append("- HoloMap guard: ").append(RuntimeGuardConfig.safeBool(RuntimeGuardConfig.HOLOMAP_GUARD_ENABLED, true)).append("\n\n");

        builder.append("Work Usage This Tick:\n");
        Map<RuntimeWorkType, Integer> usage = PerformanceBudgetService.INSTANCE.usageSnapshot();
        if (usage.isEmpty()) {
            builder.append("- none recorded\n");
        } else {
            usage.forEach((type, count) -> builder.append("- ").append(type.name()).append(": ")
                    .append(count).append('/').append(PerformanceBudgetService.INSTANCE.budgetFor(type)).append('\n'));
        }
        builder.append('\n');

        builder.append("Top Issues:\n");
        List<String> warnings = LagSpikeReporter.INSTANCE.warnings();
        if (warnings.isEmpty()) {
            builder.append("- none recorded\n");
        } else {
            warnings.stream().limit(8).forEach(warning -> builder.append("- ").append(warning).append('\n'));
        }
        builder.append('\n');

        builder.append("Auto Actions Taken:\n");
        builder.append("- particle reduction: ").append(RuntimeModeService.INSTANCE.isEmergency() ? "active" : "budgeted").append('\n');
        builder.append("- emergency mode activation: ").append(RuntimeModeService.INSTANCE.automaticEmergency() ? "auto" : RuntimeModeService.INSTANCE.forcedEmergency() ? "forced" : "inactive").append('\n');
        builder.append("- multiblock queue limiting: active\n");
        builder.append("- HoloMap refresh throttle: active\n");
        builder.append("- far AI throttle: opt-in only\n\n");

        builder.append("Top Profiled Costs:\n");
        List<ProfilerEntry> entries = RuntimeGuardProfiler.getTopCosts();
        if (entries.isEmpty()) {
            builder.append("- unavailable; no RuntimeGuardProfiler-wrapped operations recorded\n");
        } else {
            entries.forEach(entry -> builder.append("- ").append(entry.id()).append(": ")
                    .append(format(entry.totalMillis())).append("ms total, ")
                    .append(format(entry.averageMillis())).append("ms avg, calls ")
                    .append(entry.calls()).append('\n'));
        }
        builder.append('\n');

        builder.append("Recommendations:\n");
        builder.append("- reduce particle mode if particle usage reaches budget\n");
        builder.append("- increase HoloMap refresh interval if marker refresh appears in top costs\n");
        builder.append("- lower multiblock validations per tick if validation queues spike MSPT\n");
        builder.append("- reduce entity spawn caps if entity count becomes available and high\n");
        builder.append("- enable Server mode for dedicated-server-heavy packs\n");
        builder.append("- inspect specific modules that call RuntimeGuardProfiler wrappers\n\n");

        builder.append("RuntimeGuard State:\n");
        builder.append("- Block entity sleep registry: ").append(BlockEntitySleepService.INSTANCE.tracked()).append('\n');
        builder.append("- Entity AI guard: ").append(EntityAiGuardService.INSTANCE.statusLine()).append('\n');
        builder.append("- Integration guard: ").append(IntegrationThrottleService.INSTANCE.statusLine()).append('\n');
        return builder.toString();
    }

    private static String jsonContent(RuntimeMetricsSnapshot metrics, ParticleBudgetSnapshot particles,
            ValidationQueueSnapshot validations) {
        NetworkSnapshot network = NetworkBudgetService.INSTANCE.getSnapshot();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendField(builder, "schema", "echoruntimeguard:runtime_report/v1", true);
        appendField(builder, "enabled", RuntimeGuardConfig.enabled(), true);
        appendField(builder, "mode", metrics.mode().id(), true);
        appendField(builder, "modeSource", RuntimeModeService.INSTANCE.modeSource(), true);
        appendField(builder, "emergency", metrics.emergency(), true);
        appendField(builder, "currentTps", metrics.currentTps(), true);
        appendField(builder, "averageTps", metrics.averageTps(), true);
        appendField(builder, "currentMspt", metrics.currentMspt(), true);
        appendField(builder, "averageMspt", metrics.averageMspt(), true);
        appendField(builder, "worstMsptLastMinute", metrics.worstMsptLastMinute(), true);
        appendField(builder, "lagSpikesLastMinute", metrics.lagSpikesLastMinute(), true);
        appendField(builder, "sampledTicks", metrics.sampledTicks(), true);
        appendField(builder, "loadedChunks", metrics.loadedChunks(), true);
        appendField(builder, "entityCount", metrics.entityCount(), true);
        appendField(builder, "blockEntityCount", metrics.blockEntityCount(), true);
        appendField(builder, "players", metrics.players(), true);
        builder.append("  \"particles\": {");
        builder.append("\"budget\": ").append(particles.budget()).append(", ");
        builder.append("\"used\": ").append(particles.used()).append(", ");
        builder.append("\"denied\": ").append(particles.denied()).append("},\n");
        builder.append("  \"validations\": {");
        builder.append("\"queued\": ").append(validations.queued()).append(", ");
        builder.append("\"dirtyPositions\": ").append(validations.dirtyPositions()).append(", ");
        builder.append("\"ranLastTick\": ").append(validations.ranLastTick()).append(", ");
        builder.append("\"mergedRequests\": ").append(validations.mergedRequests()).append("},\n");
        builder.append("  \"network\": {");
        builder.append("\"packetsThisSecond\": ").append(network.packetsThisSecond()).append(", ");
        builder.append("\"bytesThisSecond\": ").append(network.bytesThisSecond()).append(", ");
        builder.append("\"warnings\": ").append(network.warnings()).append(", ");
        builder.append("\"duplicateDrops\": ").append(network.duplicateDrops()).append("},\n");
        builder.append("  \"topCosts\": [");
        List<ProfilerEntry> entries = RuntimeGuardProfiler.getTopCosts();
        for (int i = 0; i < entries.size(); i++) {
            ProfilerEntry entry = entries.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("{\"id\": \"").append(escape(entry.id().toString())).append("\", ");
            builder.append("\"calls\": ").append(entry.calls()).append(", ");
            builder.append("\"totalMillis\": ").append(format(entry.totalMillis())).append(", ");
            builder.append("\"maxMillis\": ").append(format(entry.maxMillis())).append('}');
        }
        builder.append("]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static void appendField(StringBuilder builder, String name, String value, boolean comma) {
        builder.append("  \"").append(name).append("\": \"").append(escape(value)).append('"');
        builder.append(comma ? ",\n" : "\n");
    }

    private static void appendField(StringBuilder builder, String name, boolean value, boolean comma) {
        builder.append("  \"").append(name).append("\": ").append(value);
        builder.append(comma ? ",\n" : "\n");
    }

    private static void appendField(StringBuilder builder, String name, double value, boolean comma) {
        builder.append("  \"").append(name).append("\": ")
                .append(String.format(java.util.Locale.ROOT, "%.3f", value));
        builder.append(comma ? ",\n" : "\n");
    }

    private static void appendField(StringBuilder builder, String name, int value, boolean comma) {
        builder.append("  \"").append(name).append("\": ").append(value);
        builder.append(comma ? ",\n" : "\n");
    }

    private static void appendField(StringBuilder builder, String name, long value, boolean comma) {
        builder.append("  \"").append(name).append("\": ").append(value);
        builder.append(comma ? ",\n" : "\n");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Path reportRoot(MinecraftServer server) {
        if (server != null) {
            try {
                Path worldRoot = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                Path runRoot = findRunRoot(worldRoot);
                return runRoot.resolve("echo-runtimeguard").resolve("reports");
            } catch (RuntimeException exception) {
                // Fall through to the conventional dev-run path.
            }
        }
        return Path.of("run", "echo-runtimeguard", "reports");
    }

    private static Path findRunRoot(Path path) {
        for (Path cursor = path; cursor != null; cursor = cursor.getParent()) {
            Path fileName = cursor.getFileName();
            if (fileName != null && "run".equals(fileName.toString())) {
                return cursor;
            }
        }
        return path.getParent() == null ? path : path.getParent();
    }
}
