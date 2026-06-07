package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

public record BuildAssistTransform(Rotation rotation, boolean mirrored, int layer) {
    public static final BuildAssistTransform DEFAULT = new BuildAssistTransform(Rotation.NONE, false, -1);

    public BuildAssistTransform {
        rotation = rotation == null ? Rotation.NONE : rotation;
    }

    public BlockPos origin(MultiblockDefinition definition, BlockPos controllerWorldPos) {
        BlockPos controllerLocal = definition.controllerLocalPosition()
                .orElseGet(() -> new BlockPos(definition.width() / 2, 0, definition.depth() / 2));
        return controllerWorldPos.subtract(transformedLocal(definition, controllerLocal));
    }

    public BlockPos localToWorld(MultiblockDefinition definition, BlockPos controllerWorldPos, BlockPos local) {
        return origin(definition, controllerWorldPos).offset(transformedLocal(definition, local));
    }

    public BlockPos transformedLocal(MultiblockDefinition definition, BlockPos local) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        int width = definition.width();
        int depth = definition.depth();
        if (mirrored && definition.mirrorable()) {
            x = width - 1 - x;
        }
        return switch (rotation) {
            case NONE -> new BlockPos(x, y, z);
            case CLOCKWISE_90 -> new BlockPos(depth - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(width - 1 - x, y, depth - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, width - 1 - x);
        };
    }

    public int visibleLayer(MultiblockDefinition definition) {
        return layer < 0 ? -1 : Math.max(0, Math.min(definition.height() - 1, layer));
    }

    public BuildAssistTransform rotate(MultiblockDefinition definition) {
        if (definition != null && !definition.allowedRotations()) {
            return new BuildAssistTransform(Rotation.NONE, mirrored, layer);
        }
        return new BuildAssistTransform(switch (rotation) {
            case NONE -> Rotation.CLOCKWISE_90;
            case CLOCKWISE_90 -> Rotation.CLOCKWISE_180;
            case CLOCKWISE_180 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.NONE;
        }, mirrored, layer);
    }

    public BuildAssistTransform toggleMirror(MultiblockDefinition definition) {
        return new BuildAssistTransform(rotation, definition != null && definition.mirrorable() && !mirrored, layer);
    }

    public BuildAssistTransform layerDelta(MultiblockDefinition definition, int delta) {
        if (definition == null || definition.height() <= 0) {
            return new BuildAssistTransform(rotation, mirrored, -1);
        }
        int next = layer < 0 ? 0 : layer + delta;
        if (next < 0 || next >= definition.height()) {
            next = -1;
        }
        return new BuildAssistTransform(rotation, mirrored, next);
    }
}
