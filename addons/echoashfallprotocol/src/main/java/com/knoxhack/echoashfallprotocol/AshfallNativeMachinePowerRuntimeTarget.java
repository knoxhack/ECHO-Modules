package com.knoxhack.echoashfallprotocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallNativeMachinePowerRuntimeTarget {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final List<String> MACHINE_POWER_LOGISTICS_BLOCK_IDS = List.of(
            "echoashfallprotocol:micro_generator",
            "echoashfallprotocol:scrap_dynamo",
            "echoashfallprotocol:battery_bank",
            "echoashfallprotocol:water_purifier",
            "echoashfallprotocol:scrap_press",
            "echoashfallprotocol:ore_grinder",
            "echoashfallprotocol:isotope_refiner",
            "echoashfallprotocol:radiation_cleanser",
            "echoashfallprotocol:crystalline_synthesizer",
            "echoashfallprotocol:deep_core_miner",
            "echoashfallprotocol:autofeed_hopper",
            "echoashfallprotocol:contaminant_condenser",
            "echoashfallprotocol:thermal_burner",
            "echoashfallprotocol:item_pipe",
            "echoashfallprotocol:power_cable",
            "echoashfallprotocol:reinforced_power_cable",
            "echoashfallprotocol:high_voltage_power_cable",
            "echoashfallprotocol:load_distributor",
            "echoashfallprotocol:factory_controller");

    private AshfallNativeMachinePowerRuntimeTarget() {
    }

    static Map<String, Object> initialize(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> rehearsal = AshfallAdapterCoreMachinePowerSimulator.runDefaultPacket();
        Map<String, Object> liveRuntime = AshfallAdapterCoreMachinePowerRuntime.runDefaultScenario();
        Map<String, Object> adapterCoreDispatch = AshfallAdapterCoreMachinePowerBridge.runDefaultBridgeScenario(safeContext);
        Map<String, Object> persistenceRoundTrip = AshfallAdapterCoreMachinePowerRuntime.runPersistenceRoundTripScenario();
        Map<String, Object> cableTierRuntime = AshfallAdapterCoreMachinePowerRuntime.runCableTierScenario();
        Map<String, Object> adjacencyPowerRuntime = AshfallAdapterCoreMachinePowerRuntime.runAdjacencyPowerFlowScenario();
        Map<String, Object> priorityRoutingRuntime = AshfallAdapterCoreMachinePowerRuntime.runPriorityRoutingScenario();
        Map<String, Object> recipeCatalogRuntime = AshfallAdapterCoreMachinePowerRuntime.runRecipeCatalogScenario();
        Map<String, Object> byproductChanceRuntime = AshfallAdapterCoreMachinePowerRuntime.runByproductChanceScenario();
        Map<String, Object> outputBackpressureRuntime = AshfallAdapterCoreMachinePowerRuntime.runOutputBackpressureScenario();
        Map<String, Object> jamRepairRuntime = AshfallAdapterCoreMachinePowerRuntime.runJamRepairScenario();
        Map<String, Object> wearThresholdRuntime = AshfallAdapterCoreMachinePowerRuntime.runWearThresholdScenario();
        Map<String, Object> generatorFailureChanceRuntime = AshfallAdapterCoreMachinePowerRuntime.runGeneratorFailureChanceScenario();
        Map<String, Object> scrapDynamoRuntime = AshfallAdapterCoreMachinePowerRuntime.runScrapDynamoScenario();
        Map<String, Object> batteryBankBalancingRuntime = AshfallAdapterCoreMachinePowerRuntime.runBatteryBankBalancingScenario();
        Map<String, Object> waterPurifierRuntime = AshfallAdapterCoreMachinePowerRuntime.runWaterPurifierScenario();
        Map<String, Object> thermalBurnerRuntime = AshfallAdapterCoreMachinePowerRuntime.runThermalBurnerScenario();
        Map<String, Object> autofeedHopperRuntime = AshfallAdapterCoreMachinePowerRuntime.runAutofeedHopperScenario();
        Map<String, Object> contaminantCondenserRuntime = AshfallAdapterCoreMachinePowerRuntime.runContaminantCondenserScenario();
        Map<String, Object> isotopeRefinerRuntime = AshfallAdapterCoreMachinePowerRuntime.runIsotopeRefinerScenario();
        Map<String, Object> radiationCleanserRuntime = AshfallAdapterCoreMachinePowerRuntime.runRadiationCleanserScenario();
        Map<String, Object> crystallineSynthesizerRuntime = AshfallAdapterCoreMachinePowerRuntime.runCrystallineSynthesizerScenario();
        Map<String, Object> deepCoreMinerRuntime = AshfallAdapterCoreMachinePowerRuntime.runDeepCoreMinerScenario();
        Map<String, Object> capabilityMutationRuntime = AshfallAdapterCoreMachinePowerRuntime.runCapabilityMutationScenario();
        Map<String, Object> sidedInventoryCapabilityRuntime = AshfallAdapterCoreMachinePowerRuntime.runSidedInventoryCapabilityScenario();
        Map<String, Object> factoryControllerToggleRuntime = AshfallAdapterCoreMachinePowerRuntime.runFactoryControllerToggleScenario();
        Map<String, Object> logisticsRoutingRuntime = AshfallAdapterCoreMachinePowerRuntime.runLogisticsRoutingScenario();
        Map<String, Object> machineOutputChainingRuntime = AshfallAdapterCoreMachinePowerRuntime.runMachineOutputChainingScenario();
        List<String> diagnostics = validate(rehearsal, liveRuntime, adapterCoreDispatch, persistenceRoundTrip,
                cableTierRuntime, adjacencyPowerRuntime, priorityRoutingRuntime, recipeCatalogRuntime,
                byproductChanceRuntime, outputBackpressureRuntime, jamRepairRuntime, wearThresholdRuntime,
                generatorFailureChanceRuntime, scrapDynamoRuntime, autofeedHopperRuntime,
                contaminantCondenserRuntime, waterPurifierRuntime, thermalBurnerRuntime,
                isotopeRefinerRuntime, radiationCleanserRuntime,
                crystallineSynthesizerRuntime, deepCoreMinerRuntime, capabilityMutationRuntime, sidedInventoryCapabilityRuntime,
                factoryControllerToggleRuntime, logisticsRoutingRuntime, machineOutputChainingRuntime,
                batteryBankBalancingRuntime);
        boolean pass = diagnostics.isEmpty();

        Map<String, Object> target = new LinkedHashMap<>();
        target.put("moduleId", MODULE_ID);
        target.put("packId", safeContext.getOrDefault("packId", "unknown"));
        target.put("serviceId", "echoashfallprotocol:machine_power_runtime_target");
        target.put("adapterCoreBridge", true);
        target.put("implementationTarget", "AdapterCore runtime target");
        target.put("standaloneDuplicateGameplaySystem", false);
        target.put("runtimeStateInitialized", true);
        target.put("serviceCodeExecuted", true);
        target.put("minecraftRuntimeAccessed", false);
        target.put("minecraftRegistryMutated", false);
        target.put("unsafeRuntimeWorkStarted", false);
        target.put("registryBindings", registryBindings());
        target.put("registryBindingCount", registryBindings().size());
        target.put("tickTargets", tickTargets());
        target.put("tickTargetCount", tickTargets().size());
        target.put("eventTargets", eventTargets());
        target.put("capabilityTargets", capabilityTargets());
        target.put("rehearsal", rehearsal);
        target.put("liveRuntime", liveRuntime);
        target.put("adapterCoreDispatch", adapterCoreDispatch);
        target.put("persistenceRoundTrip", persistenceRoundTrip);
        target.put("cableTierRuntime", cableTierRuntime);
        target.put("adjacencyPowerRuntime", adjacencyPowerRuntime);
        target.put("priorityRoutingRuntime", priorityRoutingRuntime);
        target.put("recipeCatalogRuntime", recipeCatalogRuntime);
        target.put("byproductChanceRuntime", byproductChanceRuntime);
        target.put("outputBackpressureRuntime", outputBackpressureRuntime);
        target.put("jamRepairRuntime", jamRepairRuntime);
        target.put("wearThresholdRuntime", wearThresholdRuntime);
        target.put("generatorFailureChanceRuntime", generatorFailureChanceRuntime);
        target.put("scrapDynamoRuntime", scrapDynamoRuntime);
        target.put("batteryBankBalancingRuntime", batteryBankBalancingRuntime);
        target.put("waterPurifierRuntime", waterPurifierRuntime);
        target.put("thermalBurnerRuntime", thermalBurnerRuntime);
        target.put("autofeedHopperRuntime", autofeedHopperRuntime);
        target.put("contaminantCondenserRuntime", contaminantCondenserRuntime);
        target.put("isotopeRefinerRuntime", isotopeRefinerRuntime);
        target.put("radiationCleanserRuntime", radiationCleanserRuntime);
        target.put("crystallineSynthesizerRuntime", crystallineSynthesizerRuntime);
        target.put("deepCoreMinerRuntime", deepCoreMinerRuntime);
        target.put("capabilityMutationRuntime", capabilityMutationRuntime);
        target.put("sidedInventoryCapabilityRuntime", sidedInventoryCapabilityRuntime);
        target.put("factoryControllerToggleRuntime", factoryControllerToggleRuntime);
        target.put("logisticsRoutingRuntime", logisticsRoutingRuntime);
        target.put("machineOutputChainingRuntime", machineOutputChainingRuntime);
        target.put("diagnostics", diagnostics);
        target.put("status", pass ? "PASS" : "FAIL");
        target.put("summary", pass
                ? "AdapterCore runtime target executed stateful Ashfall generator, Water Purifier, Thermal Burner, cable, router, Battery Bank balancing/distribution, machine, item-pipe hopper output chaining, capability, event, and factory scan scenarios with JDK-only packets."
                : "AdapterCore runtime target rehearsal failed validation.");
        return target;
    }

    private static List<Map<String, Object>> registryBindings() {
        return List.of(
                binding("block", "echoashfallprotocol:micro_generator", "MicroGeneratorBlockEntity", "tick.power_source"),
                binding("block", "echoashfallprotocol:scrap_dynamo", "ScrapDynamoBlockEntity", "tick.scrap_fueled_power_source"),
                binding("block", "echoashfallprotocol:battery_bank", "BatteryBankBlockEntity", "tick.energy_storage"),
                binding("block", "echoashfallprotocol:water_purifier", "WaterPurifierBlockEntity", "tick.powered_purification_processor"),
                binding("block", "echoashfallprotocol:scrap_press", "ScrapPressBlockEntity", "tick.powered_machine"),
                binding("block", "echoashfallprotocol:ore_grinder", "OreGrinderBlockEntity", "tick.powered_machine"),
                binding("block", "echoashfallprotocol:isotope_refiner", "IsotopeRefinerBlockEntity", "tick.powered_catalyst_refiner"),
                binding("block", "echoashfallprotocol:radiation_cleanser", "RadiationCleanserBlockEntity", "tick.powered_decontamination_processor"),
                binding("block", "echoashfallprotocol:crystalline_synthesizer", "CrystallineSynthesizerBlockEntity", "tick.powered_phase_synthesizer"),
                binding("block", "echoashfallprotocol:deep_core_miner", "DeepCoreMinerBlockEntity", "tick.deep_depth_resource_generator"),
                binding("block", "echoashfallprotocol:autofeed_hopper", "AutofeedHopperBlockEntity", "tick.player_feed_machine"),
                binding("block", "echoashfallprotocol:contaminant_condenser", "ContaminantCondenserBlockEntity", "tick.toxic_block_condenser"),
                binding("block", "echoashfallprotocol:thermal_burner", "ThermalBurnerBlockEntity", "tick.fuel_burn_energy_and_ash_output"),
                binding("block", "echoashfallprotocol:item_pipe", "ItemPipeBlockEntity", "tick.item_router"),
                binding("block", "echoashfallprotocol:power_cable", "PowerCableBlockEntity", "tick.power_relay.basic"),
                binding("block", "echoashfallprotocol:reinforced_power_cable", "PowerCableBlockEntity", "tick.power_relay.reinforced"),
                binding("block", "echoashfallprotocol:high_voltage_power_cable", "PowerCableBlockEntity", "tick.power_relay.high_voltage"),
                binding("block", "echoashfallprotocol:load_distributor", "LoadDistributorBlockEntity", "tick.power_router"),
                binding("block", "echoashfallprotocol:factory_controller", "FactoryControllerBlockEntity", "tick.factory_scan")
        );
    }

    private static List<Map<String, Object>> tickTargets() {
        return List.of(
                tick("echoashfallprotocol:micro_generator", "fuel_burn_and_adjacent_output", 1),
                tick("echoashfallprotocol:scrap_dynamo", "scrap_fuel_burn_and_adjacent_output", 1),
                tick("echoashfallprotocol:battery_bank", "buffer_and_adjacent_distribution", 1),
                tick("echoashfallprotocol:water_purifier", "powered_purification_progress", 1),
                tick("echoashfallprotocol:scrap_press", "powered_recipe_progress", 1),
                tick("echoashfallprotocol:ore_grinder", "powered_substrate_recipe_progress", 1),
                tick("echoashfallprotocol:isotope_refiner", "powered_catalyst_recipe_progress", 1),
                tick("echoashfallprotocol:radiation_cleanser", "powered_decontamination_recipe_progress", 1),
                tick("echoashfallprotocol:crystalline_synthesizer", "powered_phase_reaction_progress", 1),
                tick("echoashfallprotocol:deep_core_miner", "depth_gated_resource_generation", 1),
                tick("echoashfallprotocol:autofeed_hopper", "powered_player_feeding", 60),
                tick("echoashfallprotocol:contaminant_condenser", "powered_toxic_puddle_conversion", 100),
                tick("echoashfallprotocol:thermal_burner", "fuel_burn_energy_and_ash_output", 1),
                tick("echoashfallprotocol:item_pipe", "hopper_endpoint_route", 8),
                tick("echoashfallprotocol:power_cable", "capacity_limited_transfer_basic", 1),
                tick("echoashfallprotocol:reinforced_power_cable", "capacity_limited_transfer_reinforced", 1),
                tick("echoashfallprotocol:high_voltage_power_cable", "capacity_limited_transfer_high_voltage", 1),
                tick("echoashfallprotocol:load_distributor", "priority_weighted_distribution", 1),
                tick("echoashfallprotocol:factory_controller", "bounded_machine_network_scan", 20)
        );
    }

    private static List<Map<String, Object>> eventTargets() {
        return List.of(
                event("player.use_block", "echoashfallprotocol:micro_generator", "restart_failed_generator"),
                event("player.use_block", "echoashfallprotocol:load_distributor", "cycle_priority_mode"),
                event("player.use_block", "echoashfallprotocol:factory_controller", "toggle_network_enabled"),
                event("neighbor.changed", "echoashfallprotocol:power_cable", "refresh_connection_state"),
                event("data.reload", "echoashfallprotocol:ore_grinder", "reload_substrate_recipe_contracts")
        );
    }

    private static List<Map<String, Object>> capabilityTargets() {
        return List.of(
                capability("energy", "IEnergyStorage-compatible", List.of(
                        "echoashfallprotocol:micro_generator",
                        "echoashfallprotocol:scrap_dynamo",
                        "echoashfallprotocol:battery_bank",
                        "echoashfallprotocol:power_cable",
                        "echoashfallprotocol:reinforced_power_cable",
                        "echoashfallprotocol:high_voltage_power_cable",
                        "echoashfallprotocol:water_purifier",
                        "echoashfallprotocol:scrap_press",
                        "echoashfallprotocol:ore_grinder",
                        "echoashfallprotocol:isotope_refiner",
                        "echoashfallprotocol:radiation_cleanser",
                        "echoashfallprotocol:crystalline_synthesizer",
                        "echoashfallprotocol:deep_core_miner",
                        "echoashfallprotocol:autofeed_hopper",
                        "echoashfallprotocol:contaminant_condenser",
                        "echoashfallprotocol:thermal_burner",
                        "echoashfallprotocol:load_distributor")),
                capability("inventory", "HopperHandler-compatible", List.of(
                        "echoashfallprotocol:water_purifier",
                        "echoashfallprotocol:scrap_press",
                        "echoashfallprotocol:ore_grinder",
                        "echoashfallprotocol:thermal_burner",
                        "echoashfallprotocol:item_pipe")),
                capability("world_scan", "bounded-adjacent-block-graph", List.of(
                        "echoashfallprotocol:factory_controller",
                        "echoashfallprotocol:power_cable",
                        "echoashfallprotocol:item_pipe"))
        );
    }

    private static List<String> validate(
            Map<String, Object> rehearsal,
            Map<String, Object> liveRuntime,
            Map<String, Object> adapterCoreDispatch,
            Map<String, Object> persistenceRoundTrip,
            Map<String, Object> cableTierRuntime,
            Map<String, Object> adjacencyPowerRuntime,
            Map<String, Object> priorityRoutingRuntime,
            Map<String, Object> recipeCatalogRuntime,
            Map<String, Object> byproductChanceRuntime,
            Map<String, Object> outputBackpressureRuntime,
            Map<String, Object> jamRepairRuntime,
            Map<String, Object> wearThresholdRuntime,
            Map<String, Object> generatorFailureChanceRuntime,
            Map<String, Object> scrapDynamoRuntime,
            Map<String, Object> autofeedHopperRuntime,
            Map<String, Object> contaminantCondenserRuntime,
            Map<String, Object> waterPurifierRuntime,
            Map<String, Object> thermalBurnerRuntime,
            Map<String, Object> isotopeRefinerRuntime,
            Map<String, Object> radiationCleanserRuntime,
            Map<String, Object> crystallineSynthesizerRuntime,
            Map<String, Object> deepCoreMinerRuntime,
            Map<String, Object> capabilityMutationRuntime,
            Map<String, Object> sidedInventoryCapabilityRuntime,
            Map<String, Object> factoryControllerToggleRuntime,
            Map<String, Object> logisticsRoutingRuntime,
            Map<String, Object> machineOutputChainingRuntime,
            Map<String, Object> batteryBankBalancingRuntime) {
        List<String> diagnostics = new java.util.ArrayList<>();
        requireIds("registryBindings", registryBindings(), MACHINE_POWER_LOGISTICS_BLOCK_IDS, diagnostics);
        requireIds("tickTargets", tickTargets(), MACHINE_POWER_LOGISTICS_BLOCK_IDS, diagnostics);
        require(rehearsal, "adapterPacketVersion", 2, diagnostics);
        require(rehearsal, "generatedEnergy", 8, diagnostics);
        require(rehearsal, "powerTransferCount", 4, diagnostics);
        require(rehearsal, "scrapPressPowerConsumed", 1, diagnostics);
        require(rehearsal, "scrapPressProgress", 1, diagnostics);
        require(rehearsal, "batteryStoredEnergy", 7, diagnostics);
        require(rehearsal, "itemPipeMovedCount", 1, diagnostics);
        require(rehearsal, "oreGrinderInputCount", 1, diagnostics);
        require(rehearsal, "powerCapacityRespected", true, diagnostics);
        require(rehearsal, "logisticsLoopAvoided", true, diagnostics);
        require(rehearsal, "networkDiagnostic", "PASS", diagnostics);
        require(rehearsal, "minecraftRuntimeAccessed", false, diagnostics);
        require(liveRuntime, "adapterPacketVersion", 3, diagnostics);
        require(liveRuntime, "adapterCoreBridge", true, diagnostics);
        require(liveRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(liveRuntime, "tickCount", 80, diagnostics);
        require(liveRuntime, "microGeneratorFuelItems", 0, diagnostics);
        require(liveRuntime, "microGeneratorBurnTicksRemaining", 80, diagnostics);
        require(liveRuntime, "microGeneratorWear", 12, diagnostics);
        require(liveRuntime, "microGeneratorFailed", false, diagnostics);
        require(liveRuntime, "loadDistributorPriorityMode", "SURVIVAL", diagnostics);
        require(liveRuntime, "batteryStoredEnergy", 440, diagnostics);
        require(liveRuntime, "scrapPressOutputCount", 1, diagnostics);
        require(liveRuntime, "scrapPressWear", 2, diagnostics);
        require(liveRuntime, "itemPipeMovedCount", 1, diagnostics);
        require(liveRuntime, "oreGrinderInputCount", 1, diagnostics);
        require(liveRuntime, "oreGrinderOutputCount", 2, diagnostics);
        require(liveRuntime, "oreGrinderByproductCount", 1, diagnostics);
        require(liveRuntime, "oreGrinderWear", 5, diagnostics);
        require(liveRuntime, "factoryScanCount", 4, diagnostics);
        require(liveRuntime, "factoryConnectedMachines", 6, diagnostics);
        require(liveRuntime, "factoryStoredEnergy", 440, diagnostics);
        require(liveRuntime, "factoryEnergyCapacity", 19500, diagnostics);
        require(liveRuntime, "factoryScanLimitRespected", true, diagnostics);
        require(liveRuntime, "powerCapacityRespected", true, diagnostics);
        require(liveRuntime, "logisticsLoopAvoided", true, diagnostics);
        require(liveRuntime, "networkDiagnostic", "PASS", diagnostics);
        require(liveRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(liveRuntime, "minecraftRegistryMutated", false, diagnostics);
        requireNested(liveRuntime, "eventBridge", "restart_failed_generator", 1, diagnostics);
        requireNested(liveRuntime, "eventBridge", "cycle_priority_mode", 1, diagnostics);
        requireNested(liveRuntime, "capabilityBridge", "energyReceiveProbe", 37, diagnostics);
        requireNested(liveRuntime, "capabilityBridge", "energyExtractProbe", 37, diagnostics);
        requireNested(liveRuntime, "capabilityBridge", "inventoryInsertProbe", 1, diagnostics);
        requireNested(liveRuntime, "capabilityBridge", "inventoryExtractProbe", 1, diagnostics);
        require(adapterCoreDispatch, "adapterCoreBridge", true, diagnostics);
        require(adapterCoreDispatch, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(adapterCoreDispatch, "status", "PASS", diagnostics);
        require(adapterCoreDispatch, "minecraftRuntimeAccessed", false, diagnostics);
        require(adapterCoreDispatch, "minecraftRegistryMutated", false, diagnostics);
        requireNested(adapterCoreDispatch, "registryBindingPhase", "requiredBindingsPresent", true, diagnostics);
        requireNested(adapterCoreDispatch, "registryBindingPhase", "minecraftRegistryMutated", false, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "dispatchedTicks", 80, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "microGeneratorTicked", true, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "powerNetworkTicked", true, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "logisticsTicked", true, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "factoryScanTicked", true, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "waterPurifierRecipeCompleted", true, diagnostics);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "thermalBurnerFuelCycleCompleted", true, diagnostics);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "handledEventCount", 5, diagnostics);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "generatorRestartHandled", true, diagnostics);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "routerPriorityChanged", true, diagnostics);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "machineRepairHandled", true, diagnostics);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "factoryToggleHandled", true, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityDispatchPhase", "energyReceiveProbe", 37, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityDispatchPhase", "energyExtractProbe", 37, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityDispatchPhase", "inventoryInsertProbe", 1, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityDispatchPhase", "inventoryExtractProbe", 1, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityMutationPhase", "status", "PASS", diagnostics);
        requireNested(adapterCoreDispatch, "capabilityMutationPhase", "capabilityStateMutated", true, diagnostics);
        requireNested(adapterCoreDispatch, "capabilityMutationPhase", "mutatingCapabilityCalls", 4, diagnostics);
        requireNested(adapterCoreDispatch, "worldStateBridge", "networkDiagnostic", "PASS", diagnostics);
        requireNested(adapterCoreDispatch, "playerStateBridge", "useBlockEventsHandled", 5, diagnostics);
        require(persistenceRoundTrip, "adapterCoreBridge", true, diagnostics);
        require(persistenceRoundTrip, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(persistenceRoundTrip, "status", "PASS", diagnostics);
        require(persistenceRoundTrip, "snapshotNodeCount", 10, diagnostics);
        require(persistenceRoundTrip, "restoredTickCount", 80, diagnostics);
        require(persistenceRoundTrip, "restoredBatteryStoredEnergy", 440, diagnostics);
        require(persistenceRoundTrip, "restoredScrapPressOutputCount", 1, diagnostics);
        require(persistenceRoundTrip, "restoredOreGrinderOutputCount", 2, diagnostics);
        require(persistenceRoundTrip, "restoredFactoryScanCount", 4, diagnostics);
        require(persistenceRoundTrip, "restoredNetworkDiagnostic", "PASS", diagnostics);
        require(persistenceRoundTrip, "minecraftRuntimeAccessed", false, diagnostics);
        require(persistenceRoundTrip, "minecraftRegistryMutated", false, diagnostics);
        require(cableTierRuntime, "adapterCoreBridge", true, diagnostics);
        require(cableTierRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(cableTierRuntime, "basicCableCapacity", 1000, diagnostics);
        require(cableTierRuntime, "basicCableTransfer", 50, diagnostics);
        require(cableTierRuntime, "reinforcedCableCapacity", 2000, diagnostics);
        require(cableTierRuntime, "reinforcedCableTransfer", 256, diagnostics);
        require(cableTierRuntime, "highVoltageCableCapacity", 4000, diagnostics);
        require(cableTierRuntime, "highVoltageCableTransfer", 1024, diagnostics);
        require(cableTierRuntime, "basicSinkEnergy", 50, diagnostics);
        require(cableTierRuntime, "reinforcedSinkEnergy", 256, diagnostics);
        require(cableTierRuntime, "highVoltageSinkEnergy", 1024, diagnostics);
        require(cableTierRuntime, "adjacencyDetected", true, diagnostics);
        require(cableTierRuntime, "powerCapacityRespected", true, diagnostics);
        require(cableTierRuntime, "networkDiagnostic", "PASS", diagnostics);
        require(cableTierRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(cableTierRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(adjacencyPowerRuntime, "adapterCoreBridge", true, diagnostics);
        require(adjacencyPowerRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(adjacencyPowerRuntime, "status", "PASS", diagnostics);
        require(adjacencyPowerRuntime, "routeCount", 3, diagnostics);
        require(adjacencyPowerRuntime, "powerTransferCount", 12, diagnostics);
        require(adjacencyPowerRuntime, "sourceEnergyAfter", 362, diagnostics);
        require(adjacencyPowerRuntime, "scrapPressEnergy", 50, diagnostics);
        require(adjacencyPowerRuntime, "oreGrinderEnergy", 50, diagnostics);
        require(adjacencyPowerRuntime, "batteryStoredEnergy", 50, diagnostics);
        require(adjacencyPowerRuntime, "basicCableTransfer", 50, diagnostics);
        require(adjacencyPowerRuntime, "reinforcedCableTransfer", 256, diagnostics);
        require(adjacencyPowerRuntime, "routerTransfer", 512, diagnostics);
        require(adjacencyPowerRuntime, "adjacencyDetected", true, diagnostics);
        require(adjacencyPowerRuntime, "generatorToConsumerFlow", true, diagnostics);
        require(adjacencyPowerRuntime, "storageFlow", true, diagnostics);
        require(adjacencyPowerRuntime, "cableTierLimitsRespected", true, diagnostics);
        require(adjacencyPowerRuntime, "loopAvoided", true, diagnostics);
        require(adjacencyPowerRuntime, "powerCapacityRespected", true, diagnostics);
        require(adjacencyPowerRuntime, "networkDiagnostic", "PASS", diagnostics);
        require(adjacencyPowerRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(adjacencyPowerRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(priorityRoutingRuntime, "adapterCoreBridge", true, diagnostics);
        require(priorityRoutingRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(priorityRoutingRuntime, "status", "PASS", diagnostics);
        require(priorityRoutingRuntime, "survivalPrioritizedScrapPress", true, diagnostics);
        require(priorityRoutingRuntime, "factoryPrioritizedOreGrinder", true, diagnostics);
        require(priorityRoutingRuntime, "gridPrioritizedBattery", true, diagnostics);
        require(priorityRoutingRuntime, "priorityModesDistinct", true, diagnostics);
        requireNested(priorityRoutingRuntime, "survivalFirst", "scrapPressProgress", 1, diagnostics);
        requireNested(priorityRoutingRuntime, "survivalFirst", "oreGrinderProgress", 0, diagnostics);
        requireNested(priorityRoutingRuntime, "factoryFirst", "scrapPressProgress", 0, diagnostics);
        requireNested(priorityRoutingRuntime, "factoryFirst", "oreGrinderProgress", 1, diagnostics);
        requireNested(priorityRoutingRuntime, "gridFirst", "batteryStoredEnergy", 2, diagnostics);
        requireNested(priorityRoutingRuntime, "gridFirst", "scrapPressProgress", 0, diagnostics);
        requireNested(priorityRoutingRuntime, "gridFirst", "oreGrinderProgress", 0, diagnostics);
        require(priorityRoutingRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(priorityRoutingRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(recipeCatalogRuntime, "adapterCoreBridge", true, diagnostics);
        require(recipeCatalogRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(recipeCatalogRuntime, "status", "PASS", diagnostics);
        requireNested(recipeCatalogRuntime, "recipeCatalog", "resourcePath",
                "data/echoashfallprotocol/adaptercore/native_machine_recipes.properties", diagnostics);
        requireNested(recipeCatalogRuntime, "recipeCatalog", "resourceLoaded", true, diagnostics);
        requireNested(recipeCatalogRuntime, "recipeCatalog", "fallbackUsed", false, diagnostics);
        require(recipeCatalogRuntime, "scrapPressRecipeCount", 1, diagnostics);
        require(recipeCatalogRuntime, "oreGrinderRecipeCount", 28, diagnostics);
        require(recipeCatalogRuntime, "executedRecipeCount", 3, diagnostics);
        require(recipeCatalogRuntime, "scrapPressOutput", "compressed_scrap", diagnostics);
        require(recipeCatalogRuntime, "scrapPressOutputCount", 1, diagnostics);
        require(recipeCatalogRuntime, "stoneGrinderOutput", "gravel", diagnostics);
        require(recipeCatalogRuntime, "stoneGrinderOutputCount", 4, diagnostics);
        require(recipeCatalogRuntime, "stoneGrinderByproduct", "flint", diagnostics);
        require(recipeCatalogRuntime, "stoneGrinderByproductCount", 1, diagnostics);
        require(recipeCatalogRuntime, "toxicGrinderOutput", "coal_dust", diagnostics);
        require(recipeCatalogRuntime, "toxicGrinderOutputCount", 2, diagnostics);
        require(recipeCatalogRuntime, "toxicGrinderByproduct", "contaminated_redstone", diagnostics);
        require(recipeCatalogRuntime, "toxicGrinderByproductCount", 1, diagnostics);
        require(recipeCatalogRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(recipeCatalogRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(byproductChanceRuntime, "adapterCoreBridge", true, diagnostics);
        require(byproductChanceRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(byproductChanceRuntime, "status", "PASS", diagnostics);
        require(byproductChanceRuntime, "recipeInput", "toxic_slagstone", diagnostics);
        require(byproductChanceRuntime, "recipeByproduct", "contaminated_redstone", diagnostics);
        require(byproductChanceRuntime, "recipeBatches", 4, diagnostics);
        require(byproductChanceRuntime, "byproductSuccesses", 2, diagnostics);
        require(byproductChanceRuntime, "byproductSkipped", 2, diagnostics);
        require(byproductChanceRuntime, "outputCount", 8, diagnostics);
        require(byproductChanceRuntime, "byproductCount", 2, diagnostics);
        require(byproductChanceRuntime, "remainingInputCount", 0, diagnostics);
        require(byproductChanceRuntime, "chanceRespected", true, diagnostics);
        require(byproductChanceRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(byproductChanceRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(outputBackpressureRuntime, "adapterCoreBridge", true, diagnostics);
        require(outputBackpressureRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(outputBackpressureRuntime, "status", "PASS", diagnostics);
        require(outputBackpressureRuntime, "blockedProgress", 0, diagnostics);
        require(outputBackpressureRuntime, "blockedEnergy", 80, diagnostics);
        require(outputBackpressureRuntime, "blockedInputCount", 9, diagnostics);
        require(outputBackpressureRuntime, "blockedOutputCount", 64, diagnostics);
        require(outputBackpressureRuntime, "extractedOutputCount", 1, diagnostics);
        require(outputBackpressureRuntime, "resumedProgress", 0, diagnostics);
        require(outputBackpressureRuntime, "resumedEnergy", 40, diagnostics);
        require(outputBackpressureRuntime, "resumedInputCount", 0, diagnostics);
        require(outputBackpressureRuntime, "resumedOutputCount", 64, diagnostics);
        require(outputBackpressureRuntime, "resumedWear", 2, diagnostics);
        require(outputBackpressureRuntime, "outputBackpressureRespected", true, diagnostics);
        require(outputBackpressureRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(outputBackpressureRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(jamRepairRuntime, "adapterCoreBridge", true, diagnostics);
        require(jamRepairRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(jamRepairRuntime, "status", "PASS", diagnostics);
        require(jamRepairRuntime, "jamPreventedProgress", 0, diagnostics);
        require(jamRepairRuntime, "repairEventHandled", true, diagnostics);
        require(jamRepairRuntime, "scrapPressJammedAfterRepair", false, diagnostics);
        require(jamRepairRuntime, "networkDiagnostic", "PASS", diagnostics);
        require(jamRepairRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(jamRepairRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(wearThresholdRuntime, "adapterCoreBridge", true, diagnostics);
        require(wearThresholdRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(wearThresholdRuntime, "status", "PASS", diagnostics);
        require(wearThresholdRuntime, "maxWear", 1000, diagnostics);
        require(wearThresholdRuntime, "jamThreshold", 800, diagnostics);
        require(wearThresholdRuntime, "repairWearReduction", 200, diagnostics);
        require(wearThresholdRuntime, "thresholdJamTriggered", true, diagnostics);
        require(wearThresholdRuntime, "wearAtJam", 800, diagnostics);
        require(wearThresholdRuntime, "jammedProgress", 0, diagnostics);
        require(wearThresholdRuntime, "jammedOutput", 0, diagnostics);
        require(wearThresholdRuntime, "jammedEnergy", 80, diagnostics);
        require(wearThresholdRuntime, "repairEventHandled", true, diagnostics);
        require(wearThresholdRuntime, "repairedWear", 600, diagnostics);
        require(wearThresholdRuntime, "repairedJammed", false, diagnostics);
        require(wearThresholdRuntime, "resumedOutput", 1, diagnostics);
        require(wearThresholdRuntime, "resumedInput", 0, diagnostics);
        require(wearThresholdRuntime, "resumedEnergy", 40, diagnostics);
        require(wearThresholdRuntime, "resumedWear", 602, diagnostics);
        require(wearThresholdRuntime, "wearRepairRespected", true, diagnostics);
        require(wearThresholdRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(wearThresholdRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(generatorFailureChanceRuntime, "adapterCoreBridge", true, diagnostics);
        require(generatorFailureChanceRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(generatorFailureChanceRuntime, "status", "PASS", diagnostics);
        require(generatorFailureChanceRuntime, "baseFailureChance", 0.0005D, diagnostics);
        require(generatorFailureChanceRuntime, "wearFailureBonusAtMaxWear", 0.005D, diagnostics);
        require(generatorFailureChanceRuntime, "wearPercent", 1.0D, diagnostics);
        require(generatorFailureChanceRuntime, "adjustedFailureChance", 0.0055D, diagnostics);
        require(generatorFailureChanceRuntime, "safeRoll", 0.006D, diagnostics);
        require(generatorFailureChanceRuntime, "safeRollFailed", false, diagnostics);
        require(generatorFailureChanceRuntime, "energyAfterSafeRoll", 8, diagnostics);
        require(generatorFailureChanceRuntime, "burnTicksAfterSafeRoll", 159, diagnostics);
        require(generatorFailureChanceRuntime, "failureRoll", 0.004D, diagnostics);
        require(generatorFailureChanceRuntime, "failureRollFailed", true, diagnostics);
        require(generatorFailureChanceRuntime, "failedAfterRoll", true, diagnostics);
        require(generatorFailureChanceRuntime, "burnTicksAfterFailure", 0, diagnostics);
        require(generatorFailureChanceRuntime, "restartEventHandled", true, diagnostics);
        require(generatorFailureChanceRuntime, "failedAfterRestart", false, diagnostics);
        require(generatorFailureChanceRuntime, "generatorFailureChanceRespected", true, diagnostics);
        require(generatorFailureChanceRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(generatorFailureChanceRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(scrapDynamoRuntime, "adapterCoreBridge", true, diagnostics);
        require(scrapDynamoRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(scrapDynamoRuntime, "status", "PASS", diagnostics);
        require(scrapDynamoRuntime, "fuelEventHandled", true, diagnostics);
        require(scrapDynamoRuntime, "fuelBurnTicks", 80, diagnostics);
        require(scrapDynamoRuntime, "burnTicksRemaining", 70, diagnostics);
        require(scrapDynamoRuntime, "energyGenerated", 240, diagnostics);
        require(scrapDynamoRuntime, "batteryStoredEnergy", 240, diagnostics);
        require(scrapDynamoRuntime, "energyCapacity", 8000, diagnostics);
        require(scrapDynamoRuntime, "transferPerTick", 256, diagnostics);
        require(scrapDynamoRuntime, "generationPerTick", 24, diagnostics);
        require(scrapDynamoRuntime, "canReceiveEnergy", false, diagnostics);
        require(scrapDynamoRuntime, "active", true, diagnostics);
        require(scrapDynamoRuntime, "networkDiagnostic", "PASS", diagnostics);
        require(scrapDynamoRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(scrapDynamoRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(batteryBankBalancingRuntime, "adapterCoreBridge", true, diagnostics);
        require(batteryBankBalancingRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(batteryBankBalancingRuntime, "status", "PASS", diagnostics);
        require(batteryBankBalancingRuntime, "batterySlot", 0, diagnostics);
        require(batteryBankBalancingRuntime, "basicBatteryCapacity", 2_000, diagnostics);
        require(batteryBankBalancingRuntime, "basicBatteryTransfer", 64, diagnostics);
        require(batteryBankBalancingRuntime, "bankEnergyCapacity", 10_000, diagnostics);
        require(batteryBankBalancingRuntime, "bankMaxTransfer", 100, diagnostics);
        require(batteryBankBalancingRuntime, "chargeMoved", 64, diagnostics);
        require(batteryBankBalancingRuntime, "dischargeMoved", 64, diagnostics);
        require(batteryBankBalancingRuntime, "chargeBankEnergyAfter", 7_936, diagnostics);
        require(batteryBankBalancingRuntime, "chargeBatteryEnergyAfter", 64, diagnostics);
        require(batteryBankBalancingRuntime, "dischargeBankEnergyAfter", 1_064, diagnostics);
        require(batteryBankBalancingRuntime, "dischargeBatteryEnergyAfter", 1_936, diagnostics);
        require(batteryBankBalancingRuntime, "adjacentTransferMoved", 100, diagnostics);
        require(batteryBankBalancingRuntime, "distributionBankEnergyAfter", 400, diagnostics);
        require(batteryBankBalancingRuntime, "consumerEnergyAfter", 100, diagnostics);
        require(batteryBankBalancingRuntime, "transferLimitRespected", true, diagnostics);
        require(batteryBankBalancingRuntime, "capacityRespected", true, diagnostics);
        require(batteryBankBalancingRuntime, "storageChanged", true, diagnostics);
        require(batteryBankBalancingRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(batteryBankBalancingRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(waterPurifierRuntime, "adapterCoreBridge", true, diagnostics);
        require(waterPurifierRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(waterPurifierRuntime, "status", "PASS", diagnostics);
        require(waterPurifierRuntime, "processTimeTicks", 60, diagnostics);
        require(waterPurifierRuntime, "powerPerBottlePerTick", 20, diagnostics);
        require(waterPurifierRuntime, "batchSize", 1, diagnostics);
        require(waterPurifierRuntime, "maxBatchSize", 3, diagnostics);
        require(waterPurifierRuntime, "energyCapacity", 1_000, diagnostics);
        require(waterPurifierRuntime, "transferPerTick", 64, diagnostics);
        require(waterPurifierRuntime, "networkRelayTransferLimit", 50, diagnostics);
        require(waterPurifierRuntime, "networkEnergyAfter", 800, diagnostics);
        require(waterPurifierRuntime, "totalPowerConsumed", 1_200, diagnostics);
        require(waterPurifierRuntime, "inputItem", "echoashfallprotocol:dirty_water_bottle", diagnostics);
        require(waterPurifierRuntime, "filterItem", "echoashfallprotocol:filter_cartridge_basic", diagnostics);
        require(waterPurifierRuntime, "outputItem", "echoashfallprotocol:clean_water_bottle", diagnostics);
        require(waterPurifierRuntime, "dirtyWaterCountAfter", 0, diagnostics);
        require(waterPurifierRuntime, "filterCountAfter", 0, diagnostics);
        require(waterPurifierRuntime, "cleanWaterCount", 1, diagnostics);
        require(waterPurifierRuntime, "filterConsumed", true, diagnostics);
        require(waterPurifierRuntime, "progressAfter", 0, diagnostics);
        require(waterPurifierRuntime, "wearCounter", 6, diagnostics);
        require(waterPurifierRuntime, "batterySlot", 3, diagnostics);
        require(waterPurifierRuntime, "canReceiveEnergy", true, diagnostics);
        require(waterPurifierRuntime, "canExtractEnergyWhenStored", true, diagnostics);
        require(waterPurifierRuntime, "survivalPriorityConsumer", true, diagnostics);
        require(waterPurifierRuntime, "active", true, diagnostics);
        require(waterPurifierRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(waterPurifierRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(thermalBurnerRuntime, "adapterCoreBridge", true, diagnostics);
        require(thermalBurnerRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(thermalBurnerRuntime, "status", "PASS", diagnostics);
        require(thermalBurnerRuntime, "processTimeTicks", 40, diagnostics);
        require(thermalBurnerRuntime, "totalTicks", 160, diagnostics);
        require(thermalBurnerRuntime, "inputItem", "minecraft:cobblestone", diagnostics);
        require(thermalBurnerRuntime, "inputCountAfter", 0, diagnostics);
        require(thermalBurnerRuntime, "acceptedAnyItem", true, diagnostics);
        require(thermalBurnerRuntime, "energyPerItem", 50, diagnostics);
        require(thermalBurnerRuntime, "energyCapacity", 1_000, diagnostics);
        require(thermalBurnerRuntime, "energyAfter", 200, diagnostics);
        require(thermalBurnerRuntime, "simulatedEnergyExtract", 64, diagnostics);
        require(thermalBurnerRuntime, "canReceiveEnergy", false, diagnostics);
        require(thermalBurnerRuntime, "canExtractEnergy", true, diagnostics);
        require(thermalBurnerRuntime, "itemsBurnedCounterAfter", 0, diagnostics);
        require(thermalBurnerRuntime, "ashOutputItem", "echoashfallprotocol:ash", diagnostics);
        require(thermalBurnerRuntime, "ashOutputCount", 1, diagnostics);
        require(thermalBurnerRuntime, "burnProgressAfter", 0, diagnostics);
        require(thermalBurnerRuntime, "wearCounter", 16, diagnostics);
        require(thermalBurnerRuntime, "batterySlot", 2, diagnostics);
        require(thermalBurnerRuntime, "active", true, diagnostics);
        require(thermalBurnerRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(thermalBurnerRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(autofeedHopperRuntime, "adapterCoreBridge", true, diagnostics);
        require(autofeedHopperRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(autofeedHopperRuntime, "status", "PASS", diagnostics);
        require(autofeedHopperRuntime, "powerCostPerFeed", 10, diagnostics);
        require(autofeedHopperRuntime, "feedIntervalTicks", 60, diagnostics);
        require(autofeedHopperRuntime, "hungerThreshold", 10, diagnostics);
        require(autofeedHopperRuntime, "feedAmount", 4, diagnostics);
        require(autofeedHopperRuntime, "fedPlayerCount", 1, diagnostics);
        require(autofeedHopperRuntime, "hungryPlayerFoodAfter", 12, diagnostics);
        require(autofeedHopperRuntime, "satiatedPlayerFoodAfter", 16, diagnostics);
        require(autofeedHopperRuntime, "energyAfter", 10, diagnostics);
        require(autofeedHopperRuntime, "lastFeedTick", 60, diagnostics);
        require(autofeedHopperRuntime, "wearCounter", 1, diagnostics);
        require(autofeedHopperRuntime, "playerStateMutated", true, diagnostics);
        require(autofeedHopperRuntime, "active", true, diagnostics);
        require(autofeedHopperRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(autofeedHopperRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(contaminantCondenserRuntime, "adapterCoreBridge", true, diagnostics);
        require(contaminantCondenserRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(contaminantCondenserRuntime, "status", "PASS", diagnostics);
        require(contaminantCondenserRuntime, "powerCostPerOperation", 50, diagnostics);
        require(contaminantCondenserRuntime, "processIntervalTicks", 100, diagnostics);
        require(contaminantCondenserRuntime, "processRadiusBlocks", 3, diagnostics);
        require(contaminantCondenserRuntime, "blocksProcessed", 1, diagnostics);
        require(contaminantCondenserRuntime, "convertedBlockBefore", "echoashfallprotocol:toxic_puddle", diagnostics);
        require(contaminantCondenserRuntime, "convertedBlockAfter", "minecraft:sand", diagnostics);
        require(contaminantCondenserRuntime, "energyAfter", 50, diagnostics);
        require(contaminantCondenserRuntime, "wearCounter", 1, diagnostics);
        require(contaminantCondenserRuntime, "remainingToxicInRange", 0L, diagnostics);
        require(contaminantCondenserRuntime, "remainingToxicOutOfRange", 1L, diagnostics);
        require(contaminantCondenserRuntime, "worldStateMutated", true, diagnostics);
        require(contaminantCondenserRuntime, "active", true, diagnostics);
        require(contaminantCondenserRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(contaminantCondenserRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(isotopeRefinerRuntime, "adapterCoreBridge", true, diagnostics);
        require(isotopeRefinerRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(isotopeRefinerRuntime, "status", "PASS", diagnostics);
        require(isotopeRefinerRuntime, "processTimeTicks", 160, diagnostics);
        require(isotopeRefinerRuntime, "powerPerOperation", 500, diagnostics);
        require(isotopeRefinerRuntime, "powerPerTick", 3, diagnostics);
        require(isotopeRefinerRuntime, "energyCapacity", 4000, diagnostics);
        require(isotopeRefinerRuntime, "transferPerTick", 256, diagnostics);
        require(isotopeRefinerRuntime, "inputItem", "minecraft:iron_ingot", diagnostics);
        require(isotopeRefinerRuntime, "catalystItem", "echoashfallprotocol:crystal_dust", diagnostics);
        require(isotopeRefinerRuntime, "contaminatedBranchSelected", true, diagnostics);
        require(isotopeRefinerRuntime, "inputCountAfter", 0, diagnostics);
        require(isotopeRefinerRuntime, "catalystCountAfter", 0, diagnostics);
        require(isotopeRefinerRuntime, "cleanOutputCount", 0, diagnostics);
        require(isotopeRefinerRuntime, "contaminatedOutputCount", 1, diagnostics);
        require(isotopeRefinerRuntime, "energyAfter", 20, diagnostics);
        require(isotopeRefinerRuntime, "progressAfter", 0, diagnostics);
        require(isotopeRefinerRuntime, "contaminationLevelAfter", 0, diagnostics);
        require(isotopeRefinerRuntime, "wearCounter", 8, diagnostics);
        require(isotopeRefinerRuntime, "active", true, diagnostics);
        require(isotopeRefinerRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(isotopeRefinerRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(radiationCleanserRuntime, "adapterCoreBridge", true, diagnostics);
        require(radiationCleanserRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(radiationCleanserRuntime, "status", "PASS", diagnostics);
        require(radiationCleanserRuntime, "totalTicks", 400, diagnostics);
        require(radiationCleanserRuntime, "powerPerTick", 8, diagnostics);
        require(radiationCleanserRuntime, "energyCapacity", 4000, diagnostics);
        require(radiationCleanserRuntime, "transferPerTick", 256, diagnostics);
        require(radiationCleanserRuntime, "inputItem", "echoashfallprotocol:contaminated_iron", diagnostics);
        require(radiationCleanserRuntime, "filterItem", "echoashfallprotocol:filter_cartridge_advanced", diagnostics);
        require(radiationCleanserRuntime, "outputItem", "minecraft:iron_ingot", diagnostics);
        require(radiationCleanserRuntime, "filterConsumed", true, diagnostics);
        require(radiationCleanserRuntime, "inputCountAfter", 0, diagnostics);
        require(radiationCleanserRuntime, "filterCountAfter", 0, diagnostics);
        require(radiationCleanserRuntime, "outputCount", 1, diagnostics);
        require(radiationCleanserRuntime, "energyAfter", 50, diagnostics);
        require(radiationCleanserRuntime, "progressAfter", 0, diagnostics);
        require(radiationCleanserRuntime, "wearLevel", 400, diagnostics);
        require(radiationCleanserRuntime, "active", true, diagnostics);
        require(radiationCleanserRuntime, "feedbackThrottled", true, diagnostics);
        require(radiationCleanserRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(radiationCleanserRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(crystallineSynthesizerRuntime, "adapterCoreBridge", true, diagnostics);
        require(crystallineSynthesizerRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(crystallineSynthesizerRuntime, "status", "PASS", diagnostics);
        require(crystallineSynthesizerRuntime, "totalTicks", 400, diagnostics);
        require(crystallineSynthesizerRuntime, "phase2StartTick", 100, diagnostics);
        require(crystallineSynthesizerRuntime, "phase3StartTick", 240, diagnostics);
        require(crystallineSynthesizerRuntime, "phase4StartTick", 360, diagnostics);
        require(crystallineSynthesizerRuntime, "energyCapacity", 8000, diagnostics);
        require(crystallineSynthesizerRuntime, "transferPerTick", 512, diagnostics);
        require(crystallineSynthesizerRuntime, "phase1PowerCost", 3, diagnostics);
        require(crystallineSynthesizerRuntime, "phase2PowerCost", 2, diagnostics);
        require(crystallineSynthesizerRuntime, "phase3PowerCost", 2, diagnostics);
        require(crystallineSynthesizerRuntime, "phase4PowerCost", 1, diagnostics);
        require(crystallineSynthesizerRuntime, "tickCalls", 402, diagnostics);
        require(crystallineSynthesizerRuntime, "powerFailureInjected", true, diagnostics);
        require(crystallineSynthesizerRuntime, "powerFailureFallbackApplied", true, diagnostics);
        require(crystallineSynthesizerRuntime, "gemFragmentCountAfter", 0, diagnostics);
        require(crystallineSynthesizerRuntime, "denseAlloyCountAfter", 0, diagnostics);
        require(crystallineSynthesizerRuntime, "energyCellCountAfter", 0, diagnostics);
        require(crystallineSynthesizerRuntime, "outputItem", "minecraft:netherite_scrap", diagnostics);
        require(crystallineSynthesizerRuntime, "outputCount", 1, diagnostics);
        require(crystallineSynthesizerRuntime, "energyAfter", 40, diagnostics);
        require(crystallineSynthesizerRuntime, "progressAfter", 0, diagnostics);
        require(crystallineSynthesizerRuntime, "phaseAfter", 0, diagnostics);
        require(crystallineSynthesizerRuntime, "wearCounter", 28, diagnostics);
        require(crystallineSynthesizerRuntime, "active", true, diagnostics);
        require(crystallineSynthesizerRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(crystallineSynthesizerRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(deepCoreMinerRuntime, "adapterCoreBridge", true, diagnostics);
        require(deepCoreMinerRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(deepCoreMinerRuntime, "status", "PASS", diagnostics);
        require(deepCoreMinerRuntime, "totalTicks", 800, diagnostics);
        require(deepCoreMinerRuntime, "powerPerTick", 40, diagnostics);
        require(deepCoreMinerRuntime, "minYLevel", -32, diagnostics);
        require(deepCoreMinerRuntime, "shallowY", -16, diagnostics);
        require(deepCoreMinerRuntime, "miningY", -40, diagnostics);
        require(deepCoreMinerRuntime, "depthGateRespected", true, diagnostics);
        require(deepCoreMinerRuntime, "energyCapacity", 12000, diagnostics);
        require(deepCoreMinerRuntime, "transferPerTick", 512, diagnostics);
        require(deepCoreMinerRuntime, "localEnergyAfter", 0, diagnostics);
        require(deepCoreMinerRuntime, "networkEnergyAfter", 0, diagnostics);
        require(deepCoreMinerRuntime, "totalPowerConsumed", 32000, diagnostics);
        require(deepCoreMinerRuntime, "progressAfter", 0, diagnostics);
        require(deepCoreMinerRuntime, "wearLevel", 40, diagnostics);
        require(deepCoreMinerRuntime, "outputItem", "echoashfallprotocol:dense_alloy_chunk", diagnostics);
        require(deepCoreMinerRuntime, "outputSlotCountAfterPush", 0, diagnostics);
        require(deepCoreMinerRuntime, "neighborInputCount", 1, diagnostics);
        require(deepCoreMinerRuntime, "pushedToNeighbor", true, diagnostics);
        require(deepCoreMinerRuntime, "inputInsertionAllowed", false, diagnostics);
        require(deepCoreMinerRuntime, "outputExtractable", true, diagnostics);
        require(deepCoreMinerRuntime, "active", true, diagnostics);
        require(deepCoreMinerRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(deepCoreMinerRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(capabilityMutationRuntime, "adapterCoreBridge", true, diagnostics);
        require(capabilityMutationRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(capabilityMutationRuntime, "status", "PASS", diagnostics);
        require(capabilityMutationRuntime, "mutatingCapabilityCalls", 4, diagnostics);
        require(capabilityMutationRuntime, "energyReceived", 64, diagnostics);
        require(capabilityMutationRuntime, "energyExtracted", 20, diagnostics);
        require(capabilityMutationRuntime, "inventoryInserted", 3, diagnostics);
        require(capabilityMutationRuntime, "inventoryExtracted", 1, diagnostics);
        require(capabilityMutationRuntime, "batteryEnergyBefore", 440, diagnostics);
        require(capabilityMutationRuntime, "batteryEnergyAfter", 484, diagnostics);
        require(capabilityMutationRuntime, "oreGrinderInputBefore", 1, diagnostics);
        require(capabilityMutationRuntime, "oreGrinderInputAfter", 4, diagnostics);
        require(capabilityMutationRuntime, "scrapPressOutputBefore", 1, diagnostics);
        require(capabilityMutationRuntime, "scrapPressOutputAfter", 0, diagnostics);
        require(capabilityMutationRuntime, "capabilityStateMutated", true, diagnostics);
        require(capabilityMutationRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(capabilityMutationRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(sidedInventoryCapabilityRuntime, "adapterCoreBridge", true, diagnostics);
        require(sidedInventoryCapabilityRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(sidedInventoryCapabilityRuntime, "status", "PASS", diagnostics);
        require(sidedInventoryCapabilityRuntime, "scrapInsertAnySide", 9, diagnostics);
        require(sidedInventoryCapabilityRuntime, "scrapInvalidInsert", 0, diagnostics);
        require(sidedInventoryCapabilityRuntime, "scrapTopExtract", 0, diagnostics);
        require(sidedInventoryCapabilityRuntime, "scrapBottomExtract", 1, diagnostics);
        require(sidedInventoryCapabilityRuntime, "grinderInsertAnySide", 2, diagnostics);
        require(sidedInventoryCapabilityRuntime, "grinderInvalidInsert", 0, diagnostics);
        require(sidedInventoryCapabilityRuntime, "grinderSideProductExtract", 0, diagnostics);
        require(sidedInventoryCapabilityRuntime, "grinderBottomProductExtract", 2, diagnostics);
        require(sidedInventoryCapabilityRuntime, "grinderBottomByproductExtract", 1, diagnostics);
        require(sidedInventoryCapabilityRuntime, "sidedRulesRespected", true, diagnostics);
        require(sidedInventoryCapabilityRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(sidedInventoryCapabilityRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(factoryControllerToggleRuntime, "adapterCoreBridge", true, diagnostics);
        require(factoryControllerToggleRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(factoryControllerToggleRuntime, "status", "PASS", diagnostics);
        require(factoryControllerToggleRuntime, "initialScanCount", 1, diagnostics);
        require(factoryControllerToggleRuntime, "initialConnectedMachines", 6, diagnostics);
        require(factoryControllerToggleRuntime, "disableEventHandled", true, diagnostics);
        require(factoryControllerToggleRuntime, "disabledState", true, diagnostics);
        require(factoryControllerToggleRuntime, "disabledScanCount", 1, diagnostics);
        require(factoryControllerToggleRuntime, "enableEventHandled", true, diagnostics);
        require(factoryControllerToggleRuntime, "enabledState", true, diagnostics);
        require(factoryControllerToggleRuntime, "resumedScanCount", 2, diagnostics);
        require(factoryControllerToggleRuntime, "resumedConnectedMachines", 6, diagnostics);
        require(factoryControllerToggleRuntime, "toggleEventCount", 2, diagnostics);
        require(factoryControllerToggleRuntime, "scanLimitRespected", true, diagnostics);
        require(factoryControllerToggleRuntime, "scanToggleRespected", true, diagnostics);
        require(factoryControllerToggleRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(factoryControllerToggleRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(logisticsRoutingRuntime, "adapterCoreBridge", true, diagnostics);
        require(logisticsRoutingRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(logisticsRoutingRuntime, "status", "PASS", diagnostics);
        require(logisticsRoutingRuntime, "hopperSourceCount", 2, diagnostics);
        require(logisticsRoutingRuntime, "routeTransferCount", 2, diagnostics);
        require(logisticsRoutingRuntime, "scrapSourceOutputAfter", 0, diagnostics);
        require(logisticsRoutingRuntime, "oreSourceOutputAfter", 0, diagnostics);
        require(logisticsRoutingRuntime, "scrapPressInputAfter", 10, diagnostics);
        require(logisticsRoutingRuntime, "oreGrinderInputAfter", 5, diagnostics);
        require(logisticsRoutingRuntime, "loopAvoided", true, diagnostics);
        require(logisticsRoutingRuntime, "validMachineInputsSelected", true, diagnostics);
        require(logisticsRoutingRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(logisticsRoutingRuntime, "minecraftRegistryMutated", false, diagnostics);
        require(machineOutputChainingRuntime, "adapterCoreBridge", true, diagnostics);
        require(machineOutputChainingRuntime, "standaloneDuplicateGameplaySystem", false, diagnostics);
        require(machineOutputChainingRuntime, "status", "PASS", diagnostics);
        require(machineOutputChainingRuntime, "machineOutputSourceCount", 1, diagnostics);
        require(machineOutputChainingRuntime, "routeTransferCount", 2, diagnostics);
        require(machineOutputChainingRuntime, "sourceOutputItem", "scrap_metal", diagnostics);
        require(machineOutputChainingRuntime, "sourceOutputSide", "DOWN", diagnostics);
        require(machineOutputChainingRuntime, "sourceOutputAfter", 0, diagnostics);
        require(machineOutputChainingRuntime, "targetInputAfter", 10, diagnostics);
        require(machineOutputChainingRuntime, "targetInputItem", "scrap_metal", diagnostics);
        require(machineOutputChainingRuntime, "loopAvoided", true, diagnostics);
        require(machineOutputChainingRuntime, "hopperOutputPulled", true, diagnostics);
        require(machineOutputChainingRuntime, "validMachineInputSelected", true, diagnostics);
        require(machineOutputChainingRuntime, "minecraftRuntimeAccessed", false, diagnostics);
        require(machineOutputChainingRuntime, "minecraftRegistryMutated", false, diagnostics);
        return List.copyOf(diagnostics);
    }

    private static Map<String, Object> binding(String registry, String id, String sourceClass, String target) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("registry", registry);
        data.put("id", id);
        data.put("sourceClass", sourceClass);
        data.put("adapterTarget", target);
        data.put("minecraftRegistryMutated", false);
        return data;
    }

    private static Map<String, Object> tick(String id, String behavior, int intervalTicks) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("behavior", behavior);
        data.put("intervalTicks", intervalTicks);
        data.put("adapterSurface", "block_entity.tick");
        return data;
    }

    private static Map<String, Object> event(String event, String id, String behavior) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", event);
        data.put("id", id);
        data.put("behavior", behavior);
        data.put("adapterSurface", "event.dispatch");
        return data;
    }

    private static Map<String, Object> capability(String capability, String adapterContract, List<String> providers) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("capability", capability);
        data.put("adapterContract", adapterContract);
        data.put("providers", providers);
        data.put("adapterSurface", "capability.bridge");
        return data;
    }

    private static void requireIds(
            String label,
            List<Map<String, Object>> entries,
            List<String> expectedIds,
            List<String> diagnostics) {
        List<String> actualIds = entries.stream()
                .map(entry -> (String) entry.get("id"))
                .toList();
        for (String expectedId : expectedIds) {
            if (!actualIds.contains(expectedId)) {
                diagnostics.add("Expected " + label + " to include " + expectedId + ".");
            }
        }
        if (actualIds.size() != expectedIds.size()) {
            diagnostics.add("Expected " + label + " count=" + expectedIds.size()
                    + " but found " + actualIds.size() + ".");
        }
    }

    private static void require(Map<String, Object> data, String key, Object expected, List<String> diagnostics) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            diagnostics.add("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }

    @SuppressWarnings("unchecked")
    private static void requireNested(Map<String, Object> data, String parentKey, String key, Object expected, List<String> diagnostics) {
        Object parent = data.get(parentKey);
        if (!(parent instanceof Map<?, ?> parentMap)) {
            diagnostics.add("Expected " + parentKey + " to be a map but found " + parent + ".");
            return;
        }
        Object actual = ((Map<String, Object>) parentMap).get(key);
        if (!expected.equals(actual)) {
            diagnostics.add("Expected " + parentKey + "." + key + "=" + expected + " but found " + actual + ".");
        }
    }
}
