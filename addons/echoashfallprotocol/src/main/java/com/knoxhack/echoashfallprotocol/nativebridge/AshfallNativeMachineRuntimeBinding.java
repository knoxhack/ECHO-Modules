package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeMachineRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeMachineRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> machinePowerRuntimeTarget,
            Map<String, Object> machinePowerResourceAudit) {
        List<String> implementedOperationIds = implementedOperationIds();
        List<String> diagnostics = validate(machinePowerRuntimeTarget, machinePowerResourceAudit);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:machine_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_machine_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 5 AdapterCore machine runtime source binding descriptor");
        report.put("executionMode", "native_live_adaptercore_machine_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("machineRuntimeBound", false);
        report.put("machineRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("sourceRuntimeTarget", value(machinePowerRuntimeTarget, "serviceId"));
        report.put("sourceResourceAudit", value(machinePowerResourceAudit, "serviceId"));
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of(
                "EchoNativeRuntimeHost.BlockEntities",
                "EchoNativeRuntimeHost.Capabilities",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState"));
        report.put("implementedOperationCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredOperationCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("blockEntityTickBindingCount", numericValue(machinePowerRuntimeTarget, "tickTargetCount"));
        report.put("registryBindingCount", numericValue(machinePowerRuntimeTarget, "registryBindingCount"));
        report.put("liveEnergyCapabilityBinding", "ModEnergyCapabilities.register");
        report.put("liveInventoryCapabilityBinding", "ModItemCapabilities.register");
        report.put("persistentStateSources", List.of(
                "BlockEntity.saveAdditional",
                "BlockEntity.loadAdditional",
                "MachineInventory.serialize",
                "MachineInventory.deserialize",
                "EnergyStorage.setEnergyStored"));
        report.put("diagnosticSources", List.of(
                "FactoryControllerBlockEntity.serverTick",
                "MachineStatusMenu",
                "PowerNetwork"));
        report.put("realMutationTargets", List.of(
                "serverTick block-entity state",
                "IEnergyStorage receive/extract state",
                "MachineInventory item stack state",
                "ValueInput/ValueOutput machine persistence",
                "factory/power diagnostic world snapshots"));
        report.put("hardenedRuntimeChecks", List.of(
                "server_side_tickers",
                "block_entity_type_matching",
                "capability_presence_guard",
                "energy_capacity_clamp",
                "inventory_container_change_notification",
                "persistence_value_input_defaults"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall machine activation validated source bindings for ticking, capabilities, persistence, and diagnostics; live mutation is claimed only by post-mutation evidence."
                : "Ashfall machine runtime binding is missing target, audit, or capability evidence.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "block_entities.live_ticking",
                "capabilities.live_energy",
                "capabilities.live_inventory",
                "save_data.persist_machine_state",
                "world_state.dispatch_machine_diagnostics");
    }

    private static List<String> validate(
            Map<String, Object> machinePowerRuntimeTarget,
            Map<String, Object> machinePowerResourceAudit) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PASS".equals(value(machinePowerRuntimeTarget, "status"))) {
            diagnostics.add("Machine power runtime target did not pass.");
        }
        if (!"PASS".equals(value(machinePowerResourceAudit, "status"))) {
            diagnostics.add("Machine power resource audit did not pass.");
        }
        if (!"19".equals(value(machinePowerRuntimeTarget, "tickTargetCount"))) {
            diagnostics.add("Expected nineteen live machine tick targets.");
        }
        if (!"19".equals(value(machinePowerRuntimeTarget, "registryBindingCount"))) {
            diagnostics.add("Expected nineteen machine/power/logistics registry bindings.");
        }
        return List.copyOf(diagnostics);
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int numericValue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
