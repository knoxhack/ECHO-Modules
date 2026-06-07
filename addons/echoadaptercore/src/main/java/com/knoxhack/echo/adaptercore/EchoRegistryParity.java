package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EchoRegistryParity {
    private EchoRegistryParity() {
    }

    public static EchoRegistryParityResult compare(
            EchoRegistryRuntimeResolution nativeResolution,
            EchoRegistryRuntimeResolution standaloneResolution
    ) {
        Objects.requireNonNull(nativeResolution, "nativeResolution");
        Objects.requireNonNull(standaloneResolution, "standaloneResolution");

        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        check("same content IDs exist on both runtimes",
                nativeResolution.contentIdsByRegistry().equals(standaloneResolution.contentIdsByRegistry()),
                passed,
                failed);
        check("same recipes resolve",
                nativeResolution.recipesById().equals(standaloneResolution.recipesById()),
                passed,
                failed);
        check("same recipe inputs resolve",
                nativeResolution.recipeInputsById().equals(standaloneResolution.recipeInputsById()),
                passed,
                failed);
        check("same recipe types resolve",
                nativeResolution.recipeTypesById().equals(standaloneResolution.recipeTypesById()),
                passed,
                failed);
        check("same loot tables resolve",
                nativeResolution.lootById().equals(standaloneResolution.lootById()),
                passed,
                failed);
        check("same sounds resolve",
                nativeResolution.soundsById().equals(standaloneResolution.soundsById()),
                passed,
                failed);
        check("same structures resolve",
                nativeResolution.structuresById().equals(standaloneResolution.structuresById()),
                passed,
                failed);
        check("same structure types resolve",
                nativeResolution.structureTypesById().equals(standaloneResolution.structureTypesById()),
                passed,
                failed);
        check("same tags resolve",
                nativeResolution.tagsById().equals(standaloneResolution.tagsById()),
                passed,
                failed);
        check("same creative content groups resolve",
                nativeResolution.creativeGroupsById().equals(standaloneResolution.creativeGroupsById()),
                passed,
                failed);
        check("same search visibility resolves",
                nativeResolution.searchVisibleContentIds().equals(standaloneResolution.searchVisibleContentIds())
                        && nativeResolution.searchVisibleContentCount() == standaloneResolution.searchVisibleContentCount(),
                passed,
                failed);
        check("same terminal/index data resolves",
                nativeResolution.terminalPages().equals(standaloneResolution.terminalPages())
                        && nativeResolution.indexEntries().equals(standaloneResolution.indexEntries()),
                passed,
                failed);
        check("no missing required textures/models/lang",
                nativeResolution.requiredAssetIssues().isEmpty()
                        && standaloneResolution.requiredAssetIssues().isEmpty(),
                passed,
                failed);
        check("same tag overlays merge",
                nativeResolution.mergedTagOverlayCount() == standaloneResolution.mergedTagOverlayCount(),
                passed,
                failed);
        return new EchoRegistryParityResult(failed.isEmpty(), passed, failed);
    }

    private static void check(String name, boolean condition, List<String> passed, List<String> failed) {
        if (condition) {
            passed.add(name);
        } else {
            failed.add(name);
        }
    }
}
