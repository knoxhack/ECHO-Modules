package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockProgressionDefinition(
        Identifier id,
        Identifier facilityId,
        int tier,
        List<Identifier> prerequisites,
        List<Identifier> featuredRecipes,
        List<Identifier> rewardItems,
        Identifier advancementId,
        String title,
        String guideText) {
    public MultiblockProgressionDefinition {
        id = id == null ? facilityId : id;
        facilityId = facilityId == null ? id : facilityId;
        tier = Math.max(0, tier);
        prerequisites = List.copyOf(prerequisites == null ? List.of() : prerequisites);
        featuredRecipes = List.copyOf(featuredRecipes == null ? List.of() : featuredRecipes);
        rewardItems = List.copyOf(rewardItems == null ? List.of() : rewardItems);
        advancementId = advancementId == null && facilityId != null
                ? Identifier.fromNamespaceAndPath(facilityId.getNamespace(), "multiblock/" + facilityId.getPath())
                : advancementId;
        title = title == null || title.isBlank()
                ? (facilityId == null ? "Facility Progression" : facilityId.getPath().replace('_', ' '))
                : title.strip();
        guideText = guideText == null ? "" : guideText.strip();
    }

    public boolean matchesFacility(Identifier definitionId) {
        return facilityId != null && facilityId.equals(definitionId);
    }

    public String featuredRecipeSummary() {
        if (featuredRecipes.isEmpty()) {
            return "";
        }
        return featuredRecipes.stream()
                .map(id -> id.getPath().replace('_', ' '))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
