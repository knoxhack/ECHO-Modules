package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record AutoBuilderPlan(
        Identifier definitionId,
        BlockPos controllerPos,
        List<AutoBuilderStep> steps,
        int materialGroups,
        ConstructionPermissionPolicy permissionPolicy) {
    public AutoBuilderPlan {
        definitionId = definitionId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown") : definitionId;
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        steps = List.copyOf(steps == null ? List.of() : steps);
        materialGroups = Math.max(0, materialGroups);
        permissionPolicy = permissionPolicy == null ? ConstructionPermissionPolicy.OPERATOR_ONLY : permissionPolicy;
    }
}
