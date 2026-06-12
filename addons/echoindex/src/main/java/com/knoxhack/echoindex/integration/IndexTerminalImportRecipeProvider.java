package com.knoxhack.echoindex.integration;

import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexMachineLayout;
import com.echoplatform.echocore.api.index.IndexMachineLayoutTemplates;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum IndexTerminalImportRecipeProvider implements IIndexContentProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return IndexIds.PROVIDER_TERMINAL_IMPORT;
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        Player player = context == null ? null : context.player();
        List<IndexRecipeView> recipes = recipes(player);
        return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
                machineLayouts(recipes), List.of(), List.of(), List.of());
    }

    public List<IndexRecipeCategory> recipeCategories(Player player) {
        Map<Identifier, IndexRecipeCategory> categories = new LinkedHashMap<>();
        for (TerminalRecipeProvider provider : TerminalRecipeRegistry.providers()) {
            if (!importable(provider)) {
                continue;
            }
            try {
                for (TerminalRecipeCategory category : provider.categories(player)) {
                    if (category != null && category.id() != null) {
                        categories.putIfAbsent(category.id(), convert(category));
                    }
                }
            } catch (RuntimeException exception) {
                EchoIndex.LOGGER.warn("Index could not import Terminal recipe categories from {}.",
                        provider.id(), exception);
            }
        }
        return List.copyOf(categories.values());
    }

    public List<IndexRecipeView> recipes(Player player) {
        List<IndexRecipeView> views = new ArrayList<>();
        for (TerminalRecipeProvider provider : TerminalRecipeRegistry.providers()) {
            if (!importable(provider)) {
                continue;
            }
            try {
                for (TerminalRecipeEntry entry : provider.recipes(player)) {
                    if (entry != null && entry.id() != null && entry.categoryId() != null) {
                        views.add(convert(entry));
                    }
                }
            } catch (RuntimeException exception) {
                EchoIndex.LOGGER.warn("Index could not import Terminal recipes from {}.", provider.id(), exception);
            }
        }
        return List.copyOf(views);
    }

    public static boolean importableForTests(TerminalRecipeProvider provider) {
        return importable(provider);
    }

    public static IndexRecipeView convertForTests(TerminalRecipeEntry entry) {
        return convert(entry);
    }

    private static List<IndexMachineLayout> machineLayouts(List<IndexRecipeView> recipes) {
        return recipes.stream()
                .map(recipe -> IndexMachineLayoutTemplates.process(recipe, "terminal_import", false))
                .filter(layout -> layout != null && !layout.empty())
                .toList();
    }

    private static boolean importable(TerminalRecipeProvider provider) {
        return provider != null && provider.id() != null && !IndexIds.PROVIDER_TERMINAL.equals(provider.id());
    }

    private static IndexRecipeCategory convert(TerminalRecipeCategory category) {
        return new IndexRecipeCategory(
                category.id(),
                category.title(),
                category.icon(),
                category.accentColor(),
                category.order());
    }

    private static IndexRecipeView convert(TerminalRecipeEntry entry) {
        List<IndexRecipeSlot> slots = new ArrayList<>();
        for (TerminalRecipeSlot slot : entry.slots()) {
            if (slot == null) {
                continue;
            }
            slots.add(new IndexRecipeSlot(role(slot.role()), slot.stacks(), slot.label()));
        }
        List<String> notes = new ArrayList<>();
        for (TerminalRecipeNote note : entry.notes()) {
            if (note == null || note.text().getString().isBlank()) {
                continue;
            }
            String prefix = note.warning() ? "Warning: " : "";
            notes.add(prefix + note.text().getString());
        }
        return new IndexRecipeView(
                importRecipeId(entry.id()),
                entry.categoryId(),
                entry.title(),
                entry.machine(),
                slots,
                notes,
                entry.processTicks(),
                entry.locked(),
                entry.id().getNamespace());
    }

    private static Identifier importRecipeId(Identifier id) {
        return IndexIds.id("terminal_import/" + id.getNamespace() + "/" + id.getPath());
    }

    private static IndexSlotRole role(TerminalRecipeSlot.Role role) {
        return switch (role) {
            case INPUT -> IndexSlotRole.INPUT;
            case OUTPUT -> IndexSlotRole.OUTPUT;
            case CATALYST -> IndexSlotRole.CATALYST;
            case MACHINE -> IndexSlotRole.MACHINE;
            case INFO -> IndexSlotRole.INFO;
        };
    }
}
