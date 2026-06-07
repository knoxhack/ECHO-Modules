package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum IndexTerminalRecipeProvider implements TerminalRecipeProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return IndexIds.PROVIDER_TERMINAL;
    }

    @Override
    public String displayName() {
        return "ECHO: Index";
    }

    @Override
    public List<TerminalRecipeCategory> categories(Player player) {
        return IndexService.INSTANCE.recipeCategories(player).stream()
                .map(IndexTerminalRecipeProvider::convert)
                .toList();
    }

    @Override
    public List<TerminalRecipeEntry> recipes(Player player) {
        return IndexService.INSTANCE.recipes(player).stream()
                .map(IndexTerminalRecipeProvider::convert)
                .toList();
    }

    private static TerminalRecipeCategory convert(IndexRecipeCategory category) {
        return new TerminalRecipeCategory(
                category.id(),
                category.title(),
                category.icon(),
                category.accentColor(),
                category.order());
    }

    private static TerminalRecipeEntry convert(IndexRecipeView view) {
        List<TerminalRecipeSlot> slots = new ArrayList<>();
        for (IndexRecipeSlot slot : view.slots()) {
            slots.add(new TerminalRecipeSlot(role(slot.role()), slot.stacks(), slot.label()));
        }
        List<TerminalRecipeNote> notes = new ArrayList<>();
        for (String note : view.notes()) {
            notes.add(TerminalRecipeNote.info(note));
        }
        if (!view.sourceModId().isBlank()) {
            notes.add(TerminalRecipeNote.info("Indexed by " + view.sourceModId() + "."));
        }
        return new TerminalRecipeEntry(
                namespace(view.id()),
                view.categoryId(),
                view.title(),
                view.machine(),
                slots,
                notes,
                view.processTicks(),
                view.locked());
    }

    private static TerminalRecipeSlot.Role role(IndexSlotRole role) {
        if (role == IndexSlotRole.INPUT) {
            return TerminalRecipeSlot.Role.INPUT;
        }
        if (role == IndexSlotRole.OUTPUT) {
            return TerminalRecipeSlot.Role.OUTPUT;
        }
        if (role == IndexSlotRole.CATALYST) {
            return TerminalRecipeSlot.Role.CATALYST;
        }
        if (role == IndexSlotRole.MACHINE) {
            return TerminalRecipeSlot.Role.MACHINE;
        }
        return TerminalRecipeSlot.Role.INFO;
    }

    private static Identifier namespace(Identifier recipeId) {
        if (EchoIndex.MODID.equals(recipeId.getNamespace())) {
            return recipeId;
        }
        return EchoIndex.id("terminal_recipe/" + recipeId.getNamespace() + "/" + recipeId.getPath());
    }
}
