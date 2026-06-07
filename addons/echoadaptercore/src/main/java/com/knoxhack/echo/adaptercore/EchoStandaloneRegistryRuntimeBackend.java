package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoStandaloneRegistryRuntimeBackend implements EchoRegistryRuntimeBackend {
    public static final String RUNTIME_ID = "echo-standalone-runtime";

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
        return EchoRegistryRuntimeResolvers.resolve(runtimeId(), snapshot, terminalPages, indexEntries, mountResources(snapshot));
    }

    public boolean mountResources(EchoRegistryContractSnapshot snapshot) {
        return snapshot.requiredAssetIssues().isEmpty();
    }

    public int buildAssetIndex(EchoRegistryContractSnapshot snapshot) {
        return snapshot.blocks().size() + snapshot.items().size() + snapshot.entities().size() + snapshot.sounds().size();
    }

    public Map<String, String> mapRendererUiAssets(EchoRegistryContractSnapshot snapshot) {
        return EchoRegistryRuntimeResolvers.resolve(runtimeId(), snapshot, List.of(), List.of(), mountResources(snapshot))
                .rendererAssetsByContentId();
    }

    public Map<String, Map<String, List<String>>> bindRecipesLootTags(EchoRegistryContractSnapshot snapshot) {
        EchoRegistryRuntimeResolution resolution = EchoRegistryRuntimeResolvers.resolve(runtimeId(), snapshot, List.of(), List.of(), mountResources(snapshot));
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
}
