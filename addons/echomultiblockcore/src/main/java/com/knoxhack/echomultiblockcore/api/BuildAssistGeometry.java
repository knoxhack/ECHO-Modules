package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

public final class BuildAssistGeometry {
    private BuildAssistGeometry() {
    }

    public static BuildAssistTransform normalize(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform) {
        if (transform == null) {
            transform = BuildAssistTransform.DEFAULT;
        }
        Rotation rotation = snapshot != null && snapshot.rotationsAllowed() ? transform.rotation() : Rotation.NONE;
        boolean mirrored = snapshot != null && snapshot.mirrorable() && transform.mirrored();
        int layer = visibleLayer(snapshot, transform);
        return new BuildAssistTransform(rotation, mirrored, layer);
    }

    public static BuildAssistTransform rotate(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform) {
        transform = normalize(snapshot, transform);
        if (snapshot != null && !snapshot.rotationsAllowed()) {
            return new BuildAssistTransform(Rotation.NONE, transform.mirrored(), transform.layer());
        }
        return normalize(snapshot, new BuildAssistTransform(switch (transform.rotation()) {
            case NONE -> Rotation.CLOCKWISE_90;
            case CLOCKWISE_90 -> Rotation.CLOCKWISE_180;
            case CLOCKWISE_180 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.NONE;
        }, transform.mirrored(), transform.layer()));
    }

    public static BuildAssistTransform toggleMirror(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform) {
        transform = normalize(snapshot, transform);
        return normalize(snapshot, new BuildAssistTransform(transform.rotation(),
                snapshot != null && snapshot.mirrorable() && !transform.mirrored(), transform.layer()));
    }

    public static BuildAssistTransform layerDelta(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform, int delta) {
        transform = normalize(snapshot, transform);
        if (snapshot == null || snapshot.height() <= 0) {
            return new BuildAssistTransform(transform.rotation(), transform.mirrored(), -1);
        }
        int next = transform.layer() < 0 ? (delta < 0 ? snapshot.height() - 1 : 0) : transform.layer() + delta;
        if (next < 0 || next >= snapshot.height()) {
            next = -1;
        }
        return normalize(snapshot, new BuildAssistTransform(transform.rotation(), transform.mirrored(), next));
    }

    public static int visibleLayer(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform) {
        if (snapshot == null || transform == null || transform.layer() < 0) {
            return -1;
        }
        return Math.max(0, Math.min(snapshot.height() - 1, transform.layer()));
    }

    public static boolean isVisibleLayer(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform, BlockPos local) {
        int layer = visibleLayer(snapshot, transform);
        return layer < 0 || local == null || local.getY() == layer;
    }

    public static BlockPos origin(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform, BlockPos controllerWorldPos) {
        return controllerWorldPos.subtract(transformedLocal(snapshot, transform, snapshot.controllerLocalPos()));
    }

    public static BlockPos localToWorld(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform,
            BlockPos controllerWorldPos, BlockPos local) {
        return origin(snapshot, transform, controllerWorldPos).offset(transformedLocal(snapshot, transform, local));
    }

    public static BlockPos transformedLocal(MultiblockBuildAssistSnapshot snapshot, BuildAssistTransform transform, BlockPos local) {
        transform = normalize(snapshot, transform);
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        int width = snapshot.width();
        int depth = snapshot.depth();
        if (transform.mirrored()) {
            x = width - 1 - x;
        }
        return switch (transform.rotation()) {
            case NONE -> new BlockPos(x, y, z);
            case CLOCKWISE_90 -> new BlockPos(depth - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(width - 1 - x, y, depth - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, width - 1 - x);
        };
    }
}
