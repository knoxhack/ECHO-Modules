package com.knoxhack.echoindex.service;

import java.util.List;
import net.minecraft.resources.Identifier;

public record IndexRecipePlan(
        Identifier recipeId,
        IndexRecipeActionState state,
        List<IndexIngredientNeed> needs,
        boolean craftingRecipe,
        boolean sourceCard,
        boolean pinned,
        String transferBlocker) {
    public IndexRecipePlan {
        state = state == null ? IndexRecipeActionState.PLAN_ONLY : state;
        needs = needs == null ? List.of() : List.copyOf(needs);
        transferBlocker = transferBlocker == null ? "" : transferBlocker.strip();
    }

    public int missingCount() {
        int total = 0;
        for (IndexIngredientNeed need : needs) {
            total += need.missing();
        }
        return total;
    }

    public boolean ready() {
        return state == IndexRecipeActionState.READY;
    }

    public boolean canTransfer() {
        return ready() && craftingRecipe && !sourceCard && transferBlocker.isBlank();
    }
}
