package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class MultiblockDamagedEvent extends EchoBackendGameEvent {
    private final ServerLevel level;
    private final Identifier definitionId;
    private final BlockPos controllerPos;
    private final float integrity;
    private final String source;
    private final MultiblockRuntimeSnapshot beforeSnapshot;
    private final MultiblockRuntimeSnapshot afterSnapshot;

    public MultiblockDamagedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, float integrity, String source) {
        this(level, definitionId, controllerPos, integrity, source, null, null);
    }

    public MultiblockDamagedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, float integrity, String source,
            MultiblockRuntimeSnapshot beforeSnapshot, MultiblockRuntimeSnapshot afterSnapshot) {
        this.level = level;
        this.definitionId = definitionId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.integrity = integrity;
        this.source = source == null ? "generic" : source;
        this.beforeSnapshot = beforeSnapshot;
        this.afterSnapshot = afterSnapshot;
    }

    public ServerLevel level() {
        return level;
    }

    public Identifier definitionId() {
        return definitionId;
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    public float integrity() {
        return integrity;
    }

    public String source() {
        return source;
    }

    public MultiblockRuntimeSnapshot beforeSnapshot() {
        return beforeSnapshot;
    }

    public MultiblockRuntimeSnapshot afterSnapshot() {
        return afterSnapshot;
    }
}
