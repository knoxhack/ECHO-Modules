package com.knoxhack.echo.adaptercore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRegistryParityVerifier {
    private EchoRegistryParityVerifier() {
    }

    public static EchoRegistryParityResult verifySampleContract() {
        EchoRegistryContractSnapshot snapshot = new EchoRegistryContractSnapshot(
                List.of(new EchoBlockDefinition(
                        "echoagent4:test_block",
                        "echoagent4",
                        "assets/echoagent4/blockstates/test_block.json",
                        "assets/echoagent4/models/block/test_block.json",
                        "assets/echoagent4/textures/block/test_block.png",
                        "block.echoagent4.test_block",
                        "block.echoagent4.test_block"
                )),
                List.of(new EchoItemDefinition(
                        "echoagent4:test_item",
                        "echoagent4",
                        "assets/echoagent4/models/item/test_item.json",
                        "assets/echoagent4/textures/item/test_item.png",
                        "item.echoagent4.test_item",
                        "item.echoagent4.test_item",
                        List.of("echoagent4:test_recipe"),
                        List.of("echoagent4:blocks/test_block"),
                        true
                )),
                List.of(new EchoEntityDefinition(
                        "echoagent4:test_entity",
                        "echoagent4",
                        "assets/echoagent4/models/entity/test_entity.json",
                        "assets/echoagent4/textures/entity/test_entity.png",
                        "entity.echoagent4.test_entity",
                        "entity.echoagent4.test_entity"
                )),
                List.of(new EchoRecipeDefinition(
                        "echoagent4:test_recipe",
                        "echoagent4",
                        "minecraft:crafting_shaped",
                        List.of("minecraft:stone"),
                        List.of("echoagent4:test_item"),
                        "data/echoagent4/recipes/test_recipe.json"
                )),
                List.of(new EchoLootDefinition(
                        "echoagent4:blocks/test_block",
                        "echoagent4",
                        List.of("echoagent4:test_item"),
                        "data/echoagent4/loot_tables/blocks/test_block.json"
                )),
                List.of(new EchoSoundDefinition(
                        "echoagent4:test_sound",
                        "echoagent4",
                        "subtitles.echoagent4.test_sound",
                        List.of("echoagent4:test_sound"),
                        "assets/echoagent4/sounds.json"
                )),
                List.of(new EchoStructureDefinition(
                        "echoagent4:test_structure",
                        "echoagent4",
                        "nbt",
                        "data/echoagent4/structures/test_structure.nbt",
                        List.of("echoagent4:test_block")
                )),
                List.of(new EchoTagDefinition(
                        "minecraft:mineable/pickaxe",
                        "echoagent4",
                        "blocks",
                        List.of("echoagent4:test_block"),
                        "data/echoagent4/tags/blocks/mineable/pickaxe.json",
                        2
                )),
                List.of(new EchoCreativeContentGroup(
                        "echoagent4:test_group",
                        "echoagent4",
                        List.of("echoagent4:test_item"),
                        "data/echoagent4/creative_groups/test_group.json"
                ))
        );

        List<String> terminalPages = List.of("terminal/agent4/test");
        List<String> indexEntries = List.of("echoagent4:test_item");
        EchoRegistryRuntimeResolution nativeResolution = new EchoNativeLoaderRegistryBackend()
                .resolve(snapshot, terminalPages, indexEntries);
        EchoRegistryRuntimeResolution standaloneResolution = new EchoStandaloneRegistryRuntimeBackend()
                .resolve(snapshot, terminalPages, indexEntries);
        return EchoRegistryParity.compare(nativeResolution, standaloneResolution);
    }

    public static void main(String[] args) throws IOException {
        EchoRegistryParityResult result = verifySampleContract();
        if (!result.passed()) {
            throw new IllegalStateException("Agent 4 registry parity failed: " + result.failedChecks());
        }
        Path reportPath = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("reports/echo/adaptercore/echoadaptercore-registry-backend-parity-smoke.json")
                        .toAbsolutePath()
                        .normalize();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.adaptercore.registry_backend_parity_smoke.v1");
        report.put("status", "PASS");
        report.put("moduleId", "echoadaptercore");
        report.put("behavior", "adaptercore_registry_backend_resolution");
        report.put("scope", "EchoAdapterCore registry contract parity across Echo Native Loader and Echo Standalone Runtime backends.");
        report.put("nativeBackend", EchoNativeLoaderRegistryBackend.RUNTIME_ID);
        report.put("standaloneBackend", EchoStandaloneRegistryRuntimeBackend.RUNTIME_ID);
        report.put("passedCheckCount", result.passedChecks().size());
        report.put("passedChecks", result.passedChecks());
        report.put("failedChecks", result.failedChecks());
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, toJson(report) + "\n", StandardCharsets.UTF_8);
        System.out.println("agent4 adaptercore registry parity PASS checks=" + result.passedChecks().size());
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n')
                        .append("  ")
                        .append(toJson(String.valueOf(entry.getKey())))
                        .append(": ")
                        .append(indent(toJson(entry.getValue())));
                first = false;
            }
            if (!map.isEmpty()) {
                out.append('\n');
            }
            return out.append('}').toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n').append("  ").append(indent(toJson(item)));
                first = false;
            }
            if (!collection.isEmpty()) {
                out.append('\n');
            }
            return out.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private static String indent(String value) {
        return value.replace("\n", "\n  ");
    }
}
