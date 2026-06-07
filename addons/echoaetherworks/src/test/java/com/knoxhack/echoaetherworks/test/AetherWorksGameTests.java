package com.knoxhack.echoaetherworks.test;

import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.block.entity.AetherCellBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherCondenserBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherConduitBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import com.knoxhack.echoaetherworks.integration.AetherWorksMachineCoreAdapter;
import com.knoxhack.echoaetherworks.integration.AetherWorksMachineCoreRuntimeProvider;
import com.knoxhack.echoaetherworks.menu.AetherMachineMenu;
import com.knoxhack.echoaetherworks.registry.ModBlocks;
import com.knoxhack.echoaetherworks.registry.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AetherWorksGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoAetherWorks.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONDENSER_NETWORK =
            TEST_FUNCTIONS.register("condenser_feeds_cell_and_player", () -> AetherWorksGameTests::condenserFeedsCellAndPlayer);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME =
            TEST_FUNCTIONS.register("aetherworks_machinecore_runtime_snapshot_contract",
                    () -> AetherWorksGameTests::aetherWorksMachineCoreRuntimeSnapshotContract);

    private AetherWorksGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("aetherworks_first_slice"));
        register(event, environment, "condenser_feeds_cell_and_player", CONDENSER_NETWORK.getId());
        register(event, environment, "aetherworks_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
    }

    private static void aetherWorksMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        BlockPos condenserPos = new BlockPos(1, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.AETHER_CONDENSER.get().defaultBlockState());
        AetherCondenserBlockEntity condenser = helper.getBlockEntity(condenserPos, AetherCondenserBlockEntity.class);
        condenser.setAetherStorage(condenser.aetherStorage().withStoredAmount(152.0D).withContamination(0.42D));
        condenser.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        condenser.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));

        AetherWorksApi.AetherAutomationRecipe recipe = AetherWorksApi.bestAutomationRecipe(condenser);
        helper.assertTrue(recipe != null && "condenser_overload_scrubber".equals(recipe.id()),
                "AetherWorks condenser should expose its real overload scrubber recipe before MachineCore projection");

        EchoMachineRuntimeSnapshot direct = AetherWorksMachineCoreAdapter.runtimeSnapshot(condenser);
        helper.assertTrue(direct.id().value().equals(EchoAetherWorks.id("aether_condenser").toString())
                        && direct.ownerModule().value().equals(EchoAetherWorks.MODID)
                        && direct.state() == EchoMachineState.ACTIVE,
                "AetherWorks MachineCore adapter should publish the placed condenser identity and live state");
        helper.assertTrue(direct.energy().stored() == 152 && direct.energy().capacity() == 160
                        && "aether".equals(direct.energy().unit()),
                "AetherWorks MachineCore snapshot should map live aether storage into the energy contract");
        helper.assertTrue(direct.inventory().totalSlots() == 3 && direct.inventory().occupiedSlots() == 2,
                "AetherWorks MachineCore snapshot should map the physical automation inventory slots");
        helper.assertTrue("echoaetherworks:condenser_overload_scrubber".equals(direct.process().recipeContract())
                        && direct.process().attributes().containsKey("topologyNodes"),
                "AetherWorks MachineCore snapshot should map the ready recipe plus topology diagnostics");
        helper.assertTrue(direct.side().upSlots().contains("input")
                        && direct.side().upSlots().contains("secondary_input")
                        && direct.side().downSlots().contains("output")
                        && direct.side().sideSlots().contains("secondary_input"),
                "AetherWorks MachineCore snapshot should preserve sided inventory automation lanes");
        helper.assertTrue(direct.savedState().persistedKeys().contains("contamination")
                        && EchoMachineUiBridge.hasAutomationSurface(direct)
                        && EchoMachineUiBridge.position(direct).filter(condenser.getBlockPos()::equals).isPresent(),
                "AetherWorks MachineCore snapshot should expose saved-state and UI bridge position metadata");

        AetherWorksMachineCoreRuntimeProvider.register();
        EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), condenser.getBlockPos())
                .orElse(null);
        helper.assertTrue(registered != null && registered.id().value().equals(direct.id().value())
                        && "152/160 aether".equals(EchoMachineUiBridge.energyLine(registered)),
                "MachineCore registry should discover the placed AetherWorks condenser through its runtime provider");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(condenser.getBlockPos().above().getCenter());
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(direct.id().value())
                                && EchoMachineUiBridge.hasAutomationSurface(snapshot)),
                "MachineCore player snapshots should include the nearby AetherWorks machine");
        List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(player);
        helper.assertTrue(profiles.stream().anyMatch(profile -> profile.id().value().equals(direct.id().value())
                        && profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && "echoaetherworks:condenser_overload_scrubber".equals(binding.recipeId().value()))),
                "MachineCore profiles should expose the AetherWorks automation recipe binding");
        helper.succeed();
    }

    private static void condenserFeedsCellAndPlayer(GameTestHelper helper) {
        BlockPos condenserPos = new BlockPos(1, 1, 1);
        BlockPos cellPos = condenserPos.east();
        helper.setBlock(condenserPos, ModBlocks.AETHER_CONDENSER.get().defaultBlockState());
        helper.setBlock(cellPos, ModBlocks.AETHER_CELL.get().defaultBlockState());
        AetherCondenserBlockEntity condenser = helper.getBlockEntity(condenserPos, AetherCondenserBlockEntity.class);
        AetherCellBlockEntity cell = helper.getBlockEntity(cellPos, AetherCellBlockEntity.class);
        condenser.setAetherStorage(condenser.aetherStorage().withStoredAmount(24.0D));
        AetherCondenserBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(condenserPos),
                helper.getLevel().getBlockState(helper.absolutePos(condenserPos)), condenser);
        helper.assertTrue(cell.storedAmount() > 0.0D, "Aether Condenser should push generated aether into an adjacent cell");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        double before = ArcanaCoreServices.aether().getAether(player, AetherSignalType.RAW_AETHER);
        helper.assertTrue(AetherWorksApi.drawToPlayer(player, cell, AetherWorksApi.AETHER_CELL),
                "Aether Cell should draw stored aether into the player buffer");
        helper.assertTrue(ArcanaCoreServices.aether().getAether(player, AetherSignalType.RAW_AETHER) > before,
                "Player should receive raw aether from the cell");
        AetherMachineMenu menu = new AetherMachineMenu(7, player.getInventory(), helper.getLevel(),
                helper.absolutePos(cellPos));
        helper.assertTrue(menu.neighborCount() == 1 && menu.acceptTargetCount() == 1,
                "Aether machine menu should expose local topology graph counts");
        helper.assertTrue(menu.clickMenuButton(player, AetherMachineMenu.BUTTON_TOGGLE_AUTOMATION),
                "Aether machine menu should toggle automation controls");
        helper.assertTrue(!cell.automationEnabled(),
                "Automation toggle should pause network transfer on the storage node");
        menu.clickMenuButton(player, AetherMachineMenu.BUTTON_TOGGLE_AUTOMATION);
        helper.assertTrue(menu.clickMenuButton(player, AetherMachineMenu.BUTTON_CYCLE_MODE),
                "Aether machine menu should cycle network mode");
        helper.assertTrue(cell.networkMode() == AetherStorageBlockEntity.MODE_HOLD,
                "First network mode cycle should put cell into hold mode");
        cell.setAetherStorage(cell.aetherStorage().withStoredAmount(0.0D));
        condenser.setAetherStorage(condenser.aetherStorage().withStoredAmount(32.0D));
        AetherCondenserBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(condenserPos),
                helper.getLevel().getBlockState(helper.absolutePos(condenserPos)), condenser);
        helper.assertTrue(cell.storedAmount() == 0.0D,
                "Hold mode should prevent adjacent aether insertion");
        menu.clickMenuButton(player, AetherMachineMenu.BUTTON_CYCLE_MODE);
        AetherCondenserBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(condenserPos),
                helper.getLevel().getBlockState(helper.absolutePos(condenserPos)), condenser);
        helper.assertTrue(cell.networkMode() == AetherStorageBlockEntity.MODE_ACCEPT_ONLY && cell.storedAmount() > 0.0D,
                "Accept-only mode should accept but not push network storage");
        BlockPos routedCondenserPos = new BlockPos(1, 1, 4);
        BlockPos conduitPos = routedCondenserPos.east();
        BlockPos routedCellPos = conduitPos.east();
        helper.setBlock(routedCondenserPos, ModBlocks.AETHER_CONDENSER.get().defaultBlockState());
        helper.setBlock(conduitPos, ModBlocks.AETHER_CONDUIT.get().defaultBlockState());
        helper.setBlock(routedCellPos, ModBlocks.AETHER_CELL.get().defaultBlockState());
        AetherCondenserBlockEntity routedCondenser = helper.getBlockEntity(routedCondenserPos, AetherCondenserBlockEntity.class);
        AetherConduitBlockEntity routedConduit = helper.getBlockEntity(conduitPos, AetherConduitBlockEntity.class);
        AetherCellBlockEntity routedCell = helper.getBlockEntity(routedCellPos, AetherCellBlockEntity.class);
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(48.0D));
        AetherCondenserBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(routedCondenserPos),
                helper.getLevel().getBlockState(helper.absolutePos(routedCondenserPos)), routedCondenser);
        helper.assertTrue(routedCell.storedAmount() > 0.0D,
                "Long-range AetherWorks routing should move aether through a conduit graph without waiting for relay ticks");
        AetherMachineMenu routedMenu = new AetherMachineMenu(8, player.getInventory(), helper.getLevel(),
                helper.absolutePos(routedCondenserPos));
        helper.assertTrue(routedMenu.graphNodeCount() >= 3 && routedMenu.routeDepth() >= 2,
                "Aether machine menu should expose routed graph topology depth");
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(120.0D));
        helper.assertTrue(routedMenu.automationRecipeCount() > 0,
                "Aether machine menu should expose ready automation recipes");
        helper.assertTrue(routedMenu.clickMenuButton(player, AetherMachineMenu.BUTTON_RUN_AUTOMATION_RECIPE),
                "Aether machine menu should run a ready automation recipe");
        helper.assertTrue(routedCondenser.aetherStorage().outputType() == AetherSignalType.REFINED_AETHER
                        && routedMenu.completedRecipeCount() > 0,
                "Condenser automation should refine output and track completed recipes");
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(132.0D));
        routedCondenser.stageAutomationInput(1);
        helper.assertTrue(routedCondenser.getItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT).getItem()
                        == ModItems.AETHER_COIL.get(),
                "AetherWorks staged automation input should occupy a physical machine inventory slot");
        helper.assertTrue(routedCondenser.getSlotsForFace(Direction.UP)[0]
                        == AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT
                        && routedCondenser.getSlotsForFace(Direction.NORTH).length == 2
                        && routedCondenser.getSlotsForFace(Direction.NORTH)[1]
                        == AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT
                        && routedCondenser.getSlotsForFace(Direction.DOWN)[0]
                        == AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT,
                "AetherWorks machines should expose both input lanes on top/sides and output below for hopper-style automation");
        helper.assertTrue(routedCondenser.canPlaceItemThroughFace(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                        new ItemStack(ModItems.AETHER_COIL.get()), Direction.UP)
                        && routedCondenser.canPlaceItemThroughFace(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                        new ItemStack(ModItems.PURITY_CATALYST.get()), Direction.UP)
                        && !routedCondenser.canPlaceItemThroughFace(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                        new ItemStack(ModItems.AETHER_COIL.get()), Direction.DOWN)
                        && !routedCondenser.canPlaceItemThroughFace(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT,
                        new ItemStack(ModItems.AETHER_COIL.get()), Direction.UP),
                "AetherWorks sided inventory should allow recipe inputs from top/sides and reject output-slot insertion");
        AetherMachineMenu remoteMenu = new AetherMachineMenu(9, player.getInventory(), helper.getLevel(),
                helper.absolutePos(routedCellPos));
        helper.assertTrue(remoteMenu.clickMenuButton(player, AetherMachineMenu.BUTTON_RUN_AUTOMATION_RECIPE),
                "AetherWorks automation should route recipe execution across the bounded topology graph");
        helper.assertTrue(routedCondenser.automationCycles() >= 2 && routedCondenser.automationOutputItem().getItem()
                        == ModItems.AETHER_CAPACITOR.get() && routedCondenser.automationOutputItem().getCount() >= 2,
                "Remote automation should choose the richer stocked recipe and stage physical output");
        helper.assertTrue(routedCondenser.canTakeItemThroughFace(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT,
                        routedCondenser.automationOutputItem(), Direction.DOWN)
                        && !routedCondenser.canTakeItemThroughFace(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                        routedCondenser.automationInputItem(), Direction.DOWN),
                "AetherWorks sided inventory should expose recipe output downward without exposing input stock");
        helper.assertTrue(routedCondenser.extractAutomationOutput(1) == 1,
                "AetherWorks stocked automation output should be extractable without duplicating inventory stock");
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, new ItemStack(Items.DIRT));
        routedCondenser.stageAutomationInput(1);
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(132.0D));
        int blockedInput = routedCondenser.getItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT).getCount();
        double blockedStored = routedCondenser.storedAmount();
        helper.assertTrue(!AetherWorksApi.runBestAutomationRecipe(player, routedCondenser),
                "AetherWorks stocked automation should refuse to run when the physical output slot is blocked");
        helper.assertTrue(routedCondenser.getItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT).getCount() == blockedInput
                        && routedCondenser.storedAmount() == blockedStored,
                "Blocked physical automation output must not consume input items or aether");
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(88.0D).withContamination(0.2D));
        double condenserContamination = routedCondenser.aetherStorage().contaminationLevel();
        helper.assertTrue(AetherWorksApi.runBestAutomationRecipe(player, routedCondenser),
                "AetherWorks condenser should distill physical catalyst into recovered coil stock");
        helper.assertTrue(routedCondenser.automationOutputItem().getItem() == ModItems.AETHER_COIL.get()
                        && routedCondenser.automationOutputItem().getCount() == 2
                        && routedCondenser.aetherStorage().outputType() == AetherSignalType.REFINED_AETHER
                        && routedCondenser.aetherStorage().contaminationLevel() < condenserContamination,
                "Catalyst distillation should produce two physical coils, refine output type, and reduce contamination");
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(152.0D).withContamination(0.18D));
        double condenserSeedCapacity = routedCondenser.aetherStorage().maxStoredAmount();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCondenser).id().equals("condenser_reactor_seed")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCondenser),
                "AetherWorks condenser should prefer the two-input reactor seed over single-input recipes");
        helper.assertTrue(routedCondenser.automationOutputItem().getItem() == ModItems.AETHER_CAPACITOR.get()
                        && routedCondenser.automationOutputItem().getCount() == 3
                        && routedCondenser.automationInputItem().isEmpty()
                        && routedCondenser.automationSecondaryInputItem().isEmpty()
                        && routedCondenser.aetherStorage().maxStoredAmount() > condenserSeedCapacity,
                "Condenser reactor seed should consume both physical inputs, stage three capacitors, and improve storage");
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedCell.setAetherStorage(routedCell.aetherStorage().withStoredAmount(104.0D));
        double cellCapacity = routedCell.aetherStorage().maxStoredAmount();
        helper.assertTrue(AetherWorksApi.runBestAutomationRecipe(player, routedCell),
                "AetherWorks cell should wind physical coil into a capacitor for the next recipe chain step");
        helper.assertTrue(routedCell.automationOutputItem().getItem() == ModItems.AETHER_CAPACITOR.get()
                        && routedCell.aetherStorage().maxStoredAmount() > cellCapacity,
                "Coil winding should stage a physical capacitor and tune cell capacity");
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedCell.setAetherStorage(routedCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.18D));
        double stabilizedCellCapacity = routedCell.aetherStorage().maxStoredAmount();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCell).id().equals("stabilized_capacitor_array")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCell),
                "AetherWorks cell should prefer a two-input stabilized capacitor array when both physical lanes are stocked");
        helper.assertTrue(routedCell.automationOutputItem().getItem() == ModItems.AETHER_CAPACITOR.get()
                        && routedCell.automationOutputItem().getCount() >= 3
                        && routedCell.automationInputItem().isEmpty()
                        && routedCell.automationSecondaryInputItem().isEmpty()
                        && routedCell.aetherStorage().maxStoredAmount() > stabilizedCellCapacity,
                "Stabilized capacitor array should consume both input lanes, add capacitor output, and improve cell capacity");
        routedConduit.setAetherStorage(routedConduit.aetherStorage().withStoredAmount(32.0D).withContamination(0.62D));
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        double dirtyLine = routedConduit.aetherStorage().contaminationLevel();
        helper.assertTrue(AetherWorksApi.runBestAutomationRecipe(player, routedConduit),
                "AetherWorks conduit automation should run a physical catalyst line-scrub recipe");
        helper.assertTrue(routedConduit.automationOutputItem().getItem() == ModItems.AETHER_COIL.get()
                        && routedConduit.aetherStorage().contaminationLevel() < dirtyLine
                        && routedConduit.aetherStorage().transferRate() > 24.0D,
                "Catalyst line-scrub should consume physical catalyst, recover a coil, lower contamination, and improve transfer");
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        routedConduit.setAetherStorage(routedConduit.aetherStorage().withStoredAmount(48.0D).withContamination(0.42D));
        double capacitorBleedContamination = routedConduit.aetherStorage().contaminationLevel();
        helper.assertTrue(AetherWorksApi.runBestAutomationRecipe(player, routedConduit),
                "AetherWorks conduit should bleed a physical capacitor into a purity catalyst chain output");
        helper.assertTrue(routedConduit.automationOutputItem().getItem() == ModItems.PURITY_CATALYST.get()
                        && routedConduit.aetherStorage().contaminationLevel() < capacitorBleedContamination
                        && routedConduit.aetherStorage().transferRate() > 25.0D,
                "Capacitor bleed should stage a physical catalyst, reduce contamination, and improve transfer");
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedConduit.setAetherStorage(routedConduit.aetherStorage().withStoredAmount(72.0D).withContamination(0.36D));
        double filterBundleContamination = routedConduit.aetherStorage().contaminationLevel();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedConduit).id().equals("conduit_filter_bundle")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedConduit),
                "AetherWorks conduit should prefer the two-input filter bundle when both lanes are stocked");
        helper.assertTrue(routedConduit.automationOutputItem().getItem() == ModItems.AETHER_COIL.get()
                        && routedConduit.automationOutputItem().getCount() == 2
                        && routedConduit.automationInputItem().isEmpty()
                        && routedConduit.automationSecondaryInputItem().isEmpty()
                        && routedConduit.aetherStorage().contaminationLevel() < filterBundleContamination,
                "Conduit filter bundle should consume capacitor plus catalyst, stage recovered coils, and scrub contamination");
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(208.0D).withContamination(0.56D));
        double scrubberContamination = routedCondenser.aetherStorage().contaminationLevel();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCondenser).id().equals("condenser_overload_scrubber")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCondenser),
                "AetherWorks condenser should expose a generator-family overload scrubber recipe");
        helper.assertTrue(routedCondenser.automationOutputItem().getItem() == ModItems.AETHER_COIL.get()
                        && routedCondenser.automationOutputItem().getCount() == 3
                        && routedCondenser.aetherStorage().contaminationLevel() < scrubberContamination,
                "Condenser overload scrubber should spend capacitor plus catalyst, recover coils, and reduce risk");
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedCell.setAetherStorage(routedCell.aetherStorage().withStoredAmount(208.0D).withContamination(0.14D));
        double busCapacity = routedCell.aetherStorage().maxStoredAmount();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCell).id().equals("cell_storage_bus")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCell),
                "AetherWorks cell should expose a storage-bus automation family recipe");
        helper.assertTrue(routedCell.automationOutputItem().getItem() == ModItems.PURITY_CATALYST.get()
                        && routedCell.automationOutputItem().getCount() == 2
                        && routedCell.aetherStorage().maxStoredAmount() > busCapacity,
                "Cell storage bus should spend capacitor plus coil, stage catalysts, and expand cell capacity");
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get()));
        routedConduit.setAetherStorage(routedConduit.aetherStorage().withStoredAmount(96.0D).withContamination(0.24D));
        routedConduit.cycleRedstoneMode();
        helper.setBlock(conduitPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        double relayTransfer = routedConduit.aetherStorage().transferRate();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedConduit).id().equals("conduit_redstone_relay")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedConduit),
                "AetherWorks conduit should expose a redstone-aware network-control recipe");
        helper.assertTrue(routedConduit.automationOutputItem().getItem() == ModItems.PURITY_CATALYST.get()
                        && routedConduit.automationOutputItem().getCount() == 2
                        && routedConduit.aetherStorage().transferRate() > relayTransfer,
                "Conduit redstone relay should spend coil plus capacitor, stage catalysts, and raise transfer rate");
        helper.setBlock(conduitPos.above(), Blocks.AIR.defaultBlockState());
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCondenser.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get(), 2));
        routedCondenser.setAetherStorage(routedCondenser.aetherStorage().withStoredAmount(124.0D).withContamination(0.05D));
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCondenser).id().equals("condenser_signal_rectifier")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCondenser)
                        && routedCondenser.aetherStorage().outputType() == AetherSignalType.SIGNAL_AETHER
                        && routedCondenser.automationOutputItem().getItem() == ModItems.PURITY_CATALYST.get(),
                "Condenser signal rectifier should broaden generator-family automation into signal-aether output");
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_CAPACITOR.get(), 2));
        routedCell.setAetherStorage(routedCell.aetherStorage().withStoredAmount(300.0D).withContamination(0.44D));
        double overflowCapacity = routedCell.aetherStorage().maxStoredAmount();
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(routedCell).id().equals("cell_overflow_shunt")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedCell)
                        && routedCell.aetherStorage().maxStoredAmount() > overflowCapacity
                        && routedCell.automationOutputItem().getItem() == ModItems.AETHER_COIL.get(),
                "Cell overflow shunt should broaden storage-family automation with a high-capacity safety recipe");
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        routedConduit.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        routedConduit.setAetherStorage(routedConduit.aetherStorage().withStoredAmount(84.0D).withContamination(0.26D));
        routedConduit.cycleRedstoneMode();
        routedConduit.cycleRedstoneMode();
        helper.setBlock(conduitPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        double pulseTransfer = routedConduit.aetherStorage().transferRate();
        helper.assertTrue(routedConduit.redstoneMode() == AetherStorageBlockEntity.REDSTONE_PULSE
                        && routedConduit.automationActive()
                        && AetherWorksApi.bestAutomationRecipe(routedConduit).id().equals("conduit_pulse_damper")
                        && AetherWorksApi.runBestAutomationRecipe(player, routedConduit)
                        && routedConduit.aetherStorage().transferRate() > pulseTransfer
                        && !routedConduit.automationActive(),
                "Conduit pulse damper should run as a one-shot network-control recipe and consume the pulse");
        helper.setBlock(conduitPos.above(), Blocks.AIR.defaultBlockState());
        BlockPos redstoneCellPos = new BlockPos(1, 1, 8);
        helper.setBlock(redstoneCellPos, ModBlocks.AETHER_CELL.get().defaultBlockState());
        AetherCellBlockEntity redstoneCell = helper.getBlockEntity(redstoneCellPos, AetherCellBlockEntity.class);
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        redstoneCell.setAetherStorage(redstoneCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.12D));
        AetherMachineMenu redstoneMenu = new AetherMachineMenu(10, player.getInventory(), helper.getLevel(),
                helper.absolutePos(redstoneCellPos));
        helper.assertTrue(redstoneMenu.clickMenuButton(player, AetherMachineMenu.BUTTON_TOGGLE_REDSTONE_CONTROL)
                        && redstoneCell.redstoneControlEnabled() && !redstoneCell.redstonePowered()
                        && !redstoneCell.automationActive(),
                "AetherWorks redstone control should gate automation until the machine receives power");
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(redstoneCell) == null
                        && !AetherWorksApi.runBestAutomationRecipe(player, redstoneCell)
                        && redstoneMenu.automationRecipeCount() == 0,
                "Unpowered redstone-gated machines should not advertise or run automation recipes");
        helper.setBlock(redstoneCellPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(redstoneCell.redstonePowered() && redstoneCell.automationActive()
                        && AetherWorksApi.bestAutomationRecipe(redstoneCell).id().equals("stabilized_capacitor_array")
                        && AetherWorksApi.runBestAutomationRecipe(player, redstoneCell),
                "Powered redstone-gated machines should resume physical automation recipes");
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        redstoneCell.setAetherStorage(redstoneCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.12D));
        helper.assertTrue(redstoneMenu.clickMenuButton(player, AetherMachineMenu.BUTTON_TOGGLE_REDSTONE_CONTROL)
                        && redstoneCell.redstoneMode() == AetherStorageBlockEntity.REDSTONE_LOW
                        && !redstoneCell.automationActive(),
                "Low-signal redstone mode should block automation while powered");
        helper.setBlock(redstoneCellPos.above(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(redstoneCell.automationActive() && AetherWorksApi.runBestAutomationRecipe(player, redstoneCell),
                "Low-signal redstone mode should run automation when the signal drops");
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        redstoneCell.setAetherStorage(redstoneCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.12D));
        helper.assertTrue(redstoneMenu.clickMenuButton(player, AetherMachineMenu.BUTTON_TOGGLE_REDSTONE_CONTROL)
                        && redstoneCell.redstoneMode() == AetherStorageBlockEntity.REDSTONE_PULSE
                        && !redstoneCell.automationActive(),
                "Pulse redstone mode should wait for a rising edge");
        helper.setBlock(redstoneCellPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(redstoneCell.automationActive() && AetherWorksApi.runBestAutomationRecipe(player, redstoneCell)
                        && !redstoneCell.automationActive(),
                "Pulse redstone mode should consume exactly one armed automation operation");
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        redstoneCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        redstoneCell.setAetherStorage(redstoneCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.12D));
        helper.assertTrue(AetherWorksApi.bestAutomationRecipe(redstoneCell) == null,
                "Pulse redstone mode should not re-run while the same signal remains high");
        helper.setBlock(redstoneCellPos.above(), Blocks.AIR.defaultBlockState());
        redstoneCell.automationActive();
        helper.setBlock(redstoneCellPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(redstoneCell.automationActive(),
                "Pulse redstone mode should re-arm after the signal drops and rises again");
        BlockPos sidedCellPos = new BlockPos(4, 1, 8);
        helper.setBlock(sidedCellPos, ModBlocks.AETHER_CELL.get().defaultBlockState());
        AetherCellBlockEntity sidedCell = helper.getBlockEntity(sidedCellPos, AetherCellBlockEntity.class);
        sidedCell.setItem(AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT,
                new ItemStack(ModItems.AETHER_COIL.get()));
        sidedCell.setItem(AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                new ItemStack(ModItems.PURITY_CATALYST.get()));
        sidedCell.setAetherStorage(sidedCell.aetherStorage().withStoredAmount(168.0D).withContamination(0.12D));
        sidedCell.cycleRedstoneMode();
        sidedCell.cycleRedstoneControlSide();
        helper.setBlock(sidedCellPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.assertTrue(!sidedCell.automationActive(),
                "Side-specific redstone should ignore power arriving from a non-selected face");
        sidedCell.cycleRedstoneControlSide();
        helper.assertTrue(sidedCell.redstoneControlSide() == Direction.UP.ordinal()
                        && sidedCell.automationActive()
                        && AetherWorksApi.runBestAutomationRecipe(player, sidedCell),
                "Side-specific redstone should accept power from the selected face");
        AetherMachineMenu sidedMenu = new AetherMachineMenu(11, player.getInventory(), helper.getLevel(),
                helper.absolutePos(sidedCellPos));
        helper.assertTrue(sidedMenu.redstoneSide() == Direction.UP.ordinal()
                        && sidedMenu.comparatorSignal() > 0
                        && sidedMenu.comparatorSignal() == sidedCell.comparatorSignal(),
                "AetherWorks menu data should expose selected redstone side plus comparator-grade machine signal");
        double overloadCapacity = redstoneCell.aetherStorage().maxStoredAmount();
        redstoneCell.setAetherStorage(redstoneCell.aetherStorage()
                .withStoredAmount(overloadCapacity * 0.96D)
                .withContamination(0.78D));
        int ventsBefore = redstoneCell.overloadEvents();
        double storedBeforeVent = redstoneCell.storedAmount();
        double transferBeforeVent = redstoneCell.aetherStorage().transferRate();
        helper.assertTrue(redstoneCell.checkOverloadSafety((ServerLevel) helper.getLevel())
                        && redstoneCell.overloadEvents() == ventsBefore + 1
                        && redstoneCell.storedAmount() < storedBeforeVent
                        && redstoneCell.lastOverloadSeverity() >= 3
                        && redstoneCell.aetherStorage().transferRate() < transferBeforeVent
                        && !redstoneCell.automationEnabled(),
                "AetherWorks overload safety should scale severity, vent stored aether, count the event, and pause automation without deleting inventory");
        BlockPos warningCellPos = new BlockPos(7, 1, 8);
        helper.setBlock(warningCellPos, ModBlocks.AETHER_CELL.get().defaultBlockState());
        AetherCellBlockEntity warningCell = helper.getBlockEntity(warningCellPos, AetherCellBlockEntity.class);
        warningCell.setAetherStorage(warningCell.aetherStorage()
                .withStoredAmount(warningCell.aetherStorage().maxStoredAmount() * 0.9D)
                .withContamination(0.0D));
        helper.assertTrue(warningCell.checkOverloadSafety((ServerLevel) helper.getLevel())
                        && warningCell.lastOverloadSeverity() == 1
                        && warningCell.overloadLockoutTicks() > 0
                        && "pressure_bleed".equals(warningCell.lastOverloadConsequence()),
                "AetherWorks overload consequence table should include a low-severity pressure bleed before cascade lockout");
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
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
        return Identifier.fromNamespaceAndPath(EchoAetherWorks.MODID, path);
    }
}
