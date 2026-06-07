package com.knoxhack.echoashfallprotocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallNativeMachinePowerContracts {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeMachinePowerContracts() {
    }

    static Map<String, Object> describe(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        List<Map<String, Object>> powerContracts = powerContracts();
        List<Map<String, Object>> machineContracts = machineContracts();
        List<Map<String, Object>> logisticsContracts = logisticsContracts();

        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("moduleId", MODULE_ID);
        bridge.put("packId", safeContext.getOrDefault("packId", "unknown"));
        bridge.put("bridge", "adaptercore.ashfall_machine_power_logistics");
        bridge.put("adapterCoreBridge", true);
        bridge.put("implementationTarget", "AdapterCore native registry/service bridge");
        bridge.put("standaloneDuplicateGameplaySystem", false);
        bridge.put("sourceRuntime", "legacy_runtime_behavior_contracts");
        bridge.put("minecraftRuntimeAccessed", false);
        bridge.put("minecraftRegistryMutated", false);
        bridge.put("unsafeRuntimeWorkStarted", false);
        bridge.put("safeForNativeLoaderReports", true);
        bridge.put("powerContractCount", powerContracts.size());
        bridge.put("machineContractCount", machineContracts.size());
        bridge.put("logisticsContractCount", logisticsContracts.size());
        bridge.put("powerContracts", powerContracts);
        bridge.put("machineContracts", machineContracts);
        bridge.put("logisticsContracts", logisticsContracts);
        bridge.put("parityAssertions", List.of(
                "native_loader_reports_distinguish_generators_storage_cables_routers_and_consumers",
                "native_loader_reports_include_power_capacity_and_transfer_values_for_placeable_blocks",
                "native_loader_reports_include_machine_inventory_power_wear_and_automation_semantics",
                "native_loader_reports_include_item_pipe_endpoint_routing_semantics"
        ));
        bridge.put("summary", "AdapterCore-facing Ashfall native contract exposes concrete machine, power, and logistics behavior copied from existing legacy runtime block entities without importing runtime APIs or mutating registries.");
        return bridge;
    }

    private static List<Map<String, Object>> powerContracts() {
        return List.of(
                powerContract("echoashfallprotocol:micro_generator", "GENERATOR", "OUTPUT_ONLY", 3_000, 64,
                        map("generationPerTick", 8, "fuelBurnTicks", 160, "acceptsEnergy", false,
                                "failureMode", "wear_failure_requires_restart",
                                "sourceClass", "MicroGeneratorBlockEntity")),
                powerContract("echoashfallprotocol:scrap_dynamo", "GENERATOR", "OUTPUT_ONLY", 8_000, 256,
                        map("generationPerTick", 24, "scrapMetalBurnTicks", 80, "scrapPlasticBurnTicks", 120,
                                "scrapCircuitBurnTicks", 180, "coalBurnTicks", 240, "acceptsEnergy", false,
                                "fuelEvent", "ScrapDynamoFuelHandler.onRightClickBlock",
                                "activeStateReflectsBurning", true,
                                "sourceClass", "ScrapDynamoBlockEntity")),
                powerContract("echoashfallprotocol:battery_bank", "STORAGE", "BIDIRECTIONAL", 10_000, 100,
                        map("batterySlot", 0, "balancesInsertedBattery", true,
                                "batteryBalanceMode", "compares_bank_and_inserted_battery_fill_percent",
                                "batteryItemTiers", List.of("basic:2000/64", "advanced:10000/256", "elite:50000/1024"),
                                "adjacentDistribution", "pushes up to 100 FE/tick to adjacent energy consumers",
                                "sourceClass", "BatteryBankBlockEntity")),
                powerContract("echoashfallprotocol:power_cable", "CABLE", "BIDIRECTIONAL", 1_000, 50,
                        cableAttributes("basic")),
                powerContract("echoashfallprotocol:reinforced_power_cable", "CABLE", "BIDIRECTIONAL", 2_000, 256,
                        cableAttributes("reinforced")),
                powerContract("echoashfallprotocol:high_voltage_power_cable", "CABLE", "BIDIRECTIONAL", 4_000, 1_024,
                        cableAttributes("high_voltage")),
                powerContract("echoashfallprotocol:load_distributor", "ROUTER", "BIDIRECTIONAL", 2_000, 512,
                        map("priorityModes", List.of("BALANCED", "SURVIVAL", "FACTORY", "GRID"),
                                "pullsFromAdjacentSuppliers", true,
                                "pushesToAdjacentConsumers", true,
                                "sourceClass", "LoadDistributorBlockEntity"))
        );
    }

    private static List<Map<String, Object>> machineContracts() {
        return List.of(
                machineContract("echoashfallprotocol:scrap_press", "PROCESSOR", 1_500, 128,
                        map("powerCostPerTick", 1, "defaultRecipeTicks", 40, "inventorySlots", 3,
                                "inputSlots", List.of(0), "outputSlots", List.of(1), "batterySlot", 2,
                                "recipeSource", "ScrapPressRecipe.findRecipe",
                                "wear", "adds_wear_on_recipe_complete_and_can_jam",
                                "automation", "HopperHandler input from any side, output from bottom",
                                "sourceClass", "ScrapPressBlockEntity")),
                machineContract("echoashfallprotocol:ore_grinder", "PROCESSOR", 2_000, 128,
                        map("defaultRecipeTicks", 80, "defaultPowerPerOperation", 200, "inventorySlots", 5,
                                "inputSlots", List.of(0, 1), "outputSlots", List.of(2, 3), "batterySlot", 4,
                                "recipeSource", "OreGrinderBlockEntity.getSubstrateRecipes",
                                "wear", "adds_wear_during_processing_and_can_jam",
                                "automation", "HopperHandler substrate inputs, bottom product and byproduct extraction",
                                "sourceClass", "OreGrinderBlockEntity")),
                machineContract("echoashfallprotocol:isotope_refiner", "PROCESSOR", 4_000, 256,
                        map("defaultRecipeTicks", 160, "powerPerOperation", 500, "powerPerTick", 3,
                                "inventorySlots", 5, "inputSlots", List.of(0, 1),
                                "outputSlots", List.of(2, 3), "batterySlot", 4,
                                "recipeSource", "IsotopeRefinerBlockEntity.getRefinerRecipes",
                                "catalystItem", "echoashfallprotocol:crystal_dust",
                                "contaminationChance", 0.20F,
                                "wear", "adds_wear_every_20_ticks_and_can_jam",
                                "sourceClass", "IsotopeRefinerBlockEntity")),
                machineContract("echoashfallprotocol:radiation_cleanser", "PROCESSOR", 4_000, 256,
                        map("totalTicks", 400, "powerPerTick", 8, "inventorySlots", 4,
                                "inputSlots", List.of(0, 1), "outputSlots", List.of(2),
                                "batterySlot", 3,
                                "recipeSource", "RadiationCleanserBlockEntity.getDecontaminationMap",
                                "filterItem", "echoashfallprotocol:filter_cartridge_advanced",
                                "filterConsumptionChance", 0.20F,
                                "wear", "increments_every_powered_processing_tick",
                                "playerFeedback", "nearby actionbar status throttled to 160 ticks",
                                "sourceClass", "RadiationCleanserBlockEntity")),
                machineContract("echoashfallprotocol:crystalline_synthesizer", "PHASED_PROCESSOR", 8_000, 512,
                        map("totalTicks", 400, "phaseStarts", List.of(100, 240, 360),
                                "phasePowerCosts", List.of(3, 2, 2, 1),
                                "inventorySlots", 5, "inputSlots", List.of(0, 1, 2),
                                "outputSlots", List.of(3), "batterySlot", 4,
                                "inputRecipe", "4 gem_fragment + 1 dense_alloy_chunk + 2 energy_cell",
                                "possibleOutputs", List.of("minecraft:diamond", "minecraft:emerald", "minecraft:netherite_scrap"),
                                "powerFailureFallback", "phase_2_or_3_failure_forces_netherite_scrap_when_roll_would_have_been_diamond_or_emerald",
                                "wear", "adds_wear_every_20_ticks_with_double_wear_from_phase_3",
                                "sourceClass", "CrystallineSynthesizerBlockEntity")),
                machineContract("echoashfallprotocol:deep_core_miner", "RESOURCE_GENERATOR", 12_000, 512,
                        map("totalTicks", 800, "powerPerTick", 40, "minYLevel", -32,
                                "inventorySlots", 2, "inputSlots", List.of(), "outputSlots", List.of(0),
                                "batterySlot", 1,
                                "possibleOutputs", List.of(
                                        "echoashfallprotocol:dense_alloy_chunk",
                                        "echoashfallprotocol:gem_fragment",
                                        "echoashfallprotocol:crystal_dust",
                                        "minecraft:redstone",
                                        "minecraft:lapis_lazuli"),
                                "automation", "HopperHandler output extraction only; no item insertion",
                                "chaining", "pushes generated output to neighboring inventories after completion",
                                "wear", "adds_two_wear_every_40_progress_ticks_and_can_jam",
                                "sourceClass", "DeepCoreMinerBlockEntity")),
                machineContract("echoashfallprotocol:autofeed_hopper", "PLAYER_SUPPORT", 1_000, 64,
                        map("radiusBlocks", 8, "powerCostPerFeed", 10, "hungerThreshold", 10,
                                "feedAmount", 4, "feedIntervalTicks", 60,
                                "wear", "increments_after_successful_feed_and_can_jam",
                                "playerStateBridge", "mutates_food_level_for_hungry_server_players",
                                "sourceClass", "AutofeedHopperBlockEntity")),
                machineContract("echoashfallprotocol:contaminant_condenser", "WORLD_PROCESSOR", 2_000, 128,
                        map("processRadiusBlocks", 3, "verticalScanRange", "-1..2",
                                "powerCostPerOperation", 50, "processIntervalTicks", 100,
                                "inputBlock", "echoashfallprotocol:toxic_puddle",
                                "outputBlock", "minecraft:sand",
                                "wear", "increments_after_successful_conversion_and_can_jam",
                                "worldStateBridge", "mutates_nearby_toxic_puddle_to_sand",
                                "sourceClass", "ContaminantCondenserBlockEntity")),
                machineContract("echoashfallprotocol:factory_controller", "CONTROLLER", 0, 0,
                        map("scanRadiusBlocks", 16, "scanIntervalTicks", 20, "scanLimitNodes", 100,
                                "tracksPowerStored", true, "tracksActiveMachines", true,
                                "routesThrough", List.of("ItemPipeBlockEntity", "PowerCableBlockEntity"),
                                "sourceClass", "FactoryControllerBlockEntity"))
        );
    }

    private static List<Map<String, Object>> logisticsContracts() {
        return List.of(
                logisticsContract("echoashfallprotocol:item_pipe", "ITEM_PIPE",
                        map("transferCooldownTicks", 8, "sourceDirection", "block_facing",
                                "sourceContract", "HopperHandler.getOutputSlots",
                                "destinationContract", "HopperHandler.getInputSlots",
                                "routingOrder", List.of("direct_machine_input", "pipe_network_machine_output_chain"),
                                "liveRuntimeTargets", List.of("hopper_source_to_machine_input", "machine_output_to_machine_input"),
                                "loopAvoidance", "visited pipe set prevents return-path loops",
                                "sourceClass", "ItemPipeBlockEntity")),
                logisticsContract("echoashfallprotocol:scrap_press", "MACHINE_ENDPOINT",
                        map("inputs", "slot_0_any_side_scrap_metal", "outputs", "slot_1_bottom_only",
                                "chainsOutputsToNeighbors", true,
                                "sourceClass", "ScrapPressBlockEntity")),
                logisticsContract("echoashfallprotocol:ore_grinder", "MACHINE_ENDPOINT",
                        map("inputs", "slots_0_1_any_side_substrate_recipe", "outputs", "slots_2_3_bottom_only",
                                "chainsOutputsToNeighbors", true,
                                "sourceClass", "OreGrinderBlockEntity"))
        );
    }

    private static Map<String, Object> powerContract(String id, String kind, String flowMode, int capacity,
                                                    int transferPerTick, Map<String, Object> attributes) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("id", id);
        contract.put("kind", kind);
        contract.put("flowMode", flowMode);
        contract.put("energyCapacity", capacity);
        contract.put("transferPerTick", transferPerTick);
        contract.put("attributes", attributes);
        return contract;
    }

    private static Map<String, Object> machineContract(String id, String kind, int energyCapacity,
                                                      int transferPerTick, Map<String, Object> attributes) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("id", id);
        contract.put("kind", kind);
        contract.put("energyCapacity", energyCapacity);
        contract.put("transferPerTick", transferPerTick);
        contract.put("attributes", attributes);
        return contract;
    }

    private static Map<String, Object> logisticsContract(String id, String kind, Map<String, Object> attributes) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("id", id);
        contract.put("kind", kind);
        contract.put("attributes", attributes);
        return contract;
    }

    private static Map<String, Object> cableAttributes(String tier) {
        return map("tier", tier, "connectsToAdjacentEnergyBlocks", true,
                "connectionStateProperties", List.of("north", "south", "east", "west", "up", "down"),
                "activeStateReflectsStoredEnergy", true,
                "sourceClass", "PowerCableBlockEntity");
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}
