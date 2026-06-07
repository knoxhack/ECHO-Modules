package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class RoboticTaskStartedEvent extends EchoBackendGameEvent {
    public final ServerLevel level;
    public final Identifier definitionId;
    public final BlockPos controllerPos;
    public final TaskExecutionSnapshot snapshot;
    public final MultiblockRuntimeSnapshot beforeSnapshot;
    public final MultiblockRuntimeSnapshot afterSnapshot;

    public RoboticTaskStartedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, TaskExecutionSnapshot snapshot) {
        this(level, definitionId, controllerPos, snapshot, null, null);
    }

    public RoboticTaskStartedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, TaskExecutionSnapshot snapshot,
            MultiblockRuntimeSnapshot beforeSnapshot, MultiblockRuntimeSnapshot afterSnapshot) {
        this.level = level;
        this.definitionId = definitionId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.snapshot = snapshot;
        this.beforeSnapshot = beforeSnapshot;
        this.afterSnapshot = afterSnapshot;
    }
}
