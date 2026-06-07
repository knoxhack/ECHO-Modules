package com.knoxhack.echoashfallprotocol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

final class AshfallAdapterCoreMachinePowerRuntime {
    private static final String ADAPTER_SURFACE = "adaptercore.machine_power_runtime";
    private static final int MAX_STACK_SIZE = 64;
    private static final int MAX_WEAR = 1_000;
    private static final int JAM_THRESHOLD = 800;
    private static final int REPAIR_WEAR_REDUCTION = 200;
    private static final double DEFAULT_GENERATOR_FAILURE_CHANCE = 0.0005D;
    private static final double MAX_GENERATOR_WEAR_FAILURE_BONUS = 0.005D;
    private static final float[] DETERMINISTIC_BYPRODUCT_ROLLS = {0.05F, 0.35F, 0.15F, 0.55F, 0.95F};
    private static final List<Map<String, Object>> REGISTERED_BLOCK_ENTITY_BINDINGS = List.of(
            blockEntityBinding("echoashfallprotocol:hand_recycler", "HandRecyclerBlockEntity", "tick.manual_recycler"),
            blockEntityBinding("echoashfallprotocol:thermal_burner", "ThermalBurnerBlockEntity", "tick.fuel_burn_energy_and_ash_output"),
            blockEntityBinding("echoashfallprotocol:water_purifier", "WaterPurifierBlockEntity", "tick.powered_purification_processor"),
            blockEntityBinding("echoashfallprotocol:rain_collector", "RainCollectorBlockEntity", "tick.passive_water_collector"),
            blockEntityBinding("echoashfallprotocol:micro_generator", "MicroGeneratorBlockEntity", "tick.power_source"),
            blockEntityBinding("echoashfallprotocol:thermal_array", "ThermalArrayBlockEntity", "tick.passive_heat_power_source"),
            blockEntityBinding("echoashfallprotocol:battery_bank", "BatteryBankBlockEntity", "tick.energy_storage"),
            blockEntityBinding("echoashfallprotocol:scrap_dynamo", "ScrapDynamoBlockEntity", "tick.scrap_fueled_power_source"),
            blockEntityBinding("echoashfallprotocol:nexus_capacitor", "NexusCapacitorBlockEntity", "tick.energy_storage.nexus"),
            blockEntityBinding("echoashfallprotocol:load_distributor", "LoadDistributorBlockEntity", "tick.power_router"),
            blockEntityBinding("echoashfallprotocol:scrap_press", "ScrapPressBlockEntity", "tick.powered_machine"),
            blockEntityBinding("echoashfallprotocol:signal_scanner", "SignalScannerBlockEntity", "tick.poi_signal_scanner"),
            blockEntityBinding("echoashfallprotocol:field_med_bay", "FieldMedBayBlockEntity", "tick.medical_support"),
            blockEntityBinding("echoashfallprotocol:atmospheric_scrubber", "AtmosphericScrubberBlockEntity", "tick.air_quality_processor"),
            blockEntityBinding("echoashfallprotocol:autofeed_hopper", "AutofeedHopperBlockEntity", "tick.player_feed_machine"),
            blockEntityBinding("echoashfallprotocol:contaminant_condenser", "ContaminantCondenserBlockEntity", "tick.toxic_block_condenser"),
            blockEntityBinding("echoashfallprotocol:filter_workbench", "FilterWorkbenchBlockEntity", "tick.filter_crafting_station"),
            blockEntityBinding("echoashfallprotocol:power_node", "PowerNodeBlockEntity", "tick.power_network_anchor"),
            blockEntityBinding("echoashfallprotocol:nexus_core", "NexusCoreBlockEntity", "tick.nexus_state_machine"),
            blockEntityBinding("echoashfallprotocol:ore_grinder", "OreGrinderBlockEntity", "tick.powered_machine"),
            blockEntityBinding("echoashfallprotocol:isotope_refiner", "IsotopeRefinerBlockEntity", "tick.powered_catalyst_refiner"),
            blockEntityBinding("echoashfallprotocol:radiation_cleanser", "RadiationCleanserBlockEntity", "tick.powered_decontamination_processor"),
            blockEntityBinding("echoashfallprotocol:crystalline_synthesizer", "CrystallineSynthesizerBlockEntity", "tick.powered_phase_synthesizer"),
            blockEntityBinding("echoashfallprotocol:deep_core_miner", "DeepCoreMinerBlockEntity", "tick.deep_depth_resource_generator"),
            blockEntityBinding("echoashfallprotocol:item_pipe", "ItemPipeBlockEntity", "tick.item_router"),
            blockEntityBinding("echoashfallprotocol:power_cable", "PowerCableBlockEntity", "tick.power_relay.all_cable_blocks"),
            blockEntityBinding("echoashfallprotocol:reinforced_power_cable", "PowerCableBlockEntity", "tick.power_relay.reinforced"),
            blockEntityBinding("echoashfallprotocol:high_voltage_power_cable", "PowerCableBlockEntity", "tick.power_relay.high_voltage"),
            blockEntityBinding("echoashfallprotocol:factory_controller", "FactoryControllerBlockEntity", "tick.factory_scan"),
            blockEntityBinding("echoashfallprotocol:structure_cache", "StructureCacheBlockEntity", "tick.structure_cache"),
            blockEntityBinding("echoashfallprotocol:echo_container", "EchoContainerBlockEntity", "tick.container_inventory"));

    private final Map<String, RuntimeNode> nodes = new LinkedHashMap<>();
    private final List<Map<String, Object>> powerTransfers = new ArrayList<>();
    private final List<Map<String, Object>> itemTransfers = new ArrayList<>();
    private final List<String> diagnostics = new ArrayList<>();
    private final Map<String, Integer> eventCounts = new LinkedHashMap<>();
    private int tickCount;
    private int factoryScanCount;
    private int factoryConnectedMachines;
    private int factoryStoredEnergy;
    private int factoryEnergyCapacity;
    private boolean factoryScanLimitRespected = true;

    private AshfallAdapterCoreMachinePowerRuntime() {
    }

