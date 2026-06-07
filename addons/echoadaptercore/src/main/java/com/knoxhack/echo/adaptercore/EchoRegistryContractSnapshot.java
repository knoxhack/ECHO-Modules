package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoRegistryContractSnapshot(
        List<EchoBlockDefinition> blocks,
        List<EchoItemDefinition> items,
        List<EchoEntityDefinition> entities,
        List<EchoRecipeDefinition> recipes,
        List<EchoLootDefinition> lootTables,
        List<EchoSoundDefinition> sounds,
        List<EchoStructureDefinition> structures,
        List<EchoTagDefinition> tags,
        List<EchoCreativeContentGroup> creativeGroups
) {
    public EchoRegistryContractSnapshot {
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        entities = List.copyOf(Objects.requireNonNull(entities, "entities"));
        recipes = List.copyOf(Objects.requireNonNull(recipes, "recipes"));
        lootTables = List.copyOf(Objects.requireNonNull(lootTables, "lootTables"));
        sounds = List.copyOf(Objects.requireNonNull(sounds, "sounds"));
        structures = List.copyOf(Objects.requireNonNull(structures, "structures"));
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        creativeGroups = List.copyOf(Objects.requireNonNull(creativeGroups, "creativeGroups"));
    }

    public Map<String, List<String>> contentIdsByRegistry() {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        ids.put("blocks", ids(blocks.stream().map(EchoBlockDefinition::id).toList()));
        ids.put("items", ids(items.stream().map(EchoItemDefinition::id).toList()));
        ids.put("entities", ids(entities.stream().map(EchoEntityDefinition::id).toList()));
        ids.put("recipes", ids(recipes.stream().map(EchoRecipeDefinition::id).toList()));
        ids.put("lootTables", ids(lootTables.stream().map(EchoLootDefinition::id).toList()));
        ids.put("sounds", ids(sounds.stream().map(EchoSoundDefinition::id).toList()));
        ids.put("structures", ids(structures.stream().map(EchoStructureDefinition::id).toList()));
        ids.put("tags", ids(tags.stream().map(EchoTagDefinition::id).toList()));
        ids.put("creativeGroups", ids(creativeGroups.stream().map(EchoCreativeContentGroup::id).toList()));
        return ids;
    }

    public List<String> requiredAssetIssues() {
        List<String> issues = new ArrayList<>();
        for (EchoBlockDefinition block : blocks) {
            requireAsset(issues, block.id(), "blockstate", block.blockstate());
            requireAsset(issues, block.id(), "model", block.model());
            requireAsset(issues, block.id(), "texture", block.texture());
            requireAsset(issues, block.id(), "langKey", block.langKey());
            requireAsset(issues, block.id(), "lang", block.lang());
        }
        for (EchoItemDefinition item : items) {
            requireAsset(issues, item.id(), "model", item.model());
            requireAsset(issues, item.id(), "texture", item.texture());
            requireAsset(issues, item.id(), "langKey", item.langKey());
            requireAsset(issues, item.id(), "lang", item.lang());
        }
        for (EchoEntityDefinition entity : entities) {
            requireAsset(issues, entity.id(), "model", entity.model());
            requireAsset(issues, entity.id(), "texture", entity.texture());
            requireAsset(issues, entity.id(), "langKey", entity.langKey());
            requireAsset(issues, entity.id(), "lang", entity.lang());
        }
        return List.copyOf(issues);
    }

    public int totalDefinitions() {
        return blocks.size()
                + items.size()
                + entities.size()
                + recipes.size()
                + lootTables.size()
                + sounds.size()
                + structures.size()
                + tags.size()
                + creativeGroups.size();
    }

    public int mergedTagOverlayCount() {
        return tags.stream().mapToInt(tag -> Math.max(0, tag.mergedSourceCount() - 1)).sum();
    }

    private static List<String> ids(List<String> ids) {
        return ids.stream().sorted().toList();
    }

    private static void requireAsset(List<String> issues, String id, String kind, String value) {
        if (value == null || value.isBlank()) {
            issues.add(id + " missing " + kind);
        }
    }
}
