package com.knoxhack.echoritualcore.ritual;

import java.util.List;
import net.minecraft.core.BlockPos;

public record RitualStructureReport(
        BlockPos altarPos,
        int runeCircles,
        int pedestalCount,
        int stabilityPylons,
        int moonDials,
        int weatherAnchors,
        int corruptedAltars,
        int stabilityScore,
        List<String> missingAnchors) {
    public RitualStructureReport {
        altarPos = altarPos == null ? BlockPos.ZERO : altarPos.immutable();
        runeCircles = Math.max(0, runeCircles);
        pedestalCount = Math.max(0, pedestalCount);
        stabilityPylons = Math.max(0, stabilityPylons);
        moonDials = Math.max(0, moonDials);
        weatherAnchors = Math.max(0, weatherAnchors);
        corruptedAltars = Math.max(0, corruptedAltars);
        stabilityScore = Math.max(0, Math.min(100, stabilityScore));
        missingAnchors = missingAnchors == null ? List.of() : List.copyOf(missingAnchors);
    }

    public boolean validBasicArray() {
        return runeCircles >= RitualStructureValidator.REQUIRED_RUNE_CIRCLES && pedestalCount > 0;
    }

    public int missingCount() {
        return missingAnchors.size();
    }

    public int augmentCount() {
        return moonDials + weatherAnchors;
    }

    public String summary() {
        return "Runes " + runeCircles + "/" + RitualStructureValidator.REQUIRED_RUNE_CIRCLES
                + ", pedestals " + pedestalCount
                + ", stability " + stabilityScore + "%";
    }
}
