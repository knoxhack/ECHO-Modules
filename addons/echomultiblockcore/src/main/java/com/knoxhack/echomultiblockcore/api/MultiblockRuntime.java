package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record MultiblockRuntime(
        Identifier definitionId,
        BlockPos controllerPosition,
        List<BlockPos> matchedBlocks,
        Rotation matchedRotation,
        boolean mirrored,
        float integrity,
        List<Identifier> installedModules,
        List<BlockPos> installedRoboticComponents,
        List<RuntimeWorkcell> workcells,
        List<TaskSnapshot> currentTasks,
        List<String> warnings,
        long lastValidationTime,
        MultiblockCapabilityRuntime capabilityRuntime,
        List<InstalledMultiblockUpgrade> installedUpgrades,
        List<String> damageGroups,
        List<RobotAnimationState> robotAnimations,
        String constructionProgress) {
    public MultiblockRuntime(
            Identifier definitionId,
            BlockPos controllerPosition,
            List<BlockPos> matchedBlocks,
            Rotation matchedRotation,
            boolean mirrored,
            float integrity,
            List<Identifier> installedModules,
            List<BlockPos> installedRoboticComponents,
            List<RuntimeWorkcell> workcells,
            List<TaskSnapshot> currentTasks,
            List<String> warnings,
            long lastValidationTime) {
        this(definitionId, controllerPosition, matchedBlocks, matchedRotation, mirrored, integrity, installedModules,
                installedRoboticComponents, workcells, currentTasks, warnings, lastValidationTime,
                MultiblockCapabilityRuntime.EMPTY, List.of(), List.of(), List.of(), "");
    }

    public MultiblockRuntime {
        controllerPosition = controllerPosition == null ? BlockPos.ZERO : controllerPosition.immutable();
        matchedBlocks = List.copyOf(matchedBlocks == null ? List.of() : matchedBlocks.stream().map(BlockPos::immutable).toList());
        matchedRotation = matchedRotation == null ? Rotation.NONE : matchedRotation;
        integrity = Math.max(0.0F, Math.min(100.0F, integrity));
        installedModules = List.copyOf(installedModules == null ? List.of() : installedModules);
        installedRoboticComponents = List.copyOf(installedRoboticComponents == null ? List.of() : installedRoboticComponents.stream().map(BlockPos::immutable).toList());
        workcells = List.copyOf(workcells == null ? List.of() : workcells);
        currentTasks = List.copyOf(currentTasks == null ? List.of() : currentTasks);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        capabilityRuntime = capabilityRuntime == null ? MultiblockCapabilityRuntime.EMPTY : capabilityRuntime;
        installedUpgrades = List.copyOf(installedUpgrades == null ? List.of() : installedUpgrades);
        damageGroups = List.copyOf(damageGroups == null ? List.of() : damageGroups);
        robotAnimations = List.copyOf(robotAnimations == null ? List.of() : robotAnimations);
        constructionProgress = constructionProgress == null ? "" : constructionProgress.strip();
    }

    public record RuntimeWorkcell(Identifier id, WorkcellType type, BlockPos worldPosition, String status) {
        public RuntimeWorkcell {
            worldPosition = worldPosition == null ? BlockPos.ZERO : worldPosition.immutable();
            status = status == null || status.isBlank() ? "Idle" : status.strip();
        }
    }

    public record TaskSnapshot(Identifier id, String displayName, MultiblockTaskState state, int progressTicks, int durationTicks) {
        public TaskSnapshot {
            displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName.strip();
            state = state == null ? MultiblockTaskState.WAITING : state;
            progressTicks = Math.max(0, progressTicks);
            durationTicks = Math.max(1, durationTicks);
        }
    }
}
