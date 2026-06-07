package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;

public record BuildAssistAnchor(BlockPos controllerPos, Mode mode) {
    public BuildAssistAnchor {
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        mode = mode == null ? Mode.PLACEMENT : mode;
    }

    public static BuildAssistAnchor targetedController(BlockPos controllerPos) {
        return new BuildAssistAnchor(controllerPos, Mode.TARGETED_CONTROLLER);
    }

    public static BuildAssistAnchor matchingControllerBlock(BlockPos controllerPos) {
        return new BuildAssistAnchor(controllerPos, Mode.MATCHING_CONTROLLER_BLOCK);
    }

    public static BuildAssistAnchor placement(BlockPos controllerPos) {
        return new BuildAssistAnchor(controllerPos, Mode.PLACEMENT);
    }

    public enum Mode {
        TARGETED_CONTROLLER,
        MATCHING_CONTROLLER_BLOCK,
        PLACEMENT
    }
}