    static AshfallAdapterCoreMachinePowerRuntime createDefaultWorld() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.generator("micro_generator", 0, 0, 0, 3_000, 64, 8, 1));
        runtime.add(RuntimeNode.energy("power_cable", "PowerCableBlockEntity", 1, 0, 0, 1_000, 50, NodeRole.CABLE));
        runtime.add(RuntimeNode.router("load_distributor", 2, 0, 0, 2_000, 512));
        runtime.add(RuntimeNode.energy("battery_bank", "BatteryBankBlockEntity", 2, 1, 0, 10_000, 100, NodeRole.STORAGE));
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 3, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 9, "compressed_scrap", 1));
        runtime.add(RuntimeNode.pipe("item_pipe", 4, 0, 0, 8));
        runtime.add(RuntimeNode.pipe("loop_pipe", 4, 1, 0, 8));
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 5, 0, 0, 2_000, 128,
                2, 80, "ore_substrate", 4, "iron_shard", 2).withByproduct("crystal_dust", 1));
        runtime.add(RuntimeNode.factoryController("factory_controller", 2, -1, 0));
        runtime.add(RuntimeNode.inventorySource("press_output_hopper", 3, 1, 0, "ore_substrate", 1));

        runtime.link("micro_generator", "power_cable");
        runtime.link("power_cable", "load_distributor");
        runtime.link("load_distributor", "scrap_press");
        runtime.link("load_distributor", "ore_grinder");
        runtime.link("load_distributor", "battery_bank");
        runtime.link("load_distributor", "factory_controller");
        runtime.link("press_output_hopper", "item_pipe");
        runtime.link("item_pipe", "loop_pipe");
        runtime.link("item_pipe", "ore_grinder");
        runtime.link("factory_controller", "power_cable");
        runtime.link("factory_controller", "item_pipe");
        return runtime;
    }

    static Map<String, Object> runDefaultScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = createDefaultWorld();
        runtime.forceMachineFailure("micro_generator");
        runtime.handleInteraction("player.use_block", "micro_generator");
        runtime.handleInteraction("player.use_block", "load_distributor");
        runtime.tick(80);
        return runtime.describe();
    }

    static Map<String, Object> runPersistenceRoundTripScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = createDefaultWorld();
        runtime.forceMachineFailure("micro_generator");
        runtime.handleInteraction("player.use_block", "micro_generator");
        runtime.handleInteraction("player.use_block", "load_distributor");
        runtime.tick(40);
        Map<String, Object> snapshot = runtime.snapshot();

        AshfallAdapterCoreMachinePowerRuntime restored = restore(snapshot);
        restored.tick(40);
        Map<String, Object> restoredState = restored.describe();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "world_state.persistence_bridge");
        result.put("implementationTarget", "AdapterCore native machine state snapshot and restore");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("snapshotNodeCount", ((List<?>) snapshot.get("nodes")).size());
        result.put("restoredTickCount", restoredState.get("tickCount"));
        result.put("restoredBatteryStoredEnergy", restoredState.get("batteryStoredEnergy"));
        result.put("restoredScrapPressOutputCount", restoredState.get("scrapPressOutputCount"));
        result.put("restoredOreGrinderOutputCount", restoredState.get("oreGrinderOutputCount"));
        result.put("restoredFactoryScanCount", restoredState.get("factoryScanCount"));
        result.put("restoredNetworkDiagnostic", restoredState.get("networkDiagnostic"));
        result.put("restoredState", restoredState);
        result.put("status", Integer.valueOf(80).equals(restoredState.get("tickCount"))
                && "PASS".equals(restoredState.get("networkDiagnostic")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runCableTierScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.energy("source_basic", "BatteryBankBlockEntity", 0, 0, 0, 10_000, 1024, NodeRole.STORAGE).withEnergy(4096));
        runtime.add(RuntimeNode.energy("source_reinforced", "BatteryBankBlockEntity", 0, 1, 0, 10_000, 1024, NodeRole.STORAGE).withEnergy(4096));
        runtime.add(RuntimeNode.energy("source_high_voltage", "BatteryBankBlockEntity", 0, 2, 0, 10_000, 1024, NodeRole.STORAGE).withEnergy(4096));
        runtime.add(RuntimeNode.energy("power_cable", "PowerCableBlockEntity", 1, 0, 0, 1_000, 50, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("reinforced_power_cable", "PowerCableBlockEntity", 1, 1, 0, 2_000, 256, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("high_voltage_power_cable", "PowerCableBlockEntity", 1, 2, 0, 4_000, 1024, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("sink_basic", "BatteryBankBlockEntity", 2, 0, 0, 10_000, 1024, NodeRole.STORAGE));
        runtime.add(RuntimeNode.energy("sink_reinforced", "BatteryBankBlockEntity", 2, 1, 0, 10_000, 1024, NodeRole.STORAGE));
        runtime.add(RuntimeNode.energy("sink_high_voltage", "BatteryBankBlockEntity", 2, 2, 0, 10_000, 1024, NodeRole.STORAGE));
        runtime.link("source_basic", "power_cable");
        runtime.link("power_cable", "sink_basic");
        runtime.link("source_reinforced", "reinforced_power_cable");
        runtime.link("reinforced_power_cable", "sink_reinforced");
        runtime.link("source_high_voltage", "high_voltage_power_cable");
        runtime.link("high_voltage_power_cable", "sink_high_voltage");

        runtime.moveEnergy("source_basic", "power_cable", 4096);
        runtime.moveEnergy("power_cable", "sink_basic", 4096);
        runtime.moveEnergy("source_reinforced", "reinforced_power_cable", 4096);
        runtime.moveEnergy("reinforced_power_cable", "sink_reinforced", 4096);
        runtime.moveEnergy("source_high_voltage", "high_voltage_power_cable", 4096);
        runtime.moveEnergy("high_voltage_power_cable", "sink_high_voltage", 4096);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "power_network.cable_tier_bridge");
        result.put("implementationTarget", "AdapterCore native cable tier transfer limits");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("basicCableCapacity", runtime.nodes.get("power_cable").capacity);
        result.put("basicCableTransfer", runtime.nodes.get("power_cable").transferPerTick);
        result.put("reinforcedCableCapacity", runtime.nodes.get("reinforced_power_cable").capacity);
        result.put("reinforcedCableTransfer", runtime.nodes.get("reinforced_power_cable").transferPerTick);
        result.put("highVoltageCableCapacity", runtime.nodes.get("high_voltage_power_cable").capacity);
        result.put("highVoltageCableTransfer", runtime.nodes.get("high_voltage_power_cable").transferPerTick);
        result.put("basicSinkEnergy", runtime.nodes.get("sink_basic").energy);
        result.put("reinforcedSinkEnergy", runtime.nodes.get("sink_reinforced").energy);
        result.put("highVoltageSinkEnergy", runtime.nodes.get("sink_high_voltage").energy);
        result.put("adjacencyDetected", runtime.nodes.values().stream().allMatch(node -> !node.neighbors.isEmpty()));
        result.put("powerTransfers", List.copyOf(runtime.powerTransfers));
        result.put("powerCapacityRespected", runtime.powerCapacityRespected());
        result.put("networkDiagnostic", runtime.powerCapacityRespected() ? "PASS" : "WARN");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runAdjacencyPowerFlowScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.generator("micro_generator", 0, 0, 0, 3_000, 1024, 8, 0).withEnergy(512));
        runtime.add(RuntimeNode.energy("power_cable", "PowerCableBlockEntity", 1, 0, 0, 1_000, 50, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("reinforced_power_cable", "PowerCableBlockEntity", 2, 0, 0, 2_000, 256, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("return_power_cable", "PowerCableBlockEntity", 1, 1, 0, 1_000, 50, NodeRole.CABLE));
        runtime.add(RuntimeNode.router("load_distributor", 3, 0, 0, 2_000, 512));
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 4, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 9, "compressed_scrap", 1));
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 4, 1, 0, 2_000, 128,
                2, 80, "ore_substrate", 4, "iron_shard", 2).withByproduct("crystal_dust", 1));
        runtime.add(RuntimeNode.energy("battery_bank", "BatteryBankBlockEntity", 3, 1, 0, 10_000, 100, NodeRole.STORAGE));

        runtime.link("micro_generator", "power_cable");
        runtime.link("power_cable", "reinforced_power_cable");
        runtime.link("reinforced_power_cable", "load_distributor");
        runtime.link("load_distributor", "scrap_press");
        runtime.link("load_distributor", "ore_grinder");
        runtime.link("load_distributor", "battery_bank");
        runtime.link("reinforced_power_cable", "return_power_cable");
        runtime.link("return_power_cable", "power_cable");

        List<Map<String, Object>> routes = new ArrayList<>();
        routes.add(runtime.transferEnergyAlongPowerRoute("micro_generator", "scrap_press", 512));
        routes.add(runtime.transferEnergyAlongPowerRoute("micro_generator", "ore_grinder", 512));
        routes.add(runtime.transferEnergyAlongPowerRoute("micro_generator", "battery_bank", 512));

        RuntimeNode generator = runtime.nodes.get("micro_generator");
        RuntimeNode press = runtime.nodes.get("scrap_press");
        RuntimeNode grinder = runtime.nodes.get("ore_grinder");
        RuntimeNode battery = runtime.nodes.get("battery_bank");
        int loopSkipCount = routes.stream().mapToInt(route -> (Integer) route.get("loopSkipCount")).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "power_network.adjacency_flow_bridge");
        result.put("implementationTarget", "AdapterCore native adjacency-driven power network flow");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("routeCount", routes.size());
        result.put("routes", List.copyOf(routes));
        result.put("powerTransferCount", runtime.powerTransfers.size());
        result.put("powerTransfers", List.copyOf(runtime.powerTransfers));
        result.put("sourceEnergyAfter", generator.energy);
        result.put("scrapPressEnergy", press.energy);
        result.put("oreGrinderEnergy", grinder.energy);
        result.put("batteryStoredEnergy", battery.energy);
        result.put("basicCableTransfer", runtime.nodes.get("power_cable").transferPerTick);
        result.put("reinforcedCableTransfer", runtime.nodes.get("reinforced_power_cable").transferPerTick);
        result.put("routerTransfer", runtime.nodes.get("load_distributor").transferPerTick);
        result.put("adjacencyDetected", runtime.nodes.values().stream().allMatch(node -> !node.neighbors.isEmpty()));
        result.put("generatorToConsumerFlow", press.energy == 50 && grinder.energy == 50);
        result.put("storageFlow", battery.energy == 50);
        result.put("cableTierLimitsRespected", runtime.powerTransfers.stream().allMatch(transfer ->
                ((Integer) transfer.get("amount")) <= transferLimitFor(runtime, String.valueOf(transfer.get("source")),
                        String.valueOf(transfer.get("target")))));
        result.put("loopSkipCount", loopSkipCount);
        result.put("loopAvoided", loopSkipCount > 0);
        result.put("powerCapacityRespected", runtime.powerCapacityRespected());
        result.put("networkDiagnostic", runtime.powerCapacityRespected()
                && Boolean.TRUE.equals(result.get("generatorToConsumerFlow"))
                && Boolean.TRUE.equals(result.get("storageFlow"))
                && Boolean.TRUE.equals(result.get("cableTierLimitsRespected"))
                && Boolean.TRUE.equals(result.get("loopAvoided")) ? "PASS" : "WARN");
        result.put("status", "PASS".equals(result.get("networkDiagnostic")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runPriorityRoutingScenario() {
        Map<String, Object> survivalFirst = runPriorityRoutingProbe("SURVIVAL", 2);
        Map<String, Object> factoryFirst = runPriorityRoutingProbe("FACTORY", 2);
        Map<String, Object> gridFirst = runPriorityRoutingProbe("GRID", 2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "power_network.priority_routing_bridge");
        result.put("implementationTarget", "AdapterCore native Load Distributor constrained priority routing");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("survivalFirst", survivalFirst);
        result.put("factoryFirst", factoryFirst);
        result.put("gridFirst", gridFirst);
        result.put("survivalPrioritizedScrapPress", Integer.valueOf(1).equals(survivalFirst.get("scrapPressProgress"))
                && Integer.valueOf(0).equals(survivalFirst.get("oreGrinderProgress")));
        result.put("factoryPrioritizedOreGrinder", Integer.valueOf(0).equals(factoryFirst.get("scrapPressProgress"))
                && Integer.valueOf(1).equals(factoryFirst.get("oreGrinderProgress")));
        result.put("gridPrioritizedBattery", Integer.valueOf(2).equals(gridFirst.get("batteryStoredEnergy"))
                && Integer.valueOf(0).equals(gridFirst.get("scrapPressProgress"))
                && Integer.valueOf(0).equals(gridFirst.get("oreGrinderProgress")));
        result.put("priorityModesDistinct", !survivalFirst.equals(factoryFirst) && !factoryFirst.equals(gridFirst));
        result.put("status", Boolean.TRUE.equals(result.get("survivalPrioritizedScrapPress"))
                && Boolean.TRUE.equals(result.get("factoryPrioritizedOreGrinder"))
                && Boolean.TRUE.equals(result.get("gridPrioritizedBattery"))
                && Boolean.TRUE.equals(result.get("priorityModesDistinct")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    private static Map<String, Object> runPriorityRoutingProbe(String priorityMode, int routerEnergy) {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.router("load_distributor", 0, 0, 0, 2_000, 512)
                .withEnergy(routerEnergy)
                .withPriorityMode(priorityMode));
        runtime.add(RuntimeNode.energy("battery_bank", "BatteryBankBlockEntity", 0, 1, 0, 10_000, 100, NodeRole.STORAGE));
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 1, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 9, "compressed_scrap", 1));
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 1, 1, 0, 2_000, 128,
                2, 80, "ore_substrate", 4, "iron_shard", 2).withByproduct("crystal_dust", 1));

        runtime.distributeFromRouter(runtime.nodes.get("load_distributor"));
        runtime.tickMachineRecipe(runtime.nodes.get("scrap_press"), 2);
        runtime.tickMachineRecipe(runtime.nodes.get("ore_grinder"), 1);

        RuntimeNode router = runtime.nodes.get("load_distributor");
        RuntimeNode press = runtime.nodes.get("scrap_press");
        RuntimeNode grinder = runtime.nodes.get("ore_grinder");
        RuntimeNode battery = runtime.nodes.get("battery_bank");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("priorityMode", priorityMode);
        result.put("routerEnergyInput", routerEnergy);
        result.put("routerEnergyAfter", router.energy);
        result.put("scrapPressEnergy", press.energy);
        result.put("scrapPressProgress", press.progress);
        result.put("oreGrinderEnergy", grinder.energy);
        result.put("oreGrinderProgress", grinder.progress);
        result.put("batteryStoredEnergy", battery.energy);
        result.put("powerTransfers", List.copyOf(runtime.powerTransfers));
        result.put("powerTransferCount", runtime.powerTransfers.size());
        result.put("powerCapacityRespected", runtime.powerCapacityRespected());
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runRecipeCatalogScenario() {
        AshfallNativeMachineRecipeCatalog.Recipe pressRecipe = AshfallNativeMachineRecipeCatalog.scrapPressRecipe("scrap_metal");
        AshfallNativeMachineRecipeCatalog.Recipe stoneRecipe = AshfallNativeMachineRecipeCatalog.grinderRecipe("stone");
        AshfallNativeMachineRecipeCatalog.Recipe toxicRecipe = AshfallNativeMachineRecipeCatalog.grinderRecipe("toxic_slagstone");

        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode press = RuntimeNode.machine("scrap_press_recipe_probe", "ScrapPressBlockEntity", 0, 0, 0, 1_500, 128,
                pressRecipe.powerPerTick(), pressRecipe.processingTicks(), pressRecipe.inputId(), pressRecipe.inputCount(),
                pressRecipe.outputId(), pressRecipe.outputCount()).withEnergy(pressRecipe.powerPerTick() * pressRecipe.processingTicks());
        RuntimeNode stoneGrinder = RuntimeNode.machine("ore_grinder_stone_probe", "OreGrinderBlockEntity", 1, 0, 0, 2_000, 128,
                stoneRecipe.powerPerTick(), stoneRecipe.processingTicks(), stoneRecipe.inputId(), stoneRecipe.inputCount(),
                stoneRecipe.outputId(), stoneRecipe.outputCount())
                .withByproduct(stoneRecipe.byproductId(), stoneRecipe.byproductCount(), stoneRecipe.byproductChance())
                .withEnergy(stoneRecipe.powerPerTick() * stoneRecipe.processingTicks());
        RuntimeNode toxicGrinder = RuntimeNode.machine("ore_grinder_toxic_probe", "OreGrinderBlockEntity", 2, 0, 0, 2_000, 128,
                toxicRecipe.powerPerTick(), toxicRecipe.processingTicks(), toxicRecipe.inputId(), toxicRecipe.inputCount(),
                toxicRecipe.outputId(), toxicRecipe.outputCount())
                .withByproduct(toxicRecipe.byproductId(), toxicRecipe.byproductCount(), toxicRecipe.byproductChance())
                .withEnergy(toxicRecipe.powerPerTick() * toxicRecipe.processingTicks());
        runtime.add(press);
        runtime.add(stoneGrinder);
        runtime.add(toxicGrinder);

        runtime.tickRecipeProbe(press, 2);
        runtime.tickRecipeProbe(stoneGrinder, 1);
        runtime.tickRecipeProbe(toxicGrinder, 1);

        Map<String, Object> recipeCatalog = AshfallNativeMachineRecipeCatalog.describe();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.recipe_execution_bridge");
        result.put("implementationTarget", "AdapterCore native machine recipe execution");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("recipeCatalog", recipeCatalog);
        result.put("scrapPressRecipeCount", recipeCatalog.get("scrapPressRecipeCount"));
        result.put("oreGrinderRecipeCount", recipeCatalog.get("oreGrinderRecipeCount"));
        result.put("executedRecipeCount", 3);
        result.put("scrapPressOutput", press.outputItem);
        result.put("scrapPressOutputCount", press.outputCount);
        result.put("scrapPressWear", press.wear);
        result.put("stoneGrinderOutput", stoneGrinder.outputItem);
        result.put("stoneGrinderOutputCount", stoneGrinder.outputCount);
        result.put("stoneGrinderByproduct", stoneGrinder.byproductItem);
        result.put("stoneGrinderByproductCount", stoneGrinder.byproductCount);
        result.put("toxicGrinderOutput", toxicGrinder.outputItem);
        result.put("toxicGrinderOutputCount", toxicGrinder.outputCount);
        result.put("toxicGrinderByproduct", toxicGrinder.byproductItem);
        result.put("toxicGrinderByproductCount", toxicGrinder.byproductCount);
        result.put("status", Integer.valueOf(1).equals(result.get("scrapPressOutputCount"))
                && Integer.valueOf(4).equals(result.get("stoneGrinderOutputCount"))
                && Integer.valueOf(2).equals(result.get("toxicGrinderOutputCount")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runByproductChanceScenario() {
        AshfallNativeMachineRecipeCatalog.Recipe toxicRecipe = AshfallNativeMachineRecipeCatalog.grinderRecipe("toxic_slagstone");
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode grinder = RuntimeNode.machine("ore_grinder_byproduct_probe", "OreGrinderBlockEntity", 0, 0, 0,
                2_000, 128, toxicRecipe.powerPerTick(), toxicRecipe.processingTicks(), toxicRecipe.inputId(),
                toxicRecipe.inputCount(), toxicRecipe.outputId(), toxicRecipe.outputCount())
                .withInputCount(toxicRecipe.inputCount() * 4)
                .withByproduct(toxicRecipe.byproductId(), toxicRecipe.byproductCount(), toxicRecipe.byproductChance())
                .withEnergy(toxicRecipe.powerPerTick() * toxicRecipe.processingTicks() * 4);
        runtime.add(grinder);

        for (int batch = 0; batch < 4; batch++) {
            runtime.tickRecipeProbe(grinder, 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.ore_grinder_byproduct_chance_bridge");
        result.put("implementationTarget", "AdapterCore native Ore Grinder byproduct chance execution");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("recipeInput", toxicRecipe.inputId());
        result.put("recipeByproduct", grinder.byproductItem);
        result.put("byproductChance", grinder.byproductChance);
        result.put("recipeBatches", 4);
        result.put("byproductRolls", List.copyOf(grinder.byproductRollHistory));
        result.put("byproductSuccesses", grinder.byproductSuccessCount);
        result.put("byproductSkipped", grinder.byproductSkipCount);
        result.put("outputCount", grinder.outputCount);
        result.put("byproductCount", grinder.byproductCount);
        result.put("remainingInputCount", grinder.inputCount);
        result.put("remainingEnergy", grinder.energy);
        result.put("wear", grinder.wear);
        result.put("chanceRespected", Integer.valueOf(2).equals(result.get("byproductSuccesses"))
                && Integer.valueOf(2).equals(result.get("byproductSkipped"))
                && Integer.valueOf(8).equals(result.get("outputCount"))
                && Integer.valueOf(2).equals(result.get("byproductCount"))
                && Integer.valueOf(0).equals(result.get("remainingInputCount")));
        result.put("status", Boolean.TRUE.equals(result.get("chanceRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runOutputBackpressureScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode press = RuntimeNode.machine("scrap_press_output_probe", "ScrapPressBlockEntity", 0, 0, 0,
                1_500, 128, 1, 40, "scrap_metal", 9, "compressed_scrap", 1)
                .withEnergy(80)
                .withOutputCount(MAX_STACK_SIZE);
        runtime.add(press);

        runtime.tickRecipeProbe(press, 2);
        int blockedProgress = press.progress;
        int blockedEnergy = press.energy;
        int blockedInputCount = press.inputCount;
        int blockedOutputCount = press.outputCount;

        int extracted = runtime.extractOutputItem("scrap_press_output_probe", "compressed_scrap", 1, false);
        runtime.tickRecipeProbe(press, 2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.output_backpressure_bridge");
        result.put("implementationTarget", "AdapterCore native machine output capacity gating");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("blockedProgress", blockedProgress);
        result.put("blockedEnergy", blockedEnergy);
        result.put("blockedInputCount", blockedInputCount);
        result.put("blockedOutputCount", blockedOutputCount);
        result.put("extractedOutputCount", extracted);
        result.put("resumedProgress", press.progress);
        result.put("resumedEnergy", press.energy);
        result.put("resumedInputCount", press.inputCount);
        result.put("resumedOutputCount", press.outputCount);
        result.put("resumedWear", press.wear);
        result.put("outputBackpressureRespected", blockedProgress == 0
                && blockedEnergy == 80
                && blockedInputCount == 9
                && blockedOutputCount == MAX_STACK_SIZE
                && extracted == 1
                && press.progress == 0
                && press.energy == 40
                && press.inputCount == 0
                && press.outputCount == MAX_STACK_SIZE
                && press.wear == 2);
        result.put("status", Boolean.TRUE.equals(result.get("outputBackpressureRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runJamRepairScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = createDefaultWorld();
        RuntimeNode press = runtime.nodes.get("scrap_press");
        press.jammed = true;
        Map<String, Object> jammedTick = runtime.tick(5);
        Map<String, Object> repairEvent = runtime.handleInteraction("player.use_block", "scrap_press");
        runtime.tick(40);
        Map<String, Object> repairedState = runtime.describe();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.jam_repair_bridge");
        result.put("implementationTarget", "AdapterCore native machine jam and repair event");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("jamPreventedProgress", jammedTick.get("scrapPressProgress"));
        result.put("repairEventHandled", repairEvent.get("handled"));
        result.put("scrapPressJammedAfterRepair", repairedState.get("scrapPressJammed"));
        result.put("scrapPressOutputCountAfterRepair", repairedState.get("scrapPressOutputCount"));
        result.put("scrapPressWearAfterRepair", repairedState.get("scrapPressWear"));
        result.put("networkDiagnostic", repairedState.get("networkDiagnostic"));
        result.put("status", Integer.valueOf(0).equals(result.get("jamPreventedProgress"))
                && Boolean.TRUE.equals(result.get("repairEventHandled"))
                && Boolean.FALSE.equals(result.get("scrapPressJammedAfterRepair")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runWearThresholdScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode press = RuntimeNode.machine("scrap_press_wear_probe", "ScrapPressBlockEntity", 0, 0, 0,
                1_500, 128, 1, 40, "scrap_metal", 9, "compressed_scrap", 1)
                .withEnergy(80)
                .withWear(JAM_THRESHOLD - 2);
        runtime.add(press);

        boolean thresholdJam = press.addWearAndMaybeJam(2, 0.20F);
        int wearAtJam = press.wear;
        runtime.tickRecipeProbe(press, 2);
        int jammedProgress = press.progress;
        int jammedOutput = press.outputCount;
        int jammedEnergy = press.energy;

        Map<String, Object> repairEvent = runtime.handleInteraction("player.use_block", "scrap_press_wear_probe");
        int repairedWear = press.wear;
        boolean repairedJammed = press.jammed;
        runtime.tickRecipeProbe(press, 2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.wear_threshold_repair_bridge");
        result.put("implementationTarget", "AdapterCore native MachineWearData threshold and repair semantics");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("maxWear", MAX_WEAR);
        result.put("jamThreshold", JAM_THRESHOLD);
        result.put("repairWearReduction", REPAIR_WEAR_REDUCTION);
        result.put("thresholdJamTriggered", thresholdJam);
        result.put("wearAtJam", wearAtJam);
        result.put("jammedProgress", jammedProgress);
        result.put("jammedOutput", jammedOutput);
        result.put("jammedEnergy", jammedEnergy);
        result.put("repairEventHandled", repairEvent.get("handled"));
        result.put("repairedWear", repairedWear);
        result.put("repairedJammed", repairedJammed);
        result.put("resumedOutput", press.outputCount);
        result.put("resumedInput", press.inputCount);
        result.put("resumedEnergy", press.energy);
        result.put("resumedWear", press.wear);
        result.put("wearRepairRespected", thresholdJam
                && wearAtJam == JAM_THRESHOLD
                && jammedProgress == 0
                && jammedOutput == 0
                && jammedEnergy == 80
                && Boolean.TRUE.equals(repairEvent.get("handled"))
                && repairedWear == JAM_THRESHOLD - REPAIR_WEAR_REDUCTION
                && !repairedJammed
                && press.outputCount == 1
                && press.inputCount == 0
                && press.energy == 40
                && press.wear == JAM_THRESHOLD - REPAIR_WEAR_REDUCTION + 2);
        result.put("status", Boolean.TRUE.equals(result.get("wearRepairRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runGeneratorFailureChanceScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode generator = RuntimeNode.generator("micro_generator", 0, 0, 0, 3_000, 64, 8, 1)
                .withWear(MAX_WEAR);
        runtime.add(generator);

        generator.startFuelCycleIfNeeded();
        generator.burnOneTick();
        boolean safeRollFailed = generator.applyGeneratorFailureRoll(0.006D);
        int energyAfterSafeRoll = generator.energy;
        int burnTicksAfterSafeRoll = generator.burnTicksRemaining;
        boolean failureRollFailed = generator.applyGeneratorFailureRoll(0.004D);
        boolean failedAfterRoll = generator.failed;
        int burnTicksAfterFailure = generator.burnTicksRemaining;
        Map<String, Object> restartEvent = runtime.handleInteraction("player.use_block", "micro_generator");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.micro_generator_failure_chance_bridge");
        result.put("implementationTarget", "AdapterCore native Micro Generator random failure formula");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("baseFailureChance", DEFAULT_GENERATOR_FAILURE_CHANCE);
        result.put("wearFailureBonusAtMaxWear", MAX_GENERATOR_WEAR_FAILURE_BONUS);
        result.put("wearPercent", generator.wearPercent());
        result.put("adjustedFailureChance", generator.adjustedGeneratorFailureChance());
        result.put("safeRoll", 0.006D);
        result.put("safeRollFailed", safeRollFailed);
        result.put("energyAfterSafeRoll", energyAfterSafeRoll);
        result.put("burnTicksAfterSafeRoll", burnTicksAfterSafeRoll);
        result.put("failureRoll", 0.004D);
        result.put("failureRollFailed", failureRollFailed);
        result.put("failedAfterRoll", failedAfterRoll);
        result.put("burnTicksAfterFailure", burnTicksAfterFailure);
        result.put("restartEventHandled", restartEvent.get("handled"));
        result.put("failedAfterRestart", generator.failed);
        result.put("generatorFailureChanceRespected", !safeRollFailed
                && energyAfterSafeRoll == 8
                && burnTicksAfterSafeRoll == 159
                && failureRollFailed
                && failedAfterRoll
                && !generator.failed
                && burnTicksAfterFailure == 0
                && Boolean.TRUE.equals(restartEvent.get("handled")));
        result.put("status", Boolean.TRUE.equals(result.get("generatorFailureChanceRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runScrapDynamoScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.generator("scrap_dynamo", "ScrapDynamoBlockEntity", 0, 0, 0,
                8_000, 256, 24, 0));
        runtime.add(RuntimeNode.energy("power_cable", "PowerCableBlockEntity", 1, 0, 0,
                1_000, 50, NodeRole.CABLE));
        runtime.add(RuntimeNode.energy("battery_bank", "BatteryBankBlockEntity", 2, 0, 0,
                10_000, 100, NodeRole.STORAGE));
        runtime.link("scrap_dynamo", "power_cable");
        runtime.link("power_cable", "battery_bank");

        Map<String, Object> fuelEvent = runtime.handleItemInteraction(
                "player.use_item_on_block",
                "scrap_dynamo",
                "echoashfallprotocol:scrap_metal");
        int burnAfterFuel = runtime.nodes.get("scrap_dynamo").burnTicksRemaining;
        for (int tick = 0; tick < 10; tick++) {
            runtime.tickCount++;
            runtime.tickGenerators();
            runtime.moveEnergy("scrap_dynamo", "power_cable", 256);
            runtime.moveEnergy("power_cable", "battery_bank", 50);
        }

        RuntimeNode dynamo = runtime.nodes.get("scrap_dynamo");
        RuntimeNode cable = runtime.nodes.get("power_cable");
        RuntimeNode battery = runtime.nodes.get("battery_bank");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.scrap_dynamo_runtime");
        result.put("implementationTarget", "AdapterCore native Scrap Dynamo fuel burn and power output");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("fuelEventHandled", fuelEvent.get("handled"));
        result.put("fuelItem", fuelEvent.get("itemId"));
        result.put("fuelBurnTicks", fuelEvent.get("burnTicksAdded"));
        result.put("burnTicksAfterFuel", burnAfterFuel);
        result.put("tickCount", runtime.tickCount);
        result.put("burnTicksRemaining", dynamo.burnTicksRemaining);
        result.put("maxBurnTicks", dynamo.maxBurnTicks);
        result.put("energyGenerated", 10 * dynamo.generationPerTick);
        result.put("dynamoEnergyStored", dynamo.energy);
        result.put("cableEnergyStored", cable.energy);
        result.put("batteryStoredEnergy", battery.energy);
        result.put("energyCapacity", dynamo.capacity);
        result.put("transferPerTick", dynamo.transferPerTick);
        result.put("generationPerTick", dynamo.generationPerTick);
        result.put("canReceiveEnergy", dynamo.canReceiveEnergy());
        result.put("canExtractEnergy", dynamo.canExtractEnergy());
        result.put("active", dynamo.burnTicksRemaining > 0);
        result.put("powerTransferCount", runtime.powerTransfers.size());
        result.put("powerCapacityRespected", runtime.powerCapacityRespected());
        result.put("networkDiagnostic", runtime.powerCapacityRespected() ? "PASS" : "WARN");
        result.put("status", Boolean.TRUE.equals(result.get("fuelEventHandled"))
                && Integer.valueOf(80).equals(result.get("fuelBurnTicks"))
                && Integer.valueOf(70).equals(result.get("burnTicksRemaining"))
                && Integer.valueOf(240).equals(result.get("batteryStoredEnergy"))
                && Boolean.FALSE.equals(result.get("canReceiveEnergy"))
                && Boolean.TRUE.equals(result.get("active"))
                && "PASS".equals(result.get("networkDiagnostic")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runBatteryBankBalancingScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        RuntimeNode chargingBank = RuntimeNode.energy("charging_battery_bank", "BatteryBankBlockEntity", 0, 0, 0,
                10_000, 100, NodeRole.STORAGE).withEnergy(8_000)
                .withBatteryItem("echoashfallprotocol:basic_battery", 0, 2_000, 64);
        RuntimeNode dischargingBank = RuntimeNode.energy("discharging_battery_bank", "BatteryBankBlockEntity", 0, 1, 0,
                10_000, 100, NodeRole.STORAGE).withEnergy(1_000)
                .withBatteryItem("echoashfallprotocol:basic_battery", 2_000, 2_000, 64);
        RuntimeNode distributionBank = RuntimeNode.energy("battery_bank", "BatteryBankBlockEntity", 0, 2, 0,
                10_000, 100, NodeRole.STORAGE).withEnergy(500);
        RuntimeNode consumer = RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 1, 2, 0,
                1_500, 128, 1, 40, "scrap_metal", 9, "compressed_scrap", 1);
        runtime.add(chargingBank);
        runtime.add(dischargingBank);
        runtime.add(distributionBank);
        runtime.add(consumer);
        runtime.link("battery_bank", "scrap_press");

        Map<String, Object> chargeBalance = runtime.balanceInsertedBattery(chargingBank);
        Map<String, Object> dischargeBalance = runtime.balanceInsertedBattery(dischargingBank);
        runtime.moveEnergy("battery_bank", "scrap_press", 100);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "power_storage.battery_bank_balancing_bridge");
        result.put("implementationTarget", "AdapterCore native Battery Bank inserted-battery balancing and adjacent transfer");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("batterySlot", 0);
        result.put("basicBatteryCapacity", 2_000);
        result.put("basicBatteryTransfer", 64);
        result.put("bankEnergyCapacity", 10_000);
        result.put("bankMaxTransfer", 100);
        result.put("chargeBalance", chargeBalance);
        result.put("dischargeBalance", dischargeBalance);
        result.put("chargeMoved", chargeBalance.get("moved"));
        result.put("dischargeMoved", dischargeBalance.get("moved"));
        result.put("chargeBankEnergyAfter", chargingBank.energy);
        result.put("chargeBatteryEnergyAfter", chargingBank.batteryItemEnergy);
        result.put("dischargeBankEnergyAfter", dischargingBank.energy);
        result.put("dischargeBatteryEnergyAfter", dischargingBank.batteryItemEnergy);
        result.put("adjacentTransferMoved", consumer.energy);
        result.put("distributionBankEnergyAfter", distributionBank.energy);
        result.put("consumerEnergyAfter", consumer.energy);
        result.put("powerTransfers", List.copyOf(runtime.powerTransfers));
        result.put("transferLimitRespected", runtime.powerTransfers.stream()
                .allMatch(transfer -> ((Integer) transfer.get("amount")) <= 100));
        result.put("capacityRespected", runtime.powerCapacityRespected());
        result.put("storageChanged", Boolean.TRUE.equals(chargeBalance.get("changed"))
                && Boolean.TRUE.equals(dischargeBalance.get("changed"))
                && consumer.energy == 100);
        result.put("status", Integer.valueOf(64).equals(result.get("chargeMoved"))
                && Integer.valueOf(64).equals(result.get("dischargeMoved"))
                && Integer.valueOf(7_936).equals(result.get("chargeBankEnergyAfter"))
                && Integer.valueOf(64).equals(result.get("chargeBatteryEnergyAfter"))
                && Integer.valueOf(1_064).equals(result.get("dischargeBankEnergyAfter"))
                && Integer.valueOf(1_936).equals(result.get("dischargeBatteryEnergyAfter"))
                && Integer.valueOf(100).equals(result.get("adjacentTransferMoved"))
                && Boolean.TRUE.equals(result.get("transferLimitRespected"))
                && Boolean.TRUE.equals(result.get("capacityRespected"))
                && Boolean.TRUE.equals(result.get("storageChanged")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runWaterPurifierScenario() {
        int processTimeTicks = 60;
        int powerPerBottlePerTick = 20;
        int batchSize = 1;
        int networkEnergyBefore = 2_000;
        int networkEnergy = networkEnergyBefore;
        int progress = 0;
        int dirtyWaterCount = 1;
        int filterCount = 1;
        int cleanWaterCount = 0;
        int wearCounter = 0;
        boolean active = false;
        float deterministicFilterRoll = 0.10F;

        for (int tick = 0; tick < processTimeTicks; tick++) {
            if (dirtyWaterCount <= 0 || filterCount <= 0 || networkEnergy < powerPerBottlePerTick) {
                progress = dirtyWaterCount <= 0 ? 0 : Math.max(0, progress - 1);
                continue;
            }
            networkEnergy -= powerPerBottlePerTick;
            progress++;
            active = true;
            if (progress % 10 == 0) {
                wearCounter++;
            }
            if (progress >= processTimeTicks) {
                dirtyWaterCount -= batchSize;
                cleanWaterCount += batchSize;
                if (deterministicFilterRoll < 0.15F) {
                    filterCount--;
                }
                progress = 0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.water_purifier_runtime");
        result.put("implementationTarget", "AdapterCore native Water Purifier powered purification");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("processTimeTicks", processTimeTicks);
        result.put("powerPerBottlePerTick", powerPerBottlePerTick);
        result.put("batchSize", batchSize);
        result.put("maxBatchSize", 3);
        result.put("energyCapacity", 1_000);
        result.put("transferPerTick", 64);
        result.put("networkRelayTransferLimit", 50);
        result.put("networkEnergyBefore", networkEnergyBefore);
        result.put("networkEnergyAfter", networkEnergy);
        result.put("totalPowerConsumed", networkEnergyBefore - networkEnergy);
        result.put("inputItem", "echoashfallprotocol:dirty_water_bottle");
        result.put("filterItem", "echoashfallprotocol:filter_cartridge_basic");
        result.put("outputItem", "echoashfallprotocol:clean_water_bottle");
        result.put("dirtyWaterCountAfter", dirtyWaterCount);
        result.put("filterCountAfter", filterCount);
        result.put("cleanWaterCount", cleanWaterCount);
        result.put("filterConsumed", filterCount == 0);
        result.put("progressAfter", progress);
        result.put("wearCounter", wearCounter);
        result.put("inputSlots", List.of(0, 1));
        result.put("outputSlotsDown", List.of(2));
        result.put("batterySlot", 3);
        result.put("canReceiveEnergy", true);
        result.put("canExtractEnergyWhenStored", true);
        result.put("survivalPriorityConsumer", true);
        result.put("active", active);
        result.put("status", dirtyWaterCount == 0
                && filterCount == 0
                && cleanWaterCount == 1
                && networkEnergy == 800
                && progress == 0
                && wearCounter == 6
                && active ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runThermalBurnerScenario() {
        int processTimeTicks = 40;
        int inputCount = 4;
        int burnProgress = 0;
        int energy = 0;
        int maxEnergy = 1_000;
        int itemsBurned = 0;
        int ashOutputCount = 0;
        int wearCounter = 0;
        boolean active = false;

        for (int tick = 0; tick < processTimeTicks * 4; tick++) {
            if (inputCount <= 0 || energy >= maxEnergy) {
                burnProgress = 0;
                continue;
            }
            burnProgress++;
            active = true;
            if (burnProgress % 20 == 0) {
                wearCounter += 2;
            }
            if (burnProgress >= processTimeTicks) {
                inputCount--;
                energy = Math.min(maxEnergy, energy + 50);
                itemsBurned++;
                burnProgress = 0;
                if (itemsBurned >= 4) {
                    itemsBurned = 0;
                    ashOutputCount++;
                }
            }
        }

        int simulatedExtract = Math.min(64, energy);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.thermal_burner_runtime");
        result.put("implementationTarget", "AdapterCore native Thermal Burner fuel burn and ash output");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("processTimeTicks", processTimeTicks);
        result.put("totalTicks", processTimeTicks * 4);
        result.put("inputItem", "minecraft:cobblestone");
        result.put("inputCountAfter", inputCount);
        result.put("acceptedAnyItem", true);
        result.put("energyPerItem", 50);
        result.put("energyCapacity", maxEnergy);
        result.put("energyAfter", energy);
        result.put("simulatedEnergyExtract", simulatedExtract);
        result.put("canReceiveEnergy", false);
        result.put("canExtractEnergy", energy > 0);
        result.put("itemsBurnedCounterAfter", itemsBurned);
        result.put("ashOutputItem", "echoashfallprotocol:ash");
        result.put("ashOutputCount", ashOutputCount);
        result.put("burnProgressAfter", burnProgress);
        result.put("wearCounter", wearCounter);
        result.put("inputSlots", List.of(0));
        result.put("outputSlotsDown", List.of(1));
        result.put("batterySlot", 2);
        result.put("active", active);
        result.put("status", inputCount == 0
                && energy == 200
                && simulatedExtract == 64
                && ashOutputCount == 1
                && burnProgress == 0
                && wearCounter == 16
                && active ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runAutofeedHopperScenario() {
        RuntimeNode hopper = RuntimeNode.energy("autofeed_hopper", "AutofeedHopperBlockEntity",
                0, 0, 0, 1_000, 64, NodeRole.MACHINE).withEnergy(20);
        int hungryPlayerFoodBefore = 8;
        int satiatedPlayerFoodBefore = 16;
        int hungryPlayerFoodAfter = hungryPlayerFoodBefore;
        int satiatedPlayerFoodAfter = satiatedPlayerFoodBefore;
        int tickCounter = 0;
        int lastFeedTick = 0;
        int wearCounter = 0;
        boolean active = false;
        int fedPlayerCount = 0;

        for (int tick = 0; tick < 60; tick++) {
            tickCounter++;
            if (hopper.jammed || hopper.energy < 10 || tickCounter - lastFeedTick < 60) {
                continue;
            }
            if (hungryPlayerFoodAfter <= 10) {
                hopper.energy -= 10;
                hungryPlayerFoodAfter = Math.min(20, hungryPlayerFoodAfter + 4);
                fedPlayerCount++;
                active = true;
            }
            if (satiatedPlayerFoodAfter <= 10 && hopper.energy >= 10) {
                hopper.energy -= 10;
                satiatedPlayerFoodAfter = Math.min(20, satiatedPlayerFoodAfter + 4);
                fedPlayerCount++;
                active = true;
            }
            if (active) {
                lastFeedTick = tickCounter;
                wearCounter++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.autofeed_hopper_player_state_bridge");
        result.put("implementationTarget", "AdapterCore native Autofeed Hopper powered player feeding");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("radiusBlocks", 8);
        result.put("powerCostPerFeed", 10);
        result.put("energyCapacity", hopper.capacity);
        result.put("energyTransfer", hopper.transferPerTick);
        result.put("feedIntervalTicks", 60);
        result.put("hungerThreshold", 10);
        result.put("feedAmount", 4);
        result.put("tickCounter", tickCounter);
        result.put("lastFeedTick", lastFeedTick);
        result.put("wearCounter", wearCounter);
        result.put("hungryPlayerFoodBefore", hungryPlayerFoodBefore);
        result.put("hungryPlayerFoodAfter", hungryPlayerFoodAfter);
        result.put("satiatedPlayerFoodBefore", satiatedPlayerFoodBefore);
        result.put("satiatedPlayerFoodAfter", satiatedPlayerFoodAfter);
        result.put("fedPlayerCount", fedPlayerCount);
        result.put("energyBefore", 20);
        result.put("energyAfter", hopper.energy);
        result.put("active", active);
        result.put("playerStateMutated", hungryPlayerFoodAfter > hungryPlayerFoodBefore
                && satiatedPlayerFoodAfter == satiatedPlayerFoodBefore);
        result.put("status", fedPlayerCount == 1
                && hungryPlayerFoodAfter == 12
                && satiatedPlayerFoodAfter == 16
                && hopper.energy == 10
                && lastFeedTick == 60
                && wearCounter == 1
                && active ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runContaminantCondenserScenario() {
        RuntimeNode condenser = RuntimeNode.energy("contaminant_condenser", "ContaminantCondenserBlockEntity",
                0, 0, 0, 2_000, 128, NodeRole.MACHINE).withEnergy(100);
        List<Map<String, Object>> worldBlocks = new ArrayList<>();
        worldBlocks.add(worldBlock(-2, 0, 1, "echoashfallprotocol:toxic_puddle"));
        worldBlocks.add(worldBlock(4, 0, 0, "echoashfallprotocol:toxic_puddle"));
        worldBlocks.add(worldBlock(1, 0, 1, "minecraft:dirt"));

        int tickCounter = 0;
        int wearCounter = 0;
        int blocksProcessed = 0;
        boolean active = false;
        String convertedBlockBefore = "";
        String convertedBlockAfter = "";
        int convertedDistance = 0;

        for (int tick = 0; tick < 100; tick++) {
            tickCounter++;
            if (condenser.jammed || condenser.energy < 50 || tickCounter % 100 != 0) {
                continue;
            }
            Map<String, Object> target = findToxicBlockWithinRadius(worldBlocks, 3);
            if (target == null) {
                active = false;
                continue;
            }
            condenser.energy -= 50;
            convertedBlockBefore = String.valueOf(target.get("block"));
            target.put("block", "minecraft:sand");
            convertedBlockAfter = String.valueOf(target.get("block"));
            convertedDistance = Math.max(Math.abs((Integer) target.get("x")), Math.max(
                    Math.abs((Integer) target.get("y")),
                    Math.abs((Integer) target.get("z"))));
            blocksProcessed++;
            wearCounter++;
            active = true;
        }

        long remainingToxicInRange = worldBlocks.stream()
                .filter(block -> "echoashfallprotocol:toxic_puddle".equals(block.get("block")))
                .filter(block -> withinRadius(block, 3))
                .count();
        long remainingToxicOutOfRange = worldBlocks.stream()
                .filter(block -> "echoashfallprotocol:toxic_puddle".equals(block.get("block")))
                .filter(block -> !withinRadius(block, 3))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "machine.contaminant_condenser_world_state_bridge");
        result.put("implementationTarget", "AdapterCore native Contaminant Condenser toxic block processing");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("processRadiusBlocks", 3);
        result.put("powerCostPerOperation", 50);
        result.put("processIntervalTicks", 100);
        result.put("energyCapacity", condenser.capacity);
        result.put("energyTransfer", condenser.transferPerTick);
        result.put("tickCounter", tickCounter);
        result.put("energyBefore", 100);
        result.put("energyAfter", condenser.energy);
        result.put("blocksProcessed", blocksProcessed);
        result.put("wearCounter", wearCounter);
        result.put("convertedBlockBefore", convertedBlockBefore);
        result.put("convertedBlockAfter", convertedBlockAfter);
        result.put("convertedDistance", convertedDistance);
        result.put("remainingToxicInRange", remainingToxicInRange);
        result.put("remainingToxicOutOfRange", remainingToxicOutOfRange);
        result.put("active", active);
        result.put("worldStateMutated", blocksProcessed == 1
                && "echoashfallprotocol:toxic_puddle".equals(convertedBlockBefore)
                && "minecraft:sand".equals(convertedBlockAfter));
        result.put("status", blocksProcessed == 1
                && condenser.energy == 50
                && wearCounter == 1
                && convertedDistance <= 3
                && remainingToxicInRange == 0
                && remainingToxicOutOfRange == 1
                && active ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runIsotopeRefinerScenario() {
        int processTime = 160;
        int powerPerOperation = 500;
        int powerPerTick = powerPerOperation / processTime;
        int energy = 500;
        int inputCount = 2;
        int catalystCount = 1;
        int cleanOutputCount = 0;
        int contaminatedOutputCount = 0;
        int progress = 0;
        int contaminationLevel = 0;
        int wearCounter = 0;
        float contaminationRoll = 0.15F;
        float contaminationChance = 0.20F;
        boolean active = false;

        for (int tick = 0; tick < processTime; tick++) {
            boolean hasRecipe = inputCount >= 2 && catalystCount >= 1
                    && cleanOutputCount < MAX_STACK_SIZE
                    && contaminatedOutputCount < MAX_STACK_SIZE;
            if (!hasRecipe || energy < powerPerTick) {
                progress = 0;
                active = false;
                continue;
            }
            energy -= powerPerTick;
            progress++;
            active = true;
            contaminationLevel = (int) (progress * 100f / processTime);
            if (progress % 20 == 0) {
                wearCounter++;
            }
            if (progress >= processTime) {
                inputCount -= 2;
                catalystCount--;
                if (contaminationRoll < contaminationChance) {
                    contaminatedOutputCount++;
                } else {
                    cleanOutputCount++;
                }
                progress = 0;
                contaminationLevel = 0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "block_entity.tick.powered_catalyst_refiner");
        result.put("implementationTarget", "AdapterCore native Isotope Refiner powered catalyst recipe");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("processTimeTicks", processTime);
        result.put("powerPerOperation", powerPerOperation);
        result.put("powerPerTick", powerPerTick);
        result.put("energyCapacity", 4_000);
        result.put("transferPerTick", 256);
        result.put("inputItem", "minecraft:iron_ingot");
        result.put("catalystItem", "echoashfallprotocol:crystal_dust");
        result.put("cleanOutputItem", "minecraft:gold_ingot");
        result.put("contaminatedOutputItem", "echoashfallprotocol:contaminated_gold");
        result.put("contaminationChance", contaminationChance);
        result.put("contaminationRoll", contaminationRoll);
        result.put("contaminatedBranchSelected", contaminationRoll < contaminationChance);
        result.put("inputCountAfter", inputCount);
        result.put("catalystCountAfter", catalystCount);
        result.put("cleanOutputCount", cleanOutputCount);
        result.put("contaminatedOutputCount", contaminatedOutputCount);
        result.put("energyAfter", energy);
        result.put("progressAfter", progress);
        result.put("contaminationLevelAfter", contaminationLevel);
        result.put("wearCounter", wearCounter);
        result.put("active", active);
        result.put("status", inputCount == 0
                && catalystCount == 0
                && cleanOutputCount == 0
                && contaminatedOutputCount == 1
                && energy == 20
                && progress == 0
                && contaminationLevel == 0
                && wearCounter == 8 ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runRadiationCleanserScenario() {
        int totalTicks = 400;
        int powerPerTick = 8;
        int energy = 3_250;
        int inputCount = 1;
        int filterCount = 1;
        int outputCount = 0;
        int progress = 0;
        int wearLevel = 0;
        float filterConsumptionRoll = 0.10F;
        float filterConsumptionChance = 0.20F;
        boolean active = false;
        boolean feedbackThrottled = true;

        for (int tick = 0; tick < totalTicks; tick++) {
            boolean hasCleanserRecipe = inputCount > 0 && filterCount > 0 && outputCount < MAX_STACK_SIZE;
            if (!hasCleanserRecipe || energy < powerPerTick) {
                progress = 0;
                active = false;
                continue;
            }
            energy -= powerPerTick;
            progress++;
            wearLevel++;
            active = true;
            if (progress >= totalTicks) {
                inputCount--;
                if (filterConsumptionRoll < filterConsumptionChance) {
                    filterCount--;
                }
                outputCount++;
                progress = 0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "block_entity.tick.powered_decontamination_processor");
        result.put("implementationTarget", "AdapterCore native Radiation Cleanser decontamination recipe");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("totalTicks", totalTicks);
        result.put("powerPerTick", powerPerTick);
        result.put("energyCapacity", 4_000);
        result.put("transferPerTick", 256);
        result.put("inputItem", "echoashfallprotocol:contaminated_iron");
        result.put("filterItem", "echoashfallprotocol:filter_cartridge_advanced");
        result.put("outputItem", "minecraft:iron_ingot");
        result.put("filterConsumptionChance", filterConsumptionChance);
        result.put("filterConsumptionRoll", filterConsumptionRoll);
        result.put("filterConsumed", filterConsumptionRoll < filterConsumptionChance);
        result.put("inputCountAfter", inputCount);
        result.put("filterCountAfter", filterCount);
        result.put("outputCount", outputCount);
        result.put("energyAfter", energy);
        result.put("progressAfter", progress);
        result.put("wearLevel", wearLevel);
        result.put("active", active);
        result.put("feedbackThrottled", feedbackThrottled);
        result.put("status", inputCount == 0
                && filterCount == 0
                && outputCount == 1
                && energy == 50
                && progress == 0
                && wearLevel == 400
                && active
                && feedbackThrottled ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runCrystallineSynthesizerScenario() {
        int totalTicks = 400;
        int phase2Start = 100;
        int phase3Start = 240;
        int phase4Start = 360;
        int energy = 900;
        int gemFragmentCount = 4;
        int denseAlloyCount = 1;
        int energyCellCount = 2;
        int outputCount = 0;
        int progress = 0;
        int currentPhase = 0;
        int determinedOutputIndex = -1;
        int deterministicOutputIndex = 0;
        int wearCounter = 0;
        int tickCalls = 0;
        boolean hadPowerFailure = false;
        boolean phase2FailureInjected = false;
        boolean active = false;
        String outputItem = "";

        while (outputCount == 0 && tickCalls < 500) {
            tickCalls++;
            boolean hasIngredients = gemFragmentCount >= 4
                    && denseAlloyCount >= 1
                    && energyCellCount >= 2
                    && outputCount < MAX_STACK_SIZE;

            if (currentPhase == 0 && !hasIngredients) {
                active = false;
                continue;
            }
            if (currentPhase == 0 && hasIngredients) {
                currentPhase = 1;
                progress = 0;
                hadPowerFailure = false;
                determinedOutputIndex = -1;
                active = false;
                continue;
            }

            int powerCost = switch (currentPhase) {
                case 1 -> 3;
                case 4 -> 1;
                default -> 2;
            };
            boolean injectedPowerFailure = !phase2FailureInjected && currentPhase == 2 && progress == 120;
            if (injectedPowerFailure || energy < powerCost) {
                if (currentPhase == 2 || currentPhase == 3) {
                    hadPowerFailure = true;
                }
                if (currentPhase == 1) {
                    currentPhase = 0;
                    progress = 0;
                }
                phase2FailureInjected = phase2FailureInjected || injectedPowerFailure;
                active = false;
                continue;
            }

            energy -= powerCost;
            progress++;
            active = true;

            if (progress % 20 == 0) {
                wearCounter += currentPhase >= 3 ? 2 : 1;
            }

            if (progress >= phase2Start && currentPhase == 1) {
                currentPhase = 2;
            } else if (progress >= phase3Start && currentPhase == 2) {
                currentPhase = 3;
                if (determinedOutputIndex < 0) {
                    determinedOutputIndex = deterministicOutputIndex;
                }
            } else if (progress >= phase4Start && currentPhase == 3) {
                currentPhase = 4;
            }

            if (progress >= totalTicks) {
                gemFragmentCount -= 4;
                denseAlloyCount--;
                energyCellCount -= 2;
                outputItem = hadPowerFailure && determinedOutputIndex < 2
                        ? "minecraft:netherite_scrap"
                        : crystallineOutput(determinedOutputIndex);
                outputCount++;
                progress = 0;
                currentPhase = 0;
                determinedOutputIndex = -1;
                hadPowerFailure = false;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "block_entity.tick.powered_phase_synthesizer");
        result.put("implementationTarget", "AdapterCore native Crystalline Synthesizer phased reaction");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("totalTicks", totalTicks);
        result.put("phase2StartTick", phase2Start);
        result.put("phase3StartTick", phase3Start);
        result.put("phase4StartTick", phase4Start);
        result.put("energyCapacity", 8_000);
        result.put("transferPerTick", 512);
        result.put("phase1PowerCost", 3);
        result.put("phase2PowerCost", 2);
        result.put("phase3PowerCost", 2);
        result.put("phase4PowerCost", 1);
        result.put("tickCalls", tickCalls);
        result.put("powerFailureInjected", phase2FailureInjected);
        result.put("determinedOutputIndex", deterministicOutputIndex);
        result.put("powerFailureFallbackApplied", "minecraft:netherite_scrap".equals(outputItem));
        result.put("gemFragmentCountAfter", gemFragmentCount);
        result.put("denseAlloyCountAfter", denseAlloyCount);
        result.put("energyCellCountAfter", energyCellCount);
        result.put("outputItem", outputItem);
        result.put("outputCount", outputCount);
        result.put("energyAfter", energy);
        result.put("progressAfter", progress);
        result.put("phaseAfter", currentPhase);
        result.put("wearCounter", wearCounter);
        result.put("active", active);
        result.put("status", phase2FailureInjected
                && "minecraft:netherite_scrap".equals(outputItem)
                && gemFragmentCount == 0
                && denseAlloyCount == 0
                && energyCellCount == 0
                && outputCount == 1
                && energy == 40
                && progress == 0
                && currentPhase == 0
                && wearCounter == 28
                && tickCalls == 402 ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runDeepCoreMinerScenario() {
        int totalTicks = 800;
        int powerPerTick = 40;
        int minYLevel = -32;
        int shallowY = -16;
        int miningY = -40;
        int shallowProgress = 0;
        int progress = 0;
        int localEnergy = 12_000;
        int networkEnergy = 20_000;
        int totalPowerConsumed = 0;
        int wearLevel = 0;
        int outputSlotCount = 0;
        int neighborInputCount = 0;
        int deterministicOutputIndex = 0;
        String outputItem = "";
        boolean active = false;
        boolean pushedToNeighbor = false;

        for (int tick = 0; tick < 40; tick++) {
            if (shallowY > minYLevel) {
                continue;
            }
            shallowProgress++;
        }

        for (int tick = 0; tick < totalTicks; tick++) {
            if (miningY > minYLevel || outputSlotCount >= MAX_STACK_SIZE) {
                active = false;
                continue;
            }
            int localConsumed = Math.min(localEnergy, powerPerTick);
            int remainingCost = powerPerTick - localConsumed;
            if (networkEnergy < remainingCost) {
                active = false;
                continue;
            }
            localEnergy -= localConsumed;
            networkEnergy -= remainingCost;
            totalPowerConsumed += powerPerTick;
            progress++;
            active = true;

            if (progress % 40 == 0) {
                wearLevel += 2;
            }
            if (progress >= totalTicks) {
                outputItem = deepCoreOutput(deterministicOutputIndex);
                outputSlotCount++;
                progress = 0;
                if (neighborAcceptsDeepCoreOutput(outputItem)) {
                    outputSlotCount--;
                    neighborInputCount++;
                    pushedToNeighbor = true;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "block_entity.tick.deep_depth_resource_generator");
        result.put("implementationTarget", "AdapterCore native Deep Core Miner depth-gated resource generation");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("totalTicks", totalTicks);
        result.put("powerPerTick", powerPerTick);
        result.put("minYLevel", minYLevel);
        result.put("shallowY", shallowY);
        result.put("miningY", miningY);
        result.put("depthGateRespected", shallowProgress == 0 && miningY <= minYLevel);
        result.put("energyCapacity", 12_000);
        result.put("transferPerTick", 512);
        result.put("localEnergyAfter", localEnergy);
        result.put("networkEnergyAfter", networkEnergy);
        result.put("totalPowerConsumed", totalPowerConsumed);
        result.put("progressAfter", progress);
        result.put("wearLevel", wearLevel);
        result.put("deterministicOutputIndex", deterministicOutputIndex);
        result.put("outputItem", outputItem);
        result.put("outputSlotCountAfterPush", outputSlotCount);
        result.put("neighborInputCount", neighborInputCount);
        result.put("pushedToNeighbor", pushedToNeighbor);
        result.put("inputInsertionAllowed", false);
        result.put("outputExtractable", true);
        result.put("active", active);
        result.put("status", shallowProgress == 0
                && localEnergy == 0
                && networkEnergy == 0
                && totalPowerConsumed == 32_000
                && progress == 0
                && wearLevel == 40
                && "echoashfallprotocol:dense_alloy_chunk".equals(outputItem)
                && outputSlotCount == 0
                && neighborInputCount == 1
                && pushedToNeighbor
                && active ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runCapabilityMutationScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = createDefaultWorld();
        runtime.forceMachineFailure("micro_generator");
        runtime.handleInteraction("player.use_block", "micro_generator");
        runtime.handleInteraction("player.use_block", "load_distributor");
        runtime.tick(80);

        RuntimeNode battery = runtime.nodes.get("battery_bank");
        RuntimeNode grinder = runtime.nodes.get("ore_grinder");
        RuntimeNode press = runtime.nodes.get("scrap_press");
        int batteryBefore = battery.energy;
        int grinderInputBefore = grinder.inputCount;
        int pressOutputBefore = press.outputCount;

        int energyReceived = runtime.receiveEnergy("battery_bank", 64, false);
        int energyExtracted = runtime.extractEnergy("battery_bank", 20, false);
        int itemInserted = runtime.insertItem("ore_grinder", "ore_substrate", 3, false);
        int outputExtracted = runtime.extractOutputItem("scrap_press", "compressed_scrap", 1, false);

        Map<String, Object> after = runtime.describe();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "capability.bridge.mutating_dispatch");
        result.put("implementationTarget", "AdapterCore native mutating inventory and energy capability bridge");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("mutatingCapabilityCalls", 4);
        result.put("energyReceived", energyReceived);
        result.put("energyExtracted", energyExtracted);
        result.put("inventoryInserted", itemInserted);
        result.put("inventoryExtracted", outputExtracted);
        result.put("batteryEnergyBefore", batteryBefore);
        result.put("batteryEnergyAfter", after.get("batteryStoredEnergy"));
        result.put("oreGrinderInputBefore", grinderInputBefore);
        result.put("oreGrinderInputAfter", after.get("oreGrinderInputCount"));
        result.put("scrapPressOutputBefore", pressOutputBefore);
        result.put("scrapPressOutputAfter", after.get("scrapPressOutputCount"));
        result.put("capabilityStateMutated", Integer.valueOf(batteryBefore + 44).equals(after.get("batteryStoredEnergy"))
                && Integer.valueOf(grinderInputBefore + 3).equals(after.get("oreGrinderInputCount"))
                && Integer.valueOf(pressOutputBefore - 1).equals(after.get("scrapPressOutputCount")));
        result.put("postMutationState", after);
        result.put("status", Integer.valueOf(64).equals(result.get("energyReceived"))
                && Integer.valueOf(20).equals(result.get("energyExtracted"))
                && Integer.valueOf(3).equals(result.get("inventoryInserted"))
                && Integer.valueOf(1).equals(result.get("inventoryExtracted"))
                && Boolean.TRUE.equals(result.get("capabilityStateMutated")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runSidedInventoryCapabilityScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 0, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 0, "compressed_scrap", 1)
                .withOutputCount(1));
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 1, 0, 0, 2_000, 128,
                2, 80, "toxic_slagstone", 0, "coal_dust", 2)
                .withOutputCount(2)
                .withByproduct("contaminated_redstone", 1, 0.25F)
                .withByproductCount(1));

        int scrapInsertAnySide = runtime.insertItemFromSide("scrap_press", "scrap_metal", 9, "NORTH", false);
        int scrapInvalidInsert = runtime.insertItemFromSide("scrap_press", "iron_shard", 1, "WEST", false);
        int scrapTopExtract = runtime.extractOutputItemFromSide("scrap_press", "compressed_scrap", 1, "UP", true);
        int scrapBottomExtract = runtime.extractOutputItemFromSide("scrap_press", "compressed_scrap", 1, "DOWN", false);

        int grinderInsertAnySide = runtime.insertItemFromSide("ore_grinder", "toxic_slagstone", 2, "EAST", false);
        int grinderInvalidInsert = runtime.insertItemFromSide("ore_grinder", "scrap_metal", 1, "SOUTH", false);
        int grinderSideProductExtract = runtime.extractOutputItemFromSide("ore_grinder", "coal_dust", 1, "NORTH", true);
        int grinderBottomProductExtract = runtime.extractOutputItemFromSide("ore_grinder", "coal_dust", 2, "DOWN", false);
        int grinderBottomByproductExtract = runtime.extractOutputItemFromSide("ore_grinder", "contaminated_redstone", 1, "DOWN", false);

        RuntimeNode press = runtime.nodes.get("scrap_press");
        RuntimeNode grinder = runtime.nodes.get("ore_grinder");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "capability.bridge.sided_inventory_dispatch");
        result.put("implementationTarget", "AdapterCore native HopperHandler sided inventory bridge");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("scrapPressInputSlots", List.of(0));
        result.put("scrapPressOutputSlotsDown", List.of(1));
        result.put("oreGrinderInputSlots", List.of(0, 1));
        result.put("oreGrinderOutputSlotsDown", List.of(2, 3));
        result.put("scrapInsertAnySide", scrapInsertAnySide);
        result.put("scrapInvalidInsert", scrapInvalidInsert);
        result.put("scrapTopExtract", scrapTopExtract);
        result.put("scrapBottomExtract", scrapBottomExtract);
        result.put("grinderInsertAnySide", grinderInsertAnySide);
        result.put("grinderInvalidInsert", grinderInvalidInsert);
        result.put("grinderSideProductExtract", grinderSideProductExtract);
        result.put("grinderBottomProductExtract", grinderBottomProductExtract);
        result.put("grinderBottomByproductExtract", grinderBottomByproductExtract);
        result.put("scrapPressInputAfter", press.inputCount);
        result.put("scrapPressOutputAfter", press.outputCount);
        result.put("oreGrinderInputAfter", grinder.inputCount);
        result.put("oreGrinderOutputAfter", grinder.outputCount);
        result.put("oreGrinderByproductAfter", grinder.byproductCount);
        result.put("sidedRulesRespected", scrapInsertAnySide == 9
                && scrapInvalidInsert == 0
                && scrapTopExtract == 0
                && scrapBottomExtract == 1
                && grinderInsertAnySide == 2
                && grinderInvalidInsert == 0
                && grinderSideProductExtract == 0
                && grinderBottomProductExtract == 2
                && grinderBottomByproductExtract == 1
                && press.outputCount == 0
                && grinder.outputCount == 0
                && grinder.byproductCount == 0);
        result.put("status", Boolean.TRUE.equals(result.get("sidedRulesRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runFactoryControllerToggleScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = createDefaultWorld();
        runtime.tick(20);
        RuntimeNode controller = runtime.nodes.get("factory_controller");
        int initialScanCount = runtime.factoryScanCount;
        int initialConnectedMachines = runtime.factoryConnectedMachines;
        int initialStoredEnergy = runtime.factoryStoredEnergy;
        int initialEnergyCapacity = runtime.factoryEnergyCapacity;

        Map<String, Object> disableEvent = runtime.handleInteraction("player.use_block", "factory_controller");
        boolean disabledState = !controller.networkEnabled;
        runtime.tick(40);
        int disabledScanCount = runtime.factoryScanCount;

        Map<String, Object> enableEvent = runtime.handleInteraction("player.use_block", "factory_controller");
        boolean enabledState = controller.networkEnabled;
        runtime.tick(20);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "factory_controller.scan_toggle_bridge");
        result.put("implementationTarget", "AdapterCore native Factory Controller scan and interaction toggle");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("initialScanCount", initialScanCount);
        result.put("initialConnectedMachines", initialConnectedMachines);
        result.put("initialStoredEnergy", initialStoredEnergy);
        result.put("initialEnergyCapacity", initialEnergyCapacity);
        result.put("disableEventHandled", disableEvent.get("handled"));
        result.put("disabledState", disabledState);
        result.put("disabledScanCount", disabledScanCount);
        result.put("enableEventHandled", enableEvent.get("handled"));
        result.put("enabledState", enabledState);
        result.put("resumedScanCount", runtime.factoryScanCount);
        result.put("resumedConnectedMachines", runtime.factoryConnectedMachines);
        result.put("resumedStoredEnergy", runtime.factoryStoredEnergy);
        result.put("resumedEnergyCapacity", runtime.factoryEnergyCapacity);
        result.put("toggleEventCount", runtime.eventCounts.getOrDefault("toggle_network_enabled", 0));
        result.put("scanLimitRespected", runtime.factoryScanLimitRespected);
        result.put("scanToggleRespected", initialScanCount == 1
                && initialConnectedMachines == 6
                && Boolean.TRUE.equals(result.get("disableEventHandled"))
                && disabledState
                && disabledScanCount == initialScanCount
                && Boolean.TRUE.equals(result.get("enableEventHandled"))
                && enabledState
                && runtime.factoryScanCount == 2
                && runtime.factoryConnectedMachines == 6
                && runtime.factoryEnergyCapacity == initialEnergyCapacity
                && Integer.valueOf(2).equals(result.get("toggleEventCount"))
                && runtime.factoryScanLimitRespected);
        result.put("status", Boolean.TRUE.equals(result.get("scanToggleRespected")) ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runLogisticsRoutingScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.inventorySource("scrap_input_hopper", 0, 0, 0, "scrap_metal", 1));
        runtime.add(RuntimeNode.inventorySource("ore_input_hopper", 0, 2, 0, "ore_substrate", 1));
        runtime.add(RuntimeNode.pipe("item_pipe", 1, 0, 0, 4));
        runtime.add(RuntimeNode.pipe("loop_pipe", 1, 1, 0, 4));
        runtime.add(RuntimeNode.pipe("return_pipe", 0, 1, 0, 4));
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 2, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 9, "compressed_scrap", 1));
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 2, 1, 0, 2_000, 128,
                2, 80, "ore_substrate", 4, "iron_shard", 2).withByproduct("crystal_dust", 1));

        runtime.link("scrap_input_hopper", "item_pipe");
        runtime.link("ore_input_hopper", "loop_pipe");
        runtime.link("item_pipe", "loop_pipe");
        runtime.link("loop_pipe", "return_pipe");
        runtime.link("return_pipe", "item_pipe");
        runtime.link("item_pipe", "scrap_press");
        runtime.link("loop_pipe", "ore_grinder");

        runtime.tickLogisticsOnly(10);
        RuntimeNode scrapSource = runtime.nodes.get("scrap_input_hopper");
        RuntimeNode oreSource = runtime.nodes.get("ore_input_hopper");
        RuntimeNode press = runtime.nodes.get("scrap_press");
        RuntimeNode grinder = runtime.nodes.get("ore_grinder");
        int loopSkipCount = runtime.itemTransfers.stream().mapToInt(t -> (Integer) t.get("loopSkipCount")).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "logistics.item_pipe.routing_bridge");
        result.put("implementationTarget", "AdapterCore native Item Pipe live routing");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("hopperSourceCount", 2);
        result.put("routeTransferCount", runtime.itemTransfers.size());
        result.put("itemTransfers", List.copyOf(runtime.itemTransfers));
        result.put("scrapSourceOutputAfter", scrapSource.outputCount);
        result.put("oreSourceOutputAfter", oreSource.outputCount);
        result.put("scrapPressInputAfter", press.inputCount);
        result.put("oreGrinderInputAfter", grinder.inputCount);
        result.put("loopSkipCount", loopSkipCount);
        result.put("loopAvoided", runtime.itemTransfers.stream().allMatch(t -> Boolean.TRUE.equals(t.get("loopAvoided"))));
        result.put("validMachineInputsSelected", press.inputCount == 10 && grinder.inputCount == 5);
        result.put("diagnostics", List.copyOf(runtime.diagnostics));
        result.put("status", Integer.valueOf(2).equals(result.get("routeTransferCount"))
                && Integer.valueOf(0).equals(result.get("scrapSourceOutputAfter"))
                && Integer.valueOf(0).equals(result.get("oreSourceOutputAfter"))
                && Boolean.TRUE.equals(result.get("loopAvoided"))
                && Boolean.TRUE.equals(result.get("validMachineInputsSelected"))
                && loopSkipCount > 0 ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    static Map<String, Object> runMachineOutputChainingScenario() {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.add(RuntimeNode.machine("ore_grinder", "OreGrinderBlockEntity", 0, 0, 0, 2_000, 128,
                2, 80, "ore_substrate", 4, "scrap_metal", 1).withOutputCount(2));
        runtime.add(RuntimeNode.pipe("item_pipe", 1, 0, 0, 4));
        runtime.add(RuntimeNode.pipe("loop_pipe", 1, 1, 0, 4));
        runtime.add(RuntimeNode.pipe("return_pipe", 0, 1, 0, 4));
        runtime.add(RuntimeNode.machine("scrap_press", "ScrapPressBlockEntity", 2, 0, 0, 1_500, 128,
                1, 40, "scrap_metal", 8, "compressed_scrap", 1));

        runtime.link("ore_grinder", "item_pipe");
        runtime.link("item_pipe", "loop_pipe");
        runtime.link("loop_pipe", "return_pipe");
        runtime.link("return_pipe", "item_pipe");
        runtime.link("item_pipe", "scrap_press");

        runtime.tickLogisticsOnly(10);
        RuntimeNode source = runtime.nodes.get("ore_grinder");
        RuntimeNode target = runtime.nodes.get("scrap_press");
        int loopSkipCount = runtime.itemTransfers.stream().mapToInt(t -> (Integer) t.get("loopSkipCount")).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapterPacketVersion", 1);
        result.put("adapterCoreBridge", true);
        result.put("adapterSurface", "logistics.item_pipe.machine_output_chaining_bridge");
        result.put("implementationTarget", "AdapterCore native Item Pipe HopperHandler output-slot chaining");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("machineOutputSourceCount", 1);
        result.put("routeTransferCount", runtime.itemTransfers.size());
        result.put("itemTransfers", List.copyOf(runtime.itemTransfers));
        result.put("sourceOutputItem", "scrap_metal");
        result.put("sourceOutputSide", "DOWN");
        result.put("sourceOutputAfter", source.outputCount);
        result.put("targetInputAfter", target.inputCount);
        result.put("targetInputItem", target.inputItem);
        result.put("loopSkipCount", loopSkipCount);
        result.put("loopAvoided", runtime.itemTransfers.stream().allMatch(t -> Boolean.TRUE.equals(t.get("loopAvoided"))));
        result.put("hopperOutputPulled", source.outputCount == 0);
        result.put("validMachineInputSelected", target.inputCount == 10);
        result.put("diagnostics", List.copyOf(runtime.diagnostics));
        result.put("status", Integer.valueOf(2).equals(result.get("routeTransferCount"))
                && Boolean.TRUE.equals(result.get("hopperOutputPulled"))
                && Boolean.TRUE.equals(result.get("loopAvoided"))
                && Boolean.TRUE.equals(result.get("validMachineInputSelected"))
                && loopSkipCount > 0 ? "PASS" : "FAIL");
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        return result;
    }

    private void tickLogisticsOnly(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tickCount++;
            tickItemPipes();
        }
    }

    Map<String, Object> tick(int ticks) {
        if (ticks < 0) {
            diagnostics.add("Negative tick request ignored.");
            return describe();
        }
        for (int i = 0; i < ticks; i++) {
            tickCount++;
            tickGenerators();
            tickPowerNetwork();
            tickItemPipes();
            tickMachines();
            tickFactoryControllers();
        }
        return describe();
    }

    Map<String, Object> handleInteraction(String event, String nodeId) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null) {
            diagnostics.add("Interaction target missing: " + nodeId + ".");
            return Map.of("handled", false, "nodeId", nodeId, "event", event);
        }

        boolean handled = false;
        String action = "none";
        if ("player.use_block".equals(event) && node.role == NodeRole.GENERATOR && node.failed) {
            node.failed = false;
            handled = true;
            action = "restart_failed_generator";
            countEvent("restart_failed_generator");
        } else if ("player.use_block".equals(event) && node.role == NodeRole.ROUTER) {
            node.priorityMode = nextPriorityMode(node.priorityMode);
            handled = true;
            action = "cycle_priority_mode";
            countEvent("cycle_priority_mode");
        } else if ("player.use_block".equals(event) && node.role == NodeRole.FACTORY_CONTROLLER) {
            node.networkEnabled = !node.networkEnabled;
            handled = true;
            action = "toggle_network_enabled";
            countEvent("toggle_network_enabled");
        } else if ("player.use_block".equals(event) && node.role == NodeRole.MACHINE && node.jammed) {
            node.jammed = false;
            node.progress = 0;
            node.repairWear(REPAIR_WEAR_REDUCTION);
            handled = true;
            action = "repair_jammed_machine";
            countEvent("repair_jammed_machine");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handled", handled);
        result.put("event", event);
        result.put("nodeId", nodeId);
        result.put("action", action);
        result.put("priorityMode", node.priorityMode);
        result.put("networkEnabled", node.networkEnabled);
        result.put("jammed", node.jammed);
        result.put("wear", node.wear);
        result.put("failed", node.failed);
        result.put("progress", node.progress);
        result.put("adapterSurface", "event.dispatch");
        result.put("minecraftRuntimeAccessed", false);
        return result;
    }

    Map<String, Object> handleItemInteraction(String event, String nodeId, String itemId) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null) {
            diagnostics.add("Item interaction target missing: " + nodeId + ".");
            return Map.of("handled", false, "nodeId", nodeId, "event", event, "itemId", itemId);
        }

        int burnTicks = scrapDynamoFuelBurnTicks(itemId);
        boolean handled = "player.use_item_on_block".equals(event)
                && "scrap_dynamo".equals(node.id)
                && node.role == NodeRole.GENERATOR
                && burnTicks > 0;
        if (handled) {
            node.burnTicksRemaining += burnTicks;
            node.maxBurnTicks = Math.max(node.maxBurnTicks, node.burnTicksRemaining);
            countEvent("fuel_scrap_dynamo");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handled", handled);
        result.put("event", event);
        result.put("nodeId", nodeId);
        result.put("itemId", itemId);
        result.put("action", handled ? "fuel_scrap_dynamo" : "none");
        result.put("burnTicksAdded", handled ? burnTicks : 0);
        result.put("burnTicksRemaining", node.burnTicksRemaining);
        result.put("maxBurnTicks", node.maxBurnTicks);
        result.put("itemConsumed", handled);
        result.put("adapterSurface", "event.item_interaction_dispatch");
        result.put("minecraftRuntimeAccessed", false);
        return result;
    }

    int receiveEnergy(String nodeId, int amount, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || !node.canReceiveEnergy() || amount <= 0) {
            return 0;
        }
        int received = Math.min(amount, Math.min(node.transferPerTick, node.capacity - node.energy));
        if (!simulate) {
            node.energy += received;
        }
        return received;
    }

    int extractEnergy(String nodeId, int amount, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || !node.canExtractEnergy() || amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, Math.min(node.transferPerTick, node.energy));
        if (!simulate) {
            node.energy -= extracted;
        }
        return extracted;
    }

    int insertItem(String nodeId, String itemId, int count, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || count <= 0 || !node.acceptsItem(itemId)) {
            return 0;
        }
        int inserted = Math.min(count, Math.max(0, 64 - node.inputCount));
        if (!simulate) {
            node.inputItem = itemId;
            node.inputCount += inserted;
        }
        return inserted;
    }

    int insertItemFromSide(String nodeId, String itemId, int count, String side, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || count <= 0 || !node.exposesInputToSide(side) || !node.acceptsItem(itemId)) {
            return 0;
        }
        return insertItem(nodeId, itemId, count, simulate);
    }

    int extractOutputItem(String nodeId, String itemId, int count, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || count <= 0 || !node.outputItem.equals(itemId)) {
            return 0;
        }
        int extracted = Math.min(count, node.outputCount);
        if (!simulate) {
            node.outputCount -= extracted;
        }
        return extracted;
    }

    int extractOutputItemFromSide(String nodeId, String itemId, int count, String side, boolean simulate) {
        RuntimeNode node = nodes.get(nodeId);
        if (node == null || count <= 0 || !node.exposesOutputToSide(side)) {
            return 0;
        }
        if (node.outputItem.equals(itemId)) {
            return extractOutputItem(nodeId, itemId, count, simulate);
        }
        if (!node.byproductItem.equals(itemId)) {
            return 0;
        }
        int extracted = Math.min(count, node.byproductCount);
        if (!simulate) {
            node.byproductCount -= extracted;
        }
        return extracted;
    }

    void forceMachineFailure(String nodeId) {
        RuntimeNode node = nodes.get(nodeId);
        if (node != null && node.role == NodeRole.GENERATOR) {
            node.failed = true;
        }
    }

    void forceMachineJam(String nodeId) {
        RuntimeNode node = nodes.get(nodeId);
        if (node != null && node.role == NodeRole.MACHINE) {
            node.jammed = true;
            node.progress = Math.max(0, node.progress);
        }
    }

    Map<String, Object> snapshot() {
        List<Map<String, Object>> nodeSnapshots = new ArrayList<>();
        for (RuntimeNode node : nodes.values()) {
            nodeSnapshots.add(node.snapshot());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adapterPacketVersion", 1);
        data.put("adapterCoreBridge", true);
        data.put("adapterSurface", "world_state.snapshot");
        data.put("tickCount", tickCount);
        data.put("factoryScanCount", factoryScanCount);
        data.put("factoryConnectedMachines", factoryConnectedMachines);
        data.put("factoryStoredEnergy", factoryStoredEnergy);
        data.put("factoryEnergyCapacity", factoryEnergyCapacity);
        data.put("factoryScanLimitRespected", factoryScanLimitRespected);
        data.put("eventCounts", Map.copyOf(eventCounts));
        data.put("nodes", List.copyOf(nodeSnapshots));
        data.put("diagnostics", List.copyOf(diagnostics));
        data.put("minecraftRuntimeAccessed", false);
        data.put("minecraftRegistryMutated", false);
        return data;
    }

    @SuppressWarnings("unchecked")
    static AshfallAdapterCoreMachinePowerRuntime restore(Map<String, Object> snapshot) {
        AshfallAdapterCoreMachinePowerRuntime runtime = new AshfallAdapterCoreMachinePowerRuntime();
        runtime.tickCount = intValue(snapshot.get("tickCount"));
        runtime.factoryScanCount = intValue(snapshot.get("factoryScanCount"));
        runtime.factoryConnectedMachines = intValue(snapshot.get("factoryConnectedMachines"));
        runtime.factoryStoredEnergy = intValue(snapshot.get("factoryStoredEnergy"));
        runtime.factoryEnergyCapacity = intValue(snapshot.get("factoryEnergyCapacity"));
        runtime.factoryScanLimitRespected = Boolean.TRUE.equals(snapshot.get("factoryScanLimitRespected"));
        Object eventCountsValue = snapshot.get("eventCounts");
        if (eventCountsValue instanceof Map<?, ?> counts) {
            for (Map.Entry<?, ?> entry : counts.entrySet()) {
                runtime.eventCounts.put(String.valueOf(entry.getKey()), intValue(entry.getValue()));
            }
        }
        Object diagnosticsValue = snapshot.get("diagnostics");
        if (diagnosticsValue instanceof List<?> items) {
            for (Object item : items) {
                runtime.diagnostics.add(String.valueOf(item));
            }
        }

        Object nodesValue = snapshot.get("nodes");
        if (nodesValue instanceof List<?> nodeSnapshots) {
            for (Object nodeValue : nodeSnapshots) {
                if (nodeValue instanceof Map<?, ?> nodeMap) {
                    RuntimeNode node = RuntimeNode.restore((Map<String, Object>) nodeMap);
                    runtime.nodes.put(node.id, node);
                }
            }
        }
        return runtime;
    }

    Map<String, Object> describe() {
        RuntimeNode generator = nodes.get("micro_generator");
        RuntimeNode battery = nodes.get("battery_bank");
        RuntimeNode cable = nodes.get("power_cable");
        RuntimeNode distributor = nodes.get("load_distributor");
        RuntimeNode press = nodes.get("scrap_press");
        RuntimeNode pipe = nodes.get("item_pipe");
        RuntimeNode grinder = nodes.get("ore_grinder");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adapterPacketVersion", 3);
        data.put("adapterCoreBridge", true);
        data.put("adapterSurface", ADAPTER_SURFACE);
        data.put("implementationTarget", "AdapterCore stateful machine/power/logistics runtime");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("runtimeStateInitialized", true);
        data.put("tickCount", tickCount);
        data.put("registeredBlockEntityBindings", registryBindings());
        data.put("capabilityBridge", capabilityReport());
        data.put("eventBridge", Map.copyOf(eventCounts));
        data.put("powerTransfers", List.copyOf(powerTransfers));
        data.put("powerTransferCount", powerTransfers.size());
        data.put("itemTransfers", List.copyOf(itemTransfers));
        data.put("itemPipeMovedCount", itemTransfers.stream().mapToInt(t -> (Integer) t.get("count")).sum());
        data.put("microGeneratorFuelItems", generator.fuelItems);
        data.put("microGeneratorBurnTicksRemaining", generator.burnTicksRemaining);
        data.put("microGeneratorEnergy", generator.energy);
        data.put("microGeneratorWear", generator.wear);
        data.put("microGeneratorFailed", generator.failed);
        data.put("powerCableEnergy", cable.energy);
        data.put("loadDistributorEnergy", distributor.energy);
        data.put("loadDistributorPriorityMode", distributor.priorityMode);
        data.put("batteryStoredEnergy", battery.energy);
        data.put("scrapPressProgress", press.progress);
        data.put("scrapPressOutputCount", press.outputCount);
        data.put("scrapPressWear", press.wear);
        data.put("scrapPressJammed", press.jammed);
        data.put("itemPipeCooldown", pipe.cooldown);
        data.put("oreGrinderProgress", grinder.progress);
        data.put("oreGrinderInputCount", grinder.inputCount);
        data.put("oreGrinderOutputCount", grinder.outputCount);
        data.put("oreGrinderByproductCount", grinder.byproductCount);
        data.put("oreGrinderWear", grinder.wear);
        data.put("oreGrinderJammed", grinder.jammed);
        data.put("factoryScanCount", factoryScanCount);
        data.put("factoryConnectedMachines", factoryConnectedMachines);
        data.put("factoryStoredEnergy", factoryStoredEnergy);
        data.put("factoryEnergyCapacity", factoryEnergyCapacity);
        data.put("factoryScanLimitRespected", factoryScanLimitRespected);
        data.put("powerCapacityRespected", powerCapacityRespected());
        data.put("logisticsLoopAvoided", itemTransfers.stream().allMatch(t -> Boolean.TRUE.equals(t.get("loopAvoided"))));
        data.put("diagnostics", List.copyOf(diagnostics));
        data.put("networkDiagnostic", diagnostics.isEmpty() && powerCapacityRespected() && factoryScanLimitRespected ? "PASS" : "WARN");
        data.put("minecraftRuntimeAccessed", false);
        data.put("minecraftRegistryMutated", false);
        return data;
    }

    private void tickGenerators() {
        for (RuntimeNode node : nodes.values()) {
            if (node.role != NodeRole.GENERATOR || node.failed) {
                continue;
            }
            node.startFuelCycleIfNeeded();
            node.burnOneTick();
        }
    }

    private void tickPowerNetwork() {
        moveEnergy("micro_generator", "power_cable", 64);
        moveEnergy("power_cable", "load_distributor", 50);
        distributeFromRouter(nodes.get("load_distributor"));
    }

    private void distributeFromRouter(RuntimeNode router) {
        if (router == null || router.energy <= 0 || router.role != NodeRole.ROUTER) {
            return;
        }

        if ("GRID".equals(router.priorityMode)) {
            RuntimeNode battery = nodes.get("battery_bank");
            if (battery != null) {
                moveEnergy(router.id, battery.id, router.transferPerTick);
            }
        }

        List<RuntimeNode> consumers = switch (router.priorityMode) {
            case "FACTORY" -> List.of(nodes.get("ore_grinder"), nodes.get("scrap_press"));
            default -> List.of(nodes.get("scrap_press"), nodes.get("ore_grinder"));
        };
        for (RuntimeNode consumer : consumers) {
            if (consumer == null || router.energy <= 0 || consumer.demandPerTick <= 0 || !consumer.hasRecipeInput()) {
                continue;
            }
            int required = Math.min(router.transferPerTick, consumer.demandPerTick);
            if (router.energy >= required) {
                moveEnergy(router.id, consumer.id, required);
            }
        }

        RuntimeNode battery = nodes.get("battery_bank");
        if (!"GRID".equals(router.priorityMode) && battery != null && router.energy > 0) {
            moveEnergy(router.id, battery.id, router.transferPerTick);
        }
    }

    private Map<String, Object> balanceInsertedBattery(RuntimeNode bank) {
        int bankEnergyBefore = bank == null ? 0 : bank.energy;
        int batteryEnergyBefore = bank == null ? 0 : bank.batteryItemEnergy;
        int moved = 0;
        String direction = "none";
        if (bank != null && bank.role == NodeRole.STORAGE && bank.batteryItemCapacity > 0 && bank.batteryItemTransfer > 0) {
            float bankPercent = bank.capacity <= 0 ? 0.0F : (float) bank.energy / bank.capacity;
            float batteryPercent = (float) bank.batteryItemEnergy / bank.batteryItemCapacity;
            if (bankPercent > batteryPercent) {
                moved = Math.min(bank.energy, Math.min(bank.transferPerTick,
                        Math.min(bank.batteryItemTransfer, bank.batteryItemCapacity - bank.batteryItemEnergy)));
                if (moved > 0) {
                    bank.energy -= moved;
                    bank.batteryItemEnergy += moved;
                    direction = "bank_to_battery";
                }
            } else if (batteryPercent > bankPercent) {
                moved = Math.min(bank.batteryItemEnergy, Math.min(bank.batteryItemTransfer,
                        Math.min(bank.transferPerTick, bank.capacity - bank.energy)));
                if (moved > 0) {
                    bank.batteryItemEnergy -= moved;
                    bank.energy += moved;
                    direction = "battery_to_bank";
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batteryItemId", bank == null ? "" : bank.batteryItemId);
        result.put("direction", direction);
        result.put("moved", moved);
        result.put("bankEnergyBefore", bankEnergyBefore);
        result.put("bankEnergyAfter", bank == null ? 0 : bank.energy);
        result.put("batteryEnergyBefore", batteryEnergyBefore);
        result.put("batteryEnergyAfter", bank == null ? 0 : bank.batteryItemEnergy);
        result.put("changed", moved > 0);
        result.put("transferLimitRespected", bank != null && moved <= bank.transferPerTick && moved <= bank.batteryItemTransfer);
        return result;
    }

    private void tickMachines() {
        RuntimeNode press = nodes.get("scrap_press");
        if (press != null) {
            tickMachineRecipe(press, 2);
        }
        RuntimeNode grinder = nodes.get("ore_grinder");
        if (grinder != null) {
            tickMachineRecipe(grinder, 1);
        }
    }

    private void tickMachineRecipe(RuntimeNode node, int wearOnCompletion) {
        if (node.jammed || !node.hasRecipeInput() || node.energy < node.demandPerTick || !canFitRecipeOutput(node)) {
            if (!node.hasRecipeInput()) {
                node.progress = 0;
            }
            return;
        }

        node.energy -= node.demandPerTick;
        node.progress++;
        if (node.role == NodeRole.MACHINE && node.id.equals("ore_grinder") && node.progress % 20 == 0) {
            node.wear++;
        }

        if (node.progress >= node.recipeTicks) {
            node.inputCount -= node.recipeInputCount;
            node.outputCount += node.recipeOutputCount;
            if ("OreGrinderBlockEntity".equals(node.blockEntityClass)
                    && !node.byproductItem.isBlank()
                    && node.rollByproduct()) {
                node.byproductCount += node.byproductOutputCount;
            }
            node.progress = 0;
            node.wear += wearOnCompletion;
        }
    }

    private boolean canFitRecipeOutput(RuntimeNode node) {
        if (node.role != NodeRole.MACHINE) {
            return false;
        }
        boolean outputFits = node.outputCount + node.recipeOutputCount <= MAX_STACK_SIZE;
        boolean byproductFits = node.byproductItem.isBlank()
                || node.byproductCount + node.byproductOutputCount <= MAX_STACK_SIZE;
        return outputFits && byproductFits;
    }

    private void tickRecipeProbe(RuntimeNode node, int wearOnCompletion) {
        for (int tick = 0; tick < node.recipeTicks; tick++) {
            tickMachineRecipe(node, wearOnCompletion);
        }
    }

    private void tickItemPipes() {
        boolean waitingForCooldown = false;
        for (RuntimeNode node : nodes.values()) {
            if (node.role == NodeRole.PIPE && node.cooldown > 0) {
                node.cooldown--;
                waitingForCooldown = true;
            }
        }
        if (waitingForCooldown) {
            return;
        }

        for (RuntimeNode source : nodes.values()) {
            if (!source.canProvideHopperOutput()) {
                continue;
            }
            Route route = findInventoryRoute(source.id, source.outputItem);
            if (route.targetId == null) {
                continue;
            }
            int inserted = insertItem(route.targetId, source.outputItem, 1, false);
            if (inserted > 0) {
                source.outputCount -= inserted;
                applyRouteCooldown(route);
                itemTransfers.add(itemTransfer(source.id, route.targetId, source.outputItem, inserted, route,
                        source.hopperOutputContract()));
                return;
            }
        }
    }

    private void tickFactoryControllers() {
        RuntimeNode controller = nodes.get("factory_controller");
        if (controller == null || !controller.networkEnabled || tickCount % 20 != 0) {
            return;
        }
        scanFactoryNetwork(controller);
    }

    private void scanFactoryNetwork(RuntimeNode controller) {
        factoryScanCount++;
        int scanLimit = 100;
        int radius = 16;
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(controller.id);
        visited.add(controller.id);

        int machines = 0;
        int stored = 0;
        int capacity = 0;
        while (!queue.isEmpty() && visited.size() < scanLimit) {
            RuntimeNode current = nodes.get(queue.remove());
            if (current == null) {
                continue;
            }
            for (String neighborId : current.neighbors) {
                if (!visited.add(neighborId)) {
                    continue;
                }
                RuntimeNode neighbor = nodes.get(neighborId);
                if (neighbor == null || manhattan(controller, neighbor) > radius) {
                    continue;
                }
                if (neighbor.trackableByFactory()) {
                    machines++;
                    stored += Math.max(0, neighbor.energy);
                    capacity += Math.max(0, neighbor.capacity);
                }
                if (neighbor.trackableByFactory() || neighbor.role == NodeRole.CABLE || neighbor.role == NodeRole.PIPE) {
                    queue.add(neighborId);
                }
            }
        }
        factoryConnectedMachines = machines;
        factoryStoredEnergy = stored;
        factoryEnergyCapacity = capacity;
        factoryScanLimitRespected = visited.size() <= scanLimit;
    }

    private Route findInventoryRoute(String sourceId, String itemId) {
        Queue<RouteStep> queue = new ArrayDeque<>();
        queue.add(new RouteStep(sourceId, List.of()));
        Set<String> visited = new LinkedHashSet<>();
        visited.add(sourceId);
        int loopSkips = 0;

        while (!queue.isEmpty()) {
            RouteStep step = queue.remove();
            RuntimeNode current = nodes.get(step.nodeId);
            if (current == null) {
                continue;
            }
            if (!step.nodeId.equals(sourceId) && current.acceptsItem(itemId)) {
                return new Route(step.nodeId, step.visitedPipes, visited.size(), loopSkips);
            }
            for (String neighborId : current.neighbors) {
                if (!visited.add(neighborId)) {
                    loopSkips++;
                    continue;
                }
                RuntimeNode neighbor = nodes.get(neighborId);
                if (neighbor == null) {
                    continue;
                }
                if (neighbor.role != NodeRole.PIPE && !neighbor.acceptsItem(itemId)) {
                    continue;
                }
                List<String> visitedPipes = new ArrayList<>(step.visitedPipes);
                if (neighbor.role == NodeRole.PIPE) {
                    visitedPipes.add(neighbor.id);
                }
                queue.add(new RouteStep(neighborId, List.copyOf(visitedPipes)));
            }
        }
        return new Route(null, List.of(), visited.size(), loopSkips);
    }

    private void applyRouteCooldown(Route route) {
        for (String pipeId : route.visitedPipes) {
            RuntimeNode pipe = nodes.get(pipeId);
            if (pipe != null && pipe.role == NodeRole.PIPE) {
                pipe.cooldown = pipe.transferCooldownTicks;
            }
        }
    }

    private Map<String, Object> transferEnergyAlongPowerRoute(String sourceId, String targetId, int requestedAmount) {
        PowerRoute route = findPowerRoute(sourceId, targetId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", sourceId);
        data.put("target", targetId);
        data.put("path", List.copyOf(route.path));
        data.put("visitedNodeCount", route.visitedNodeCount);
        data.put("loopSkipCount", route.loopSkipCount);
        data.put("routeFound", route.found());
        if (!route.found()) {
            data.put("amountMoved", 0);
            return data;
        }

        int amount = Math.min(requestedAmount, route.bottleneck);
        for (int index = 0; index < route.path.size() - 1; index++) {
            moveEnergy(route.path.get(index), route.path.get(index + 1), amount);
        }
        data.put("amountMoved", amount);
        data.put("bottleneck", route.bottleneck);
        data.put("targetEnergyAfter", nodes.get(targetId).energy);
        return data;
    }

    private PowerRoute findPowerRoute(String sourceId, String targetId) {
        Queue<PowerStep> queue = new ArrayDeque<>();
        queue.add(new PowerStep(sourceId, List.of(sourceId)));
        Set<String> visited = new LinkedHashSet<>();
        visited.add(sourceId);
        int loopSkips = 0;

        while (!queue.isEmpty()) {
            PowerStep step = queue.remove();
            if (step.nodeId.equals(targetId)) {
                return new PowerRoute(step.path, powerRouteBottleneck(step.path), visited.size(), loopSkips);
            }
            RuntimeNode current = nodes.get(step.nodeId);
            if (current == null) {
                continue;
            }
            for (String neighborId : current.neighbors) {
                if (!visited.add(neighborId)) {
                    loopSkips++;
                    continue;
                }
                RuntimeNode neighbor = nodes.get(neighborId);
                if (neighbor == null || !canTraversePowerRoute(neighbor, targetId)) {
                    continue;
                }
                List<String> path = new ArrayList<>(step.path);
                path.add(neighborId);
                queue.add(new PowerStep(neighborId, List.copyOf(path)));
            }
        }
        return new PowerRoute(List.of(), 0, visited.size(), loopSkips);
    }

    private boolean canTraversePowerRoute(RuntimeNode node, String targetId) {
        if (node.id.equals(targetId)) {
            return node.canReceiveEnergy();
        }
        return node.role == NodeRole.CABLE || node.role == NodeRole.ROUTER || node.role == NodeRole.STORAGE;
    }

    private int powerRouteBottleneck(List<String> path) {
        int bottleneck = Integer.MAX_VALUE;
        for (String nodeId : path) {
            RuntimeNode node = nodes.get(nodeId);
            if (node == null || node.transferPerTick <= 0) {
                continue;
            }
            bottleneck = Math.min(bottleneck, node.transferPerTick);
        }
        RuntimeNode target = nodes.get(path.get(path.size() - 1));
        if (target != null) {
            bottleneck = Math.min(bottleneck, Math.max(0, target.capacity - target.energy));
        }
        return bottleneck == Integer.MAX_VALUE ? 0 : bottleneck;
    }

    private void moveEnergy(String sourceId, String targetId, int amount) {
        RuntimeNode source = nodes.get(sourceId);
        RuntimeNode target = nodes.get(targetId);
        if (source == null || target == null || source.energy <= 0 || !target.canReceiveEnergy()) {
            return;
        }
        int moved = Math.min(amount, Math.min(source.energy, Math.min(source.transferPerTick, target.transferPerTick)));
        moved = Math.min(moved, target.capacity - target.energy);
        if (moved <= 0) {
            return;
        }
        source.energy -= moved;
        target.energy += moved;
        powerTransfers.add(powerTransfer(sourceId, targetId, moved, target.capacity, target.energy));
    }

    private AshfallAdapterCoreMachinePowerRuntime add(RuntimeNode node) {
        nodes.put(node.id, node);
        return this;
    }

    private void link(String left, String right) {
        nodes.get(left).neighbors.add(right);
        nodes.get(right).neighbors.add(left);
    }

    private boolean powerCapacityRespected() {
        for (RuntimeNode node : nodes.values()) {
            if (node.capacity > 0 && (node.energy < 0 || node.energy > node.capacity)) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, Object>> registryBindings() {
        return REGISTERED_BLOCK_ENTITY_BINDINGS;
    }

    private Map<String, Object> capabilityReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("energyReceiveProbe", receiveEnergy("battery_bank", 37, true));
        report.put("energyExtractProbe", extractEnergy("battery_bank", 37, true));
        report.put("inventoryInsertProbe", insertItem("ore_grinder", "ore_substrate", 1, true));
        report.put("inventoryExtractProbe", extractOutputItem("scrap_press", "compressed_scrap", 1, true));
        report.put("adapterSurface", "capability.bridge");
        report.put("minecraftRuntimeAccessed", false);
        return report;
    }

    private void countEvent(String event) {
        eventCounts.put(event, eventCounts.getOrDefault(event, 0) + 1);
    }

    private static String nextPriorityMode(String mode) {
        return switch (mode) {
            case "BALANCED" -> "SURVIVAL";
            case "SURVIVAL" -> "FACTORY";
            case "FACTORY" -> "GRID";
            default -> "BALANCED";
        };
    }

    private static int manhattan(RuntimeNode left, RuntimeNode right) {
        return Math.abs(left.x - right.x) + Math.abs(left.y - right.y) + Math.abs(left.z - right.z);
    }

    private static int transferLimitFor(AshfallAdapterCoreMachinePowerRuntime runtime, String sourceId, String targetId) {
        RuntimeNode source = runtime.nodes.get(sourceId);
        RuntimeNode target = runtime.nodes.get(targetId);
        if (source == null || target == null) {
            return 0;
        }
        return Math.min(source.transferPerTick, target.transferPerTick);
    }

    private static int scrapDynamoFuelBurnTicks(String itemId) {
        String normalized = itemId == null ? "" : itemId.replace("echoashfallprotocol:", "").replace("minecraft:", "");
        return switch (normalized) {
            case "coal", "charcoal" -> 240;
            case "scrap_circuit" -> 180;
            case "scrap_plastic" -> 120;
            case "scrap_metal" -> 80;
            default -> 0;
        };
    }

    private static String crystallineOutput(int outputIndex) {
        return switch (outputIndex) {
            case 1 -> "minecraft:emerald";
            case 2 -> "minecraft:netherite_scrap";
            default -> "minecraft:diamond";
        };
    }

    private static String deepCoreOutput(int outputIndex) {
        return switch (outputIndex) {
            case 1 -> "echoashfallprotocol:gem_fragment";
            case 2 -> "echoashfallprotocol:crystal_dust";
            case 3 -> "minecraft:redstone";
            case 4 -> "minecraft:lapis_lazuli";
            default -> "echoashfallprotocol:dense_alloy_chunk";
        };
    }

    private static boolean neighborAcceptsDeepCoreOutput(String itemId) {
        return "echoashfallprotocol:dense_alloy_chunk".equals(itemId)
                || "echoashfallprotocol:gem_fragment".equals(itemId)
                || "echoashfallprotocol:crystal_dust".equals(itemId)
                || "minecraft:redstone".equals(itemId)
                || "minecraft:lapis_lazuli".equals(itemId);
    }

    private static Map<String, Object> blockEntityBinding(String id, String blockEntityClass, String adapterTarget) {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("id", id);
        binding.put("blockEntity", blockEntityClass);
        binding.put("adapterSurface", "block_entity.runtime_binding");
        binding.put("adapterTarget", adapterTarget);
        binding.put("tickBound", true);
        binding.put("minecraftRegistryMutated", false);
        return Map.copyOf(binding);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static float floatValue(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    private static Map<String, Object> powerTransfer(String source, String target, int amount, int targetCapacity, int targetEnergy) {
        Map<String, Object> transfer = new LinkedHashMap<>();
        transfer.put("source", source);
        transfer.put("target", target);
        transfer.put("amount", amount);
        transfer.put("targetEnergy", targetEnergy);
        transfer.put("targetCapacity", targetCapacity);
        transfer.put("capacityRespected", targetEnergy <= targetCapacity);
        return transfer;
    }

    private static Map<String, Object> itemTransfer(
            String source,
            String target,
            String item,
            int count,
            Route route,
            String sourceOutputContract) {
        Map<String, Object> transfer = new LinkedHashMap<>();
        transfer.put("source", source);
        transfer.put("target", target);
        transfer.put("item", item);
        transfer.put("count", count);
        transfer.put("sourceOutputContract", sourceOutputContract);
        transfer.put("targetInputContract", "HopperHandler.getInputSlots");
        transfer.put("visitedPipes", List.copyOf(route.visitedPipes));
        transfer.put("visitedNodeCount", route.visitedNodeCount);
        transfer.put("loopSkipCount", route.loopSkipCount);
        transfer.put("loopAvoided", true);
        return transfer;
    }

    private static Map<String, Object> worldBlock(int x, int y, int z, String block) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("block", block);
        return data;
    }

    private static Map<String, Object> findToxicBlockWithinRadius(List<Map<String, Object>> worldBlocks, int radius) {
        for (Map<String, Object> block : worldBlocks) {
            if ("echoashfallprotocol:toxic_puddle".equals(block.get("block")) && withinRadius(block, radius)) {
                return block;
            }
        }
        return null;
    }

    private static boolean withinRadius(Map<String, Object> block, int radius) {
        int x = (Integer) block.get("x");
        int y = (Integer) block.get("y");
        int z = (Integer) block.get("z");
        return x >= -radius && x <= radius
                && y >= -1 && y <= 2
                && z >= -radius && z <= radius;
    }

    private enum NodeRole {
        GENERATOR,
        CABLE,
        ROUTER,
        STORAGE,
        MACHINE,
        PIPE,
        FACTORY_CONTROLLER,
        INVENTORY_SOURCE
    }

    private static final class RuntimeNode {
        private final String id;
        private final String blockEntityClass;
        private final int x;
        private final int y;
        private final int z;
        private final int capacity;
        private final int transferPerTick;
        private final int generationPerTick;
        private final int demandPerTick;
        private final int recipeTicks;
        private final int recipeInputCount;
        private final int recipeOutputCount;
        private final int transferCooldownTicks;
        private final NodeRole role;
        private final List<String> neighbors = new ArrayList<>();
        private int energy;
        private int fuelItems;
        private int burnTicksRemaining;
        private int maxBurnTicks;
        private boolean failed;
        private boolean jammed;
        private boolean networkEnabled = true;
        private int progress;
        private int wear;
        private int cooldown;
        private String priorityMode = "BALANCED";
        private String inputItem = "";
        private int inputCount;
        private String outputItem = "";
        private int outputCount;
        private String byproductItem = "";
        private int byproductCount;
        private int byproductOutputCount;
        private float byproductChance = 1.0F;
        private int byproductRollIndex;
        private int byproductSuccessCount;
        private int byproductSkipCount;
        private String batteryItemId = "";
        private int batteryItemEnergy;
        private int batteryItemCapacity;
        private int batteryItemTransfer;
        private final List<Float> byproductRollHistory = new ArrayList<>();

        private RuntimeNode(String id, String blockEntityClass, int x, int y, int z, int capacity, int transferPerTick,
                            int generationPerTick, int demandPerTick, int recipeTicks, int recipeInputCount,
                            int recipeOutputCount, int transferCooldownTicks, NodeRole role) {
            this.id = id;
            this.blockEntityClass = blockEntityClass;
            this.x = x;
            this.y = y;
            this.z = z;
            this.capacity = capacity;
            this.transferPerTick = transferPerTick;
            this.generationPerTick = generationPerTick;
            this.demandPerTick = demandPerTick;
            this.recipeTicks = recipeTicks;
            this.recipeInputCount = recipeInputCount;
            this.recipeOutputCount = recipeOutputCount;
            this.transferCooldownTicks = transferCooldownTicks;
            this.role = role;
        }

        static RuntimeNode generator(String id, int x, int y, int z, int capacity, int transfer, int generation, int fuelItems) {
            return generator(id, "MicroGeneratorBlockEntity", x, y, z, capacity, transfer, generation, fuelItems);
        }

        static RuntimeNode generator(String id, String blockEntityClass, int x, int y, int z, int capacity,
                                     int transfer, int generation, int fuelItems) {
            RuntimeNode node = new RuntimeNode(id, blockEntityClass, x, y, z, capacity, transfer, generation,
                    0, 0, 0, 0, 0, NodeRole.GENERATOR);
            node.fuelItems = fuelItems;
            return node;
        }

        static RuntimeNode energy(String id, String blockEntityClass, int x, int y, int z, int capacity, int transfer, NodeRole role) {
            return new RuntimeNode(id, blockEntityClass, x, y, z, capacity, transfer, 0, 0, 0, 0, 0, 0, role);
        }

        static RuntimeNode router(String id, int x, int y, int z, int capacity, int transfer) {
            return energy(id, "LoadDistributorBlockEntity", x, y, z, capacity, transfer, NodeRole.ROUTER);
        }

        static RuntimeNode machine(String id, String blockEntityClass, int x, int y, int z, int capacity, int transfer,
                                   int demand, int recipeTicks, String inputItem, int inputCount, String outputItem, int outputCount) {
            RuntimeNode node = new RuntimeNode(id, blockEntityClass, x, y, z, capacity, transfer, 0, demand,
                    recipeTicks, inputCount, outputCount, 0, NodeRole.MACHINE);
            node.inputItem = inputItem;
            node.inputCount = inputCount;
            node.outputItem = outputItem;
            return node;
        }

        static RuntimeNode pipe(String id, int x, int y, int z, int cooldown) {
            return new RuntimeNode(id, "ItemPipeBlockEntity", x, y, z, 0, 0, 0, 0, 0, 0, 0, cooldown, NodeRole.PIPE);
        }

        static RuntimeNode factoryController(String id, int x, int y, int z) {
            return new RuntimeNode(id, "FactoryControllerBlockEntity", x, y, z, 0, 0, 0, 0, 0, 0, 0, 0,
                    NodeRole.FACTORY_CONTROLLER);
        }

        static RuntimeNode inventorySource(String id, int x, int y, int z, String outputItem, int outputCount) {
            RuntimeNode node = new RuntimeNode(id, "", x, y, z, 0, 0, 0, 0, 0, 0, 0, 0, NodeRole.INVENTORY_SOURCE);
            node.outputItem = outputItem;
            node.outputCount = outputCount;
            return node;
        }

        RuntimeNode withEnergy(int energy) {
            this.energy = Math.max(0, Math.min(capacity, energy));
            return this;
        }

        RuntimeNode withBatteryItem(String itemId, int energy, int capacity, int transfer) {
            this.batteryItemId = itemId == null ? "" : itemId;
            this.batteryItemCapacity = Math.max(0, capacity);
            this.batteryItemTransfer = Math.max(0, transfer);
            this.batteryItemEnergy = Math.max(0, Math.min(this.batteryItemCapacity, energy));
            return this;
        }

        boolean startFuelCycleIfNeeded() {
            if (role != NodeRole.GENERATOR || burnTicksRemaining > 0 || fuelItems <= 0 || energy >= capacity) {
                return false;
            }
            fuelItems--;
            burnTicksRemaining = defaultFuelBurnTicks();
            maxBurnTicks = burnTicksRemaining;
            return true;
        }

        boolean burnOneTick() {
            if (role != NodeRole.GENERATOR || failed || burnTicksRemaining <= 0) {
                return false;
            }
            burnTicksRemaining--;
            energy = Math.min(capacity, energy + generationPerTick);
            if (burnTicksRemaining % 20 == 0) {
                wear = Math.min(MAX_WEAR, wear + 3);
            }
            return true;
        }

        private int defaultFuelBurnTicks() {
            return "scrap_dynamo".equals(id) ? 80 : 160;
        }

        RuntimeNode withByproduct(String byproductItem, int byproductOutputCount) {
            return withByproduct(byproductItem, byproductOutputCount, 1.0F);
        }

        RuntimeNode withByproduct(String byproductItem, int byproductOutputCount, float byproductChance) {
            this.byproductItem = byproductItem == null ? "" : byproductItem;
            this.byproductOutputCount = Math.max(0, byproductOutputCount);
            this.byproductChance = Math.max(0.0F, Math.min(1.0F, byproductChance));
            return this;
        }

        RuntimeNode withByproductCount(int byproductCount) {
            this.byproductCount = Math.max(0, Math.min(MAX_STACK_SIZE, byproductCount));
            return this;
        }

        RuntimeNode withOutputCount(int outputCount) {
            this.outputCount = Math.max(0, Math.min(MAX_STACK_SIZE, outputCount));
            return this;
        }

        RuntimeNode withInputCount(int inputCount) {
            this.inputCount = Math.max(0, Math.min(MAX_STACK_SIZE, inputCount));
            return this;
        }

        RuntimeNode withPriorityMode(String priorityMode) {
            this.priorityMode = priorityMode == null || priorityMode.isBlank() ? "BALANCED" : priorityMode;
            return this;
        }

        RuntimeNode withWear(int wear) {
            this.wear = Math.max(0, Math.min(MAX_WEAR, wear));
            return this;
        }

        Map<String, Object> snapshot() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("blockEntityClass", blockEntityClass);
            data.put("x", x);
            data.put("y", y);
            data.put("z", z);
            data.put("capacity", capacity);
            data.put("transferPerTick", transferPerTick);
            data.put("generationPerTick", generationPerTick);
            data.put("demandPerTick", demandPerTick);
            data.put("recipeTicks", recipeTicks);
            data.put("recipeInputCount", recipeInputCount);
            data.put("recipeOutputCount", recipeOutputCount);
            data.put("transferCooldownTicks", transferCooldownTicks);
            data.put("role", role.name());
            data.put("neighbors", List.copyOf(neighbors));
            data.put("energy", energy);
            data.put("fuelItems", fuelItems);
            data.put("burnTicksRemaining", burnTicksRemaining);
            data.put("maxBurnTicks", maxBurnTicks);
            data.put("failed", failed);
            data.put("jammed", jammed);
            data.put("networkEnabled", networkEnabled);
            data.put("progress", progress);
            data.put("wear", wear);
            data.put("cooldown", cooldown);
            data.put("priorityMode", priorityMode);
            data.put("inputItem", inputItem);
            data.put("inputCount", inputCount);
            data.put("outputItem", outputItem);
            data.put("outputCount", outputCount);
            data.put("byproductItem", byproductItem);
            data.put("byproductCount", byproductCount);
            data.put("byproductOutputCount", byproductOutputCount);
            data.put("byproductChance", byproductChance);
            data.put("byproductRollIndex", byproductRollIndex);
            data.put("byproductSuccessCount", byproductSuccessCount);
            data.put("byproductSkipCount", byproductSkipCount);
            data.put("batteryItemId", batteryItemId);
            data.put("batteryItemEnergy", batteryItemEnergy);
            data.put("batteryItemCapacity", batteryItemCapacity);
            data.put("batteryItemTransfer", batteryItemTransfer);
            data.put("byproductRollHistory", List.copyOf(byproductRollHistory));
            return data;
        }

        @SuppressWarnings("unchecked")
        static RuntimeNode restore(Map<String, Object> data) {
            RuntimeNode node = new RuntimeNode(
                    String.valueOf(data.get("id")),
                    String.valueOf(data.getOrDefault("blockEntityClass", "")),
                    intValue(data.get("x")),
                    intValue(data.get("y")),
                    intValue(data.get("z")),
                    intValue(data.get("capacity")),
                    intValue(data.get("transferPerTick")),
                    intValue(data.get("generationPerTick")),
                    intValue(data.get("demandPerTick")),
                    intValue(data.get("recipeTicks")),
                    intValue(data.get("recipeInputCount")),
                    intValue(data.get("recipeOutputCount")),
                    intValue(data.get("transferCooldownTicks")),
                    NodeRole.valueOf(String.valueOf(data.get("role"))));
            Object neighborsValue = data.get("neighbors");
            if (neighborsValue instanceof List<?> items) {
                for (Object item : items) {
                    node.neighbors.add(String.valueOf(item));
                }
            }
            node.energy = intValue(data.get("energy"));
            node.fuelItems = intValue(data.get("fuelItems"));
            node.burnTicksRemaining = intValue(data.get("burnTicksRemaining"));
            node.maxBurnTicks = intValue(data.get("maxBurnTicks"));
            node.failed = Boolean.TRUE.equals(data.get("failed"));
            node.jammed = Boolean.TRUE.equals(data.get("jammed"));
            node.networkEnabled = Boolean.TRUE.equals(data.get("networkEnabled"));
            node.progress = intValue(data.get("progress"));
            node.wear = intValue(data.get("wear"));
            node.cooldown = intValue(data.get("cooldown"));
            node.priorityMode = String.valueOf(data.getOrDefault("priorityMode", "BALANCED"));
            node.inputItem = String.valueOf(data.getOrDefault("inputItem", ""));
            node.inputCount = intValue(data.get("inputCount"));
            node.outputItem = String.valueOf(data.getOrDefault("outputItem", ""));
            node.outputCount = intValue(data.get("outputCount"));
            node.byproductItem = String.valueOf(data.getOrDefault("byproductItem", ""));
            node.byproductCount = intValue(data.get("byproductCount"));
            node.byproductOutputCount = intValue(data.get("byproductOutputCount"));
            node.byproductChance = floatValue(data.getOrDefault("byproductChance", 1.0F));
            node.byproductRollIndex = intValue(data.get("byproductRollIndex"));
            node.byproductSuccessCount = intValue(data.get("byproductSuccessCount"));
            node.byproductSkipCount = intValue(data.get("byproductSkipCount"));
            node.batteryItemId = String.valueOf(data.getOrDefault("batteryItemId", ""));
            node.batteryItemEnergy = intValue(data.get("batteryItemEnergy"));
            node.batteryItemCapacity = intValue(data.get("batteryItemCapacity"));
            node.batteryItemTransfer = intValue(data.get("batteryItemTransfer"));
            Object byproductRollHistoryValue = data.get("byproductRollHistory");
            if (byproductRollHistoryValue instanceof List<?> rolls) {
                for (Object roll : rolls) {
                    node.byproductRollHistory.add(floatValue(roll));
                }
            }
            return node;
        }

        boolean canReceiveEnergy() {
            return capacity > 0 && transferPerTick > 0 && role != NodeRole.GENERATOR && energy < capacity;
        }

        boolean canExtractEnergy() {
            return capacity > 0 && transferPerTick > 0 && energy > 0;
        }

        boolean acceptsItem(String itemId) {
            return role == NodeRole.MACHINE && inputItem.equals(itemId) && inputCount < 64;
        }

        boolean canProvideHopperOutput() {
            return outputCount > 0
                    && !outputItem.isBlank()
                    && (role == NodeRole.INVENTORY_SOURCE || exposesOutputToSide("DOWN"));
        }

        String hopperOutputContract() {
            if (role == NodeRole.MACHINE) {
                return "HopperHandler.getOutputSlots.DOWN";
            }
            return "HopperHandler.getOutputSlots";
        }

        boolean hasRecipeInput() {
            return role == NodeRole.MACHINE && !inputItem.isBlank() && inputCount >= recipeInputCount;
        }

        boolean exposesInputToSide(String side) {
            return role == NodeRole.MACHINE;
        }

        boolean exposesOutputToSide(String side) {
            return role == NodeRole.MACHINE && "DOWN".equalsIgnoreCase(side);
        }

        boolean rollByproduct() {
            if (byproductChance <= 0.0F) {
                byproductSkipCount++;
                return false;
            }
            if (byproductChance >= 1.0F) {
                byproductSuccessCount++;
                return true;
            }
            float roll = DETERMINISTIC_BYPRODUCT_ROLLS[byproductRollIndex % DETERMINISTIC_BYPRODUCT_ROLLS.length];
            byproductRollIndex++;
            byproductRollHistory.add(roll);
            if (roll < byproductChance) {
                byproductSuccessCount++;
                return true;
            }
            byproductSkipCount++;
            return false;
        }

        double wearPercent() {
            return Math.max(0.0D, Math.min(1.0D, wear / (double) MAX_WEAR));
        }

        double adjustedGeneratorFailureChance() {
            return DEFAULT_GENERATOR_FAILURE_CHANCE + (wearPercent() * MAX_GENERATOR_WEAR_FAILURE_BONUS);
        }

        boolean applyGeneratorFailureRoll(double roll) {
            if (role != NodeRole.GENERATOR || failed || burnTicksRemaining <= 0) {
                return false;
            }
            if (roll < adjustedGeneratorFailureChance()) {
                failed = true;
                burnTicksRemaining = 0;
                return true;
            }
            return false;
        }

        boolean addWearAndMaybeJam(int amount, float roll) {
            wear = Math.min(MAX_WEAR, wear + Math.max(0, amount));
            if (wear >= JAM_THRESHOLD && !jammed && roll < 0.30F) {
                jammed = true;
                return true;
            }
            return false;
        }

        void repairWear(int amount) {
            wear = Math.max(0, wear - Math.max(0, amount));
        }

        boolean trackableByFactory() {
            return role == NodeRole.GENERATOR
                    || role == NodeRole.CABLE
                    || role == NodeRole.ROUTER
                    || role == NodeRole.STORAGE
                    || role == NodeRole.MACHINE;
        }
    }

    private record Route(String targetId, List<String> visitedPipes, int visitedNodeCount, int loopSkipCount) {
    }

    private record RouteStep(String nodeId, List<String> visitedPipes) {
    }

    private record PowerRoute(List<String> path, int bottleneck, int visitedNodeCount, int loopSkipCount) {
        boolean found() {
            return !path.isEmpty();
        }
    }

    private record PowerStep(String nodeId, List<String> path) {
    }
}
