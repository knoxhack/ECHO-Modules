package com.knoxhack.echorelictech.test;

import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.relic.RelicDefinition;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.block.entity.ContainmentLockerBlockEntity;
import com.knoxhack.echorelictech.block.entity.NullBatteryDockBlockEntity;
import com.knoxhack.echorelictech.integration.RelicTechMachineCoreAdapter;
import com.knoxhack.echorelictech.integration.RelicTechMachineCoreRuntimeProvider;
import com.knoxhack.echorelictech.registry.ModBlocks;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.registry.ModItems;
import com.knoxhack.echorelictech.server.RelicInstabilitySavedData;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RelicTechGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoRelicTech.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELIC_ANALYZER_PLACES =
            TEST_FUNCTIONS.register("relic_analyzer_places", () -> RelicTechGameTests::relicAnalyzerPlaces);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTAINMENT_LOCKER_PLACES =
            TEST_FUNCTIONS.register("containment_locker_places", () -> RelicTechGameTests::containmentLockerPlaces);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PHASE_ANCHOR_HAS_DATA =
            TEST_FUNCTIONS.register("phase_anchor_has_data", () -> RelicTechGameTests::phaseAnchorHasData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NULL_BATTERY_STORES_CHARGE =
            TEST_FUNCTIONS.register("null_battery_stores_charge", () -> RelicTechGameTests::nullBatteryStoresCharge);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSTABILITY_SAVED_DATA =
            TEST_FUNCTIONS.register("instability_saved_data", () -> RelicTechGameTests::instabilitySavedData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELIC_API_CREATES_IDENTIFIED_STACK =
            TEST_FUNCTIONS.register("relic_api_creates_identified_stack", () -> RelicTechGameTests::relicApiCreatesIdentifiedStack);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_RELICS_CREATE_STACKS =
            TEST_FUNCTIONS.register("starter_relics_create_stacks", () -> RelicTechGameTests::starterRelicsCreateStacks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORKBENCH_ACTION_CONDITION_GATES =
            TEST_FUNCTIONS.register("workbench_action_condition_gates", () -> RelicTechGameTests::workbenchActionConditionGates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONTAINMENT_LOCKER_FLAGS_AND_CLEARS =
            TEST_FUNCTIONS.register("containment_locker_flags_and_clears", () -> RelicTechGameTests::containmentLockerFlagsAndClears);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME =
            TEST_FUNCTIONS.register("relictech_machinecore_runtime_snapshot_contract",
                    () -> RelicTechGameTests::relicTechMachineCoreRuntimeSnapshotContract);

    private RelicTechGameTests() {}

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("relictech_hardening"));
        register(event, environment, "relic_analyzer_places", RELIC_ANALYZER_PLACES.getId());
        register(event, environment, "containment_locker_places", CONTAINMENT_LOCKER_PLACES.getId());
        register(event, environment, "phase_anchor_has_data", PHASE_ANCHOR_HAS_DATA.getId());
        register(event, environment, "null_battery_stores_charge", NULL_BATTERY_STORES_CHARGE.getId());
        register(event, environment, "instability_saved_data", INSTABILITY_SAVED_DATA.getId());
        register(event, environment, "relic_api_creates_identified_stack", RELIC_API_CREATES_IDENTIFIED_STACK.getId());
        register(event, environment, "starter_relics_create_stacks", STARTER_RELICS_CREATE_STACKS.getId());
        register(event, environment, "workbench_action_condition_gates", WORKBENCH_ACTION_CONDITION_GATES.getId());
        register(event, environment, "containment_locker_flags_and_clears", CONTAINMENT_LOCKER_FLAGS_AND_CLEARS.getId());
        register(event, environment, "relictech_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
    }

    private static void relicTechMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        BlockPos dockPos = new BlockPos(1, 1, 1);
        helper.setBlock(dockPos, ModBlocks.NULL_BATTERY_DOCK.get().defaultBlockState());
        NullBatteryDockBlockEntity dock = helper.getBlockEntity(dockPos, NullBatteryDockBlockEntity.class);
        ItemStack battery = new ItemStack(ModItems.NULL_BATTERY.get());
        battery.set(ModDataComponents.NULL_CHARGE.get(), 5);
        dock.setItem(NullBatteryDockBlockEntity.BATTERY_SLOT, battery);
        dock.setItem(NullBatteryDockBlockEntity.CELL_SLOT, new ItemStack(ModItems.NULL_CELL.get(), 2));

        EchoMachineRuntimeSnapshot direct = RelicTechMachineCoreAdapter.runtimeSnapshot(dock);
        helper.assertTrue(direct.id().value().equals(EchoRelicTech.id("null_battery_dock").toString())
                        && direct.ownerModule().value().equals(EchoRelicTech.MODID)
                        && direct.kind() == EchoMachineKind.POWERED_STATION,
                "RelicTech MachineCore adapter should publish the placed Null Battery Dock identity");
        helper.assertTrue(direct.energy().stored() == 5 && direct.energy().capacity() == 8
                        && "null_charge".equals(direct.energy().unit()),
                "RelicTech MachineCore snapshot should map live null charge into the energy contract");
        helper.assertTrue(direct.inventory().totalSlots() == 2 && direct.inventory().occupiedSlots() == 2,
                "RelicTech MachineCore snapshot should map the dock battery and cell slots");
        helper.assertTrue("echorelictech:null_battery_charge".equals(direct.process().recipeContract())
                        && direct.savedState().persistedKeys().contains("power_grid_charge_tick")
                        && direct.savedState().persistedKeys().contains("null_charge"),
                "RelicTech MachineCore snapshot should expose the dock process and saved charge keys");
        helper.assertTrue(EchoMachineUiBridge.hasAutomationSurface(direct)
                        && EchoMachineUiBridge.position(direct).filter(dock.getBlockPos()::equals).isPresent(),
                "RelicTech MachineCore snapshot should expose UI bridge side and position metadata");

        BlockPos lockerPos = new BlockPos(4, 1, 1);
        helper.setBlock(lockerPos, ModBlocks.CONTAINMENT_LOCKER.get().defaultBlockState());
        ContainmentLockerBlockEntity locker = helper.getBlockEntity(lockerPos, ContainmentLockerBlockEntity.class);
        helper.assertTrue(locker.addRelic(RelicTechApi.createRelicStack(id("phase_anchor")), null),
                "Containment locker should accept a real relic before MachineCore projection");
        EchoMachineRuntimeSnapshot lockerSnapshot = RelicTechMachineCoreAdapter.runtimeSnapshot(locker);
        helper.assertTrue(lockerSnapshot.inventory().occupiedSlots() == 1
                        && lockerSnapshot.inventory().slots().get(0).attributes().getOrDefault("contained", "false").equals("true")
                        && lockerSnapshot.savedState().persistedKeys().contains("relic_data.contained"),
                "RelicTech MachineCore snapshot should preserve contained relic slot metadata");

        RelicTechMachineCoreRuntimeProvider.register();
        EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), dock.getBlockPos())
                .orElse(null);
        helper.assertTrue(registered != null && registered.id().value().equals(direct.id().value())
                        && "5/8 null_charge".equals(EchoMachineUiBridge.energyLine(registered)),
                "MachineCore registry should discover the placed RelicTech Null Battery Dock through its runtime provider");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(dock.getBlockPos().above().getCenter());
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(EchoRelicTech.id("null_battery_dock").toString())
                                && EchoMachineUiBridge.hasAutomationSurface(snapshot)),
                "MachineCore player snapshots should include the nearby RelicTech dock");
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(EchoRelicTech.id("containment_locker").toString())
                                && snapshot.inventory().occupiedSlots() == 1),
                "MachineCore player snapshots should include the nearby RelicTech containment locker");
        List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(player);
        helper.assertTrue(profiles.stream().anyMatch(profile -> profile.id().value().equals(EchoRelicTech.id("null_battery_dock").toString())
                        && profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && "echorelictech:null_battery_charge".equals(binding.recipeId().value()))),
                "MachineCore profiles should expose the RelicTech null battery charge binding");
        helper.succeed();
    }

    private static void relicAnalyzerPlaces(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.setBlock(pos, ModBlocks.RELIC_ANALYZER.get().defaultBlockState());
        helper.assertBlock(pos, b -> b == ModBlocks.RELIC_ANALYZER.get(), b -> net.minecraft.network.chat.Component.literal("Relic Analyzer should place"));
        helper.assertTrue(helper.getBlockEntity(pos, com.knoxhack.echorelictech.block.entity.RelicAnalyzerBlockEntity.class) != null, "Relic Analyzer should have block entity");
        helper.succeed();
    }

    private static void containmentLockerPlaces(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.setBlock(pos, ModBlocks.CONTAINMENT_LOCKER.get().defaultBlockState());
        helper.assertBlock(pos, b -> b == ModBlocks.CONTAINMENT_LOCKER.get(), b -> net.minecraft.network.chat.Component.literal("Containment Locker should place"));
        helper.succeed();
    }

    private static void phaseAnchorHasData(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.PHASE_ANCHOR.get());
        stack.set(ModDataComponents.RELIC_DATA.get(), new RelicInstanceData(
            Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, "phase_anchor"),
            RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0));
        helper.assertTrue(stack.has(ModDataComponents.RELIC_DATA.get()), "Phase Anchor should have relic data component");
        RelicInstanceData data = stack.get(ModDataComponents.RELIC_DATA.get());
        helper.assertTrue(data != null && data.relicId().getPath().equals("phase_anchor"), "Relic ID should be phase_anchor");
        helper.succeed();
    }

    private static void nullBatteryStoresCharge(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.NULL_BATTERY.get());
        stack.set(ModDataComponents.NULL_CHARGE.get(), 5);
        helper.assertTrue(stack.getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0) == 5, "Null Battery should store charge 5");
        helper.succeed();
    }

    private static void instabilitySavedData(GameTestHelper helper) {
        var data = RelicInstabilitySavedData.get(helper.getLevel());
        var inst = data.get(java.util.UUID.randomUUID());
        helper.assertTrue(inst.value == 0, "Default instability should be 0");
        helper.succeed();
    }

    private static void relicApiCreatesIdentifiedStack(GameTestHelper helper) {
        ItemStack stack = RelicTechApi.createRelicStack(id("phase_anchor"));
        helper.assertFalse(stack.isEmpty(), "RelicTech API should create a Phase Anchor stack");
        helper.assertTrue(stack.is(ModItems.PHASE_ANCHOR.get()), "Created relic should use registered item");
        RelicInstanceData data = stack.get(ModDataComponents.RELIC_DATA.get());
        helper.assertTrue(data != null && data.identified(), "Created relic should carry identified relic data");
        helper.assertTrue(data != null && data.relicId().equals(id("phase_anchor")), "Created relic should preserve relic id");
        helper.succeed();
    }

    private static void starterRelicsCreateStacks(GameTestHelper helper) {
        for (String path : List.of(
                "phase_anchor",
                "echo_mirror",
                "gravity_clamp",
                "rift_lantern",
                "blood_circuit",
                "broken_climate_key",
                "soul_capacitor",
                "void_compass")) {
            ItemStack stack = RelicTechApi.createRelicStack(id(path));
            helper.assertFalse(stack.isEmpty(), "RelicTech API should create stack for " + path);
            RelicInstanceData data = stack.get(ModDataComponents.RELIC_DATA.get());
            helper.assertTrue(data != null && data.identified(), path + " should carry identified relic data");
            helper.assertTrue(data != null && data.relicId().equals(id(path)), path + " should preserve relic id");
        }
        helper.assertFalse(new ItemStack(ModItems.LEGENDARY_RELIC_FRAME.get()).isEmpty(), "Legendary Relic Frame should be registered");
        helper.succeed();
    }

    private static void workbenchActionConditionGates(GameTestHelper helper) {
        RelicDefinition.WorkbenchAction action = new RelicDefinition.WorkbenchAction(
                Optional.of(RelicCondition.DAMAGED),
                RelicCondition.STABILIZED,
                List.of(new RelicDefinition.RepairMaterial("echorelictech:relic_shard", 1)),
                0,
                0,
                0.0D);
        helper.assertTrue(action.canApplyTo(RelicCondition.DAMAGED), "Workbench action should allow configured source condition");
        helper.assertFalse(action.canApplyTo(RelicCondition.STABILIZED), "Workbench action should reject other source conditions");
        helper.succeed();
    }

    private static void containmentLockerFlagsAndClears(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.setBlock(pos, ModBlocks.CONTAINMENT_LOCKER.get().defaultBlockState());
        ContainmentLockerBlockEntity locker = helper.getBlockEntity(pos, ContainmentLockerBlockEntity.class);
        ItemStack stack = RelicTechApi.createRelicStack(id("phase_anchor"));
        helper.assertTrue(locker.addRelic(stack, null), "Containment locker should accept relic stacks");
        ItemStack stored = locker.getItem(0);
        helper.assertTrue(RelicTechApi.isContained(stored), "Inserted relic should be marked contained");
        ItemStack removed = locker.removeRelic(0);
        helper.assertFalse(RelicTechApi.isContained(removed), "Removed relic should clear contained flag");
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
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
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
    }
}
