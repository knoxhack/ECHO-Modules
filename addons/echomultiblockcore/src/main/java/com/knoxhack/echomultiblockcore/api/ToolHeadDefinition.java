package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record ToolHeadDefinition(
        Identifier id,
        RobotToolType toolType,
        int precisionBonus,
        int strengthBonus,
        int heatGeneration,
        float energyCostMultiplier,
        List<Identifier> allowedTasks) {
    public ToolHeadDefinition {
        id = id == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown_tool") : id;
        toolType = toolType == null ? RobotToolType.GRIPPER : toolType;
        heatGeneration = Math.max(0, heatGeneration);
        energyCostMultiplier = energyCostMultiplier <= 0.0F ? 1.0F : energyCostMultiplier;
        allowedTasks = List.copyOf(allowedTasks == null ? List.of() : allowedTasks);
    }
}
