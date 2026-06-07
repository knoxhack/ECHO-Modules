package com.knoxhack.echoarcanacore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record ArcaneRelicDefinition(
        Identifier id,
        String translationKey,
        String category,
        RelicLifecycle lifecycle,
        AetherStorage storage,
        double instability,
        double curseRisk,
        List<Identifier> discoveredAbilities,
        List<Identifier> hiddenAbilities,
        int upgradeSlots,
        Identifier indexPageId,
        Identifier grimoirePageId) {
    public ArcaneRelicDefinition {
        translationKey = translationKey == null || translationKey.isBlank() ? id.toString() : translationKey.strip();
        category = category == null || category.isBlank() ? "stable_relic" : category.strip();
        lifecycle = lifecycle == null ? RelicLifecycle.UNKNOWN : lifecycle;
        instability = Math.max(0.0D, instability);
        curseRisk = Math.max(0.0D, curseRisk);
        discoveredAbilities = List.copyOf(discoveredAbilities == null ? List.of() : discoveredAbilities);
        hiddenAbilities = List.copyOf(hiddenAbilities == null ? List.of() : hiddenAbilities);
        upgradeSlots = Math.max(0, upgradeSlots);
    }
}
