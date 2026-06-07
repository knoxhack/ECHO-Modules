package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.InstalledMultiblockUpgrade;
import com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.RobotAnimationState;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.api.WorkcellDefinition;
import com.knoxhack.echomultiblockcore.task.MultiblockTaskQueue;
import com.knoxhack.echomultiblockcore.validation.MultiblockValidationEngine;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class MultiblockRuntimeBuilder {
    private MultiblockRuntimeBuilder() {
    }

    public static MultiblockRuntime build(MultiblockDefinition definition, ValidationResult result, float integrity,
            List<BlockPos> robotPositions, MultiblockTaskQueue queue) {
        return build(definition, result, integrity, robotPositions, queue, MultiblockCapabilityRuntime.EMPTY,
                List.of(), List.of(), List.of(), "");
    }

    public static MultiblockRuntime build(MultiblockDefinition definition, ValidationResult result, float integrity,
            List<BlockPos> robotPositions, MultiblockTaskQueue queue, MultiblockCapabilityRuntime capabilityRuntime,
            List<InstalledMultiblockUpgrade> installedUpgrades, List<String> damageGroups,
            List<RobotAnimationState> robotAnimations, String constructionProgress) {
        return new MultiblockRuntime(
                definition.id(),
                result.controllerPosition(),
                result.matchedBlocks(),
                result.matchedRotation(),
                result.mirrored(),
                integrity,
                List.of(),
                robotPositions,
                workcells(definition, result),
                queue == null ? List.of() : queue.tasks().stream()
                        .map(task -> new MultiblockRuntime.TaskSnapshot(
                                task.taskId(),
                                AutomationRecipeRegistry.byId(task.taskId())
                                        .map(MultiblockAutomationRecipe::displayName)
                                        .orElse(task.taskId().toString()),
                                task.state() == null ? MultiblockTaskState.WAITING : task.state(),
                                task.progressTicks(),
                                task.durationTicks()))
                        .toList(),
                result.warnings(),
                result.validationTime(),
                capabilityRuntime,
                installedUpgrades,
                damageGroups,
                robotAnimations,
                constructionProgress);
    }

    private static List<MultiblockRuntime.RuntimeWorkcell> workcells(MultiblockDefinition definition, ValidationResult result) {
        return definition.workcells().stream()
                .map(workcell -> resolvedWorkcell(definition, result, workcell))
                .toList();
    }

    private static MultiblockRuntime.RuntimeWorkcell resolvedWorkcell(MultiblockDefinition definition,
            ValidationResult result, WorkcellDefinition workcell) {
        BlockPos local = MultiblockValidationEngine.transform(workcell.localPosition(), definition,
                result.matchedRotation(), result.mirrored());
        return new MultiblockRuntime.RuntimeWorkcell(workcell.id(), workcell.type(), result.matchedOrigin().offset(local), workcell.status());
    }
}
