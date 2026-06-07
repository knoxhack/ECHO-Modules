package com.knoxhack.echoruntimeguard.command;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echoruntimeguard.api.LensScanType;
import com.knoxhack.echoruntimeguard.api.NetworkSnapshot;
import com.knoxhack.echoruntimeguard.api.ParticleBudgetSnapshot;
import com.knoxhack.echoruntimeguard.api.ProfilerEntry;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.api.ValidationQueueSnapshot;
import com.knoxhack.echoruntimeguard.report.RuntimeGuardReportWriter;
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
import com.knoxhack.echoruntimeguard.runtime.SmartTickService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class RuntimeGuardCommands {
    private RuntimeGuardCommands() {
    }

    public static void register(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echo_perf")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("mode")
                        .requires(RuntimeGuardCommands::canMutate)
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .executes(context -> setMode(context.getSource(),
                                        StringArgumentType.getString(context, "mode")))))
                .then(Commands.literal("emergency")
                        .requires(RuntimeGuardCommands::canMutate)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> emergency(context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled")))))
                .then(Commands.literal("dump")
                        .requires(RuntimeGuardCommands::canMutate)
                        .executes(context -> dump(context.getSource())))
                .then(Commands.literal("top").executes(context -> top(context.getSource())))
                .then(Commands.literal("particles").executes(context -> particles(context.getSource())))
                .then(Commands.literal("multiblocks").executes(context -> multiblocks(context.getSource())))
                .then(Commands.literal("holomap").executes(context -> holomap(context.getSource())))
                .then(Commands.literal("lens").executes(context -> lens(context.getSource())))
                .then(Commands.literal("network").executes(context -> network(context.getSource())))
                .then(Commands.literal("entities").executes(context -> entities(context.getSource())))
                .then(Commands.literal("blockentities").executes(context -> blockEntities(context.getSource())))
                .then(Commands.literal("reset")
                        .requires(RuntimeGuardCommands::canMutate)
                        .executes(context -> reset(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        RuntimeMetricsSnapshot metrics = RuntimeProfilerService.INSTANCE.snapshot(source.getServer());
        tell(source, "ECHO RuntimeGuard", ChatFormatting.AQUA);
        tell(source, "TPS: " + one(metrics.averageTps()), ChatFormatting.GRAY);
        tell(source, "MSPT: " + Math.round(metrics.averageMspt()) + "ms", ChatFormatting.GRAY);
        tell(source, "Mode: " + metrics.mode().displayName(), ChatFormatting.GRAY);
        tell(source, "Mode Source: " + RuntimeModeService.INSTANCE.modeSource(), ChatFormatting.GRAY);
        tell(source, "Emergency: " + (metrics.emergency() ? "On" : "Off"), metrics.emergency() ? ChatFormatting.RED : ChatFormatting.GREEN);
        tell(source, "Lag Spikes Last Minute: " + metrics.lagSpikesLastMinute(), ChatFormatting.GRAY);
        tell(source, "FPS: client FPS unavailable on dedicated server", ChatFormatting.DARK_GRAY);
        tell(source, "Particles: " + ParticleBudgetService.INSTANCE.getSnapshot().used()
                + "/" + ParticleBudgetService.INSTANCE.getSnapshot().budget(), ChatFormatting.GRAY);
        tell(source, "Entities: " + metrics.entityCount() + ", Block entities: " + metrics.blockEntityCount(), ChatFormatting.GRAY);
        tell(source, "Validation queue: " + MultiblockValidationScheduler.INSTANCE.getSnapshot().queued(), ChatFormatting.GRAY);
        PerformanceBudgetService.INSTANCE.usageSnapshot().forEach((type, used) ->
                tell(source, "Budget " + type.name() + ": " + used + "/"
                        + PerformanceBudgetService.INSTANCE.budgetFor(type), ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int setMode(CommandSourceStack source, String rawMode) {
        RuntimeMode mode = RuntimeMode.byId(rawMode, null);
        if (mode == null) {
            tell(source, "Unknown mode '" + rawMode + "'.", ChatFormatting.RED);
            return 0;
        }
        RuntimeModeService.INSTANCE.setMode(mode);
        tell(source, "Runtime mode set to " + mode.displayName() + ".", ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int emergency(CommandSourceStack source, boolean enabled) {
        RuntimeModeService.INSTANCE.forceEmergency(enabled);
        tell(source, "Emergency mode " + (enabled ? "forced on" : "released") + ".", enabled ? ChatFormatting.RED : ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int dump(CommandSourceStack source) {
        try {
            Path path = RuntimeGuardReportWriter.write(source.getServer());
            tell(source, "RuntimeGuard text and JSON reports created: " + path, ChatFormatting.GREEN);
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            tell(source, "Could not create RuntimeGuard report: " + exception.getMessage(), ChatFormatting.RED);
            return 0;
        }
    }

    private static int top(CommandSourceStack source) {
        List<ProfilerEntry> entries = RuntimeGuardProfiler.getTopCosts();
        if (entries.isEmpty()) {
            tell(source, "No RuntimeGuardProfiler-wrapped costs recorded.", ChatFormatting.DARK_GRAY);
            return Command.SINGLE_SUCCESS;
        }
        entries.forEach(entry -> tell(source, entry.id() + " total " + two(entry.totalMillis())
                + "ms avg " + two(entry.averageMillis()) + "ms calls " + entry.calls(), ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int particles(CommandSourceStack source) {
        ParticleBudgetSnapshot snapshot = ParticleBudgetService.INSTANCE.getSnapshot();
        tell(source, "Particle budget " + snapshot.used() + "/" + snapshot.budget()
                + ", denied " + snapshot.denied() + ", mode " + snapshot.mode().displayName(), ChatFormatting.AQUA);
        snapshot.byPriority().forEach((priority, count) -> tell(source, priority.name() + ": " + count, ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int multiblocks(CommandSourceStack source) {
        ValidationQueueSnapshot snapshot = MultiblockValidationScheduler.INSTANCE.getSnapshot();
        tell(source, "Validation queue " + snapshot.queued() + ", dirty " + snapshot.dirtyPositions()
                + ", ran last tick " + snapshot.ranLastTick() + ", merged " + snapshot.mergedRequests(), ChatFormatting.AQUA);
        snapshot.bySystem().forEach((system, count) -> tell(source, system + ": " + count, ChatFormatting.GRAY));
        if (snapshot.bySystem().isEmpty()) {
            tell(source, "No queued systems.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int holomap(CommandSourceStack source) {
        tell(source, "HoloMap refresh interval "
                + IntegrationThrottleService.INSTANCE.getHoloMapRefreshIntervalTicks()
                + " ticks, max animated markers "
                + IntegrationThrottleService.INSTANCE.getMaxAnimatedMarkers(), ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int lens(CommandSourceStack source) {
        tell(source, "Lens target blocks " + IntegrationThrottleService.INSTANCE.getMaxBlocksPerScan(null, LensScanType.TARGET)
                + ", entities " + IntegrationThrottleService.INSTANCE.getMaxEntitiesPerScan(null, LensScanType.TARGET)
                + ", deep budget " + IntegrationThrottleService.INSTANCE.getDeepScanBudgetPerTick(null), ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int network(CommandSourceStack source) {
        NetworkSnapshot snapshot = NetworkBudgetService.INSTANCE.getSnapshot();
        tell(source, "Network packets/sec " + snapshot.packetsThisSecond()
                + ", bytes/sec " + snapshot.bytesThisSecond()
                + ", warnings " + snapshot.warnings()
                + ", duplicate drops " + snapshot.duplicateDrops(), ChatFormatting.AQUA);
        snapshot.packetsByChannel().forEach((channel, packets) -> tell(source, channel + ": "
                + packets + " packet(s), " + snapshot.bytesByChannel().getOrDefault(channel, 0) + " byte(s)",
                ChatFormatting.GRAY));
        if (snapshot.packetsByChannel().isEmpty()) {
            tell(source, "No channel traffic recorded in the current second.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int entities(CommandSourceStack source) {
        tell(source, RuntimeProfilerService.INSTANCE.snapshot(source.getServer()).entityCount()
                + " entities tracked by server snapshot; " + EntityAiGuardService.INSTANCE.statusLine(), ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int blockEntities(CommandSourceStack source) {
        tell(source, RuntimeProfilerService.INSTANCE.snapshot(source.getServer()).blockEntityCount()
                + " ticking block entities sampled; opt-in sleep registry tracks "
                + BlockEntitySleepService.INSTANCE.tracked() + " position(s).", ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandSourceStack source) {
        RuntimeProfilerService.INSTANCE.reset();
        RuntimeModeService.INSTANCE.reset();
        RuntimeGuardProfiler.reset();
        LagSpikeReporter.INSTANCE.reset();
        MultiblockValidationScheduler.INSTANCE.reset();
        NetworkBudgetService.INSTANCE.reset();
        ParticleBudgetService.INSTANCE.reset();
        PerformanceBudgetService.INSTANCE.reset();
        IntegrationThrottleService.INSTANCE.reset();
        BlockEntitySleepService.INSTANCE.reset();
        EntityAiGuardService.INSTANCE.reset();
        SmartTickService.INSTANCE.reset();
        tell(source, "RuntimeGuard counters reset. Smart tick tracked positions: "
                + SmartTickService.INSTANCE.trackedPositions(), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean canMutate(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static void tell(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal("[ECHO PERF] " + message).withStyle(color), false);
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String two(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
