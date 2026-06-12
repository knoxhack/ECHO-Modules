package com.knoxhack.echoruntimeguard.registry;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.api.DirtyReason;
import com.knoxhack.echoruntimeguard.api.NetworkPriority;
import com.knoxhack.echoruntimeguard.api.ParticlePriority;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardBudgetCategories;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardServices;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import com.knoxhack.echoruntimeguard.api.ValidationPriority;
import com.knoxhack.echoruntimeguard.report.RuntimeGuardReportWriter;
import com.knoxhack.echoruntimeguard.runtime.RuntimeGuardDiagnostics;
import com.knoxhack.echoruntimeguard.runtime.MultiblockValidationScheduler;
import com.knoxhack.echoruntimeguard.runtime.NetworkBudgetService;
import com.knoxhack.echoruntimeguard.runtime.ParticleBudgetService;
import com.knoxhack.echoruntimeguard.runtime.PerformanceBudgetService;
import com.knoxhack.echoruntimeguard.runtime.RuntimeModeService;
import com.knoxhack.echoruntimeguard.runtime.RuntimeSpineGuardBridge;
import com.knoxhack.echoruntimeguard.runtime.SmartTickService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoRuntimeGuard.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODE_TRANSITIONS =
            TEST_FUNCTIONS.register("mode_transitions", () -> ModGameTests::modeTransitions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SMART_TICK_RATES =
            TEST_FUNCTIONS.register("smart_tick_rates", () -> ModGameTests::smartTickRates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PARTICLE_BUDGET =
            TEST_FUNCTIONS.register("particle_budget", () -> ModGameTests::particleBudget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VALIDATION_QUEUE_MERGE =
            TEST_FUNCTIONS.register("validation_queue_merge", () -> ModGameTests::validationQueueMerge);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NETWORK_DUPLICATE_TRACKING =
            TEST_FUNCTIONS.register("network_duplicate_tracking", () -> ModGameTests::networkDuplicateTracking);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNAVAILABLE_METRICS =
            TEST_FUNCTIONS.register("unavailable_metrics", () -> ModGameTests::unavailableMetrics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REPORT_GENERATION =
            TEST_FUNCTIONS.register("report_generation", () -> ModGameTests::reportGeneration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STATUS_AND_DIAGNOSTICS =
            TEST_FUNCTIONS.register("status_and_diagnostics", () -> ModGameTests::statusAndDiagnostics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CORE_RUNTIME_BUDGET_SERVICE =
            TEST_FUNCTIONS.register("core_runtime_budget_service", () -> ModGameTests::coreRuntimeBudgetService);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OBSERVABLE_METRICS =
            TEST_FUNCTIONS.register("observable_metrics", () -> ModGameTests::observableMetrics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JSON_REPORT_GENERATION =
            TEST_FUNCTIONS.register("json_report_generation", () -> ModGameTests::jsonReportGeneration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESET_CLEARS_COUNTERS =
            TEST_FUNCTIONS.register("reset_clears_counters", () -> ModGameTests::resetClearsCounters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNTIME_SPINE_GATE_THROTTLE =
            TEST_FUNCTIONS.register("runtime_spine_gate_throttle", () -> ModGameTests::runtimeSpineGateThrottle);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("runtimeguard"));
        register(event, environment, "mode_transitions", MODE_TRANSITIONS.getId());
        register(event, environment, "smart_tick_rates", SMART_TICK_RATES.getId());
        register(event, environment, "particle_budget", PARTICLE_BUDGET.getId());
        register(event, environment, "validation_queue_merge", VALIDATION_QUEUE_MERGE.getId());
        register(event, environment, "network_duplicate_tracking", NETWORK_DUPLICATE_TRACKING.getId());
        register(event, environment, "unavailable_metrics", UNAVAILABLE_METRICS.getId());
        register(event, environment, "report_generation", REPORT_GENERATION.getId());
        register(event, environment, "status_and_diagnostics", STATUS_AND_DIAGNOSTICS.getId());
        register(event, environment, "core_runtime_budget_service", CORE_RUNTIME_BUDGET_SERVICE.getId());
        register(event, environment, "observable_metrics", OBSERVABLE_METRICS.getId());
        register(event, environment, "json_report_generation", JSON_REPORT_GENERATION.getId());
        register(event, environment, "reset_clears_counters", RESET_CLEARS_COUNTERS.getId());
        register(event, environment, "runtime_spine_gate_throttle", RUNTIME_SPINE_GATE_THROTTLE.getId());
    }

    private static void modeTransitions(GameTestHelper helper) {
        RuntimeModeService.INSTANCE.reset();
        RuntimeModeService.INSTANCE.setMode(RuntimeMode.POTATO);
        helper.assertTrue(RuntimeModeService.INSTANCE.mode() == RuntimeMode.POTATO,
                "Manual Potato mode should be effective.");
        RuntimeModeService.INSTANCE.forceEmergency(true);
        helper.assertTrue(RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY,
                "Forced emergency should override the manual preset.");
        RuntimeModeService.INSTANCE.forceEmergency(false);
        helper.assertTrue(RuntimeModeService.INSTANCE.mode() == RuntimeMode.POTATO,
                "Releasing emergency should restore the manual preset.");
        RuntimeModeService.INSTANCE.reset();
        helper.succeed();
    }

    private static void smartTickRates(GameTestHelper helper) {
        RuntimeModeService.INSTANCE.reset();
        RuntimeModeService.INSTANCE.setMode(RuntimeMode.BALANCED);
        int balanced = SmartTickService.INSTANCE.getRecommendedTickRate(helper.getLevel(),
                new BlockPos(512, 80, 512), RuntimeWorkType.BLOCK_ENTITY);
        RuntimeModeService.INSTANCE.forceEmergency(true);
        int emergency = SmartTickService.INSTANCE.getRecommendedTickRate(helper.getLevel(),
                new BlockPos(512, 80, 512), RuntimeWorkType.BLOCK_ENTITY);
        helper.assertTrue(emergency >= balanced, "Emergency mode should not recommend faster distant ticks.");
        RuntimeModeService.INSTANCE.reset();
        helper.succeed();
    }

    private static void particleBudget(GameTestHelper helper) {
        ParticleBudgetService.INSTANCE.beginTick();
        helper.assertTrue(ParticleBudgetService.INSTANCE.canSpawnParticle(ParticlePriority.CRITICAL, null),
                "Critical particles should be preserved.");
        helper.assertTrue(ParticleBudgetService.INSTANCE.canSpawnParticle(ParticlePriority.DECORATIVE, null),
                "Initial decorative particles should fit inside the budget.");
        ParticleBudgetService.INSTANCE.recordParticleSpawn(ParticlePriority.DECORATIVE);
        helper.assertTrue(ParticleBudgetService.INSTANCE.getSnapshot().used() == 1,
                "Particle usage should be recorded.");
        helper.succeed();
    }

    private static void validationQueueMerge(GameTestHelper helper) {
        MultiblockValidationScheduler.INSTANCE.reset();
        AtomicInteger calls = new AtomicInteger();
        BlockPos pos = new BlockPos(1, 2, 3);
        MultiblockValidationScheduler.INSTANCE.markDirty(helper.getLevel(), pos, DirtyReason.BLOCK_PLACED);
        MultiblockValidationScheduler.INSTANCE.requestValidation(id("test"), helper.getLevel(), pos,
                ValidationPriority.BLOCK_CHANGED, calls::incrementAndGet);
        MultiblockValidationScheduler.INSTANCE.requestValidation(id("test"), helper.getLevel(), pos,
                ValidationPriority.PLAYER_REQUEST, calls::incrementAndGet);
        helper.assertTrue(MultiblockValidationScheduler.INSTANCE.getSnapshot().queued() == 1,
                "Duplicate validation requests should merge.");
        helper.assertTrue(MultiblockValidationScheduler.INSTANCE.getSnapshot().mergedRequests() >= 1,
                "Merged request counter should increment.");
        MultiblockValidationScheduler.INSTANCE.reset();
        helper.succeed();
    }

    private static void networkDuplicateTracking(GameTestHelper helper) {
        NetworkBudgetService.INSTANCE.reset();
        Identifier channel = id("network/test");
        helper.assertFalse(NetworkBudgetService.INSTANCE.shouldDropDuplicate(channel, 1234),
                "First payload hash should not be considered duplicate.");
        helper.assertTrue(NetworkBudgetService.INSTANCE.shouldDropDuplicate(channel, 1234),
                "Repeated same-window payload hash should be considered duplicate.");
        NetworkBudgetService.INSTANCE.recordSend(channel, 64, NetworkPriority.BACKGROUND_SYNC);
        helper.assertTrue(NetworkBudgetService.INSTANCE.getSnapshot().packetsThisSecond() == 1,
                "Network send accounting should increment packet counters.");
        NetworkBudgetService.INSTANCE.reset();
        helper.succeed();
    }

    private static void unavailableMetrics(GameTestHelper helper) {
        RuntimeMetricsSnapshot snapshot = RuntimeMetricsSnapshot.unavailable(RuntimeMode.BALANCED, false);
        helper.assertTrue("unavailable".equals(snapshot.entityCount()), "Unavailable entity metrics should be explicit.");
        helper.assertTrue("unavailable".equals(snapshot.blockEntityCount()), "Unavailable block entity metrics should be explicit.");
        helper.succeed();
    }

    private static void reportGeneration(GameTestHelper helper) {
        try {
            Path report = RuntimeGuardReportWriter.write(helper.getLevel().getServer());
            helper.assertTrue(Files.isRegularFile(report), "RuntimeGuard report writer should create a report file.");
            String content = Files.readString(report);
            helper.assertTrue(content.contains("ECHO RuntimeGuard Performance Report"),
                    "RuntimeGuard report should include its title.");
            helper.assertTrue(content.contains("- Entity count: ") && content.contains("- Block entity count: "),
                    "RuntimeGuard report should include world metric availability fields.");
            helper.succeed();
        } catch (Exception exception) {
            helper.fail("RuntimeGuard report generation failed: " + exception.getMessage());
        }
    }

    private static void statusAndDiagnostics(GameTestHelper helper) {
        RuntimeModeService.INSTANCE.reset();
        RuntimeModeService.INSTANCE.forceEmergency(true);
        helper.assertTrue(RuntimeGuardServices.status().forcedEmergency(),
                "RuntimeGuard status snapshot should expose forced emergency state.");
        helper.assertTrue("forced emergency".equals(RuntimeGuardServices.status().modeSource()),
                "RuntimeGuard status snapshot should expose the mode source.");
        helper.assertFalse(RuntimeGuardDiagnostics.diagnostics(null).isEmpty(),
                "RuntimeGuard diagnostics should report emergency state.");
        RuntimeModeService.INSTANCE.reset();
        helper.succeed();
    }

    private static void coreRuntimeBudgetService(GameTestHelper helper) {
        helper.assertTrue(EchoCoreServices.runtimeBudgetService().available(),
                "RuntimeGuard should register Echo Core's runtime budget service.");
        helper.assertTrue(EchoCoreServices.runtimeBudgetService().categories()
                        .contains(RuntimeGuardBudgetCategories.SERVER_TICK),
                "Runtime budget service should expose stable RuntimeGuard category ids.");
        helper.assertTrue(EchoCoreServices.runtimeBudgetService().budgetMs(RuntimeGuardBudgetCategories.SERVER_TICK) > 0.0D,
                "Server tick budget should be a positive millisecond threshold.");
        helper.succeed();
    }

    private static void observableMetrics(GameTestHelper helper) {
        RuntimeMetricsSnapshot snapshot = RuntimeGuardServices.metrics().snapshot(helper.getLevel().getServer());
        helper.assertTrue(isNumeric(snapshot.entityCount()), "Entity count should be numeric when safely observable.");
        helper.assertTrue(isNumeric(snapshot.blockEntityCount()), "Block entity count should be numeric when safely observable.");
        helper.succeed();
    }

    private static void jsonReportGeneration(GameTestHelper helper) {
        try {
            Path report = RuntimeGuardReportWriter.write(helper.getLevel().getServer());
            Path json = report.resolveSibling(report.getFileName().toString().replace(".txt", ".json"));
            helper.assertTrue(Files.isRegularFile(json), "RuntimeGuard report writer should create a JSON report sidecar.");
            String content = Files.readString(json);
            helper.assertTrue(content.contains("\"schema\": \"echoruntimeguard:runtime_report/v1\""),
                    "RuntimeGuard JSON report should include its schema id.");
            helper.assertTrue(content.contains("\"network\""), "RuntimeGuard JSON report should include network data.");
            helper.succeed();
        } catch (Exception exception) {
            helper.fail("RuntimeGuard JSON report generation failed: " + exception.getMessage());
        }
    }

    private static void resetClearsCounters(GameTestHelper helper) {
        RuntimeGuardProfiler.record(id("test/profiled_work"), 1_000_000L);
        ParticleBudgetService.INSTANCE.recordParticleSpawn(ParticlePriority.DECORATIVE);
        NetworkBudgetService.INSTANCE.recordSend(id("network/reset"), 64, NetworkPriority.BACKGROUND_SYNC);
        PerformanceBudgetService.INSTANCE.tryAcquire(RuntimeWorkType.BLOCK_ENTITY, 4);
        SmartTickService.INSTANCE.markIdle(id("reset"), new BlockPos(2, 2, 2));
        RuntimeGuardProfiler.reset();
        ParticleBudgetService.INSTANCE.reset();
        NetworkBudgetService.INSTANCE.reset();
        PerformanceBudgetService.INSTANCE.reset();
        SmartTickService.INSTANCE.reset();
        helper.assertTrue(RuntimeGuardProfiler.getTopCosts().isEmpty(), "Profiler entries should reset.");
        helper.assertTrue(ParticleBudgetService.INSTANCE.getSnapshot().used() == 0, "Particle usage should reset.");
        helper.assertTrue(NetworkBudgetService.INSTANCE.getSnapshot().packetsThisSecond() == 0, "Network counters should reset.");
        helper.assertTrue(PerformanceBudgetService.INSTANCE.usageSnapshot().isEmpty(), "Performance usage should reset.");
        helper.assertTrue(SmartTickService.INSTANCE.trackedPositions() == 0, "Smart tick activity should reset.");
        helper.succeed();
    }

    private static void runtimeSpineGateThrottle(GameTestHelper helper) {
        RuntimeModeService.INSTANCE.reset();
        RuntimeModeService.INSTANCE.forceEmergency(true);
        PerformanceBudgetService.INSTANCE.reset();
        EchoRuntimeSpineBus.clearForTests();
        RuntimeSpineGuardBridge.registerForTests();

        AtomicInteger delivered = new AtomicInteger();
        Identifier acceptedId = id("runtime_spine/accepted_budget_event");
        Identifier droppedId = id("runtime_spine/dropped_budget_event");
        int networkBudget = PerformanceBudgetService.INSTANCE.budgetFor(RuntimeWorkType.NETWORK_SYNC);
        try {
            AutoCloseable listener = EchoRuntimeSpineBus.register(event -> delivered.incrementAndGet());
            try {
                boolean accepted = EchoRuntimeSpineBus.publish(EchoRuntimeSpineEvent.of(
                        EchoRuntimeGuard.MODID,
                        acceptedId,
                        null,
                        acceptedId,
                        networkBudget,
                        java.util.Map.of("proof", "runtimeguard_accepts_budgeted_runtime_spine_event")));
                boolean dropped = EchoRuntimeSpineBus.publish(EchoRuntimeSpineEvent.of(
                        EchoRuntimeGuard.MODID,
                        droppedId,
                        null,
                        droppedId,
                        1,
                        java.util.Map.of("proof", "runtimeguard_drops_over_budget_runtime_spine_event")));

                helper.assertTrue(accepted, "RuntimeGuard should allow the first runtime-spine event within the live network budget.");
                helper.assertFalse(dropped, "RuntimeGuard should drop over-budget runtime-spine fanout.");
                helper.assertTrue(delivered.get() == 1, "RuntimeGuard should prevent dropped runtime-spine events from reaching listeners.");
                helper.assertTrue(RuntimeSpineGuardBridge.acceptedEvents() == 1,
                        "RuntimeGuard runtime-spine bridge should count the accepted event.");
                helper.assertTrue(RuntimeSpineGuardBridge.droppedEvents() == 1,
                        "RuntimeGuard runtime-spine bridge should count the dropped event.");
                helper.assertTrue(PerformanceBudgetService.INSTANCE.usageSnapshot()
                                .getOrDefault(RuntimeWorkType.NETWORK_SYNC, 0) == networkBudget,
                        "Dropped runtime-spine events must not consume additional network-sync budget.");
            } finally {
                listener.close();
            }
        } catch (Exception exception) {
            helper.fail("RuntimeGuard runtime-spine listener cleanup failed: " + exception.getMessage());
        } finally {
            EchoRuntimeSpineBus.clearForTests();
            RuntimeSpineGuardBridge.reset();
            PerformanceBudgetService.INSTANCE.reset();
            RuntimeModeService.INSTANCE.reset();
        }
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoRuntimeGuard.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRuntimeGuard.MODID, path);
    }
}
