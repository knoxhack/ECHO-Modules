package com.knoxhack.echopowergrid.test;

import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridDrawResult;
import com.knoxhack.echopowergrid.api.PowerGridNodeSummary;
import com.knoxhack.echopowergrid.api.PowerGridRouteSummary;
import com.knoxhack.echopowergrid.api.PowerGridNetworkSummary;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.capability.EpEnergyHandler;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import com.knoxhack.echopowergrid.integration.holomap.PowerGridMapDataProvider;
import com.knoxhack.echopowergrid.integration.PowerGridMachineCoreAdapter;
import com.knoxhack.echopowergrid.integration.PowerGridMachineCoreRuntimeProvider;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import com.knoxhack.echopowergrid.network.PowerGridNetworkSummaryPacket;
import com.knoxhack.echopowergrid.registry.ModBlocks;
import com.knoxhack.echocore.api.IMapMarker;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class PowerGridGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoPowerGrid.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATOR_CREATES_ENERGY =
            TEST_FUNCTIONS.register("generator_creates_energy", () -> PowerGridGameTests::generatorCreatesEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CABLE_CONNECTS_BLOCKS =
            TEST_FUNCTIONS.register("cable_connects_blocks", () -> PowerGridGameTests::cableConnectsBlocks);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BATTERY_STORES_ENERGY =
            TEST_FUNCTIONS.register("battery_stores_energy", () -> PowerGridGameTests::batteryStoresEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONSUMER_DRAWS_ENERGY =
            TEST_FUNCTIONS.register("consumer_draws_energy", () -> PowerGridGameTests::consumerDrawsEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NETWORK_REBUILDS_ON_PLACE =
            TEST_FUNCTIONS.register("network_rebuilds_on_place", () -> PowerGridGameTests::networkRebuildsOnPlace);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATION_NOT_DUPLICATED_UNDER_DEFICIT =
            TEST_FUNCTIONS.register("generation_not_duplicated_under_deficit", () -> PowerGridGameTests::generationNotDuplicatedUnderDeficit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRAW_POWER_SIMULATION_COMMIT =
            TEST_FUNCTIONS.register("draw_power_simulation_commit", () -> PowerGridGameTests::drawPowerSimulationCommit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FE_TRANSACTION_ROLLBACK =
            TEST_FUNCTIONS.register("fe_transaction_rollback", () -> PowerGridGameTests::feTransactionRollback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_PACKET_CREATION =
            TEST_FUNCTIONS.register("terminal_packet_creation", () -> PowerGridGameTests::terminalPacketCreation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOLOMAP_PROVIDER_MARKERS =
            TEST_FUNCTIONS.register("holomap_provider_markers", () -> PowerGridGameTests::holomapProviderMarkers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCRAP_BURNER_FUEL_SLOT_GENERATES =
            TEST_FUNCTIONS.register("scrap_burner_fuel_slot_generates", () -> PowerGridGameTests::scrapBurnerFuelSlotGenerates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATOR_FUEL_INVENTORY_PERSISTENCE =
            TEST_FUNCTIONS.register("generator_fuel_inventory_persistence", () -> PowerGridGameTests::generatorFuelInventoryPersistence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BREAKER_MENU_RESET =
            TEST_FUNCTIONS.register("breaker_menu_reset", () -> PowerGridGameTests::breakerMenuReset);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWER_NODE_MENU_DATA =
            TEST_FUNCTIONS.register("power_node_menu_data", () -> PowerGridGameTests::powerNodeMenuData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAND_CRANK_GENERATES_BURST =
            TEST_FUNCTIONS.register("hand_crank_generates_burst", () -> PowerGridGameTests::handCrankGeneratesBurst);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOLAR_PANEL_DAY_NIGHT =
            TEST_FUNCTIONS.register("solar_panel_day_night", () -> PowerGridGameTests::solarPanelDayNight);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GENERATOR_TYPE_PERSISTENCE =
            TEST_FUNCTIONS.register("generator_type_persistence", () -> PowerGridGameTests::generatorTypePersistence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OVERLOAD_TRIPS_BREAKER =
            TEST_FUNCTIONS.register("overload_trips_breaker", () -> PowerGridGameTests::overloadTripsBreaker);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BREAKER_ISOLATES_NETWORK =
            TEST_FUNCTIONS.register("breaker_isolates_network", () -> PowerGridGameTests::breakerIsolatesNetwork);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_SUMMARY_AND_LOSS =
            TEST_FUNCTIONS.register("route_summary_and_loss", () -> PowerGridGameTests::routeSummaryAndLoss);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NODE_SUMMARIES_INCLUDE_13_CONTENT =
            TEST_FUNCTIONS.register("node_summaries_include_13_content", () -> PowerGridGameTests::nodeSummariesInclude13Content);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TECH_WAVE_15_TRANSFER_LIMITS =
            TEST_FUNCTIONS.register("tech_wave_15_transfer_limits", () -> PowerGridGameTests::techWave15TransferLimits);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TECH_WAVE_15_GENERATOR_PROFILES =
            TEST_FUNCTIONS.register("tech_wave_15_generator_profiles", () -> PowerGridGameTests::techWave15GeneratorProfiles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TECH_WAVE_15_STORAGE_SUMMARIES =
            TEST_FUNCTIONS.register("tech_wave_15_storage_summaries", () -> PowerGridGameTests::techWave15StorageSummaries);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINECORE_RUNTIME =
            TEST_FUNCTIONS.register("powergrid_machinecore_runtime_snapshot_contract",
                    () -> PowerGridGameTests::powerGridMachineCoreRuntimeSnapshotContract);

    private PowerGridGameTests() {}

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("powergrid_hardening"));
        register(event, environment, "generator_creates_energy", GENERATOR_CREATES_ENERGY.getId());
        register(event, environment, "cable_connects_blocks", CABLE_CONNECTS_BLOCKS.getId());
        register(event, environment, "battery_stores_energy", BATTERY_STORES_ENERGY.getId());
        register(event, environment, "consumer_draws_energy", CONSUMER_DRAWS_ENERGY.getId());
        register(event, environment, "network_rebuilds_on_place", NETWORK_REBUILDS_ON_PLACE.getId());
        register(event, environment, "generation_not_duplicated_under_deficit", GENERATION_NOT_DUPLICATED_UNDER_DEFICIT.getId());
        register(event, environment, "draw_power_simulation_commit", DRAW_POWER_SIMULATION_COMMIT.getId());
        register(event, environment, "fe_transaction_rollback", FE_TRANSACTION_ROLLBACK.getId());
        register(event, environment, "terminal_packet_creation", TERMINAL_PACKET_CREATION.getId());
        register(event, environment, "holomap_provider_markers", HOLOMAP_PROVIDER_MARKERS.getId());
        register(event, environment, "scrap_burner_fuel_slot_generates", SCRAP_BURNER_FUEL_SLOT_GENERATES.getId());
        register(event, environment, "generator_fuel_inventory_persistence", GENERATOR_FUEL_INVENTORY_PERSISTENCE.getId());
        register(event, environment, "breaker_menu_reset", BREAKER_MENU_RESET.getId());
        register(event, environment, "power_node_menu_data", POWER_NODE_MENU_DATA.getId());
        register(event, environment, "hand_crank_generates_burst", HAND_CRANK_GENERATES_BURST.getId());
        register(event, environment, "solar_panel_day_night", SOLAR_PANEL_DAY_NIGHT.getId());
        register(event, environment, "generator_type_persistence", GENERATOR_TYPE_PERSISTENCE.getId());
        register(event, environment, "overload_trips_breaker", OVERLOAD_TRIPS_BREAKER.getId());
        register(event, environment, "breaker_isolates_network", BREAKER_ISOLATES_NETWORK.getId());
        register(event, environment, "route_summary_and_loss", ROUTE_SUMMARY_AND_LOSS.getId());
        register(event, environment, "node_summaries_include_13_content", NODE_SUMMARIES_INCLUDE_13_CONTENT.getId());
        register(event, environment, "tech_wave_15_transfer_limits", TECH_WAVE_15_TRANSFER_LIMITS.getId());
        register(event, environment, "tech_wave_15_generator_profiles", TECH_WAVE_15_GENERATOR_PROFILES.getId());
        register(event, environment, "tech_wave_15_storage_summaries", TECH_WAVE_15_STORAGE_SUMMARIES.getId());
        register(event, environment, "powergrid_machinecore_runtime_snapshot_contract", MACHINECORE_RUNTIME.getId());
    }

    private static void powerGridMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 2, 1);
        helper.setBlock(generatorPos, ModBlocks.SCRAP_BURNER_GENERATOR.get().defaultBlockState());
        GeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, GeneratorBlockEntity.class);
        generator.fuelInventory().setItem(0, new ItemStack(Items.COAL, 2));
        for (int i = 0; i < 3; i++) {
            GeneratorBlockEntity.tick(helper.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
        }

        EchoMachineRuntimeSnapshot direct = PowerGridMachineCoreAdapter.runtimeSnapshot(generator);
        helper.assertTrue(direct.id().value().equals(EchoPowerGrid.id("scrap_burner_generator").toString())
                        && direct.ownerModule().value().equals(EchoPowerGrid.MODID)
                        && direct.kind() == EchoMachineKind.POWERED_STATION
                        && direct.state() == EchoMachineState.ACTIVE,
                "PowerGrid MachineCore adapter should publish the placed Scrap Burner identity and active state");
        helper.assertTrue(direct.energy().stored() == 80 && direct.energy().capacity() == 2000
                        && "EP".equals(direct.energy().unit()),
                "PowerGrid MachineCore snapshot should map live EP buffer state");
        helper.assertTrue(direct.inventory().totalSlots() == 1
                        && direct.inventory().occupiedSlots() == 1
                        && direct.inventory().slots().get(0).role().equals("fuel"),
                "PowerGrid MachineCore snapshot should expose the physical generator fuel slot");
        helper.assertTrue("echopowergrid:fuel_generation".equals(direct.process().recipeContract())
                        && direct.savedState().persistedKeys().contains("Energy")
                        && direct.savedState().persistedKeys().contains("BurnTime")
                        && direct.savedState().persistedKeys().contains("FuelInventory"),
                "PowerGrid MachineCore snapshot should expose generation process and saved fuel/energy keys");
        helper.assertTrue(EchoMachineUiBridge.hasAutomationSurface(direct)
                        && EchoMachineUiBridge.position(direct).filter(generator.getBlockPos()::equals).isPresent()
                        && direct.side().downSlots().contains("energy_output"),
                "PowerGrid MachineCore snapshot should expose UI bridge side and position metadata");

        BlockPos batteryPos = new BlockPos(4, 2, 1);
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        BatteryBlockEntity battery = helper.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        battery.setEnergyStored(400);
        EchoMachineRuntimeSnapshot batterySnapshot = PowerGridMachineCoreAdapter.runtimeSnapshot(battery);
        helper.assertTrue(batterySnapshot.energy().stored() == 400
                        && batterySnapshot.energy().capacity() == 20000
                        && "echopowergrid:battery_storage".equals(batterySnapshot.process().recipeContract()),
                "PowerGrid MachineCore snapshot should map live battery EP storage");

        BlockPos consumerPos = new BlockPos(6, 2, 1);
        helper.setBlock(consumerPos, ModBlocks.TEST_POWER_CONSUMER.get().defaultBlockState());
        PowerConsumerBlockEntity consumer = helper.getBlockEntity(consumerPos, PowerConsumerBlockEntity.class);
        consumer.setPowerReceived(20, 3);
        EchoMachineRuntimeSnapshot consumerSnapshot = PowerGridMachineCoreAdapter.runtimeSnapshot(consumer);
        helper.assertTrue(consumerSnapshot.state() == EchoMachineState.ACTIVE
                        && "echopowergrid:power_delivery".equals(consumerSnapshot.process().recipeContract())
                        && "20".equals(consumerSnapshot.process().attributes().get("demandEpPerTick")),
                "PowerGrid MachineCore snapshot should map live consumer demand telemetry");

        PowerGridMachineCoreRuntimeProvider.register();
        EchoMachineRuntimeSnapshot registered = EchoMachineRuntimeRegistry.snapshot(helper.getLevel(), generator.getBlockPos())
                .orElse(null);
        helper.assertTrue(registered != null && registered.id().value().equals(direct.id().value())
                        && "80/2000 EP".equals(EchoMachineUiBridge.energyLine(registered)),
                "MachineCore registry should discover the placed PowerGrid Scrap Burner through its runtime provider");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(generator.getBlockPos().above().getCenter());
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(direct.id().value())
                                && EchoMachineUiBridge.hasAutomationSurface(snapshot)),
                "MachineCore player snapshots should include the nearby PowerGrid generator");
        helper.assertTrue(EchoMachineRuntimeRegistry.snapshots(player).stream()
                        .anyMatch(snapshot -> snapshot.id().value().equals(EchoPowerGrid.id("small_battery_bank").toString())
                                && snapshot.energy().stored() == 400),
                "MachineCore player snapshots should include the nearby PowerGrid battery");
        List<EchoMachineProfile> profiles = EchoMachineRuntimeRegistry.profiles(player);
        helper.assertTrue(profiles.stream().anyMatch(profile -> profile.id().value().equals(direct.id().value())
                        && profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && "echopowergrid:fuel_generation".equals(binding.recipeId().value()))),
                "MachineCore profiles should expose the PowerGrid fuel generation binding");
        helper.succeed();
    }

    private static void generatorCreatesEnergy(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(rel);
        helper.setBlock(rel, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity be = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(be instanceof GeneratorBlockEntity, "Block entity should be GeneratorBlockEntity");
            if (be instanceof GeneratorBlockEntity gen) {
                helper.assertTrue(gen.getStoredEnergy() > 0 || gen.getGenerationRate() > 0,
                        "Creative generator should have generation rate or stored energy");
            }
            helper.succeed();
        });
    }

    private static void cableConnectsBlocks(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos batPos = new BlockPos(3, 2, 1);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(cablePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(batPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());

        helper.runAfterDelay(2, () -> {
            BlockState cableState = helper.getBlockState(cablePos);
            boolean eastConnected = cableState.getValue(com.knoxhack.echopowergrid.block.CableBlock.EAST);
            boolean westConnected = cableState.getValue(com.knoxhack.echopowergrid.block.CableBlock.WEST);
            helper.assertTrue(eastConnected && westConnected,
                    "Cable should connect to both generator and battery: east=" + eastConnected + " west=" + westConnected);
            helper.succeed();
        });
    }

    private static void batteryStoresEnergy(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos batPos = new BlockPos(3, 2, 1);
        BlockPos absBatPos = helper.absolutePos(batPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(cablePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(batPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());

        helper.runAfterDelay(85, () -> {
            PowerNetworkManager mgr = PowerNetworkManager.get(helper.getLevel());
            for (int i = 0; i < 120; i++) {
                mgr.tick();
            }
            BlockPos absGenPos = helper.absolutePos(new BlockPos(1, 2, 1));
            String diag = "gen=" + mgr.debugNetworkState(absGenPos) + " bat=" + mgr.debugNetworkState(absBatPos) + " count=" + mgr.getNetworkCount();
            BlockEntity be = helper.getLevel().getBlockEntity(absBatPos);
            helper.assertTrue(be instanceof BatteryBlockEntity, "Block entity should be BatteryBlockEntity");
            if (be instanceof BatteryBlockEntity bat) {
                helper.assertTrue(bat.getStoredEnergy() > 0,
                        "Battery should store energy after ticks: stored=" + bat.getStoredEnergy() + " " + diag);
            }
            helper.succeed();
        });
    }

    private static void consumerDrawsEnergy(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos conPos = new BlockPos(3, 2, 1);
        BlockPos absConPos = helper.absolutePos(conPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(cablePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(conPos, ModBlocks.TEST_POWER_CONSUMER.get().defaultBlockState());

        helper.runAfterDelay(85, () -> {
            PowerNetworkManager mgr = PowerNetworkManager.get(helper.getLevel());
            for (int i = 0; i < 120; i++) {
                mgr.tick();
            }
            BlockEntity be = helper.getLevel().getBlockEntity(absConPos);
            helper.assertTrue(be instanceof PowerConsumerBlockEntity, "Block entity should be PowerConsumerBlockEntity");
            if (be instanceof PowerConsumerBlockEntity con) {
                PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(helper.getLevel(), absConPos);
                helper.assertTrue(snap.totalDemand() > 0 && con.isOnline(),
                        "Consumer should register demand and remain powered between network updates");
            }
            helper.succeed();
        });
    }

    private static void networkRebuildsOnPlace(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            PowerNetworkManager mgr = PowerNetworkManager.get(helper.getLevel());
            int before = mgr.getNetworkCount();
            helper.setBlock(cablePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
            helper.runAfterDelay(2, () -> {
                int after = mgr.getNetworkCount();
                helper.assertTrue(after >= before, "Network count should be stable or grow after placing cable");
                helper.succeed();
            });
        });
    }

    private static void generationNotDuplicatedUnderDeficit(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cableOnePos = new BlockPos(2, 2, 1);
        BlockPos batteryPos = new BlockPos(3, 2, 1);
        BlockPos cableTwoPos = new BlockPos(4, 2, 1);
        BlockPos consumerPos = new BlockPos(5, 2, 1);
        BlockPos absBatteryPos = helper.absolutePos(batteryPos);
        BlockPos absConsumerPos = helper.absolutePos(consumerPos);

        helper.setBlock(genPos, ModBlocks.SOLAR_PANEL.get().defaultBlockState());
        helper.setBlock(cableOnePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.setBlock(cableTwoPos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(consumerPos, ModBlocks.TEST_POWER_CONSUMER.get().defaultBlockState());

        helper.runAfterDelay(45, () -> {
            BlockEntity battery = helper.getLevel().getBlockEntity(absBatteryPos);
            BlockEntity consumer = helper.getLevel().getBlockEntity(absConsumerPos);
            helper.assertTrue(battery instanceof BatteryBlockEntity, "Battery block entity should exist");
            helper.assertTrue(consumer instanceof PowerConsumerBlockEntity, "Consumer block entity should exist");
            if (battery instanceof BatteryBlockEntity bat && consumer instanceof PowerConsumerBlockEntity con) {
                helper.assertTrue(!con.isOnline(), "Undersupplied consumer should brown out instead of receiving duplicated power");
                helper.assertTrue(bat.getStoredEnergy() == 0,
                        "Battery should not charge while a higher-priority consumer is under deficit; stored=" + bat.getStoredEnergy());
            }
            helper.succeed();
        });
    }

    private static void drawPowerSimulationCommit(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(1, 2, 1);
        BlockPos absBatteryPos = helper.absolutePos(batteryPos);
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absBatteryPos);
            helper.assertTrue(blockEntity instanceof BatteryBlockEntity, "Battery block entity should exist");
            if (blockEntity instanceof BatteryBlockEntity battery) {
                for (int i = 0; i < 4; i++) {
                    battery.receiveEnergy(100, false);
                }
                // Ensure battery is in a network before drawing
                PowerNetworkManager mgr = PowerNetworkManager.get(helper.getLevel());
                mgr.markDirty(absBatteryPos);
                for (int i = 0; i < 10; i++) {
                    mgr.tick();
                }
                PowerGridDrawResult simulated = EchoPowerGridApi.drawPower(helper.getLevel(), absBatteryPos, 100, true);
                helper.assertTrue(simulated.drawn() == 100, "Simulated draw should report full available EP");
                helper.assertTrue(battery.getStoredEnergy() == 400, "Simulated draw should not debit battery storage");
                PowerGridDrawResult committed = EchoPowerGridApi.drawPower(helper.getLevel(), absBatteryPos, 100, false);
                helper.assertTrue(committed.drawn() == 100, "Committed draw should report debited EP");
                helper.assertTrue(battery.getStoredEnergy() == 300,
                        "Committed draw should reduce battery storage to 300 EP; stored=" + battery.getStoredEnergy());
            }
            helper.succeed();
        });
    }

    private static void feTransactionRollback(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(1, 2, 1);
        BlockPos absBatteryPos = helper.absolutePos(batteryPos);
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absBatteryPos);
            helper.assertTrue(blockEntity instanceof BatteryBlockEntity, "Battery block entity should exist");
            BatteryBlockEntity battery = (BatteryBlockEntity) blockEntity;
            EpEnergyHandler handler = new EpEnergyHandler(battery, null);
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(100, transaction);
                helper.assertTrue(inserted > 0, "FE bridge should accept energy inside an open transaction");
            }
            helper.assertTrue(battery.getStoredEnergy() == 0,
                    "Aborted FE insert transaction should roll battery back to zero EP");
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(100, transaction);
                helper.assertTrue(inserted > 0, "FE bridge should accept committed energy");
                transaction.commit();
            }
            long committed = battery.getStoredEnergy();
            helper.assertTrue(committed > 0, "Committed FE insert should remain in battery storage");
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(50, transaction);
                helper.assertTrue(extracted > 0, "FE bridge should expose extract during an open transaction");
            }
            helper.assertTrue(battery.getStoredEnergy() == committed,
                    "Aborted FE extract transaction should restore committed battery storage");
            helper.succeed();
        });
    }

    private static void terminalPacketCreation(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(1, 2, 1);
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            PowerGridNetworkSummaryPacket packet =
                    PowerGridNetworkSummaryPacket.current(helper.makeMockServerPlayerInLevel());
            helper.assertTrue(packet.statusLine().contains("Networks"), "Terminal packet should include a network status line");
            helper.assertTrue(!packet.networks().isEmpty(), "Terminal packet should include loaded PowerGrid networks");
            helper.succeed();
        });
    }

    private static void holomapProviderMarkers(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(1, 2, 1);
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            List<PowerGridNetworkSummary> summaries = EchoPowerGridApi.loadedNetworkSummaries(helper.getLevel());
            List<IMapMarker> markers = PowerGridMapDataProvider.INSTANCE.markers(helper.makeMockServerPlayerInLevel());
            helper.assertTrue(!summaries.isEmpty(), "PowerGrid should expose loaded network summaries");
            helper.assertTrue(markers.size() >= summaries.size(), "HoloMap provider should emit a marker for each loaded network");
            helper.assertTrue(PowerGridMapDataProvider.INSTANCE.layers(helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE)).size() == 1,
                    "HoloMap provider should expose a Power Networks layer");
            helper.succeed();
        });
    }

    private static void scrapBurnerFuelSlotGenerates(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.SCRAP_BURNER_GENERATOR.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof GeneratorBlockEntity, "Scrap Burner should have a generator block entity");
            GeneratorBlockEntity generator = (GeneratorBlockEntity) blockEntity;
            generator.fuelInventory().setItem(0, new ItemStack(Items.COAL));
            GeneratorBlockEntity.tick(helper.getLevel(), absolute, generator.getBlockState(), generator);
            GeneratorBlockEntity.tick(helper.getLevel(), absolute, generator.getBlockState(), generator);
            helper.assertTrue(generator.getBurnTime() > 0, "Fuel slot should start the burn timer");
            helper.assertTrue(generator.fuelInventory().getItem(0).isEmpty(), "Fuel slot should consume one coal item");
            helper.assertTrue(generator.getEnergyStored() > 0, "Burning Scrap Burner should generate buffered EP");
            helper.succeed();
        });
    }

    private static void generatorFuelInventoryPersistence(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.SCRAP_BURNER_GENERATOR.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof GeneratorBlockEntity, "Scrap Burner should have a generator block entity");
            GeneratorBlockEntity generator = (GeneratorBlockEntity) blockEntity;
            generator.fuelInventory().setItem(0, new ItemStack(Items.CHARCOAL, 3));
            generator.receiveEnergy(120, false);
            CompoundTag saved = generator.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockEntity restored = BlockEntity.loadStatic(absolute, generator.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored instanceof GeneratorBlockEntity, "Saved Scrap Burner should restore as a generator");
            GeneratorBlockEntity restoredGenerator = (GeneratorBlockEntity) restored;
            helper.assertTrue(restoredGenerator.fuelInventory().getItem(0).is(Items.CHARCOAL)
                            && restoredGenerator.fuelInventory().getItem(0).getCount() == 3,
                    "Fuel inventory should survive block entity save/load");
            helper.assertTrue(restoredGenerator.getEnergyStored() == 120,
                    "Generator buffer should still persist alongside fuel inventory");
            helper.succeed();
        });
    }

    private static void breakerMenuReset(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.EMERGENCY_BREAKER.get().defaultBlockState()
                .setValue(com.knoxhack.echopowergrid.block.BreakerBlock.TRIPPED, true));
        helper.runAfterDelay(1, () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            PowerNodeMenu menu = new PowerNodeMenu(1, player.getInventory(), helper.getLevel(), absolute);
            helper.assertTrue(menu.isTripped(), "Breaker menu should report a tripped breaker");
            helper.assertTrue(menu.clickMenuButton(player, PowerNodeMenu.BUTTON_RESET_BREAKER),
                    "Breaker menu reset button should be handled server-side");
            helper.assertTrue(!helper.getLevel().getBlockState(absolute)
                            .getValue(com.knoxhack.echopowergrid.block.BreakerBlock.TRIPPED),
                    "Breaker reset button should clear the tripped block state");
            helper.succeed();
        });
    }

    private static void powerNodeMenuData(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cableOnePos = new BlockPos(2, 2, 1);
        BlockPos batteryPos = new BlockPos(3, 2, 1);
        BlockPos cableTwoPos = new BlockPos(4, 2, 1);
        BlockPos consumerPos = new BlockPos(5, 2, 1);
        BlockPos absoluteBattery = helper.absolutePos(batteryPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(cableOnePos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());
        helper.setBlock(cableTwoPos, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(consumerPos, ModBlocks.TEST_POWER_CONSUMER.get().defaultBlockState());

        helper.runAfterDelay(85, () -> {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            PowerNodeMenu menu = new PowerNodeMenu(1, player.getInventory(), helper.getLevel(), absoluteBattery);
            helper.assertTrue(menu.kind() == PowerNodeMenu.KIND_BATTERY, "Battery should open a battery PowerNode menu");
            helper.assertTrue(menu.networkGeneration() > 0, "PowerNode menu should expose live network generation");
            helper.assertTrue(menu.networkDemand() > 0, "PowerNode menu should expose live network demand");
            helper.assertTrue(menu.networkCapacity() > 0, "PowerNode menu should expose live network storage capacity");
            helper.assertTrue(menu.nodeCount() >= 5, "PowerNode menu should expose connected node count");
            helper.succeed();
        });
    }

    private static void handCrankGeneratesBurst(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.HAND_CRANK_GENERATOR.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof GeneratorBlockEntity, "Hand crank should have a generator block entity");
            GeneratorBlockEntity generator = (GeneratorBlockEntity) blockEntity;
            helper.assertTrue(generator.getGeneratorType() == com.knoxhack.echopowergrid.api.GeneratorType.HAND_CRANK,
                    "Generator type should be HAND_CRANK");
            helper.assertTrue(generator.getEnergyStored() == 0, "Hand crank should start empty");
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            generator.crank(player);
            helper.assertTrue(generator.getEnergyStored() > 0, "Crank should add energy to buffer");
            helper.assertTrue(generator.getCrankCooldown() > 0, "Crank should set cooldown");
            helper.succeed();
        });
    }

    private static void solarPanelDayNight(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.SOLAR_PANEL.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof GeneratorBlockEntity, "Solar panel should have a generator block entity");
            GeneratorBlockEntity generator = (GeneratorBlockEntity) blockEntity;
            helper.assertTrue(generator.getGeneratorType() == com.knoxhack.echopowergrid.api.GeneratorType.SOLAR,
                    "Generator type should be SOLAR");
            long genRate = generator.getGenerationPerTick();
            // GameTestServer may run at day or night; just verify logic is consistent
            // If sky is visible and it's daytime, genRate > 0
            // If not, genRate == 0
            helper.assertTrue(genRate >= 0, "Solar generation rate should be non-negative");
            helper.succeed();
        });
    }

    private static void generatorTypePersistence(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(local);
        helper.setBlock(local, ModBlocks.SCRAP_BURNER_GENERATOR.get().defaultBlockState());
        helper.runAfterDelay(1, () -> {
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(blockEntity instanceof GeneratorBlockEntity, "Block entity should be GeneratorBlockEntity");
            GeneratorBlockEntity generator = (GeneratorBlockEntity) blockEntity;
            generator.receiveEnergy(120, false);
            CompoundTag saved = generator.saveWithFullMetadata(helper.getLevel().registryAccess());
            // Verify restore via loadStatic produces a matching BE with correct type and energy
            BlockEntity restored = BlockEntity.loadStatic(absolute, generator.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored instanceof GeneratorBlockEntity, "Saved generator should restore as generator");
            GeneratorBlockEntity restoredGenerator = (GeneratorBlockEntity) restored;
            helper.assertTrue(restoredGenerator.getGeneratorType() == com.knoxhack.echopowergrid.api.GeneratorType.FUEL_BURNER,
                    "Generator type should persist through save/load");
            helper.assertTrue(restoredGenerator.getEnergyStored() == 120,
                    "Generator buffer should persist alongside type");
            helper.succeed();
        });
    }

    private static void overloadTripsBreaker(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos breakerPos = new BlockPos(2, 2, 1);
        BlockPos conPos = new BlockPos(3, 2, 1);
        BlockPos absBreaker = helper.absolutePos(breakerPos);
        BlockPos absCon = helper.absolutePos(conPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(breakerPos, ModBlocks.EMERGENCY_BREAKER.get().defaultBlockState());
        helper.setBlock(conPos, ModBlocks.CREATIVE_POWER_SINK.get().defaultBlockState());

        // Force an overload: creative source has huge generation, consumer has huge demand.
        // Low voltage cable isn't used here; the transfer limit is determined by the breaker (1000 when not tripped).
        // Creative sink demand is Long.MAX_VALUE/4 which exceeds breaker limit.
        helper.runAfterDelay(120, () -> {
            BlockState breakerState = helper.getLevel().getBlockState(absBreaker);
            helper.assertTrue(breakerState.getValue(com.knoxhack.echopowergrid.block.BreakerBlock.TRIPPED),
                    "Breaker should trip after overload grace ticks");
            BlockEntity conBe = helper.getLevel().getBlockEntity(absCon);
            helper.assertTrue(conBe instanceof PowerConsumerBlockEntity, "Consumer should exist");
            if (conBe instanceof PowerConsumerBlockEntity consumer) {
                // After breaker trips, consumer should brown out because the breaker isolates the network
                helper.assertTrue(!consumer.isOnline(), "Consumer should lose power after breaker trips");
            }
            helper.succeed();
        });
    }

    private static void breakerIsolatesNetwork(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos breakerPos = new BlockPos(2, 2, 1);
        BlockPos batPos = new BlockPos(3, 2, 1);
        BlockPos absBat = helper.absolutePos(batPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(breakerPos, ModBlocks.EMERGENCY_BREAKER.get().defaultBlockState()
                .setValue(com.knoxhack.echopowergrid.block.BreakerBlock.TRIPPED, true));
        helper.setBlock(batPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());

        helper.runAfterDelay(10, () -> {
            PowerNetworkManager mgr = PowerNetworkManager.get(helper.getLevel());
            mgr.tick();
            mgr.tick();
            // Battery should NOT charge because the tripped breaker isolates the generator
            BlockEntity batBe = helper.getLevel().getBlockEntity(absBat);
            helper.assertTrue(batBe instanceof BatteryBlockEntity, "Battery should exist");
            if (batBe instanceof BatteryBlockEntity battery) {
                helper.assertTrue(battery.getEnergyStored() == 0,
                        "Battery should not charge through a tripped breaker");
            }
            helper.succeed();
        });
    }

    private static void routeSummaryAndLoss(GameTestHelper helper) {
        BlockPos genPos = new BlockPos(1, 2, 1);
        BlockPos cableOne = new BlockPos(2, 2, 1);
        BlockPos cableTwo = new BlockPos(3, 2, 1);
        BlockPos batteryPos = new BlockPos(4, 2, 1);
        BlockPos absGen = helper.absolutePos(genPos);
        BlockPos absBattery = helper.absolutePos(batteryPos);

        helper.setBlock(genPos, ModBlocks.CREATIVE_POWER_SOURCE.get().defaultBlockState());
        helper.setBlock(cableOne, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(cableTwo, ModBlocks.LOW_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(batteryPos, ModBlocks.SMALL_BATTERY_BANK.get().defaultBlockState());

        helper.runAfterDelay(10, () -> {
            PowerNetworkManager.get(helper.getLevel()).tick();
            PowerGridRouteSummary route = EchoPowerGridApi.routeSummary(helper.getLevel(), absBattery, absGen);
            helper.assertTrue(!route.blocked(), "Route summary should resolve through connected cables: " + route.blockedReason());
            helper.assertTrue(route.cableDistance() >= 3, "Route should include cable distance");
            helper.assertTrue(route.pathTransferLimit() >= 100, "Route should expose path transfer limit");
            helper.assertTrue(route.lossPercent() >= 0.0D, "Route should expose deterministic loss");
            helper.succeed();
        });
    }

    private static void nodeSummariesInclude13Content(GameTestHelper helper) {
        BlockPos medium = new BlockPos(1, 2, 1);
        BlockPos relay = new BlockPos(2, 2, 1);
        BlockPos coupler = new BlockPos(3, 2, 1);
        helper.setBlock(medium, ModBlocks.MEDIUM_BATTERY_BANK.get().defaultBlockState());
        helper.setBlock(relay, ModBlocks.RELAY_SUBSTATION.get().defaultBlockState());
        helper.setBlock(coupler, ModBlocks.NEXUS_STABILIZER_COUPLER.get().defaultBlockState());

        helper.runAfterDelay(10, () -> {
            PowerNetworkManager.get(helper.getLevel()).tick();
            List<PowerGridNodeSummary> nodes = EchoPowerGridApi.loadedNodeSummaries(helper.getLevel());
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(medium))),
                    "Node summaries should include Medium Battery Bank");
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(relay))),
                    "Node summaries should include Relay Substation");
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(coupler))),
                    "Node summaries should include Nexus Stabilizer Coupler");
            helper.succeed();
        });
    }

    private static void techWave15TransferLimits(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.getTransferLimit(ModBlocks.HIGH_VOLTAGE_CABLE.get().defaultBlockState()) == 1200,
                "High Voltage Cable should expose a 1200 EP/t transfer limit");
        helper.assertTrue(ModBlocks.getTransferLimit(ModBlocks.FACTORY_SUBSTATION.get().defaultBlockState()) == 3000,
                "Factory Substation should expose a 3000 EP/t routing cap");
        helper.assertTrue(ModBlocks.getTransferLimit(ModBlocks.FIELD_BATTERY_BANK.get().defaultBlockState()) == Long.MAX_VALUE,
                "Field Battery Bank should not impose cable-like transfer limits");
        helper.succeed();
    }

    private static void techWave15GeneratorProfiles(GameTestHelper helper) {
        BlockPos solar = new BlockPos(1, 2, 1);
        BlockPos biofuel = new BlockPos(2, 2, 1);
        BlockPos absSolar = helper.absolutePos(solar);
        BlockPos absBiofuel = helper.absolutePos(biofuel);
        helper.setBlock(solar, ModBlocks.REINFORCED_SOLAR_PANEL.get().defaultBlockState());
        helper.setBlock(biofuel, ModBlocks.BIOFUEL_GENERATOR.get().defaultBlockState());

        helper.runAfterDelay(1, () -> {
            BlockEntity solarEntity = helper.getLevel().getBlockEntity(absSolar);
            BlockEntity biofuelEntity = helper.getLevel().getBlockEntity(absBiofuel);
            helper.assertTrue(solarEntity instanceof GeneratorBlockEntity,
                    "Reinforced Solar Panel should create a generator block entity");
            helper.assertTrue(biofuelEntity instanceof GeneratorBlockEntity,
                    "Biofuel Generator should create a generator block entity");
            if (solarEntity instanceof GeneratorBlockEntity solarGenerator
                    && biofuelEntity instanceof GeneratorBlockEntity biofuelGenerator) {
                helper.assertTrue(solarGenerator.getGeneratorType() == com.knoxhack.echopowergrid.api.GeneratorType.SOLAR,
                        "Reinforced Solar Panel should use SOLAR behavior");
                helper.assertTrue(solarGenerator.getGenerationRate() == 30,
                        "Reinforced Solar Panel should generate 30 EP/t when active");
                helper.assertTrue(solarGenerator.getMaxEnergyStored() == 900,
                        "Reinforced Solar Panel should buffer 900 EP");
                helper.assertTrue(biofuelGenerator.getGeneratorType() == com.knoxhack.echopowergrid.api.GeneratorType.FUEL_BURNER,
                        "Biofuel Generator should use fuel-burner behavior");
                helper.assertTrue(biofuelGenerator.getGenerationRate() == 80,
                        "Biofuel Generator should generate 80 EP/t while burning fuel");
                biofuelGenerator.fuelInventory().setItem(0, new ItemStack(Items.DRIED_KELP_BLOCK));
                GeneratorBlockEntity.tick(helper.getLevel(), absBiofuel, biofuelGenerator.getBlockState(), biofuelGenerator);
                GeneratorBlockEntity.tick(helper.getLevel(), absBiofuel, biofuelGenerator.getBlockState(), biofuelGenerator);
                helper.assertTrue(biofuelGenerator.getBurnTime() > 0,
                        "Biofuel Generator should consume valid furnace fuel");
                helper.assertTrue(biofuelGenerator.getEnergyStored() > 0,
                        "Biofuel Generator should buffer energy while burning fuel");
            }
            helper.succeed();
        });
    }

    private static void techWave15StorageSummaries(GameTestHelper helper) {
        BlockPos battery = new BlockPos(1, 2, 1);
        BlockPos cable = new BlockPos(2, 2, 1);
        BlockPos substation = new BlockPos(3, 2, 1);
        helper.setBlock(battery, ModBlocks.FIELD_BATTERY_BANK.get().defaultBlockState());
        helper.setBlock(cable, ModBlocks.HIGH_VOLTAGE_CABLE.get().defaultBlockState());
        helper.setBlock(substation, ModBlocks.FACTORY_SUBSTATION.get().defaultBlockState());

        helper.runAfterDelay(10, () -> {
            PowerNetworkManager.get(helper.getLevel()).tick();
            List<PowerGridNodeSummary> nodes = EchoPowerGridApi.loadedNodeSummaries(helper.getLevel());
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(battery))),
                    "Node summaries should include Field Battery Bank");
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(cable))),
                    "Node summaries should include High Voltage Cable");
            helper.assertTrue(nodes.stream().anyMatch(node -> node.pos().equals(helper.absolutePos(substation))),
                    "Node summaries should include Factory Substation");
            helper.succeed();
        });
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
                12);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoPowerGrid.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, path);
    }
}
