package com.echoplatform.echocore.api.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class IndexContentBuilder implements IIndexRegistry {
    private final Identifier providerId;
    private final List<IndexCategory> categories = new ArrayList<>();
    private final List<IndexEntry> entries = new ArrayList<>();
    private final List<IndexRecipeCategory> recipeCategories = new ArrayList<>();
    private final List<IndexRecipeView> recipes = new ArrayList<>();
    private final List<IndexMachineLayout> machineLayouts = new ArrayList<>();
    private final List<IndexSourceFact> sourceFacts = new ArrayList<>();
    private final List<IndexRelation> relations = new ArrayList<>();
    private final List<IndexProviderDiagnostic> diagnostics = new ArrayList<>();

    private IndexContentBuilder(Identifier providerId) {
        this.providerId = providerId;
    }

    public static IndexContentBuilder create(Identifier providerId) {
        return new IndexContentBuilder(providerId);
    }

    @Override
    public boolean register(IndexEntry entry) {
        return registerEntry(entry);
    }

    @Override
    public boolean registerCategory(IndexCategory category) {
        if (category != null) {
            categories.add(category);
            return true;
        }
        return false;
    }

    @Override
    public boolean registerEntry(IndexEntry entry) {
        if (entry != null) {
            entries.add(entry);
            return true;
        }
        return false;
    }

    @Override
    public Optional<IndexEntry> find(String id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    @Override
    public List<IndexEntry> all() {
        return List.copyOf(entries);
    }

    public IndexContentSnapshot snapshot() {
        return new IndexContentSnapshot(providerId, categories, entries, recipeCategories, recipes, machineLayouts,
                sourceFacts, relations, diagnostics);
    }

    public void addRecipeCategories(List<IndexRecipeCategory> categories) {
        if (categories != null) {
            categories.stream().filter(category -> category != null).forEach(recipeCategories::add);
        }
    }

    public void addRecipes(List<IndexRecipeView> views) {
        if (views != null) {
            views.stream().filter(view -> view != null).forEach(recipes::add);
        }
    }

    public void addMachineLayouts(List<IndexMachineLayout> layouts) {
        if (layouts != null) {
            layouts.stream().filter(layout -> layout != null).forEach(machineLayouts::add);
        }
    }

    public void addSourceFacts(List<IndexSourceFact> facts) {
        if (facts != null) {
            facts.stream().filter(fact -> fact != null).forEach(sourceFacts::add);
        }
    }

    public void addRelations(List<IndexRelation> values) {
        if (values != null) {
            values.stream().filter(relation -> relation != null).forEach(relations::add);
        }
    }

    public void addDiagnostics(List<IndexProviderDiagnostic> values) {
        if (values != null) {
            values.stream().filter(diagnostic -> diagnostic != null).forEach(diagnostics::add);
        }
    }
}
