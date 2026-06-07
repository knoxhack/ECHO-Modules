package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.List;

public record TaskExecutionSnapshot(
        Identifier taskId,
        String displayName,
        MultiblockTaskState state,
        int progressTicks,
        int durationTicks,
        BlockPos robotPos,
        String blockedReason,
        int retryCount,
        String recipeCategory,
        Identifier robotId,
        Identifier workcellId,
        String inputSummary,
        String outputSummary,
        List<Identifier> effectIds,
        String effectDiagnostic,
        String capabilitySummary,
        String upgradeSummary,
        int repairPriority,
        Identifier animationProfile,
        boolean autoBuilderEligible) {
    public TaskExecutionSnapshot(
            Identifier taskId,
            String displayName,
            MultiblockTaskState state,
            int progressTicks,
            int durationTicks,
            BlockPos robotPos,
            String blockedReason,
            int retryCount,
            String recipeCategory,
            Identifier robotId,
            Identifier workcellId,
            String inputSummary,
            String outputSummary,
            List<Identifier> effectIds,
            String effectDiagnostic) {
        this(taskId, displayName, state, progressTicks, durationTicks, robotPos, blockedReason, retryCount,
                recipeCategory, robotId, workcellId, inputSummary, outputSummary, effectIds, effectDiagnostic,
                "", "", 0, null, true);
    }

    public TaskExecutionSnapshot(
            Identifier taskId,
            String displayName,
            MultiblockTaskState state,
            int progressTicks,
            int durationTicks,
            BlockPos robotPos,
            String blockedReason,
            int retryCount,
            String recipeCategory,
            Identifier robotId,
            Identifier workcellId,
            String inputSummary,
            String outputSummary) {
        this(taskId, displayName, state, progressTicks, durationTicks, robotPos, blockedReason, retryCount,
                recipeCategory, robotId, workcellId, inputSummary, outputSummary, List.of(), "");
    }

    public TaskExecutionSnapshot(
            Identifier taskId,
            String displayName,
            MultiblockTaskState state,
            int progressTicks,
            int durationTicks,
            BlockPos robotPos,
            String blockedReason,
            int retryCount) {
        this(taskId, displayName, state, progressTicks, durationTicks, robotPos, blockedReason, retryCount,
                "general", null, null, "", "", List.of(), "");
    }

    public TaskExecutionSnapshot {
        displayName = displayName == null || displayName.isBlank() ? (taskId == null ? "Task" : taskId.toString()) : displayName.strip();
        state = state == null ? MultiblockTaskState.WAITING : state;
        progressTicks = Math.max(0, progressTicks);
        durationTicks = Math.max(1, durationTicks);
        robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        blockedReason = blockedReason == null ? "" : blockedReason.strip();
        retryCount = Math.max(0, retryCount);
        recipeCategory = recipeCategory == null || recipeCategory.isBlank() ? "general" : recipeCategory.strip();
        inputSummary = inputSummary == null ? "" : inputSummary.strip();
        outputSummary = outputSummary == null ? "" : outputSummary.strip();
        effectIds = List.copyOf(effectIds == null ? List.of() : effectIds);
        effectDiagnostic = effectDiagnostic == null ? "" : effectDiagnostic.strip();
        capabilitySummary = capabilitySummary == null ? "" : capabilitySummary.strip();
        upgradeSummary = upgradeSummary == null ? "" : upgradeSummary.strip();
        repairPriority = Math.max(0, repairPriority);
        animationProfile = animationProfile == null
                ? (taskId == null ? null : Identifier.fromNamespaceAndPath(taskId.getNamespace(), "default"))
                : animationProfile;
    }

    public Identifier recipeId() {
        return taskId;
    }
}
