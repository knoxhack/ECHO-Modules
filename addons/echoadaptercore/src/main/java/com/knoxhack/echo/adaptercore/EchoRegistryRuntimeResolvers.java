package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoRegistryRuntimeResolvers {
    private EchoRegistryRuntimeResolvers() {
    }

    static EchoRegistryRuntimeResolution resolve(
            String runtimeId,
            EchoRegistryContractSnapshot snapshot,
            List<String> terminalPages,
            List<String> indexEntries,
            boolean resourcePackMaterialized
    ) {
        Map<String, List<String>> recipes = new LinkedHashMap<>();
        Map<String, List<String>> recipeInputs = new LinkedHashMap<>();
        Map<String, String> recipeTypes = new LinkedHashMap<>();
        for (EchoRecipeDefinition recipe : snapshot.recipes()) {
            recipes.put(recipe.id(), recipe.outputs());
            recipeInputs.put(recipe.id(), recipe.inputs());
            recipeTypes.put(recipe.id(), recipe.type());
        }

        Map<String, List<String>> loot = new LinkedHashMap<>();
        for (EchoLootDefinition lootTable : snapshot.lootTables()) {
            loot.put(lootTable.id(), lootTable.entries());
        }

        Map<String, List<String>> sounds = new LinkedHashMap<>();
        for (EchoSoundDefinition sound : snapshot.sounds()) {
            sounds.put(sound.id(), sound.sounds());
        }

        Map<String, List<String>> structures = new LinkedHashMap<>();
        Map<String, String> structureTypes = new LinkedHashMap<>();
        for (EchoStructureDefinition structure : snapshot.structures()) {
            structures.put(structure.id(), structure.references());
            structureTypes.put(structure.id(), structure.kind());
        }

        Map<String, List<String>> tags = new LinkedHashMap<>();
        for (EchoTagDefinition tag : snapshot.tags()) {
            tags.put(tag.id(), tag.values());
        }

        Map<String, List<String>> creativeGroups = new LinkedHashMap<>();
        for (EchoCreativeContentGroup group : snapshot.creativeGroups()) {
            creativeGroups.put(group.id(), group.entries());
        }

        Map<String, String> rendererAssets = new LinkedHashMap<>();
        for (EchoBlockDefinition block : snapshot.blocks()) {
            rendererAssets.put(block.id(), block.model());
        }
        for (EchoItemDefinition item : snapshot.items()) {
            rendererAssets.put(item.id(), item.model());
        }
        for (EchoEntityDefinition entity : snapshot.entities()) {
            rendererAssets.put(entity.id(), entity.model());
        }

        return new EchoRegistryRuntimeResolution(
                runtimeId,
                snapshot.contentIdsByRegistry(),
                recipes,
                recipeInputs,
                recipeTypes,
                loot,
                sounds,
                structures,
                structureTypes,
                tags,
                creativeGroups,
                terminalPages,
                indexEntries,
                snapshot.requiredAssetIssues(),
                resourcePackMaterialized,
                snapshot.totalDefinitions(),
                searchVisibleContentIds(snapshot),
                searchVisibleContentIds(snapshot).size(),
                rendererAssets,
                List.copyOf(terminalPages),
                snapshot.mergedTagOverlayCount()
        );
    }

    static List<String> searchVisibleContentIds(EchoRegistryContractSnapshot snapshot) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (EchoBlockDefinition block : snapshot.blocks()) {
            ids.add(block.id());
        }
        for (EchoEntityDefinition entity : snapshot.entities()) {
            ids.add(entity.id());
        }
        for (EchoItemDefinition item : snapshot.items()) {
            if (item.searchVisible()) {
                ids.add(item.id());
            }
        }
        return ids.stream().sorted().toList();
    }
}
