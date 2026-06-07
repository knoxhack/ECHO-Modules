package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockProgressionSnapshot(
        Identifier progressionId,
        Identifier facilityId,
        int tier,
        String title,
        List<Identifier> prerequisites,
        List<Identifier> featuredRecipes,
        List<Identifier> rewardItems,
        Identifier advancementId,
        String guideText,
        boolean prerequisitesMet,
        String completionHint) {
    public MultiblockProgressionSnapshot {
        tier = Math.max(0, tier);
        title = title == null || title.isBlank()
                ? (facilityId == null ? "Facility Progression" : facilityId.getPath().replace('_', ' '))
                : title.strip();
        prerequisites = List.copyOf(prerequisites == null ? List.of() : prerequisites);
        featuredRecipes = List.copyOf(featuredRecipes == null ? List.of() : featuredRecipes);
        rewardItems = List.copyOf(rewardItems == null ? List.of() : rewardItems);
        guideText = guideText == null ? "" : guideText.strip();
        completionHint = completionHint == null ? "" : completionHint.strip();
    }

    public static MultiblockProgressionSnapshot from(MultiblockProgressionDefinition definition, boolean prerequisitesMet, String hint) {
        if (definition == null) {
            return new MultiblockProgressionSnapshot(null, null, 0, "", List.of(), List.of(), List.of(), null, "", true, "");
        }
        return new MultiblockProgressionSnapshot(
                definition.id(),
                definition.facilityId(),
                definition.tier(),
                definition.title(),
                definition.prerequisites(),
                definition.featuredRecipes(),
                definition.rewardItems(),
                definition.advancementId(),
                definition.guideText(),
                prerequisitesMet,
                hint);
    }
}
