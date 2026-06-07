package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record TerminalMissionDefinition(
        Identifier id,
        Identifier chapterId,
        String phaseId,
        String phaseTitle,
        int phaseOrder,
        int missionOrder,
        String title,
        String briefing,
        String fieldGuide,
        String category,
        String difficulty,
        ItemStack icon,
        List<String> prerequisites,
        List<TerminalMissionRequirement> requirements,
        List<TerminalMissionReward> rewards) {
    public TerminalMissionDefinition {
        id = TerminalApiIds.requireLowercase(id, "Terminal mission");
        chapterId = TerminalApiIds.requireLowercase(chapterId, "Terminal mission chapter");
        phaseId = phaseId == null || phaseId.isBlank() ? "phase_" + Math.max(0, phaseOrder) : phaseId;
        phaseTitle = phaseTitle == null || phaseTitle.isBlank() ? phaseId : phaseTitle;
        title = title == null || title.isBlank() ? id.getPath() : title;
        briefing = briefing == null ? "" : briefing;
        fieldGuide = fieldGuide == null ? "" : fieldGuide;
        category = category == null ? "" : category;
        difficulty = difficulty == null ? "" : difficulty;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        prerequisites = List.copyOf(prerequisites == null ? List.of() : prerequisites);
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }
}
