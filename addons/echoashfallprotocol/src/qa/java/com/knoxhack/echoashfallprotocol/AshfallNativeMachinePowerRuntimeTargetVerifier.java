package com.knoxhack.echoashfallprotocol;

import java.util.Map;
import java.util.List;

public final class AshfallNativeMachinePowerRuntimeTargetVerifier {
    private AshfallNativeMachinePowerRuntimeTargetVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> result = AshfallNativeMachinePowerRuntimeTarget.initialize(Map.of("packId", "ashfall"));
        Object status = result.get("status");
        if (!"PASS".equals(status)) {
            throw new IllegalStateException("Expected PASS but found " + status + ": " + result.get("diagnostics"));
        }
        if (!Boolean.TRUE.equals(result.get("adapterCoreBridge"))) {
            throw new IllegalStateException("Runtime target must remain AdapterCore-backed.");
        }
        if (!Boolean.FALSE.equals(result.get("standaloneDuplicateGameplaySystem"))) {
            throw new IllegalStateException("Runtime target must not be standalone duplicate gameplay.");
        }
        if (!Boolean.FALSE.equals(result.get("minecraftRuntimeAccessed"))) {
            throw new IllegalStateException("Verifier must not touch Minecraft runtime state.");
        }
        require(result, "registryBindingCount", 19);
        require(result, "tickTargetCount", 19);
        requireIds(result, "registryBindings", List.of(
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
                "echoashfallprotocol:factory_controller"));
        requireIds(result, "tickTargets", List.of(
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
                "echoashfallprotocol:factory_controller"));
        Object rehearsalObject = result.get("rehearsal");
        if (!(rehearsalObject instanceof Map<?, ?> rehearsal)) {
            throw new IllegalStateException("Runtime target must include rehearsal evidence.");
        }
        require(rehearsal, "adapterPacketVersion", 2);
        require(rehearsal, "powerTransferCount", 4);
        require(rehearsal, "scrapPressProgress", 1);
        require(rehearsal, "batteryStoredEnergy", 7);
        require(rehearsal, "itemPipeMovedCount", 1);
        require(rehearsal, "oreGrinderInputCount", 1);
        require(rehearsal, "networkDiagnostic", "PASS");
        Object liveRuntimeObject = result.get("liveRuntime");
        if (!(liveRuntimeObject instanceof Map<?, ?> liveRuntime)) {
            throw new IllegalStateException("Runtime target must include live runtime evidence.");
        }
        require(liveRuntime, "adapterPacketVersion", 3);
        require(liveRuntime, "adapterCoreBridge", true);
        require(liveRuntime, "standaloneDuplicateGameplaySystem", false);
        require(liveRuntime, "tickCount", 80);
        require(liveRuntime, "microGeneratorFuelItems", 0);
        require(liveRuntime, "microGeneratorBurnTicksRemaining", 80);
        require(liveRuntime, "microGeneratorWear", 12);
        require(liveRuntime, "microGeneratorFailed", false);
        require(liveRuntime, "loadDistributorPriorityMode", "SURVIVAL");
        require(liveRuntime, "batteryStoredEnergy", 440);
        require(liveRuntime, "scrapPressOutputCount", 1);
        require(liveRuntime, "scrapPressWear", 2);
        require(liveRuntime, "itemPipeMovedCount", 1);
        require(liveRuntime, "oreGrinderInputCount", 1);
        require(liveRuntime, "oreGrinderOutputCount", 2);
        require(liveRuntime, "oreGrinderByproductCount", 1);
        require(liveRuntime, "oreGrinderWear", 5);
        require(liveRuntime, "factoryScanCount", 4);
        require(liveRuntime, "factoryConnectedMachines", 6);
        require(liveRuntime, "factoryStoredEnergy", 440);
        require(liveRuntime, "factoryEnergyCapacity", 19500);
        require(liveRuntime, "factoryScanLimitRespected", true);
        require(liveRuntime, "networkDiagnostic", "PASS");
        Object adapterCoreDispatchObject = result.get("adapterCoreDispatch");
        if (!(adapterCoreDispatchObject instanceof Map<?, ?> adapterCoreDispatch)) {
            throw new IllegalStateException("Runtime target must include AdapterCore dispatch evidence.");
        }
        require(adapterCoreDispatch, "adapterCoreBridge", true);
        require(adapterCoreDispatch, "standaloneDuplicateGameplaySystem", false);
        require(adapterCoreDispatch, "status", "PASS");
        requireNested(adapterCoreDispatch, "registryBindingPhase", "requiredBindingsPresent", true);
        requireNested(adapterCoreDispatch, "registryBindingPhase", "bindingCount", 19);
        requireNested(adapterCoreDispatch, "registryBindingPhase", "requiredBindingCount", 19);
        requireNested(adapterCoreDispatch, "registryBindingPhase", "bindingSetExact", true);
        requireNestedIds(adapterCoreDispatch, "registryBindingPhase", "bindings", List.of(
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
                "echoashfallprotocol:factory_controller"));
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "dispatchedTicks", 80);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "scrapPressRecipeCompleted", true);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "oreGrinderRecipeCompleted", true);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "waterPurifierRecipeCompleted", true);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "thermalBurnerFuelCycleCompleted", true);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "batteryStoredEnergy", 440);
        requireNested(adapterCoreDispatch, "tickDispatchPhase", "oreGrinderByproductCount", 1);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "handledEventCount", 5);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "generatorRestartHandled", true);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "routerPriorityChanged", true);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "machineRepairHandled", true);
        requireNested(adapterCoreDispatch, "eventDispatchPhase", "factoryToggleHandled", true);
        requireNested(adapterCoreDispatch, "capabilityDispatchPhase", "energyReceiveProbe", 37);
        requireNested(adapterCoreDispatch, "worldStateBridge", "networkDiagnostic", "PASS");
        requireNested(adapterCoreDispatch, "playerStateBridge", "useBlockEventsHandled", 5);
        Object persistenceObject = result.get("persistenceRoundTrip");
        if (!(persistenceObject instanceof Map<?, ?> persistence)) {
            throw new IllegalStateException("Runtime target must include persistence roundtrip evidence.");
        }
        require(persistence, "status", "PASS");
        require(persistence, "snapshotNodeCount", 10);
        require(persistence, "restoredTickCount", 80);
        require(persistence, "restoredBatteryStoredEnergy", 440);
        require(persistence, "restoredScrapPressOutputCount", 1);
        require(persistence, "restoredOreGrinderOutputCount", 2);
        require(persistence, "restoredNetworkDiagnostic", "PASS");
        Object cableTierObject = result.get("cableTierRuntime");
        if (!(cableTierObject instanceof Map<?, ?> cableTier)) {
            throw new IllegalStateException("Runtime target must include cable tier evidence.");
        }
        require(cableTier, "basicCableTransfer", 50);
        require(cableTier, "reinforcedCableTransfer", 256);
        require(cableTier, "highVoltageCableTransfer", 1024);
        require(cableTier, "basicSinkEnergy", 50);
        require(cableTier, "reinforcedSinkEnergy", 256);
        require(cableTier, "highVoltageSinkEnergy", 1024);
        require(cableTier, "networkDiagnostic", "PASS");
        Object adjacencyPowerObject = result.get("adjacencyPowerRuntime");
        if (!(adjacencyPowerObject instanceof Map<?, ?> adjacencyPower)) {
            throw new IllegalStateException("Runtime target must include adjacency power flow evidence.");
        }
        require(adjacencyPower, "status", "PASS");
        require(adjacencyPower, "routeCount", 3);
        require(adjacencyPower, "powerTransferCount", 12);
        require(adjacencyPower, "sourceEnergyAfter", 362);
        require(adjacencyPower, "scrapPressEnergy", 50);
        require(adjacencyPower, "oreGrinderEnergy", 50);
        require(adjacencyPower, "batteryStoredEnergy", 50);
        require(adjacencyPower, "adjacencyDetected", true);
        require(adjacencyPower, "generatorToConsumerFlow", true);
        require(adjacencyPower, "storageFlow", true);
        require(adjacencyPower, "cableTierLimitsRespected", true);
        require(adjacencyPower, "loopAvoided", true);
        Object priorityRoutingObject = result.get("priorityRoutingRuntime");
        if (!(priorityRoutingObject instanceof Map<?, ?> priorityRouting)) {
            throw new IllegalStateException("Runtime target must include priority routing evidence.");
        }
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
        Object recipeObject = result.get("recipeCatalogRuntime");
        if (!(recipeObject instanceof Map<?, ?> recipes)) {
            throw new IllegalStateException("Runtime target must include recipe catalog runtime evidence.");
        }
        require(recipes, "status", "PASS");
        requireNested(recipes, "recipeCatalog", "resourcePath",
                "data/echoashfallprotocol/adaptercore/native_machine_recipes.properties");
        requireNested(recipes, "recipeCatalog", "resourceLoaded", true);
        requireNested(recipes, "recipeCatalog", "fallbackUsed", false);
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
        require(recipes, "toxicGrinderByproduct", "charged_ash_circuit");
        require(recipes, "toxicGrinderByproductCount", 1);
        Object byproductChanceObject = result.get("byproductChanceRuntime");
        if (!(byproductChanceObject instanceof Map<?, ?> byproductChance)) {
            throw new IllegalStateException("Runtime target must include byproduct chance evidence.");
        }
        require(byproductChance, "status", "PASS");
        require(byproductChance, "recipeInput", "toxic_slagstone");
        require(byproductChance, "recipeByproduct", "charged_ash_circuit");
        require(byproductChance, "recipeBatches", 4);
        require(byproductChance, "byproductSuccesses", 2);
        require(byproductChance, "byproductSkipped", 2);
        require(byproductChance, "outputCount", 8);
        require(byproductChance, "byproductCount", 2);
        require(byproductChance, "remainingInputCount", 0);
        require(byproductChance, "chanceRespected", true);
        Object outputBackpressureObject = result.get("outputBackpressureRuntime");
        if (!(outputBackpressureObject instanceof Map<?, ?> outputBackpressure)) {
            throw new IllegalStateException("Runtime target must include output backpressure evidence.");
        }
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
        Object jamRepairObject = result.get("jamRepairRuntime");
        if (!(jamRepairObject instanceof Map<?, ?> jamRepair)) {
            throw new IllegalStateException("Runtime target must include jam repair runtime evidence.");
        }
        require(jamRepair, "status", "PASS");
        require(jamRepair, "jamPreventedProgress", 0);
        require(jamRepair, "repairEventHandled", true);
        require(jamRepair, "scrapPressJammedAfterRepair", false);
        require(jamRepair, "networkDiagnostic", "PASS");
        Object wearThresholdObject = result.get("wearThresholdRuntime");
        if (!(wearThresholdObject instanceof Map<?, ?> wearThreshold)) {
            throw new IllegalStateException("Runtime target must include wear threshold evidence.");
        }
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
        Object generatorFailureObject = result.get("generatorFailureChanceRuntime");
        if (!(generatorFailureObject instanceof Map<?, ?> generatorFailure)) {
            throw new IllegalStateException("Runtime target must include generator failure chance evidence.");
        }
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
        Object scrapDynamoObject = result.get("scrapDynamoRuntime");
        if (!(scrapDynamoObject instanceof Map<?, ?> scrapDynamo)) {
            throw new IllegalStateException("Runtime target must include Scrap Dynamo runtime evidence.");
        }
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
        Object batteryBankBalancingObject = result.get("batteryBankBalancingRuntime");
        if (!(batteryBankBalancingObject instanceof Map<?, ?> batteryBankBalancing)) {
            throw new IllegalStateException("Runtime target must include Battery Bank balancing evidence.");
        }
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
        Object waterPurifierObject = result.get("waterPurifierRuntime");
        if (!(waterPurifierObject instanceof Map<?, ?> waterPurifier)) {
            throw new IllegalStateException("Runtime target must include Water Purifier runtime evidence.");
        }
        require(waterPurifier, "status", "PASS");
        require(waterPurifier, "processTimeTicks", 60);
        require(waterPurifier, "powerPerBottlePerTick", 20);
        require(waterPurifier, "batchSize", 1);
        require(waterPurifier, "maxBatchSize", 3);
        require(waterPurifier, "energyCapacity", 1_000);
        require(waterPurifier, "transferPerTick", 64);
        require(waterPurifier, "networkRelayTransferLimit", 50);
        require(waterPurifier, "networkEnergyAfter", 800);
        require(waterPurifier, "totalPowerConsumed", 1_200);
        require(waterPurifier, "inputItem", "echoashfallprotocol:dirty_water_bottle");
        require(waterPurifier, "filterItem", "echoashfallprotocol:filter_cartridge_basic");
        require(waterPurifier, "outputItem", "echoashfallprotocol:clean_water_bottle");
        require(waterPurifier, "dirtyWaterCountAfter", 0);
        require(waterPurifier, "filterCountAfter", 0);
        require(waterPurifier, "cleanWaterCount", 1);
        require(waterPurifier, "filterConsumed", true);
        require(waterPurifier, "progressAfter", 0);
        require(waterPurifier, "wearCounter", 6);
        require(waterPurifier, "batterySlot", 3);
        require(waterPurifier, "canReceiveEnergy", true);
        require(waterPurifier, "canExtractEnergyWhenStored", true);
        require(waterPurifier, "survivalPriorityConsumer", true);
        require(waterPurifier, "active", true);
        Object thermalBurnerObject = result.get("thermalBurnerRuntime");
        if (!(thermalBurnerObject instanceof Map<?, ?> thermalBurner)) {
            throw new IllegalStateException("Runtime target must include Thermal Burner runtime evidence.");
        }
        require(thermalBurner, "status", "PASS");
        require(thermalBurner, "processTimeTicks", 40);
        require(thermalBurner, "totalTicks", 160);
        require(thermalBurner, "inputItem", "minecraft:cobblestone");
        require(thermalBurner, "inputCountAfter", 0);
        require(thermalBurner, "acceptedAnyItem", true);
        require(thermalBurner, "energyPerItem", 50);
        require(thermalBurner, "energyCapacity", 1_000);
        require(thermalBurner, "energyAfter", 200);
        require(thermalBurner, "simulatedEnergyExtract", 64);
        require(thermalBurner, "canReceiveEnergy", false);
        require(thermalBurner, "canExtractEnergy", true);
        require(thermalBurner, "itemsBurnedCounterAfter", 0);
        require(thermalBurner, "ashOutputItem", "echoashfallprotocol:ash");
        require(thermalBurner, "ashOutputCount", 1);
        require(thermalBurner, "burnProgressAfter", 0);
        require(thermalBurner, "wearCounter", 16);
        require(thermalBurner, "batterySlot", 2);
        require(thermalBurner, "active", true);
        Object autofeedObject = result.get("autofeedHopperRuntime");
        if (!(autofeedObject instanceof Map<?, ?> autofeed)) {
            throw new IllegalStateException("Runtime target must include Autofeed Hopper runtime evidence.");
        }
        require(autofeed, "status", "PASS");
        require(autofeed, "powerCostPerFeed", 10);
        require(autofeed, "feedIntervalTicks", 60);
        require(autofeed, "hungerThreshold", 10);
        require(autofeed, "feedAmount", 4);
        require(autofeed, "fedPlayerCount", 1);
        require(autofeed, "hungryPlayerFoodAfter", 12);
        require(autofeed, "satiatedPlayerFoodAfter", 16);
        require(autofeed, "energyAfter", 10);
        require(autofeed, "lastFeedTick", 60);
        require(autofeed, "wearCounter", 1);
        require(autofeed, "playerStateMutated", true);
        require(autofeed, "active", true);
        Object condenserObject = result.get("contaminantCondenserRuntime");
        if (!(condenserObject instanceof Map<?, ?> condenser)) {
            throw new IllegalStateException("Runtime target must include Contaminant Condenser runtime evidence.");
        }
        require(condenser, "status", "PASS");
        require(condenser, "powerCostPerOperation", 50);
        require(condenser, "processIntervalTicks", 100);
        require(condenser, "processRadiusBlocks", 3);
        require(condenser, "blocksProcessed", 1);
        require(condenser, "convertedBlockBefore", "echoashfallprotocol:toxic_puddle");
        require(condenser, "convertedBlockAfter", "minecraft:sand");
        require(condenser, "energyAfter", 50);
        require(condenser, "wearCounter", 1);
        require(condenser, "remainingToxicInRange", 0L);
        require(condenser, "remainingToxicOutOfRange", 1L);
        require(condenser, "worldStateMutated", true);
        require(condenser, "active", true);
        Object isotopeObject = result.get("isotopeRefinerRuntime");
        if (!(isotopeObject instanceof Map<?, ?> isotope)) {
            throw new IllegalStateException("Runtime target must include Isotope Refiner runtime evidence.");
        }
        require(isotope, "status", "PASS");
        require(isotope, "processTimeTicks", 160);
        require(isotope, "powerPerOperation", 500);
        require(isotope, "powerPerTick", 3);
        require(isotope, "energyCapacity", 4000);
        require(isotope, "transferPerTick", 256);
        require(isotope, "inputItem", "minecraft:iron_ingot");
        require(isotope, "catalystItem", "echoashfallprotocol:crystal_dust");
        require(isotope, "cleanOutputItem", "minecraft:gold_ingot");
        require(isotope, "contaminatedOutputItem", "echoashfallprotocol:contaminated_gold");
        require(isotope, "contaminatedBranchSelected", true);
        require(isotope, "inputCountAfter", 0);
        require(isotope, "catalystCountAfter", 0);
        require(isotope, "cleanOutputCount", 0);
        require(isotope, "contaminatedOutputCount", 1);
        require(isotope, "energyAfter", 20);
        require(isotope, "progressAfter", 0);
        require(isotope, "contaminationLevelAfter", 0);
        require(isotope, "wearCounter", 8);
        require(isotope, "active", true);
        Object radiationObject = result.get("radiationCleanserRuntime");
        if (!(radiationObject instanceof Map<?, ?> radiation)) {
            throw new IllegalStateException("Runtime target must include Radiation Cleanser runtime evidence.");
        }
        require(radiation, "status", "PASS");
        require(radiation, "totalTicks", 400);
        require(radiation, "powerPerTick", 8);
        require(radiation, "energyCapacity", 4000);
        require(radiation, "transferPerTick", 256);
        require(radiation, "inputItem", "echoashfallprotocol:contaminated_iron");
        require(radiation, "filterItem", "echoashfallprotocol:filter_cartridge_advanced");
        require(radiation, "outputItem", "minecraft:iron_ingot");
        require(radiation, "filterConsumed", true);
        require(radiation, "inputCountAfter", 0);
        require(radiation, "filterCountAfter", 0);
        require(radiation, "outputCount", 1);
        require(radiation, "energyAfter", 50);
        require(radiation, "progressAfter", 0);
        require(radiation, "wearLevel", 400);
        require(radiation, "active", true);
        require(radiation, "feedbackThrottled", true);
        Object crystallineObject = result.get("crystallineSynthesizerRuntime");
        if (!(crystallineObject instanceof Map<?, ?> crystalline)) {
            throw new IllegalStateException("Runtime target must include Crystalline Synthesizer runtime evidence.");
        }
        require(crystalline, "status", "PASS");
        require(crystalline, "totalTicks", 400);
        require(crystalline, "phase2StartTick", 100);
        require(crystalline, "phase3StartTick", 240);
        require(crystalline, "phase4StartTick", 360);
        require(crystalline, "energyCapacity", 8000);
        require(crystalline, "transferPerTick", 512);
        require(crystalline, "phase1PowerCost", 3);
        require(crystalline, "phase2PowerCost", 2);
        require(crystalline, "phase3PowerCost", 2);
        require(crystalline, "phase4PowerCost", 1);
        require(crystalline, "tickCalls", 402);
        require(crystalline, "powerFailureInjected", true);
        require(crystalline, "powerFailureFallbackApplied", true);
        require(crystalline, "gemFragmentCountAfter", 0);
        require(crystalline, "denseAlloyCountAfter", 0);
        require(crystalline, "energyCellCountAfter", 0);
        require(crystalline, "outputItem", "minecraft:netherite_scrap");
        require(crystalline, "outputCount", 1);
        require(crystalline, "energyAfter", 40);
        require(crystalline, "progressAfter", 0);
        require(crystalline, "phaseAfter", 0);
        require(crystalline, "wearCounter", 28);
        require(crystalline, "active", true);
        Object deepCoreObject = result.get("deepCoreMinerRuntime");
        if (!(deepCoreObject instanceof Map<?, ?> deepCore)) {
            throw new IllegalStateException("Runtime target must include Deep Core Miner runtime evidence.");
        }
        require(deepCore, "status", "PASS");
        require(deepCore, "totalTicks", 800);
        require(deepCore, "powerPerTick", 40);
        require(deepCore, "minYLevel", -32);
        require(deepCore, "shallowY", -16);
        require(deepCore, "miningY", -40);
        require(deepCore, "depthGateRespected", true);
        require(deepCore, "energyCapacity", 12000);
        require(deepCore, "transferPerTick", 512);
        require(deepCore, "localEnergyAfter", 0);
        require(deepCore, "networkEnergyAfter", 0);
        require(deepCore, "totalPowerConsumed", 32000);
        require(deepCore, "progressAfter", 0);
        require(deepCore, "wearLevel", 40);
        require(deepCore, "outputItem", "echoashfallprotocol:dense_alloy_chunk");
        require(deepCore, "outputSlotCountAfterPush", 0);
        require(deepCore, "neighborInputCount", 1);
        require(deepCore, "pushedToNeighbor", true);
        require(deepCore, "inputInsertionAllowed", false);
        require(deepCore, "outputExtractable", true);
        require(deepCore, "active", true);
        Object capabilityMutationObject = result.get("capabilityMutationRuntime");
        if (!(capabilityMutationObject instanceof Map<?, ?> capabilityMutation)) {
            throw new IllegalStateException("Runtime target must include capability mutation evidence.");
        }
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
        Object sidedInventoryObject = result.get("sidedInventoryCapabilityRuntime");
        if (!(sidedInventoryObject instanceof Map<?, ?> sidedInventory)) {
            throw new IllegalStateException("Runtime target must include sided inventory capability evidence.");
        }
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
        Object factoryToggleObject = result.get("factoryControllerToggleRuntime");
        if (!(factoryToggleObject instanceof Map<?, ?> factoryToggle)) {
            throw new IllegalStateException("Runtime target must include factory controller toggle evidence.");
        }
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
        Object logisticsRoutingObject = result.get("logisticsRoutingRuntime");
        if (!(logisticsRoutingObject instanceof Map<?, ?> logisticsRouting)) {
            throw new IllegalStateException("Runtime target must include logistics routing evidence.");
        }
        require(logisticsRouting, "status", "PASS");
        require(logisticsRouting, "hopperSourceCount", 2);
        require(logisticsRouting, "routeTransferCount", 2);
        require(logisticsRouting, "scrapSourceOutputAfter", 0);
        require(logisticsRouting, "oreSourceOutputAfter", 0);
        require(logisticsRouting, "scrapPressInputAfter", 10);
        require(logisticsRouting, "oreGrinderInputAfter", 5);
        require(logisticsRouting, "loopAvoided", true);
        require(logisticsRouting, "validMachineInputsSelected", true);
        Object machineOutputChainingObject = result.get("machineOutputChainingRuntime");
        if (!(machineOutputChainingObject instanceof Map<?, ?> machineOutputChaining)) {
            throw new IllegalStateException("Runtime target must include machine output chaining evidence.");
        }
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
        System.out.println("Ashfall native machine/power runtime target verifier PASS");
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

    private static void requireNestedIds(Map<?, ?> data, String parentKey, String key, List<String> expectedIds) {
        Object parent = data.get(parentKey);
        if (!(parent instanceof Map<?, ?> parentMap)) {
            throw new IllegalStateException("Expected " + parentKey + " to be a map but found " + parent + ".");
        }
        Object entriesObject = parentMap.get(key);
        if (!(entriesObject instanceof List<?> entries)) {
            throw new IllegalStateException("Expected " + parentKey + "." + key
                    + " to be a list but found " + entriesObject + ".");
        }
        if (entries.size() != expectedIds.size()) {
            throw new IllegalStateException("Expected " + parentKey + "." + key + " count="
                    + expectedIds.size() + " but found " + entries.size() + ".");
        }
        for (String expectedId : expectedIds) {
            boolean found = entries.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .anyMatch(entry -> expectedId.equals(entry.get("id")));
            if (!found) {
                throw new IllegalStateException("Expected " + parentKey + "." + key
                        + " to include " + expectedId + ".");
            }
        }
    }
}
