package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.resources.Identifier;

public record IndexContentSnapshot(
        Identifier providerId,
        List<IndexCategory> categories,
        List<IndexEntry> entries,
        List<IndexRecipeCategory> recipeCategories,
        List<IndexRecipeView> recipes,
        List<IndexMachineLayout> machineLayouts,
        List<IndexSourceFact> sourceFacts,
        List<IndexRelation> relations,
        List<IndexProviderDiagnostic> diagnostics,
        List<String> warnings) {
    public IndexContentSnapshot {
        categories = categories == null ? List.of() : List.copyOf(categories);
        entries = entries == null ? List.of() : List.copyOf(entries);
        recipeCategories = recipeCategories == null ? List.of() : List.copyOf(recipeCategories);
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        machineLayouts = machineLayouts == null ? List.of() : List.copyOf(machineLayouts);
        sourceFacts = sourceFacts == null ? List.of() : List.copyOf(sourceFacts);
        relations = relations == null ? List.of() : List.copyOf(relations);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public IndexContentSnapshot(Identifier providerId, List<IndexCategory> categories, List<IndexEntry> entries) {
        this(providerId, categories, entries, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public IndexContentSnapshot(
            Identifier providerId,
            List<IndexCategory> categories,
            List<IndexEntry> entries,
            List<IndexRecipeCategory> recipeCategories,
            List<IndexRecipeView> recipes,
            List<?> sixth,
            List<?> seventh,
            List<?> eighth) {
        this(providerId, categories, entries, recipeCategories, recipes, machineLayoutsFrom(sixth),
                sourceFactsFrom(sixth, seventh),
                relationsFrom(seventh, eighth),
                diagnosticsFrom(eighth),
                warningsFrom(eighth));
    }

    public IndexContentSnapshot(
            Identifier providerId,
            List<IndexCategory> categories,
            List<IndexEntry> entries,
            List<IndexRecipeCategory> recipeCategories,
            List<IndexRecipeView> recipes,
            List<?> sixth,
            List<?> seventh,
            List<?> eighth,
            List<?> ninth) {
        this(providerId, categories, entries, recipeCategories, recipes, machineLayoutsFrom(sixth),
                sourceFactsFrom(sixth, seventh),
                relationsFrom(seventh, eighth),
                diagnosticsFrom(ninth),
                warningsFrom(ninth));
    }

    public static IndexContentSnapshot empty(Identifier providerId) {
        return new IndexContentSnapshot(providerId, List.of(), List.of());
    }

    private static List<IndexSourceFact> sourceFactsFrom(List<?> values) {
        if (values == null || values.isEmpty() || !(values.get(0) instanceof IndexSourceFact)) {
            return List.of();
        }
        return values.stream().filter(IndexSourceFact.class::isInstance).map(IndexSourceFact.class::cast).toList();
    }

    private static List<IndexMachineLayout> machineLayoutsFrom(List<?> values) {
        if (values == null || values.isEmpty() || !(values.get(0) instanceof IndexMachineLayout)) {
            return List.of();
        }
        return values.stream()
                .filter(IndexMachineLayout.class::isInstance)
                .map(IndexMachineLayout.class::cast)
                .toList();
    }

    private static List<IndexSourceFact> sourceFactsFrom(List<?> first, List<?> second) {
        List<IndexSourceFact> firstFacts = sourceFactsFrom(first);
        return firstFacts.isEmpty() ? sourceFactsFrom(second) : firstFacts;
    }

    private static List<IndexRelation> relationsFrom(List<?> seventh, List<?> eighth) {
        List<?> values = seventh != null && !seventh.isEmpty() && seventh.get(0) instanceof IndexRelation ? seventh : eighth;
        if (values == null || values.isEmpty() || !(values.get(0) instanceof IndexRelation)) {
            return List.of();
        }
        return values.stream().filter(IndexRelation.class::isInstance).map(IndexRelation.class::cast).toList();
    }

    private static List<IndexProviderDiagnostic> diagnosticsFrom(List<?> values) {
        if (values == null || values.isEmpty() || !(values.get(0) instanceof IndexProviderDiagnostic)) {
            return List.of();
        }
        return values.stream()
                .filter(IndexProviderDiagnostic.class::isInstance)
                .map(IndexProviderDiagnostic.class::cast)
                .toList();
    }

    private static List<String> warningsFrom(List<?> values) {
        if (values == null || values.isEmpty() || !(values.get(0) instanceof String)) {
            return List.of();
        }
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}
