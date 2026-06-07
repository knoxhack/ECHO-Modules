package com.knoxhack.echoashfallprotocol;

import java.util.Map;
import java.util.List;

public final class AshfallAdapterCoreMachinePowerBridgeVerifier {
    private AshfallAdapterCoreMachinePowerBridgeVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> result = AshfallAdapterCoreMachinePowerBridge.runDefaultBridgeScenario(Map.of(
                "packId", "ashfall",
                "playerId", "native_bridge_probe"));
        require(result, "adapterCoreBridge", true);
        require(result, "standaloneDuplicateGameplaySystem", false);
        require(result, "status", "PASS");
        require(result, "minecraftRuntimeAccessed", false);
        require(result, "minecraftRegistryMutated", false);
        requireNested(result, "registryBindingPhase", "bindingCount", 17);
        requireNested(result, "registryBindingPhase", "requiredBindingCount", 17);
        requireNested(result, "registryBindingPhase", "requiredBindingsPresent", true);
        requireNested(result, "registryBindingPhase", "bindingSetExact", true);
        requireNestedIds(result, "registryBindingPhase", "bindings", List.of(
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:scrap_dynamo",
                "echoashfallprotocol:battery_bank",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:ore_grinder",
                "echoashfallprotocol:isotope_refiner",
                "echoashfallprotocol:radiation_cleanser",
                "echoashfallprotocol:crystalline_synthesizer",
                "echoashfallprotocol:deep_core_miner",
                "echoashfallprotocol:autofeed_hopper",
                "echoashfallprotocol:contaminant_condenser",
                "echoashfallprotocol:item_pipe",
                "echoashfallprotocol:power_cable",
                "echoashfallprotocol:reinforced_power_cable",
                "echoashfallprotocol:high_voltage_power_cable",
                "echoashfallprotocol:load_distributor",
                "echoashfallprotocol:factory_controller"));
        requireNested(result, "registryBindingPhase", "minecraftRegistryMutated", false);
        requireNested(result, "tickDispatchPhase", "dispatchedTicks", 80);
        requireNested(result, "tickDispatchPhase", "microGeneratorTicked", true);
        requireNested(result, "tickDispatchPhase", "powerNetworkTicked", true);
        requireNested(result, "tickDispatchPhase", "logisticsTicked", true);
        requireNested(result, "tickDispatchPhase", "factoryScanTicked", true);
        requireNested(result, "tickDispatchPhase", "batteryStoredEnergy", 440);
        requireNested(result, "tickDispatchPhase", "scrapPressRecipeCompleted", true);
        requireNested(result, "tickDispatchPhase", "scrapPressWear", 2);
        requireNested(result, "tickDispatchPhase", "oreGrinderRecipeCompleted", true);
        requireNested(result, "tickDispatchPhase", "oreGrinderByproductCount", 1);
        requireNested(result, "tickDispatchPhase", "loadDistributorPriorityMode", "SURVIVAL");
        requireNested(result, "tickDispatchPhase", "factoryConnectedMachines", 6);
        requireNested(result, "tickDispatchPhase", "networkDiagnostic", "PASS");
        requireNested(result, "eventDispatchPhase", "handledEventCount", 5);
        requireNested(result, "eventDispatchPhase", "generatorRestartHandled", true);
        requireNested(result, "eventDispatchPhase", "routerPriorityChanged", true);
        requireNested(result, "eventDispatchPhase", "machineRepairHandled", true);
        requireNested(result, "eventDispatchPhase", "factoryToggleHandled", true);
        requireNested(result, "eventDispatchPhase", "minecraftRuntimeAccessed", false);
        requireNested(result, "capabilityDispatchPhase", "energyReceiveProbe", 37);
        requireNested(result, "capabilityDispatchPhase", "energyExtractProbe", 37);
        requireNested(result, "capabilityDispatchPhase", "inventoryInsertProbe", 1);
        requireNested(result, "capabilityDispatchPhase", "inventoryExtractProbe", 1);
        requireNested(result, "capabilityMutationPhase", "status", "PASS");
        requireNested(result, "capabilityMutationPhase", "mutatingCapabilityCalls", 4);
        requireNested(result, "capabilityMutationPhase", "capabilityStateMutated", true);
        requireNested(result, "capabilityMutationPhase", "batteryEnergyAfter", 484);
        requireNested(result, "capabilityMutationPhase", "oreGrinderInputAfter", 4);
        requireNested(result, "capabilityMutationPhase", "scrapPressOutputAfter", 0);
        requireNested(result, "worldStateBridge", "networkDiagnostic", "PASS");
        requireNested(result, "worldStateBridge", "powerCapacityRespected", true);
        requireNested(result, "worldStateBridge", "logisticsLoopAvoided", true);
        requireNested(result, "playerStateBridge", "playerId", "native_bridge_probe");
        requireNested(result, "playerStateBridge", "useBlockEventsHandled", 5);
        System.out.println("Ashfall AdapterCore machine/power bridge verifier PASS");
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }

    private static void requireNested(Map<String, Object> data, String parentKey, String key, Object expected) {
        Object parent = data.get(parentKey);
        if (!(parent instanceof Map<?, ?> parentMap)) {
            throw new IllegalStateException("Expected " + parentKey + " to be a map but found " + parent + ".");
        }
        require(parentMap, key, expected);
    }

    private static void requireNestedIds(
            Map<String, Object> data,
            String parentKey,
            String key,
            List<String> expectedIds) {
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
