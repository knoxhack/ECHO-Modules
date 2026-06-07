package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record MultiblockRuntimeSnapshot(
        Identifier definitionId,
        BlockPos controllerPos,
        MultiblockState state,
        float integrity,
        double completion,
        int matchedBlockCount,
        int roboticComponentCount,
        List<TaskExecutionSnapshot> tasks,
        List<String> warnings,
        long lastValidationTime,
        ResourceKey<Level> dimension,
        String displayName,
        String category,
        MultiblockRole role,
        int markerColor,
        int taskCount,
        int warningCount,
        MultiblockCapabilityRuntime capabilityRuntime,
        List<InstalledMultiblockUpgrade> installedUpgrades,
        List<String> damageGroups,
        List<String> repairActions,
        List<RobotAnimationState> robotAnimations,
        String constructionProgress,
        Identifier progressionId,
        int progressionTier,
        String progressionTitle,
        String featuredRecipeSummary,
        int facilityTier,
        String facilityStage,
        String facilityRoute,
        String primaryUse,
        String blockedReasonCode,
        Identifier lastTaskId,
        String lastTaskResult,
        List<String> integrationHints) {
    public MultiblockRuntimeSnapshot(
            Identifier definitionId,
            BlockPos controllerPos,
            MultiblockState state,
            float integrity,
            double completion,
            int matchedBlockCount,
            int roboticComponentCount,
            List<TaskExecutionSnapshot> tasks,
            List<String> warnings,
            long lastValidationTime,
            ResourceKey<Level> dimension,
            String displayName,
            String category,
            MultiblockRole role,
            int markerColor,
            int taskCount,
            int warningCount,
            MultiblockCapabilityRuntime capabilityRuntime,
            List<InstalledMultiblockUpgrade> installedUpgrades,
            List<String> damageGroups,
            List<String> repairActions,
            List<RobotAnimationState> robotAnimations,
            String constructionProgress,
            Identifier progressionId,
            int progressionTier,
            String progressionTitle,
            String featuredRecipeSummary) {
        this(definitionId, controllerPos, state, integrity, completion, matchedBlockCount, roboticComponentCount,
                tasks, warnings, lastValidationTime, dimension, displayName, category, role, markerColor,
                taskCount, warningCount, capabilityRuntime, installedUpgrades, damageGroups, repairActions,
                robotAnimations, constructionProgress, progressionId, progressionTier, progressionTitle,
                featuredRecipeSummary, progressionTier, "", "", "", "", null, "", List.of());
    }

    public MultiblockRuntimeSnapshot(
            Identifier definitionId,
            BlockPos controllerPos,
            MultiblockState state,
            float integrity,
            double completion,
            int matchedBlockCount,
            int roboticComponentCount,
            List<TaskExecutionSnapshot> tasks,
            List<String> warnings,
            long lastValidationTime,
            ResourceKey<Level> dimension,
            String displayName,
            String category,
            MultiblockRole role,
            int markerColor,
            int taskCount,
            int warningCount,
            MultiblockCapabilityRuntime capabilityRuntime,
            List<InstalledMultiblockUpgrade> installedUpgrades,
            List<String> damageGroups,
            List<String> repairActions,
            List<RobotAnimationState> robotAnimations,
            String constructionProgress) {
        this(definitionId, controllerPos, state, integrity, completion, matchedBlockCount, roboticComponentCount,
                tasks, warnings, lastValidationTime, dimension, displayName, category, role, markerColor,
                taskCount, warningCount, capabilityRuntime, installedUpgrades, damageGroups, repairActions,
                robotAnimations, constructionProgress, null, 0, "", "");
    }

    public MultiblockRuntimeSnapshot(
            Identifier definitionId,
            BlockPos controllerPos,
            MultiblockState state,
            float integrity,
            double completion,
            int matchedBlockCount,
            int roboticComponentCount,
            List<TaskExecutionSnapshot> tasks,
            List<String> warnings,
            long lastValidationTime,
            ResourceKey<Level> dimension,
            String displayName,
            String category,
            MultiblockRole role,
            int markerColor,
            int taskCount,
            int warningCount) {
        this(definitionId, controllerPos, state, integrity, completion, matchedBlockCount, roboticComponentCount,
                tasks, warnings, lastValidationTime, dimension, displayName, category, role, markerColor,
                taskCount, warningCount, MultiblockCapabilityRuntime.EMPTY, List.of(), List.of(), List.of(),
                List.of(), "", null, 0, "", "");
    }

    public MultiblockRuntimeSnapshot {
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        state = state == null ? MultiblockState.UNBUILT : state;
        integrity = Math.max(0.0F, Math.min(100.0F, integrity));
        completion = Math.max(0.0D, Math.min(1.0D, completion));
        matchedBlockCount = Math.max(0, matchedBlockCount);
        roboticComponentCount = Math.max(0, roboticComponentCount);
        tasks = List.copyOf(tasks == null ? List.of() : tasks);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        displayName = displayName == null || displayName.isBlank()
                ? (definitionId == null ? "Multiblock" : definitionId.getPath())
                : displayName.strip();
        category = category == null || category.isBlank() ? "general" : category.strip();
        role = role == null ? MultiblockRole.INFRASTRUCTURE : role;
        markerColor = markerColor == 0 ? 0xFF00D8FF : markerColor;
        taskCount = Math.max(taskCount, tasks.size());
        warningCount = Math.max(warningCount, warnings.size());
        capabilityRuntime = capabilityRuntime == null ? MultiblockCapabilityRuntime.EMPTY : capabilityRuntime;
        installedUpgrades = List.copyOf(installedUpgrades == null ? List.of() : installedUpgrades);
        damageGroups = List.copyOf(damageGroups == null ? List.of() : damageGroups);
        repairActions = List.copyOf(repairActions == null ? List.of() : repairActions);
        robotAnimations = List.copyOf(robotAnimations == null ? List.of() : robotAnimations);
        constructionProgress = constructionProgress == null ? "" : constructionProgress.strip();
        progressionTier = Math.max(0, progressionTier);
        progressionTitle = progressionTitle == null ? "" : progressionTitle.strip();
        featuredRecipeSummary = featuredRecipeSummary == null ? "" : featuredRecipeSummary.strip();
        facilityTier = Math.max(0, facilityTier <= 0 ? progressionTier : facilityTier);
        facilityStage = facilityStage == null ? "" : facilityStage.strip();
        facilityRoute = facilityRoute == null ? "" : facilityRoute.strip();
        primaryUse = primaryUse == null ? "" : primaryUse.strip();
        blockedReasonCode = blockedReasonCode == null ? "" : blockedReasonCode.strip();
        lastTaskResult = lastTaskResult == null ? "" : lastTaskResult.strip();
        integrationHints = List.copyOf(integrationHints == null ? List.of() : integrationHints);
    }

    public MultiblockRuntimeSnapshot(
            Identifier definitionId,
            BlockPos controllerPos,
            MultiblockState state,
            float integrity,
            double completion,
            int matchedBlockCount,
            int roboticComponentCount,
            List<TaskExecutionSnapshot> tasks,
            List<String> warnings,
            long lastValidationTime) {
        this(definitionId, controllerPos, state, integrity, completion, matchedBlockCount, roboticComponentCount,
                tasks, warnings, lastValidationTime, Level.OVERWORLD,
                definitionId == null ? "Multiblock" : definitionId.getPath(), "general",
                MultiblockRole.INFRASTRUCTURE, 0xFF00D8FF,
                tasks == null ? 0 : tasks.size(), warnings == null ? 0 : warnings.size());
    }

    public static MultiblockRuntimeSnapshot from(MultiblockRuntime runtime, MultiblockState state, double completion) {
        if (runtime == null) {
            return new MultiblockRuntimeSnapshot(null, BlockPos.ZERO, state, 0.0F, completion,
                    0, 0, List.of(), List.of(), 0L);
        }
        return new MultiblockRuntimeSnapshot(
                runtime.definitionId(),
                runtime.controllerPosition(),
                state,
                runtime.integrity(),
                completion,
                runtime.matchedBlocks().size(),
                runtime.installedRoboticComponents().size(),
                runtime.currentTasks().stream()
                        .map(task -> new TaskExecutionSnapshot(task.id(), task.displayName(), task.state(),
                                task.progressTicks(), task.durationTicks(), BlockPos.ZERO, "", 0))
                        .toList(),
                runtime.warnings(),
                runtime.lastValidationTime(),
                Level.OVERWORLD,
                runtime.definitionId() == null ? "Multiblock" : runtime.definitionId().getPath(),
                "general",
                MultiblockRole.INFRASTRUCTURE,
                0xFF00D8FF,
                runtime.currentTasks().size(),
                runtime.warnings().size(),
                runtime.capabilityRuntime(),
                runtime.installedUpgrades(),
                runtime.damageGroups(),
                List.of(),
                runtime.robotAnimations(),
                runtime.constructionProgress(),
                null,
                0,
                "",
                "",
                0,
                "",
                "",
                "",
                "",
                null,
                "",
                List.of());
    }
}
