package com.knoxhack.echosettlementcore.registry;

import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import com.knoxhack.echo.settlementcore.api.JobType;
import com.knoxhack.echo.settlementcore.api.LogisticsRequest;
import com.knoxhack.echo.settlementcore.api.Settlement;
import com.knoxhack.echo.settlementcore.api.SettlementService;
import com.knoxhack.echo.settlementcore.job.SettlementJobs;
import com.knoxhack.echo.settlementcore.registry.ModBlocks;
import com.knoxhack.echo.settlementcore.settlement.SettlementManager;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, EchoSettlementCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BLOCK_REGISTRATION =
        TEST_FUNCTIONS.register("block_registration", () -> ModGameTests::blockRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAFE_HABITAT =
        TEST_FUNCTIONS.register("safe_habitat", () -> ModGameTests::safeHabitat);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> JOB_DEFINITIONS =
        TEST_FUNCTIONS.register("job_definitions", () -> ModGameTests::jobDefinitions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOGISTICS_REQUEST =
        TEST_FUNCTIONS.register("logistics_request", () -> ModGameTests::logisticsRequest);

    private static final int TEST_PADDING = 24;

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "block_registration", BLOCK_REGISTRATION.getId());
        register(event, "safe_habitat", SAFE_HABITAT.getId());
        register(event, "job_definitions", JOB_DEFINITIONS.getId());
        register(event, "logistics_request", LOGISTICS_REQUEST.getId());
    }

    private static void blockRegistration(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.AIRLOCK.get() != Blocks.AIR, "Airlock should be registered");
        helper.assertTrue(ModBlocks.OXYGEN_RECYCLER.get() != Blocks.AIR, "Oxygen Recycler should be registered");
        helper.assertTrue(ModBlocks.PRESSURE_PUMP.get() != Blocks.AIR, "Pressure Pump should be registered");
        helper.assertTrue(ModBlocks.WORKSHOP.get() != Blocks.AIR, "Workshop should be registered");
        helper.assertTrue(ModBlocks.MED_BAY.get() != Blocks.AIR, "Med Bay should be registered");
        helper.assertTrue(ModBlocks.DIVERS_QUARTERS.get() != Blocks.AIR, "Divers Quarters should be registered");
        helper.assertTrue(ModBlocks.CARGO_LOCKER.get() != Blocks.AIR, "Cargo Locker should be registered");
        helper.assertTrue(ModBlocks.SUBMERSIBLE_DOCK.get() != Blocks.AIR, "Submersible Dock should be registered");
        helper.succeed();
    }

    private static void safeHabitat(GameTestHelper helper) {
        SettlementService.resetForTests();
        BlockPos center = new BlockPos(2, 2, 2);
        buildHabitat(helper, center);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5D, 3.0D, 2.5D)));

        Settlement settlement = SettlementManager.scanFrom(helper.getLevel(), helper.absolutePos(center));
        helper.assertTrue(settlement != null, "A sealed habitat should be detected");
        if (settlement != null) {
            SettlementService.find().registerSettlement(settlement);
            helper.assertTrue(SettlementService.find().isPlayerInSafeHabitat(player),
                "Player inside sealed habitat should be safe");
        }
        SettlementService.resetForTests();
        helper.succeed();
    }

    private static void jobDefinitions(GameTestHelper helper) {
        helper.assertTrue(SettlementJobs.jobs().size() == 4, "Four settlement jobs should be registered");
        helper.assertTrue(SettlementJobs.byType(JobType.DIVER).isPresent(), "Diver job should be registered");
        helper.assertTrue(SettlementJobs.byType(JobType.ENGINEER).isPresent(), "Engineer job should be registered");
        helper.assertTrue(SettlementJobs.byType(JobType.MEDIC).isPresent(), "Medic job should be registered");
        helper.assertTrue(SettlementJobs.byType(JobType.CARTOGRAPHER).isPresent(), "Cartographer job should be registered");
        helper.assertTrue(!SettlementJobs.poiBindings().isEmpty(), "POI bindings should not be empty");
        helper.succeed();
    }

    private static void logisticsRequest(GameTestHelper helper) {
        LogisticsRequest request = new LogisticsRequest(Identifier.fromNamespaceAndPath("minecraft", "iron_ingot"), 16, 4, 2);
        helper.assertTrue(request.amountRemaining() == 12, "Remaining amount should be 12");
        helper.assertTrue(!request.isFulfilled(), "Request with remaining amount should not be fulfilled");
        LogisticsRequest fulfilled = new LogisticsRequest(Identifier.fromNamespaceAndPath("minecraft", "copper_ingot"), 8, 8, 1);
        helper.assertTrue(fulfilled.isFulfilled(), "Request with enough fulfilled amount should be fulfilled");
        helper.succeed();
    }

    private static void buildHabitat(GameTestHelper helper, BlockPos center) {
        BlockPos absolute = helper.absolutePos(center);
        helper.setBlock(absolute, ModBlocks.AIRLOCK.get());
        helper.setBlock(absolute.east(), ModBlocks.OXYGEN_RECYCLER.get());
        helper.setBlock(absolute.west(), ModBlocks.PRESSURE_PUMP.get());
        helper.setBlock(absolute.north(), ModBlocks.WORKSHOP.get());
        helper.setBlock(absolute.south(), ModBlocks.MED_BAY.get());
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("settlementcore"));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"),
            400,
            0,
            true,
            Rotation.NONE,
            false,
            1,
            1,
            false,
            TEST_PADDING
        );
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSettlementCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoSettlementCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }
}
