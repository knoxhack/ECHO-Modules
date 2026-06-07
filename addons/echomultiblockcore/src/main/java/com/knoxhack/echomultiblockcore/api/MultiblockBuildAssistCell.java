package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;

public record MultiblockBuildAssistCell(
        BlockPos localPos,
        StructureBlockRequirement.SlotKind kind,
        String expected,
        boolean optional,
        boolean air,
        boolean wildcard) {
    public MultiblockBuildAssistCell {
        localPos = localPos == null ? BlockPos.ZERO : localPos;
        kind = kind == null ? StructureBlockRequirement.SlotKind.WILDCARD : kind;
        expected = expected == null || expected.isBlank() ? kind.name().toLowerCase(java.util.Locale.ROOT) : expected.strip();
    }

    public static MultiblockBuildAssistCell from(BlockPos localPos, StructureBlockRequirement requirement) {
        requirement = requirement == null ? StructureBlockRequirement.wildcard() : requirement;
        return new MultiblockBuildAssistCell(
                localPos,
                requirement.kind(),
                requirement.expectedName(),
                requirement.optional(),
                requirement.kind() == StructureBlockRequirement.SlotKind.AIR,
                requirement.kind() == StructureBlockRequirement.SlotKind.WILDCARD);
    }
}
