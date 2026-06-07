package com.knoxhack.echoindex.service;

import net.minecraft.resources.Identifier;

public record IndexRecipeProviderStats(
        Identifier providerId,
        int categoryCount,
        int rawRecipeCount,
        int adaptedRecipeCount,
        int sourceFactCount,
        int sourceCardCount,
        int skippedRecipeCount,
        String lastError) {
    public IndexRecipeProviderStats {
        categoryCount = Math.max(0, categoryCount);
        rawRecipeCount = Math.max(0, rawRecipeCount);
        adaptedRecipeCount = Math.max(0, adaptedRecipeCount);
        sourceFactCount = Math.max(0, sourceFactCount);
        sourceCardCount = Math.max(0, sourceCardCount);
        skippedRecipeCount = Math.max(0, skippedRecipeCount);
        lastError = lastError == null ? "" : lastError.strip();
    }

    public boolean hasWarning() {
        return skippedRecipeCount > 0 || !lastError.isBlank();
    }

    public String summaryLine() {
        String base = providerId + " | categories " + categoryCount
                + ", raw " + rawRecipeCount
                + ", adapted " + adaptedRecipeCount;
        if (sourceFactCount > 0 || sourceCardCount > 0) {
            base += ", source facts " + sourceFactCount + ", cards " + sourceCardCount;
        }
        if (skippedRecipeCount > 0) {
            base += ", skipped " + skippedRecipeCount;
        }
        if (!lastError.isBlank()) {
            base += ", error " + lastError;
        }
        return base;
    }
}
