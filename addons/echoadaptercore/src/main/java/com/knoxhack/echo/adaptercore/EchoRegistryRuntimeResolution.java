package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoRegistryRuntimeResolution(
        String runtimeId,
        Map<String, List<String>> contentIdsByRegistry,
        Map<String, List<String>> recipesById,
        Map<String, List<String>> recipeInputsById,
        Map<String, String> recipeTypesById,
        Map<String, List<String>> lootById,
        Map<String, List<String>> soundsById,
        Map<String, List<String>> structuresById,
        Map<String, String> structureTypesById,
        Map<String, List<String>> tagsById,
        Map<String, List<String>> creativeGroupsById,
        List<String> terminalPages,
        List<String> indexEntries,
        List<String> requiredAssetIssues,
        boolean resourcePackMaterialized,
        int registeredContentCount,
        List<String> searchVisibleContentIds,
        int searchVisibleContentCount,
        Map<String, String> rendererAssetsByContentId,
        List<String> uiAssets,
        int mergedTagOverlayCount
) {
    public EchoRegistryRuntimeResolution {
        runtimeId = AdapterContractGuards.requireText(runtimeId, "runtime id");
        contentIdsByRegistry = copyMapOfLists(contentIdsByRegistry);
        recipesById = copyMapOfLists(recipesById);
        recipeInputsById = copyMapOfLists(recipeInputsById);
        recipeTypesById = Map.copyOf(Objects.requireNonNull(recipeTypesById, "recipeTypesById"));
        lootById = copyMapOfLists(lootById);
        soundsById = copyMapOfLists(soundsById);
        structuresById = copyMapOfLists(structuresById);
        structureTypesById = Map.copyOf(Objects.requireNonNull(structureTypesById, "structureTypesById"));
        tagsById = copyMapOfLists(tagsById);
        creativeGroupsById = copyMapOfLists(creativeGroupsById);
        terminalPages = List.copyOf(Objects.requireNonNull(terminalPages, "terminalPages"));
        indexEntries = List.copyOf(Objects.requireNonNull(indexEntries, "indexEntries"));
        requiredAssetIssues = List.copyOf(Objects.requireNonNull(requiredAssetIssues, "requiredAssetIssues"));
        searchVisibleContentIds = List.copyOf(Objects.requireNonNull(searchVisibleContentIds, "searchVisibleContentIds"));
        rendererAssetsByContentId = Map.copyOf(Objects.requireNonNull(rendererAssetsByContentId, "rendererAssetsByContentId"));
        uiAssets = List.copyOf(Objects.requireNonNull(uiAssets, "uiAssets"));
        if (registeredContentCount < 0) {
            throw new IllegalArgumentException("registered content count must not be negative");
        }
        if (searchVisibleContentCount < 0) {
            throw new IllegalArgumentException("search visible content count must not be negative");
        }
        if (mergedTagOverlayCount < 0) {
            throw new IllegalArgumentException("merged tag overlay count must not be negative");
        }
    }

    private static Map<String, List<String>> copyMapOfLists(Map<String, List<String>> values) {
        Objects.requireNonNull(values, "values");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            copy.put(
                    AdapterContractGuards.requireText(entry.getKey(), "map key"),
                    List.copyOf(Objects.requireNonNull(entry.getValue(), entry.getKey()))
            );
        }
        return copy;
    }
}
