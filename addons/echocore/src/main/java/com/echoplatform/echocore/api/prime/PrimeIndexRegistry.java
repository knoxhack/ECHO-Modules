package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeIndexRegistry {
    boolean registerCategory(PrimeIndexCategory category);

    boolean registerRecipeHint(PrimeRecipeHint hint);

    List<PrimeIndexCategory> categories();

    List<PrimeRecipeHint> recipeHints();

    record PrimeIndexCategory(
            Identifier id,
            String title,
            String summary,
            Identifier unlockFlag,
            String sourceModule,
            int order) {
        public PrimeIndexCategory {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
            sourceModule = sourceModule == null ? "" : sourceModule;
        }
    }

    record PrimeRecipeHint(
            Identifier id,
            Identifier categoryId,
            String title,
            String hint,
            Identifier unlockFlag,
            String sourceModule,
            int order) {
        public PrimeRecipeHint {
            title = title == null ? "" : title;
            hint = hint == null ? "" : hint;
            sourceModule = sourceModule == null ? "" : sourceModule;
        }
    }
}
