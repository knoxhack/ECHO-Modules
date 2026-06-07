package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record AutomationExecutionPlan(
        Identifier recipeId,
        Identifier robotId,
        BlockPos robotPos,
        Identifier workcellId,
        WorkcellType workcellType,
        BlockPos workPos,
        List<String> inputSummary,
        List<String> outputSummary) {
    public AutomationExecutionPlan {
        robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        workPos = workPos == null ? BlockPos.ZERO : workPos.immutable();
        inputSummary = List.copyOf(inputSummary == null ? List.of() : inputSummary);
        outputSummary = List.copyOf(outputSummary == null ? List.of() : outputSummary);
    }

    public String inputLine() {
        return inputSummary.isEmpty() ? "" : String.join(", ", inputSummary);
    }

    public String outputLine() {
        return outputSummary.isEmpty() ? "" : String.join(", ", outputSummary);
    }
}
