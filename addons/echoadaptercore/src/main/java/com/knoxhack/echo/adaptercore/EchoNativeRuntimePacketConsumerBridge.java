package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRuntimePacketConsumerBridge {
    private final String moduleId;
    private final List<String> acceptedPacketIds = new ArrayList<>();
    private final List<String> acceptedConsumers = new ArrayList<>();

    public EchoNativeRuntimePacketConsumerBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> consume(
            String id,
            Map<String, Object> packetBindingReport,
            List<String> requiredConsumers) {
        List<Map<String, Object>> packets = packets(packetBindingReport);
        List<String> diagnostics = new ArrayList<>();
        if (requiredConsumers == null || requiredConsumers.isEmpty()) {
            diagnostics.add("Expected at least one required runtime packet consumer.");
        } else {
            for (String consumer : requiredConsumers) {
                acceptConsumer(consumer, packets, diagnostics);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "consumer bridge id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_runtime_packet_consumer");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native runtime packet consumer application");
        report.put("executionMode", "adaptercore_jdk_runtime_packet_consumer_binding");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("acceptedPacketIds", List.copyOf(acceptedPacketIds));
        report.put("acceptedConsumers", List.copyOf(acceptedConsumers));
        report.put("acceptedConsumerCount", acceptedConsumers.size());
        report.put("sourcePacketBindingReport", packetBindingReport == null
                ? ""
                : packetBindingReport.getOrDefault("id", ""));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore consumer bridge accepted all required prepared native runtime packets for this module without claiming live host mutation."
                : "AdapterCore consumer bridge is missing required prepared native runtime packet bindings.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> packets(Map<String, Object> packetBindingReport) {
        Object rawPackets = packetBindingReport == null ? null : packetBindingReport.get("packets");
        if (rawPackets instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private void acceptConsumer(String consumer, List<Map<String, Object>> packets, List<String> diagnostics) {
        String requiredConsumer = AdapterContractGuards.requireText(consumer, "runtime packet consumer");
        List<String> localDiagnostics = new ArrayList<>();
        Map<String, Object> packet = packetForConsumer(requiredConsumer, packets);
        if (packet.isEmpty()) {
            diagnostics.add("Missing runtime packet for consumer " + requiredConsumer + ".");
            return;
        }
        if (!Boolean.TRUE.equals(packet.get("adapterCorePacket"))) {
            localDiagnostics.add("Runtime packet for consumer " + requiredConsumer + " is not AdapterCore-backed.");
        }
        if (!Boolean.TRUE.equals(packet.get("nativeStateMutated"))) {
            localDiagnostics.add("Runtime packet for consumer " + requiredConsumer + " has no mutated native state.");
        }
        if (Boolean.TRUE.equals(packet.get("minecraftRuntimeAccessed"))) {
            localDiagnostics.add("Runtime packet for consumer " + requiredConsumer + " accessed Minecraft runtime.");
        }
        if (!localDiagnostics.isEmpty()) {
            diagnostics.addAll(localDiagnostics);
            return;
        }
        acceptedConsumers.add(requiredConsumer);
        acceptedPacketIds.add(String.valueOf(packet.get("id")));
    }

    private Map<String, Object> packetForConsumer(String consumer, List<Map<String, Object>> packets) {
        for (Map<String, Object> packet : packets) {
            if (packet.get("consumers") instanceof List<?> consumers && consumers.contains(consumer)) {
                return packet;
            }
        }
        return Map.of();
    }
}
