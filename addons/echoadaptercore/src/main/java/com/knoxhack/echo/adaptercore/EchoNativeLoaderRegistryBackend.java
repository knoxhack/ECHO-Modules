package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLoaderRegistryBackend implements EchoRegistryRuntimeBackend {
    public static final String RUNTIME_ID = "echo-native-loader";

    @Override
    public String runtimeId() {
        return RUNTIME_ID;
    }

    @Override
    public EchoRegistryRuntimeResolution resolve(
            EchoRegistryContractSnapshot snapshot,
            List<String> terminalPages,
            List<String> indexEntries
    ) {
        return EchoRegistryRuntimeResolvers.resolve(runtimeId(), snapshot, terminalPages, indexEntries, materializeResourcePack(snapshot));
    }

    public boolean materializeResourcePack(EchoRegistryContractSnapshot snapshot) {
        return snapshot.requiredAssetIssues().isEmpty();
    }

    public Map<String, List<String>> registerContentDefinitions(EchoRegistryContractSnapshot snapshot) {
        return snapshot.contentIdsByRegistry();
    }

    public Map<String, Map<String, List<String>>> bindRecipesLootTags(EchoRegistryContractSnapshot snapshot) {
        EchoRegistryRuntimeResolution resolution = EchoRegistryRuntimeResolvers.resolve(runtimeId(), snapshot, List.of(), List.of(), materializeResourcePack(snapshot));
        Map<String, Map<String, List<String>>> bindings = new LinkedHashMap<>();
        bindings.put("recipes", resolution.recipesById());
        bindings.put("recipeInputs", resolution.recipeInputsById());
        bindings.put("lootTables", resolution.lootById());
        bindings.put("tags", resolution.tagsById());
        return bindings;
    }

    public int mergeTagOverlays(EchoRegistryContractSnapshot snapshot) {
        return snapshot.mergedTagOverlayCount();
    }

    public List<String> resolveSearchVisibility(EchoRegistryContractSnapshot snapshot) {
        return EchoRegistryRuntimeResolvers.searchVisibleContentIds(snapshot);
    }
}
