package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRuntimeApiFreeze {
    private EchoNativeRuntimeApiFreeze() {
    }

    public static Map<String, Object> describe(String moduleId) {
        List<Map<String, Object>> interfaces = interfaces();
        List<String> diagnostics = validate(interfaces);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(moduleId, "module id") + ":adaptercore_native_runtime_api_freeze");
        report.put("moduleId", moduleId);
        report.put("apiVersion", EchoNativeRuntimeHost.API_VERSION);
        report.put("bridge", "adaptercore.native_runtime_api_freeze");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Frozen AdapterCore native runtime host interfaces");
        report.put("executionMode", "adaptercore_jdk_api_contract");
        report.put("interfaceCount", interfaces.size());
        report.put("interfaces", interfaces);
        report.put("requiredCategories", requiredCategories());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("frozenForReleaseCandidate", diagnostics.isEmpty());
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore native runtime host API is frozen for inventory, player state, world blocks/state, structures, block entities, capabilities, events, packets, HUD, and save data."
                : "AdapterCore native runtime host API freeze is missing required surfaces.");
        return Map.copyOf(report);
    }

    private static List<Map<String, Object>> interfaces() {
        return List.of(
                surface("inventory", "EchoNativeRuntimeHost.PlayerInventory",
                        "grant", "remove", "snapshot"),
                surface("player_state", "EchoNativeRuntimeHost.PlayerState",
                        "teleport", "bindRespawn", "grantAdvancement", "writePersistentState"),
                surface("world_blocks", "EchoNativeRuntimeHost.WorldBlocks",
                        "setBlock", "clearBlock", "blockState", "isLoaded"),
                surface("world_state", "EchoNativeRuntimeHost.WorldState",
                        "writeMarker", "writeWeatherState", "writeRouteState"),
                surface("structures", "EchoNativeRuntimeHost.Structures",
                        "placeStructure"),
                surface("block_entities", "EchoNativeRuntimeHost.BlockEntities",
                        "tick", "snapshot", "applySnapshot"),
                surface("capabilities", "EchoNativeRuntimeHost.Capabilities",
                        "insertItem", "extractItem", "receiveEnergy", "extractEnergy", "readCapability"),
                surface("events", "EchoNativeRuntimeHost.Events",
                        "publish"),
                surface("packets", "EchoNativeRuntimeHost.Packets",
                        "sendToPlayer", "broadcast"),
                surface("hud", "EchoNativeRuntimeHost.Hud",
                        "publishNotification"),
                surface("save_data", "EchoNativeRuntimeHost.SaveData",
                        "write", "read", "delete")
        );
    }

    private static Map<String, Object> surface(String category, String interfaceName, String... operations) {
        Map<String, Object> surface = new LinkedHashMap<>();
        surface.put("category", category);
        surface.put("interface", interfaceName);
        surface.put("operations", List.of(operations));
        surface.put("nativeRuntimeHostSurface", true);
        surface.put("apiVersion", EchoNativeRuntimeHost.API_VERSION);
        return Map.copyOf(surface);
    }

    private static List<String> validate(List<Map<String, Object>> interfaces) {
        List<String> diagnostics = new ArrayList<>();
        for (String category : requiredCategories()) {
            boolean found = interfaces.stream().anyMatch(surface -> category.equals(surface.get("category")));
            if (!found) {
                diagnostics.add("Missing frozen native runtime interface for " + category + ".");
            }
        }
        for (Map<String, Object> surface : interfaces) {
            Object operations = surface.get("operations");
            if (!(operations instanceof List<?> list) || list.isEmpty()) {
                diagnostics.add("Frozen native runtime interface " + surface.get("interface") + " has no operations.");
            }
        }
        return diagnostics;
    }

    private static List<String> requiredCategories() {
        return List.of(
                "inventory",
                "player_state",
                "world_blocks",
                "world_state",
                "structures",
                "block_entities",
                "capabilities",
                "events",
                "packets",
                "hud",
                "save_data"
        );
    }
}
