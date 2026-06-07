package com.knoxhack.echoashfallprotocol;

import java.util.List;
import java.util.Map;

public final class AshfallAdapterCoreMachinePowerRuntimeVerifier {
    private AshfallAdapterCoreMachinePowerRuntimeVerifier() {
    }

    public static void main(String[] args) {
        AshfallAdapterCoreMachinePowerRuntime runtime = AshfallAdapterCoreMachinePowerRuntime.createDefaultWorld();
        runtime.forceMachineFailure("micro_generator");
        require(runtime.handleInteraction("player.use_block", "micro_generator"), "handled", true);
        require(runtime.handleInteraction("player.use_block", "load_distributor"), "handled", true);

        Map<String, Object> result = runtime.tick(80);
        require(result, "adapterPacketVersion", 3);
        require(result, "adapterCoreBridge", true);
        require(result, "standaloneDuplicateGameplaySystem", false);
        require(result, "tickCount", 80);
        require(result, "microGeneratorFuelItems", 0);
        require(result, "microGeneratorBurnTicksRemaining", 80);
        require(result, "microGeneratorWear", 12);
        require(result, "microGeneratorFailed", false);
        require(result, "loadDistributorPriorityMode", "SURVIVAL");
        require(result, "batteryStoredEnergy", 440);
        require(result, "scrapPressOutputCount", 1);
        require(result, "scrapPressWear", 2);
        require(result, "itemPipeMovedCount", 1);
        require(result, "oreGrinderInputCount", 1);
        require(result, "oreGrinderOutputCount", 2);
        require(result, "oreGrinderByproductCount", 1);
        require(result, "oreGrinderWear", 5);
        require(result, "factoryScanCount", 4);
        require(result, "factoryConnectedMachines", 6);
        require(result, "factoryStoredEnergy", 440);
        require(result, "factoryEnergyCapacity", 19500);
        require(result, "factoryScanLimitRespected", true);
        require(result, "powerCapacityRespected", true);
        require(result, "logisticsLoopAvoided", true);
        require(result, "networkDiagnostic", "PASS");
        require(result, "minecraftRuntimeAccessed", false);
        require(result, "minecraftRegistryMutated", false);
        requireIds(result, "registeredBlockEntityBindings", List.of(
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:thermal_burner",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:rain_collector",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:thermal_array",
                "echoashfallprotocol:battery_bank",
                "echoashfallprotocol:scrap_dynamo",
                "echoashfallprotocol:nexus_capacitor",
                "echoashfallprotocol:load_distributor",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:signal_scanner",
                "echoashfallprotocol:field_med_bay",
                "echoashfallprotocol:atmospheric_scrubber",
                "echoashfallprotocol:autofeed_hopper",
                "echoashfallprotocol:contaminant_condenser",
                "echoashfallprotocol:filter_workbench",
                "echoashfallprotocol:power_node",
                "echoashfallprotocol:nexus_core",
                "echoashfallprotocol:ore_grinder",
                "echoashfallprotocol:isotope_refiner",
                "echoashfallprotocol:radiation_cleanser",
                "echoashfallprotocol:crystalline_synthesizer",
                "echoashfallprotocol:deep_core_miner",
                "echoashfallprotocol:item_pipe",
                "echoashfallprotocol:power_cable",
                "echoashfallprotocol:reinforced_power_cable",
                "echoashfallprotocol:high_voltage_power_cable",
                "echoashfallprotocol:factory_controller",
                "echoashfallprotocol:structure_cache",
                "echoashfallprotocol:echo_container"));

        Object capability = result.get("capabilityBridge");
        if (!(capability instanceof Map<?, ?> capabilityMap)) {
            throw new IllegalStateException("Expected capability bridge evidence.");
        }
        require(capabilityMap, "energyReceiveProbe", 37);
        require(capabilityMap, "energyExtractProbe", 37);
        require(capabilityMap, "inventoryInsertProbe", 1);
        require(capabilityMap, "inventoryExtractProbe", 1);

        Map<String, Object> persistence = AshfallAdapterCoreMachinePowerRuntime.runPersistenceRoundTripScenario();
        require(persistence, "adapterCoreBridge", true);
        require(persistence, "standaloneDuplicateGameplaySystem", false);
        require(persistence, "status", "PASS");
        require(persistence, "snapshotNodeCount", 10);
        require(persistence, "restoredTickCount", 80);
        require(persistence, "restoredBatteryStoredEnergy", 440);
        require(persistence, "restoredScrapPressOutputCount", 1);
        require(persistence, "restoredOreGrinderOutputCount", 2);
        require(persistence, "restoredFactoryScanCount", 4);
        require(persistence, "restoredNetworkDiagnostic", "PASS");
        require(persistence, "minecraftRuntimeAccessed", false);
        require(persistence, "minecraftRegistryMutated", false);

        Map<String, Object> cableTiers = AshfallAdapterCoreMachinePowerRuntime.runCableTierScenario();
        require(cableTiers, "adapterCoreBridge", true);
        require(cableTiers, "standaloneDuplicateGameplaySystem", false);
        require(cableTiers, "basicCableCapacity", 1000);
        require(cableTiers, "basicCableTransfer", 50);
        require(cableTiers, "reinforcedCableCapacity", 2000);
        require(cableTiers, "reinforcedCableTransfer", 256);
        require(cableTiers, "highVoltageCableCapacity", 4000);
        require(cableTiers, "highVoltageCableTransfer", 1024);
        require(cableTiers, "basicSinkEnergy", 50);
        require(cableTiers, "reinforcedSinkEnergy", 256);
        require(cableTiers, "highVoltageSinkEnergy", 1024);
        require(cableTiers, "adjacencyDetected", true);
        require(cableTiers, "powerCapacityRespected", true);
        require(cableTiers, "networkDiagnostic", "PASS");
        require(cableTiers, "minecraftRuntimeAccessed", false);
        require(cableTiers, "minecraftRegistryMutated", false);

        Map<String, Object> adjacencyPower = AshfallAdapterCoreMachinePowerRuntime.runAdjacencyPowerFlowScenario();
        require(adjacencyPower, "adapterCoreBridge", true);
        require(adjacencyPower, "standaloneDuplicateGameplaySystem", false);
        require(adjacencyPower, "status", "PASS");
        require(adjacencyPower, "routeCount", 3);
        require(adjacencyPower, "powerTransferCount", 12);
        require(adjacencyPower, "sourceEnergyAfter", 362);
        require(adjacencyPower, "scrapPressEnergy", 50);
        require(adjacencyPower, "oreGrinderEnergy", 50);
        require(adjacencyPower, "batteryStoredEnergy", 50);
        require(adjacencyPower, "basicCableTransfer", 50);
        require(adjacencyPower, "reinforcedCableTransfer", 256);
        require(adjacencyPower, "routerTransfer", 512);
        require(adjacencyPower, "adjacencyDetected", true);
        require(adjacencyPower, "generatorToConsumerFlow", true);
        require(adjacencyPower, "storageFlow", true);
        require(adjacencyPower, "cableTierLimitsRespected", true);
        require(adjacencyPower, "loopAvoided", true);
        require(adjacencyPower, "powerCapacityRespected", true);
        require(adjacencyPower, "networkDiagnostic", "PASS");
        require(adjacencyPower, "minecraftRuntimeAccessed", false);
        require(adjacencyPower, "minecraftRegistryMutated", false);

        Map<String, Object> priorityRouting = AshfallAdapterCoreMachinePowerRuntime.runPriorityRoutingScenario();
        require(priorityRouting, "adapterCoreBridge", true);
        require(priorityRouting, "standaloneDuplicateGameplaySystem", false);
        require(priorityRouting, "status", "PASS");
        require(priorityRouting, "survivalPrioritizedScrapPress", true);
        require(priorityRouting, "factoryPrioritizedOreGrinder", true);
        require(priorityRouting, "gridPrioritizedBattery", true);
        require(priorityRouting, "priorityModesDistinct", true);
        requireNested(priorityRouting, "survivalFirst", "scrapPressProgress", 1);
        requireNested(priorityRouting, "survivalFirst", "oreGrinderProgress", 0);
        requireNested(priorityRouting, "factoryFirst", "scrapPressProgress", 0);
        requireNested(priorityRouting, "factoryFirst", "oreGrinderProgress", 1);
        requireNested(priorityRouting, "gridFirst", "batteryStoredEnergy", 2);
        requireNested(priorityRouting, "gridFirst", "scrapPressProgress", 0);
        requireNested(priorityRouting, "gridFirst", "oreGrinderProgress", 0);
        require(priorityRouting, "minecraftRuntimeAccessed", false);
        require(priorityRouting, "minecraftRegistryMutated", false);

        Map<String, Object> recipes = AshfallAdapterCoreMachinePowerRuntime.runRecipeCatalogScenario();
        require(recipes, "adapterCoreBridge", true);
        require(recipes, "standaloneDuplicateGameplaySystem", false);
        require(recipes, "status", "PASS");
        require(recipes, "scrapPressRecipeCount", 1);
        require(recipes, "oreGrinderRecipeCount", 28);
        require(recipes, "executedRecipeCount", 3);
        require(recipes, "scrapPressOutput", "compressed_scrap");
        require(recipes, "scrapPressOutputCount", 1);
        require(recipes, "stoneGrinderOutput", "gravel");
        require(recipes, "stoneGrinderOutputCount", 4);
        require(recipes, "stoneGrinderByproduct", "flint");
        require(recipes, "stoneGrinderByproductCount", 1);
        require(recipes, "toxicGrinderOutput", "coal_dust");
        require(recipes, "toxicGrinderOutputCount", 2);
        require(recipes, "toxicGrinderByproduct", "contaminated_redstone");
        require(recipes, "toxicGrinderByproductCount", 1);
        require(recipes, "minecraftRuntimeAccessed", false);
        require(recipes, "minecraftRegistryMutated", false);

        Map<String, Object> byproductChance = AshfallAdapterCoreMachinePowerRuntime.runByproductChanceScenario();
        require(byproductChance, "adapterCoreBridge", true);
        require(byproductChance, "standaloneDuplicateGameplaySystem", false);
        require(byproductChance, "status", "PASS");
        require(byproductChance, "recipeInput", "toxic_slagstone");
        require(byproductChance, "recipeByproduct", "contaminated_redstone");
        require(byproductChance, "recipeBatches", 4);
        require(byproductChance, "byproductSuccesses", 2);
        require(byproductChance, "byproductSkipped", 2);
        require(byproductChance, "outputCount", 8);
        require(byproductChance, "byproductCount", 2);
        require(byproductChance, "remainingInputCount", 0);
        require(byproductChance, "chanceRespected", true);
        require(byproductChance, "minecraftRuntimeAccessed", false);
        require(byproductChance, "minecraftRegistryMutated", false);

        Map<String, Object> outputBackpressure = AshfallAdapterCoreMachinePowerRuntime.runOutputBackpressureScenario();
        require(outputBackpressure, "adapterCoreBridge", true);
        require(outputBackpressure, "standaloneDuplicateGameplaySystem", false);
        require(outputBackpressure, "status", "PASS");
        require(outputBackpressure, "blockedProgress", 0);
        require(outputBackpressure, "blockedEnergy", 80);
        require(outputBackpressure, "blockedInputCount", 9);
        require(outputBackpressure, "blockedOutputCount", 64);
        require(outputBackpressure, "extractedOutputCount", 1);
        require(outputBackpressure, "resumedProgress", 0);
        require(outputBackpressure, "resumedEnergy", 40);
        require(outputBackpressure, "resumedInputCount", 0);
        require(outputBackpressure, "resumedOutputCount", 64);
        require(outputBackpressure, "resumedWear", 2);
        require(outputBackpressure, "outputBackpressureRespected", true);
        require(outputBackpressure, "minecraftRuntimeAccessed", false);
        require(outputBackpressure, "minecraftRegistryMutated", false);

        Map<String, Object> jamRepair = AshfallAdapterCoreMachinePowerRuntime.runJamRepairScenario();
        require(jamRepair, "adapterCoreBridge", true);
        require(jamRepair, "standaloneDuplicateGameplaySystem", false);
        require(jamRepair, "status", "PASS");
        require(jamRepair, "jamPreventedProgress", 0);
        require(jamRepair, "repairEventHandled", true);
        require(jamRepair, "scrapPressJammedAfterRepair", false);
        require(jamRepair, "networkDiagnostic", "PASS");
        require(jamRepair, "minecraftRuntimeAccessed", false);
        require(jamRepair, "minecraftRegistryMutated", false);

        Map<String, Object> wearThreshold = AshfallAdapterCoreMachinePowerRuntime.runWearThresholdScenario();
        require(wearThreshold, "adapterCoreBridge", true);
        require(wearThreshold, "standaloneDuplicateGameplaySystem", false);
        require(wearThreshold, "status", "PASS");
        require(wearThreshold, "maxWear", 1000);
        require(wearThreshold, "jamThreshold", 800);
        require(wearThreshold, "repairWearReduction", 200);
        require(wearThreshold, "thresholdJamTriggered", true);
        require(wearThreshold, "wearAtJam", 800);
        require(wearThreshold, "jammedProgress", 0);
        require(wearThreshold, "jammedOutput", 0);
        require(wearThreshold, "jammedEnergy", 80);
        require(wearThreshold, "repairEventHandled", true);
        require(wearThreshold, "repairedWear", 600);
        require(wearThreshold, "repairedJammed", false);
        require(wearThreshold, "resumedOutput", 1);
        require(wearThreshold, "resumedInput", 0);
        require(wearThreshold, "resumedEnergy", 40);
        require(wearThreshold, "resumedWear", 602);
        require(wearThreshold, "wearRepairRespected", true);
        require(wearThreshold, "minecraftRuntimeAccessed", false);
        require(wearThreshold, "minecraftRegistryMutated", false);

        Map<String, Object> generatorFailure = AshfallAdapterCoreMachinePowerRuntime.runGeneratorFailureChanceScenario();
        require(generatorFailure, "adapterCoreBridge", true);
        require(generatorFailure, "standaloneDuplicateGameplaySystem", false);
        require(generatorFailure, "status", "PASS");
        require(generatorFailure, "baseFailureChance", 0.0005D);
        require(generatorFailure, "wearFailureBonusAtMaxWear", 0.005D);
        require(generatorFailure, "wearPercent", 1.0D);
        require(generatorFailure, "adjustedFailureChance", 0.0055D);
        require(generatorFailure, "safeRoll", 0.006D);
        require(generatorFailure, "safeRollFailed", false);
        require(generatorFailure, "energyAfterSafeRoll", 8);
        require(generatorFailure, "burnTicksAfterSafeRoll", 159);
        require(generatorFailure, "failureRoll", 0.004D);
        require(generatorFailure, "failureRollFailed", true);
        require(generatorFailure, "failedAfterRoll", true);
        require(generatorFailure, "burnTicksAfterFailure", 0);
        require(generatorFailure, "restartEventHandled", true);
        require(generatorFailure, "failedAfterRestart", false);
        require(generatorFailure, "generatorFailureChanceRespected", true);
        require(generatorFailure, "minecraftRuntimeAccessed", false);
        require(generatorFailure, "minecraftRegistryMutated", false);

        Map<String, Object> capabilityMutation = AshfallAdapterCoreMachinePowerRuntime.runCapabilityMutationScenario();
        require(capabilityMutation, "adapterCoreBridge", true);
        require(capabilityMutation, "standaloneDuplicateGameplaySystem", false);
        require(capabilityMutation, "status", "PASS");
        require(capabilityMutation, "mutatingCapabilityCalls", 4);
        require(capabilityMutation, "energyReceived", 64);
        require(capabilityMutation, "energyExtracted", 20);
        require(capabilityMutation, "inventoryInserted", 3);
        require(capabilityMutation, "inventoryExtracted", 1);
        require(capabilityMutation, "batteryEnergyBefore", 440);
        require(capabilityMutation, "batteryEnergyAfter", 484);
        require(capabilityMutation, "oreGrinderInputBefore", 1);
        require(capabilityMutation, "oreGrinderInputAfter", 4);
        require(capabilityMutation, "scrapPressOutputBefore", 1);
        require(capabilityMutation, "scrapPressOutputAfter", 0);
        require(capabilityMutation, "capabilityStateMutated", true);
        require(capabilityMutation, "minecraftRuntimeAccessed", false);
        require(capabilityMutation, "minecraftRegistryMutated", false);

        Map<String, Object> sidedInventory = AshfallAdapterCoreMachinePowerRuntime.runSidedInventoryCapabilityScenario();
        require(sidedInventory, "adapterCoreBridge", true);
        require(sidedInventory, "standaloneDuplicateGameplaySystem", false);
        require(sidedInventory, "status", "PASS");
        require(sidedInventory, "scrapInsertAnySide", 9);
        require(sidedInventory, "scrapInvalidInsert", 0);
        require(sidedInventory, "scrapTopExtract", 0);
        require(sidedInventory, "scrapBottomExtract", 1);
        require(sidedInventory, "grinderInsertAnySide", 2);
        require(sidedInventory, "grinderInvalidInsert", 0);
        require(sidedInventory, "grinderSideProductExtract", 0);
        require(sidedInventory, "grinderBottomProductExtract", 2);
        require(sidedInventory, "grinderBottomByproductExtract", 1);
        require(sidedInventory, "sidedRulesRespected", true);
        require(sidedInventory, "minecraftRuntimeAccessed", false);
        require(sidedInventory, "minecraftRegistryMutated", false);

        Map<String, Object> factoryToggle = AshfallAdapterCoreMachinePowerRuntime.runFactoryControllerToggleScenario();
        require(factoryToggle, "adapterCoreBridge", true);
        require(factoryToggle, "standaloneDuplicateGameplaySystem", false);
        require(factoryToggle, "status", "PASS");
        require(factoryToggle, "initialScanCount", 1);
        require(factoryToggle, "initialConnectedMachines", 6);
        require(factoryToggle, "disableEventHandled", true);
        require(factoryToggle, "disabledState", true);
        require(factoryToggle, "disabledScanCount", 1);
        require(factoryToggle, "enableEventHandled", true);
        require(factoryToggle, "enabledState", true);
        require(factoryToggle, "resumedScanCount", 2);
        require(factoryToggle, "resumedConnectedMachines", 6);
        require(factoryToggle, "toggleEventCount", 2);
        require(factoryToggle, "scanLimitRespected", true);
        require(factoryToggle, "scanToggleRespected", true);
        require(factoryToggle, "minecraftRuntimeAccessed", false);
        require(factoryToggle, "minecraftRegistryMutated", false);

        Map<String, Object> logisticsRouting = AshfallAdapterCoreMachinePowerRuntime.runLogisticsRoutingScenario();
        require(logisticsRouting, "adapterCoreBridge", true);
        require(logisticsRouting, "standaloneDuplicateGameplaySystem", false);
        require(logisticsRouting, "status", "PASS");
        require(logisticsRouting, "hopperSourceCount", 2);
        require(logisticsRouting, "routeTransferCount", 2);
        require(logisticsRouting, "scrapSourceOutputAfter", 0);
        require(logisticsRouting, "oreSourceOutputAfter", 0);
        require(logisticsRouting, "scrapPressInputAfter", 10);
        require(logisticsRouting, "oreGrinderInputAfter", 5);
        require(logisticsRouting, "loopAvoided", true);
        require(logisticsRouting, "validMachineInputsSelected", true);
        require(logisticsRouting, "minecraftRuntimeAccessed", false);
        require(logisticsRouting, "minecraftRegistryMutated", false);

        Map<String, Object> machineOutputChaining = AshfallAdapterCoreMachinePowerRuntime.runMachineOutputChainingScenario();
        require(machineOutputChaining, "adapterCoreBridge", true);
        require(machineOutputChaining, "standaloneDuplicateGameplaySystem", false);
        require(machineOutputChaining, "status", "PASS");
        require(machineOutputChaining, "machineOutputSourceCount", 1);
        require(machineOutputChaining, "routeTransferCount", 2);
        require(machineOutputChaining, "sourceOutputItem", "scrap_metal");
        require(machineOutputChaining, "sourceOutputSide", "DOWN");
        require(machineOutputChaining, "sourceOutputAfter", 0);
        require(machineOutputChaining, "targetInputAfter", 10);
        require(machineOutputChaining, "targetInputItem", "scrap_metal");
        require(machineOutputChaining, "loopAvoided", true);
        require(machineOutputChaining, "hopperOutputPulled", true);
        require(machineOutputChaining, "validMachineInputSelected", true);
        require(machineOutputChaining, "minecraftRuntimeAccessed", false);
        require(machineOutputChaining, "minecraftRegistryMutated", false);

        Map<String, Object> scrapDynamo = AshfallAdapterCoreMachinePowerRuntime.runScrapDynamoScenario();
        require(scrapDynamo, "adapterCoreBridge", true);
        require(scrapDynamo, "standaloneDuplicateGameplaySystem", false);
        require(scrapDynamo, "status", "PASS");
        require(scrapDynamo, "fuelEventHandled", true);
        require(scrapDynamo, "fuelBurnTicks", 80);
        require(scrapDynamo, "burnTicksRemaining", 70);
        require(scrapDynamo, "energyGenerated", 240);
        require(scrapDynamo, "batteryStoredEnergy", 240);
        require(scrapDynamo, "energyCapacity", 8000);
        require(scrapDynamo, "transferPerTick", 256);
        require(scrapDynamo, "generationPerTick", 24);
        require(scrapDynamo, "canReceiveEnergy", false);
        require(scrapDynamo, "active", true);
        require(scrapDynamo, "networkDiagnostic", "PASS");
        require(scrapDynamo, "minecraftRuntimeAccessed", false);
        require(scrapDynamo, "minecraftRegistryMutated", false);

        Map<String, Object> batteryBankBalancing = AshfallAdapterCoreMachinePowerRuntime.runBatteryBankBalancingScenario();
        require(batteryBankBalancing, "adapterCoreBridge", true);
        require(batteryBankBalancing, "standaloneDuplicateGameplaySystem", false);
        require(batteryBankBalancing, "status", "PASS");
        require(batteryBankBalancing, "batterySlot", 0);
        require(batteryBankBalancing, "basicBatteryCapacity", 2_000);
        require(batteryBankBalancing, "basicBatteryTransfer", 64);
        require(batteryBankBalancing, "bankEnergyCapacity", 10_000);
        require(batteryBankBalancing, "bankMaxTransfer", 100);
        require(batteryBankBalancing, "chargeMoved", 64);
        require(batteryBankBalancing, "dischargeMoved", 64);
        require(batteryBankBalancing, "chargeBankEnergyAfter", 7_936);
        require(batteryBankBalancing, "chargeBatteryEnergyAfter", 64);
        require(batteryBankBalancing, "dischargeBankEnergyAfter", 1_064);
        require(batteryBankBalancing, "dischargeBatteryEnergyAfter", 1_936);
        require(batteryBankBalancing, "adjacentTransferMoved", 100);
        require(batteryBankBalancing, "distributionBankEnergyAfter", 400);
        require(batteryBankBalancing, "consumerEnergyAfter", 100);
        require(batteryBankBalancing, "transferLimitRespected", true);
        require(batteryBankBalancing, "capacityRespected", true);
        require(batteryBankBalancing, "storageChanged", true);
        require(batteryBankBalancing, "minecraftRuntimeAccessed", false);
        require(batteryBankBalancing, "minecraftRegistryMutated", false);

        Map<String, Object> autofeedHopper = AshfallAdapterCoreMachinePowerRuntime.runAutofeedHopperScenario();
        require(autofeedHopper, "adapterCoreBridge", true);
        require(autofeedHopper, "standaloneDuplicateGameplaySystem", false);
        require(autofeedHopper, "status", "PASS");
        require(autofeedHopper, "powerCostPerFeed", 10);
        require(autofeedHopper, "feedIntervalTicks", 60);
        require(autofeedHopper, "hungerThreshold", 10);
        require(autofeedHopper, "feedAmount", 4);
        require(autofeedHopper, "fedPlayerCount", 1);
        require(autofeedHopper, "hungryPlayerFoodAfter", 12);
        require(autofeedHopper, "satiatedPlayerFoodAfter", 16);
        require(autofeedHopper, "energyAfter", 10);
        require(autofeedHopper, "lastFeedTick", 60);
        require(autofeedHopper, "wearCounter", 1);
        require(autofeedHopper, "playerStateMutated", true);
        require(autofeedHopper, "active", true);
        require(autofeedHopper, "minecraftRuntimeAccessed", false);
        require(autofeedHopper, "minecraftRegistryMutated", false);

        Map<String, Object> contaminantCondenser = AshfallAdapterCoreMachinePowerRuntime.runContaminantCondenserScenario();
        require(contaminantCondenser, "adapterCoreBridge", true);
        require(contaminantCondenser, "standaloneDuplicateGameplaySystem", false);
        require(contaminantCondenser, "status", "PASS");
        require(contaminantCondenser, "powerCostPerOperation", 50);
        require(contaminantCondenser, "processIntervalTicks", 100);
        require(contaminantCondenser, "processRadiusBlocks", 3);
        require(contaminantCondenser, "blocksProcessed", 1);
        require(contaminantCondenser, "convertedBlockBefore", "echoashfallprotocol:toxic_puddle");
        require(contaminantCondenser, "convertedBlockAfter", "minecraft:sand");
        require(contaminantCondenser, "energyAfter", 50);
        require(contaminantCondenser, "wearCounter", 1);
        require(contaminantCondenser, "remainingToxicInRange", 0L);
        require(contaminantCondenser, "remainingToxicOutOfRange", 1L);
        require(contaminantCondenser, "worldStateMutated", true);
        require(contaminantCondenser, "active", true);
        require(contaminantCondenser, "minecraftRuntimeAccessed", false);
        require(contaminantCondenser, "minecraftRegistryMutated", false);

        Map<String, Object> isotopeRefiner = AshfallAdapterCoreMachinePowerRuntime.runIsotopeRefinerScenario();
        require(isotopeRefiner, "adapterCoreBridge", true);
        require(isotopeRefiner, "standaloneDuplicateGameplaySystem", false);
        require(isotopeRefiner, "status", "PASS");
        require(isotopeRefiner, "processTimeTicks", 160);
        require(isotopeRefiner, "powerPerOperation", 500);
        require(isotopeRefiner, "powerPerTick", 3);
        require(isotopeRefiner, "energyCapacity", 4000);
        require(isotopeRefiner, "transferPerTick", 256);
        require(isotopeRefiner, "inputItem", "minecraft:iron_ingot");
        require(isotopeRefiner, "catalystItem", "echoashfallprotocol:crystal_dust");
        require(isotopeRefiner, "contaminatedBranchSelected", true);
        require(isotopeRefiner, "inputCountAfter", 0);
        require(isotopeRefiner, "catalystCountAfter", 0);
        require(isotopeRefiner, "cleanOutputCount", 0);
        require(isotopeRefiner, "contaminatedOutputCount", 1);
        require(isotopeRefiner, "energyAfter", 20);
        require(isotopeRefiner, "progressAfter", 0);
        require(isotopeRefiner, "contaminationLevelAfter", 0);
        require(isotopeRefiner, "wearCounter", 8);
        require(isotopeRefiner, "active", true);
        require(isotopeRefiner, "minecraftRuntimeAccessed", false);
        require(isotopeRefiner, "minecraftRegistryMutated", false);

        Map<String, Object> radiationCleanser = AshfallAdapterCoreMachinePowerRuntime.runRadiationCleanserScenario();
        require(radiationCleanser, "adapterCoreBridge", true);
        require(radiationCleanser, "standaloneDuplicateGameplaySystem", false);
        require(radiationCleanser, "status", "PASS");
        require(radiationCleanser, "totalTicks", 400);
        require(radiationCleanser, "powerPerTick", 8);
        require(radiationCleanser, "energyCapacity", 4000);
        require(radiationCleanser, "transferPerTick", 256);
        require(radiationCleanser, "inputItem", "echoashfallprotocol:contaminated_iron");
        require(radiationCleanser, "filterItem", "echoashfallprotocol:filter_cartridge_advanced");
        require(radiationCleanser, "outputItem", "minecraft:iron_ingot");
        require(radiationCleanser, "filterConsumed", true);
        require(radiationCleanser, "inputCountAfter", 0);
        require(radiationCleanser, "filterCountAfter", 0);
        require(radiationCleanser, "outputCount", 1);
        require(radiationCleanser, "energyAfter", 50);
        require(radiationCleanser, "progressAfter", 0);
        require(radiationCleanser, "wearLevel", 400);
        require(radiationCleanser, "active", true);
        require(radiationCleanser, "feedbackThrottled", true);
        require(radiationCleanser, "minecraftRuntimeAccessed", false);
        require(radiationCleanser, "minecraftRegistryMutated", false);

        Map<String, Object> crystallineSynthesizer = AshfallAdapterCoreMachinePowerRuntime.runCrystallineSynthesizerScenario();
        require(crystallineSynthesizer, "adapterCoreBridge", true);
        require(crystallineSynthesizer, "standaloneDuplicateGameplaySystem", false);
        require(crystallineSynthesizer, "status", "PASS");
        require(crystallineSynthesizer, "totalTicks", 400);
        require(crystallineSynthesizer, "phase2StartTick", 100);
        require(crystallineSynthesizer, "phase3StartTick", 240);
        require(crystallineSynthesizer, "phase4StartTick", 360);
        require(crystallineSynthesizer, "energyCapacity", 8000);
        require(crystallineSynthesizer, "transferPerTick", 512);
        require(crystallineSynthesizer, "phase1PowerCost", 3);
        require(crystallineSynthesizer, "phase2PowerCost", 2);
        require(crystallineSynthesizer, "phase3PowerCost", 2);
        require(crystallineSynthesizer, "phase4PowerCost", 1);
        require(crystallineSynthesizer, "tickCalls", 402);
        require(crystallineSynthesizer, "powerFailureInjected", true);
        require(crystallineSynthesizer, "powerFailureFallbackApplied", true);
        require(crystallineSynthesizer, "gemFragmentCountAfter", 0);
        require(crystallineSynthesizer, "denseAlloyCountAfter", 0);
        require(crystallineSynthesizer, "energyCellCountAfter", 0);
        require(crystallineSynthesizer, "outputItem", "minecraft:netherite_scrap");
        require(crystallineSynthesizer, "outputCount", 1);
        require(crystallineSynthesizer, "energyAfter", 40);
        require(crystallineSynthesizer, "progressAfter", 0);
        require(crystallineSynthesizer, "phaseAfter", 0);
        require(crystallineSynthesizer, "wearCounter", 28);
        require(crystallineSynthesizer, "active", true);
        require(crystallineSynthesizer, "minecraftRuntimeAccessed", false);
        require(crystallineSynthesizer, "minecraftRegistryMutated", false);

        Map<String, Object> deepCoreMiner = AshfallAdapterCoreMachinePowerRuntime.runDeepCoreMinerScenario();
        require(deepCoreMiner, "adapterCoreBridge", true);
        require(deepCoreMiner, "standaloneDuplicateGameplaySystem", false);
        require(deepCoreMiner, "status", "PASS");
        require(deepCoreMiner, "totalTicks", 800);
        require(deepCoreMiner, "powerPerTick", 40);
        require(deepCoreMiner, "minYLevel", -32);
        require(deepCoreMiner, "shallowY", -16);
        require(deepCoreMiner, "miningY", -40);
        require(deepCoreMiner, "depthGateRespected", true);
        require(deepCoreMiner, "energyCapacity", 12000);
        require(deepCoreMiner, "transferPerTick", 512);
        require(deepCoreMiner, "localEnergyAfter", 0);
        require(deepCoreMiner, "networkEnergyAfter", 0);
        require(deepCoreMiner, "totalPowerConsumed", 32000);
        require(deepCoreMiner, "progressAfter", 0);
        require(deepCoreMiner, "wearLevel", 40);
        require(deepCoreMiner, "outputItem", "echoashfallprotocol:dense_alloy_chunk");
        require(deepCoreMiner, "outputSlotCountAfterPush", 0);
        require(deepCoreMiner, "neighborInputCount", 1);
        require(deepCoreMiner, "pushedToNeighbor", true);
        require(deepCoreMiner, "inputInsertionAllowed", false);
        require(deepCoreMiner, "outputExtractable", true);
        require(deepCoreMiner, "active", true);
        require(deepCoreMiner, "minecraftRuntimeAccessed", false);
        require(deepCoreMiner, "minecraftRegistryMutated", false);

        System.out.println("Ashfall AdapterCore machine/power runtime verifier PASS");
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }

    private static void requireIds(Map<String, Object> data, String key, List<String> expectedIds) {
        Object entriesObject = data.get(key);
        if (!(entriesObject instanceof List<?> entries)) {
            throw new IllegalStateException("Expected " + key + " to be a list but found " + entriesObject + ".");
        }
        if (entries.size() != expectedIds.size()) {
            throw new IllegalStateException("Expected " + key + " count=" + expectedIds.size()
                    + " but found " + entries.size() + ".");
        }
        for (String expectedId : expectedIds) {
            boolean found = entries.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .anyMatch(entry -> expectedId.equals(entry.get("id")));
            if (!found) {
                throw new IllegalStateException("Expected " + key + " to include " + expectedId + ".");
            }
        }
    }

    private static void requireNested(Map<?, ?> data, String parentKey, String key, Object expected) {
        Object parent = data.get(parentKey);
        if (!(parent instanceof Map<?, ?> parentMap)) {
            throw new IllegalStateException("Expected " + parentKey + " to be a map but found " + parent + ".");
        }
        require(parentMap, key, expected);
    }
}
