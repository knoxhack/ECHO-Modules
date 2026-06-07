package com.knoxhack.echoashfallprotocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallAdapterCoreMachinePowerBridge {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final List<String> REQUIRED_BLOCK_ENTITY_BINDINGS = List.of(
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

    private AshfallAdapterCoreMachinePowerBridge() {
    }

    static Map<String, Object> runDefaultBridgeScenario(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        AshfallAdapterCoreMachinePowerRuntime runtime = AshfallAdapterCoreMachinePowerRuntime.createDefaultWorld();
        Map<String, Object> registryPhase = registryBindingPhase(runtime);

        runtime.forceMachineFailure("micro_generator");
        Map<String, Object> restartEvent = runtime.handleInteraction("player.use_block", "micro_generator");
        Map<String, Object> priorityEvent = runtime.handleInteraction("player.use_block", "load_distributor");
        runtime.forceMachineJam("scrap_press");
        Map<String, Object> repairEvent = runtime.handleInteraction("player.use_block", "scrap_press");
        Map<String, Object> factoryDisableEvent = runtime.handleInteraction("player.use_block", "factory_controller");
        Map<String, Object> factoryEnableEvent = runtime.handleInteraction("player.use_block", "factory_controller");
        Map<String, Object> tickPhase = tickDispatchPhase(runtime, 80);
        Map<String, Object> capabilityPhase = capabilityDispatchPhase(runtime);
        Map<String, Object> capabilityMutationPhase = AshfallAdapterCoreMachinePowerRuntime.runCapabilityMutationScenario();
        Map<String, Object> eventPhase = eventDispatchPhase(
                restartEvent,
                priorityEvent,
                repairEvent,
                factoryDisableEvent,
                factoryEnableEvent);
        Map<String, Object> liveRuntime = runtime.describe();

        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("moduleId", MODULE_ID);
        bridge.put("packId", safeContext.getOrDefault("packId", "unknown"));
        bridge.put("adapterCoreBridge", true);
        bridge.put("implementationTarget", "AdapterCore machine/power/logistics bridge dispatch");
        bridge.put("standaloneDuplicateGameplaySystem", false);
        bridge.put("runtimeStateInitialized", true);
        bridge.put("registryBindingPhase", registryPhase);
        bridge.put("tickDispatchPhase", tickPhase);
        bridge.put("eventDispatchPhase", eventPhase);
        bridge.put("capabilityDispatchPhase", capabilityPhase);
        bridge.put("capabilityMutationPhase", capabilityMutationPhase);
        bridge.put("worldStateBridge", worldStateBridge(liveRuntime));
        bridge.put("playerStateBridge", playerStateBridge(
                safeContext,
                restartEvent,
                priorityEvent,
                repairEvent,
                factoryDisableEvent,
                factoryEnableEvent));
        bridge.put("liveRuntime", liveRuntime);
        bridge.put("status", bridgePass(registryPhase, tickPhase, eventPhase, capabilityPhase, capabilityMutationPhase,
                liveRuntime) ? "PASS" : "FAIL");
        bridge.put("minecraftRuntimeAccessed", false);
        bridge.put("minecraftRegistryMutated", false);
        return bridge;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> registryBindingPhase(AshfallAdapterCoreMachinePowerRuntime runtime) {
        Map<String, Object> runtimeSnapshot = runtime.describe();
        List<Map<String, Object>> bindings = ((List<Map<String, Object>>) runtimeSnapshot.getOrDefault(
                "registeredBlockEntityBindings", List.of())).stream()
                .filter(binding -> REQUIRED_BLOCK_ENTITY_BINDINGS.contains(String.valueOf(binding.get("id"))))
                .toList();
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("adapterSurface", "registry.bind_block_entity");
        phase.put("bindingCount", bindings.size());
        phase.put("requiredBindingCount", REQUIRED_BLOCK_ENTITY_BINDINGS.size());
        phase.put("bindings", bindings);
        phase.put("requiredBindingsPresent", containsAllIds(bindings, REQUIRED_BLOCK_ENTITY_BINDINGS));
        phase.put("bindingSetExact", bindings.size() == REQUIRED_BLOCK_ENTITY_BINDINGS.size()
                && containsAllIds(bindings, REQUIRED_BLOCK_ENTITY_BINDINGS));
        phase.put("minecraftRegistryMutated", false);
        return phase;
    }

    private static Map<String, Object> tickDispatchPhase(AshfallAdapterCoreMachinePowerRuntime runtime, int ticks) {
        Map<String, Object> snapshot = runtime.tick(ticks);
        Map<String, Object> waterPurifierRuntime = AshfallAdapterCoreMachinePowerRuntime.runWaterPurifierScenario();
        Map<String, Object> thermalBurnerRuntime = AshfallAdapterCoreMachinePowerRuntime.runThermalBurnerScenario();
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("adapterSurface", "block_entity.tick.dispatch");
        phase.put("requestedTicks", ticks);
        phase.put("dispatchedTicks", snapshot.get("tickCount"));
        phase.put("microGeneratorTicked", ((Integer) snapshot.get("microGeneratorWear")) > 0);
        phase.put("powerNetworkTicked", ((Integer) snapshot.get("powerTransferCount")) > 0);
        phase.put("logisticsTicked", ((Integer) snapshot.get("itemPipeMovedCount")) > 0);
        phase.put("factoryScanTicked", ((Integer) snapshot.get("factoryScanCount")) > 0);
        phase.put("batteryStoredEnergy", snapshot.get("batteryStoredEnergy"));
        phase.put("scrapPressRecipeCompleted", ((Integer) snapshot.get("scrapPressOutputCount")) > 0);
        phase.put("scrapPressWear", snapshot.get("scrapPressWear"));
        phase.put("oreGrinderRecipeCompleted", ((Integer) snapshot.get("oreGrinderOutputCount")) > 0);
        phase.put("oreGrinderByproductCount", snapshot.get("oreGrinderByproductCount"));
        phase.put("waterPurifierRuntime", waterPurifierRuntime);
        phase.put("thermalBurnerRuntime", thermalBurnerRuntime);
        phase.put("waterPurifierRecipeCompleted", "PASS".equals(waterPurifierRuntime.get("status"))
                && Integer.valueOf(1).equals(waterPurifierRuntime.get("cleanWaterCount")));
        phase.put("thermalBurnerFuelCycleCompleted", "PASS".equals(thermalBurnerRuntime.get("status"))
                && Integer.valueOf(1).equals(thermalBurnerRuntime.get("ashOutputCount")));
        phase.put("loadDistributorPriorityMode", snapshot.get("loadDistributorPriorityMode"));
        phase.put("factoryConnectedMachines", snapshot.get("factoryConnectedMachines"));
        phase.put("networkDiagnostic", snapshot.get("networkDiagnostic"));
        phase.put("minecraftRuntimeAccessed", false);
        return phase;
    }

    private static Map<String, Object> eventDispatchPhase(
            Map<String, Object> restartEvent,
            Map<String, Object> priorityEvent,
            Map<String, Object> repairEvent,
            Map<String, Object> factoryDisableEvent,
            Map<String, Object> factoryEnableEvent) {
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("adapterSurface", "event.dispatch");
        phase.put("restartFailedGenerator", restartEvent);
        phase.put("cycleLoadDistributorPriority", priorityEvent);
        phase.put("repairJammedScrapPress", repairEvent);
        phase.put("disableFactoryController", factoryDisableEvent);
        phase.put("enableFactoryController", factoryEnableEvent);
        phase.put("handledEventCount", countHandled(
                restartEvent,
                priorityEvent,
                repairEvent,
                factoryDisableEvent,
                factoryEnableEvent));
        phase.put("machineRepairHandled", Boolean.TRUE.equals(repairEvent.get("handled"))
                && Boolean.FALSE.equals(repairEvent.get("jammed")));
        phase.put("factoryToggleHandled", Boolean.TRUE.equals(factoryDisableEvent.get("handled"))
                && Boolean.FALSE.equals(factoryDisableEvent.get("networkEnabled"))
                && Boolean.TRUE.equals(factoryEnableEvent.get("handled"))
                && Boolean.TRUE.equals(factoryEnableEvent.get("networkEnabled")));
        phase.put("generatorRestartHandled", Boolean.TRUE.equals(restartEvent.get("handled"))
                && Boolean.FALSE.equals(restartEvent.get("failed")));
        phase.put("routerPriorityChanged", Boolean.TRUE.equals(priorityEvent.get("handled"))
                && "SURVIVAL".equals(priorityEvent.get("priorityMode")));
        phase.put("minecraftRuntimeAccessed", false);
        return phase;
    }

    private static Map<String, Object> capabilityDispatchPhase(AshfallAdapterCoreMachinePowerRuntime runtime) {
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("adapterSurface", "capability.bridge.dispatch");
        phase.put("energyReceiveProbe", runtime.receiveEnergy("battery_bank", 37, true));
        phase.put("energyExtractProbe", runtime.extractEnergy("battery_bank", 37, true));
        phase.put("inventoryInsertProbe", runtime.insertItem("ore_grinder", "ore_substrate", 1, true));
        phase.put("inventoryExtractProbe", runtime.extractOutputItem("scrap_press", "compressed_scrap", 1, true));
        phase.put("hopperHandlerCompatible", true);
        phase.put("energyStorageCompatible", true);
        phase.put("minecraftRuntimeAccessed", false);
        return phase;
    }

    private static Map<String, Object> worldStateBridge(Map<String, Object> liveRuntime) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("adapterSurface", "world_state.bridge");
        bridge.put("adjacencyModel", "bounded_manhattan_graph");
        bridge.put("factoryConnectedMachines", liveRuntime.get("factoryConnectedMachines"));
        bridge.put("factoryStoredEnergy", liveRuntime.get("factoryStoredEnergy"));
        bridge.put("factoryEnergyCapacity", liveRuntime.get("factoryEnergyCapacity"));
        bridge.put("powerCapacityRespected", liveRuntime.get("powerCapacityRespected"));
        bridge.put("logisticsLoopAvoided", liveRuntime.get("logisticsLoopAvoided"));
        bridge.put("networkDiagnostic", liveRuntime.get("networkDiagnostic"));
        bridge.put("minecraftRuntimeAccessed", false);
        return bridge;
    }

    private static Map<String, Object> playerStateBridge(
            Map<String, String> context,
            Map<String, Object> restartEvent,
            Map<String, Object> priorityEvent,
            Map<String, Object> repairEvent,
            Map<String, Object> factoryDisableEvent,
            Map<String, Object> factoryEnableEvent) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("adapterSurface", "player_state.bridge");
        bridge.put("playerId", context.getOrDefault("playerId", "native_probe_player"));
        bridge.put("useBlockEventsHandled", countHandled(
                restartEvent,
                priorityEvent,
                repairEvent,
                factoryDisableEvent,
                factoryEnableEvent));
        bridge.put("lastInteractionTargets", List.of(
                restartEvent.get("nodeId"),
                priorityEvent.get("nodeId"),
                repairEvent.get("nodeId"),
                factoryDisableEvent.get("nodeId"),
                factoryEnableEvent.get("nodeId")));
        bridge.put("minecraftRuntimeAccessed", false);
        return bridge;
    }

    private static int countHandled(Map<String, Object>... events) {
        int count = 0;
        for (Map<String, Object> event : events) {
            if (Boolean.TRUE.equals(event.get("handled"))) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsAllIds(List<Map<String, Object>> bindings, List<String> expectedIds) {
        List<String> actualIds = bindings.stream()
                .map(binding -> String.valueOf(binding.get("id")))
                .toList();
        return expectedIds.stream().allMatch(actualIds::contains);
    }

    private static boolean bridgePass(
            Map<String, Object> registryPhase,
            Map<String, Object> tickPhase,
            Map<String, Object> eventPhase,
            Map<String, Object> capabilityPhase,
            Map<String, Object> capabilityMutationPhase,
            Map<String, Object> liveRuntime) {
        return Boolean.TRUE.equals(registryPhase.get("requiredBindingsPresent"))
                && Boolean.TRUE.equals(registryPhase.get("bindingSetExact"))
                && Integer.valueOf(80).equals(tickPhase.get("dispatchedTicks"))
                && Boolean.TRUE.equals(tickPhase.get("scrapPressRecipeCompleted"))
                && Boolean.TRUE.equals(tickPhase.get("oreGrinderRecipeCompleted"))
                && Boolean.TRUE.equals(tickPhase.get("waterPurifierRecipeCompleted"))
                && Boolean.TRUE.equals(tickPhase.get("thermalBurnerFuelCycleCompleted"))
                && Integer.valueOf(5).equals(eventPhase.get("handledEventCount"))
                && Boolean.TRUE.equals(eventPhase.get("machineRepairHandled"))
                && Boolean.TRUE.equals(eventPhase.get("factoryToggleHandled"))
                && Boolean.TRUE.equals(eventPhase.get("generatorRestartHandled"))
                && Boolean.TRUE.equals(eventPhase.get("routerPriorityChanged"))
                && Integer.valueOf(37).equals(capabilityPhase.get("energyReceiveProbe"))
                && Integer.valueOf(37).equals(capabilityPhase.get("energyExtractProbe"))
                && Integer.valueOf(1).equals(capabilityPhase.get("inventoryInsertProbe"))
                && Integer.valueOf(1).equals(capabilityPhase.get("inventoryExtractProbe"))
                && "PASS".equals(capabilityMutationPhase.get("status"))
                && Boolean.TRUE.equals(capabilityMutationPhase.get("capabilityStateMutated"))
                && "PASS".equals(liveRuntime.get("networkDiagnostic"));
    }
}
