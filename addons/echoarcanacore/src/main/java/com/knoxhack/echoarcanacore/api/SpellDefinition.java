package com.knoxhack.echoarcanacore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record SpellDefinition(
        Identifier id,
        String translationKey,
        SpellSchool school,
        Identifier icon,
        Identifier requiredResearch,
        double aetherCost,
        int cooldownTicks,
        int castTimeTicks,
        int channelDurationTicks,
        double range,
        TargetingMode targetingMode,
        CastType castType,
        Identifier effectProvider,
        int modifierSlots,
        double corruptionRisk,
        double curseRisk,
        Identifier visualProfile,
        Identifier soundProfile,
        Identifier indexPageId,
        Identifier grimoirePageId,
        List<String> tags) {
    public SpellDefinition {
        school = school == null ? SpellSchool.SIGNAL : school;
        translationKey = translationKey == null || translationKey.isBlank() ? id.toString() : translationKey.strip();
        aetherCost = Math.max(0.0D, aetherCost);
        cooldownTicks = Math.max(0, cooldownTicks);
        castTimeTicks = Math.max(0, castTimeTicks);
        channelDurationTicks = Math.max(0, channelDurationTicks);
        range = Math.max(0.0D, range);
        targetingMode = targetingMode == null ? TargetingMode.SELF : targetingMode;
        castType = castType == null ? CastType.INSTANT : castType;
        modifierSlots = Math.max(0, modifierSlots);
        corruptionRisk = Math.max(0.0D, corruptionRisk);
        curseRisk = Math.max(0.0D, curseRisk);
        tags = List.copyOf(tags == null ? List.of() : tags);
    }
}
