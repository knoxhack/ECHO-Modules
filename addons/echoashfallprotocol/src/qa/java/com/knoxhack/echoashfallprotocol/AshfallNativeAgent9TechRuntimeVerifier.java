package com.knoxhack.echoashfallprotocol;

import java.util.List;
import java.util.Map;

public final class AshfallNativeAgent9TechRuntimeVerifier {
    private AshfallNativeAgent9TechRuntimeVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> result = AshfallNativeAgent9TechRuntime.run(Map.of("packId", "ashfall"));
        require(result, "status", "PASS");
        require(result, "adapterCoreBridge", true);
        require(result, "adapterCoreContract", AshfallNativeAgent9TechRuntime.CONTRACT_ID);
        require(result, "runtime", "echo_native_loader");
        require(result, "standaloneDuplicateGameplaySystem", false);
        require(result, "serviceCodeExecuted", true);
        require(result, "minecraftRuntimeAccessed", false);
        require(result, "minecraftRegistryMutated", false);
        require(result, "machinePowerRuntimeStatus", "PASS");
        require(result, "machinePowerResourceAuditStatus", "PASS");
        require(result, "recipeCatalogResourceLoaded", true);
        require(result, "placeMachine", true);
        require(result, "openMachineUi", true);
        require(result, "insertInput", true);
        require(result, "insertedInputCount", 9);
        require(result, "consumePower", true);
        require(result, "processRecipe", true);
        require(result, "recipeProgressTicks", 40);
        require(result, "outputResult", true);
        require(result, "outputCountBeforeLogistics", 1);
        require(result, "powerGraphConnects", true);
        require(result, "logisticsTransfer", true);
        require(result, "oreGrinderInputCount", 1);
        require(result, "saveMachineState", true);
        require(result, "reloadMachineState", true);
        require(result, "missionDependsOnMachineCompletion", true);
        require(result, "multiblockValidation", true);
        requireNested(result, "vehicleMovementAction", "completed", true);
        requireNested(result, "vehicleMovementAction", "movedSteps", 4);
        requireNested(result, "economyCost", "paid", true);
        requireNested(result, "economyCost", "balanceAfter", 75);

        Object loot = result.get("lootOutputs");
        if (!(loot instanceof List<?> lootOutputs) || lootOutputs.size() != 2) {
            throw new IllegalStateException("Expected two loot outputs but found " + loot + ".");
        }
        Object graph = result.get("powerGraph");
        if (!(graph instanceof List<?> powerGraph) || powerGraph.size() != 5) {
            throw new IllegalStateException("Expected five power graph nodes but found " + graph + ".");
        }
        Object ports = result.get("inventoryPorts");
        if (!(ports instanceof List<?> inventoryPorts) || inventoryPorts.size() != 3) {
            throw new IllegalStateException("Expected three inventory ports but found " + ports + ".");
        }

        System.out.println("Ashfall native Agent 9 tech runtime verifier PASS");
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
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
