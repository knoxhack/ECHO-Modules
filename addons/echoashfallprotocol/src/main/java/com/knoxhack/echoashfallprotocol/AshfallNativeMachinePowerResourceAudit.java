package com.knoxhack.echoashfallprotocol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallNativeMachinePowerResourceAudit {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final List<String> BLOCKS = List.of(
            "micro_generator",
            "scrap_dynamo",
            "battery_bank",
            "scrap_press",
            "ore_grinder",
            "isotope_refiner",
            "radiation_cleanser",
            "crystalline_synthesizer",
            "deep_core_miner",
            "autofeed_hopper",
            "contaminant_condenser",
            "item_pipe",
            "power_cable",
            "reinforced_power_cable",
            "high_voltage_power_cable",
            "load_distributor",
            "factory_controller"
    );

    private AshfallNativeMachinePowerResourceAudit() {
    }

    static Map<String, Object> run(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Path root = Path.of("").toAbsolutePath().normalize();
        Path resourceRoot = root.resolve("addons/echoashfallprotocol/src/main/resources");
        List<Map<String, Object>> entries = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();

        for (String block : BLOCKS) {
            Map<String, Object> entry = auditBlock(resourceRoot, block);
            entries.add(entry);
            @SuppressWarnings("unchecked")
            List<String> missing = (List<String>) entry.get("missing");
            for (String miss : missing) {
                diagnostics.add(block + " missing " + miss);
            }
        }

        boolean pass = diagnostics.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("serviceId", "echoashfallprotocol:machine_power_resource_fidelity");
        result.put("adapterCoreBridge", true);
        result.put("implementationTarget", "AdapterCore resource visibility report");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("resourceRoot", resourceRoot.toString());
        result.put("auditedBlockCount", entries.size());
        result.put("passingBlockCount", entries.stream().filter(e -> Boolean.TRUE.equals(e.get("complete"))).count());
        result.put("requiredSurfaceCountPerBlock", 6);
        result.put("auditedBlocks", entries);
        result.put("diagnostics", List.copyOf(diagnostics));
        result.put("nativeReportVisibility", pass);
        result.put("minecraftRuntimeAccessed", false);
        result.put("minecraftRegistryMutated", false);
        result.put("status", pass ? "PASS" : "FAIL");
        result.put("summary", pass
                ? "All Ashfall machine, power, and logistics blocks have blockstate, block model, item model, item definition, block texture, loot table, and native report visibility evidence."
                : "Ashfall machine, power, and logistics resource fidelity is incomplete.");
        return result;
    }

    private static Map<String, Object> auditBlock(Path resourceRoot, String block) {
        Map<String, Path> required = new LinkedHashMap<>();
        required.put("blockstate", resourceRoot.resolve("assets/echoashfallprotocol/blockstates/" + block + ".json"));
        required.put("blockModel", resourceRoot.resolve("assets/echoashfallprotocol/models/block/" + block + ".json"));
        required.put("itemModel", resourceRoot.resolve("assets/echoashfallprotocol/models/item/" + block + ".json"));
        required.put("itemDefinition", resourceRoot.resolve("assets/echoashfallprotocol/items/" + block + ".json"));
        required.put("blockTexture", resourceRoot.resolve("assets/echoashfallprotocol/textures/block/" + block + ".png"));
        required.put("lootTable", resourceRoot.resolve("data/echoashfallprotocol/loot_table/blocks/" + block + ".json"));

        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, String> paths = new LinkedHashMap<>();
        for (Map.Entry<String, Path> check : required.entrySet()) {
            paths.put(check.getKey(), check.getValue().toString());
            if (Files.isRegularFile(check.getValue())) {
                present.add(check.getKey());
            } else {
                missing.add(check.getKey());
            }
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", MODULE_ID + ":" + block);
        entry.put("surfacesPresent", List.copyOf(present));
        entry.put("missing", List.copyOf(missing));
        entry.put("paths", paths);
        entry.put("nativeReportVisible", true);
        entry.put("complete", missing.isEmpty());
        return entry;
    }
}
