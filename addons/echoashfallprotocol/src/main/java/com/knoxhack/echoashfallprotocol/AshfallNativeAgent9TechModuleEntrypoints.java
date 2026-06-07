package com.knoxhack.echoashfallprotocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallNativeAgent9TechModuleEntrypoints {
    private static final List<ModuleSpec> MODULES = List.of(
            new ModuleSpec("echomachinecore", "machine_runtime",
                    checks("placeMachine", "openMachineUi", "insertInput", "saveMachineState", "reloadMachineState")),
            new ModuleSpec("echopowercore", "power_node_runtime",
                    checks("consumePower", "powerGraphConnects")),
            new ModuleSpec("echopowergrid", "power_graph_runtime",
                    checks("powerGraphConnects", "powerGraph")),
            new ModuleSpec("echoindustrialnexus", "industrial_nexus_runtime",
                    checks("processRecipe", "multiblockValidation", "missionDependsOnMachineCompletion")),
            new ModuleSpec("echomultiblockcore", "multiblock_runtime",
                    checks("multiblockValidation")),
            new ModuleSpec("echologisticscore", "inventory_port_runtime",
                    checks("logisticsTransfer", "inventoryPorts")),
            new ModuleSpec("echologisticsnetwork", "logistics_route_runtime",
                    checks("logisticsTransfer", "oreGrinderInputCount")),
            new ModuleSpec("echobasegrid", "base_machine_dependency_runtime",
                    checks("saveMachineState", "reloadMachineState", "missionDependsOnMachineCompletion")),
            new ModuleSpec("echoconvoyprotocol", "convoy_vehicle_runtime",
                    checks("vehicleMovementAction.completed", "logisticsTransfer")),
            new ModuleSpec("echovehiclecore", "vehicle_runtime",
                    checks("vehicleMovementAction.completed")),
            new ModuleSpec("echoeconomycore", "trade_rule_runtime",
                    checks("economyCost.paid")),
            new ModuleSpec("echolootcore", "loot_output_runtime",
                    checks("lootOutputs")),
            new ModuleSpec("echorecipecore", "machine_recipe_runtime",
                    checks("recipeCatalogResourceLoaded", "processRecipe", "outputResult")));

    private AshfallNativeAgent9TechModuleEntrypoints() {
    }

    static Map<String, Object> execute(Map<String, String> context) {
        return fromRuntime(AshfallNativeAgent9TechRuntime.run(context));
    }

    static Map<String, Object> fromRuntime(Map<String, Object> runtime) {
        List<Map<String, Object>> entries = new ArrayList<>();
        boolean allPassed = "PASS".equals(value(runtime, "status"));
        for (ModuleSpec module : MODULES) {
            Map<String, Object> entry = module.execute(runtime);
            entries.add(entry);
            allPassed = allPassed && "PASS".equals(value(entry, "status"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceId", "echoashfallprotocol:agent9_tech_module_entrypoints");
        result.put("adapterCoreContract", AshfallNativeAgent9TechRuntime.CONTRACT_ID);
        result.put("runtime", "echo_native_loader");
        result.put("referenceBehavior", value(runtime, "referenceBehavior"));
        result.put("nativeRuntimeServiceId", value(runtime, "serviceId"));
        result.put("moduleEntrypointCount", entries.size());
        result.put("moduleEntrypoints", entries);
        result.put("status", allPassed ? "PASS" : "FAIL");
        result.put("summary", allPassed
                ? "Every Agent 9 owned module exposes a native-loader entrypoint backed by the executed Agent 9 tech runtime packet."
                : "One or more Agent 9 owned module entrypoints did not execute its required runtime behavior.");
        return Map.copyOf(result);
    }

    private static List<String> checks(String... keys) {
        return List.of(keys);
    }

    private static String value(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Object nestedValue(Map<String, Object> runtime, String path) {
        Object current = runtime;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    private static boolean passes(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() > 0;
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return value != null;
    }

    private record ModuleSpec(String moduleId, String entrypointKind, List<String> requiredEvidenceKeys) {
        Map<String, Object> execute(Map<String, Object> runtime) {
            List<Map<String, Object>> evidence = new ArrayList<>();
            boolean passed = true;
            for (String key : requiredEvidenceKeys) {
                Object observed = nestedValue(runtime, key);
                boolean checkPassed = passes(observed);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("key", key);
                row.put("observed", observed);
                row.put("passed", checkPassed);
                evidence.add(row);
                passed = passed && checkPassed;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("moduleId", moduleId);
            entry.put("entrypointId", moduleId + ":" + entrypointKind);
            entry.put("adapterCoreContract", AshfallNativeAgent9TechRuntime.CONTRACT_ID);
            entry.put("nativeRuntimeServiceId", value(runtime, "serviceId"));
            entry.put("runtime", "echo_native_loader");
            entry.put("referenceBehavior", value(runtime, "referenceBehavior"));
            entry.put("executedBehaviorIds", requiredEvidenceKeys);
            entry.put("behaviorEvidence", evidence);
            entry.put("status", passed ? "PASS" : "FAIL");
            return Map.copyOf(entry);
        }
    }
}
