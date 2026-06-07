package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRuntimePacketBridge {
    private final String moduleId;
    private final List<Map<String, Object>> packets = new ArrayList<>();

    public EchoNativeRuntimePacketBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeRuntimePacketBridge packet(
            String id,
            String surface,
            String sourceRuntimeTarget,
            Map<String, Object> payload,
            List<String> consumers) {
        Map<String, Object> packet = new LinkedHashMap<>();
        packet.put("id", AdapterContractGuards.requireText(id, "packet id"));
        packet.put("surface", AdapterContractGuards.requireText(surface, "packet surface"));
        packet.put("sourceRuntimeTarget", AdapterContractGuards.requireText(sourceRuntimeTarget, "packet source runtime target"));
        packet.put("adapterCorePacket", true);
        packet.put("adapterCoreBridge", "adaptercore.native_runtime_packet");
        packet.put("runtimeStateInitialized", true);
        packet.put("nativeStateMutated", payload != null && Boolean.TRUE.equals(payload.get("nativeStateMutated")));
        packet.put("liveRuntimeMutation", payload != null && Boolean.TRUE.equals(payload.get("liveRuntimeMutation")));
        packet.put("runtimePacketPreparedForHostDispatch", true);
        packet.put("minecraftRuntimeAccessed", false);
        packet.put("minecraftRuntimeMutated", false);
        packet.put("minecraftRegistryMutated", false);
        packet.put("standaloneDuplicateGameplaySystem", false);
        packet.put("consumers", consumers == null ? List.of() : List.copyOf(consumers));
        packet.put("consumerCount", consumers == null ? 0 : consumers.size());
        packet.put("payload", payload == null ? Map.of() : Map.copyOf(payload));
        packets.add(packet);
        return this;
    }

    public Map<String, Object> describe(String id) {
        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime packet bridge id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_runtime_packet");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native runtime packet binding bridge");
        report.put("executionMode", "adaptercore_jdk_runtime_packet_binding");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("packetCount", packets.size());
        report.put("boundConsumerCount", boundConsumerCount());
        report.put("packets", List.copyOf(packets));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared native runtime packets for their declared consumer modules without claiming live host mutation."
                : "AdapterCore runtime packet bindings are missing required packet or consumer evidence.");
        return report;
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        if (packets.isEmpty()) {
            diagnostics.add("Expected at least one AdapterCore runtime packet.");
        }
        for (Map<String, Object> packet : packets) {
            if (!Boolean.TRUE.equals(packet.get("adapterCorePacket"))) {
                diagnostics.add("Packet " + packet.get("id") + " is not AdapterCore-backed.");
            }
            if (!Boolean.TRUE.equals(packet.get("nativeStateMutated"))) {
                diagnostics.add("Packet " + packet.get("id") + " does not expose mutated native state.");
            }
            if (Boolean.TRUE.equals(packet.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Packet " + packet.get("id") + " accessed Minecraft runtime.");
            }
            if (!(packet.get("consumers") instanceof List<?> consumers) || consumers.isEmpty()) {
                diagnostics.add("Packet " + packet.get("id") + " has no bound consumers.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private int boundConsumerCount() {
        int count = 0;
        for (Map<String, Object> packet : packets) {
            if (packet.get("consumers") instanceof List<?> consumers) {
                count += consumers.size();
            }
        }
        return count;
    }
}
