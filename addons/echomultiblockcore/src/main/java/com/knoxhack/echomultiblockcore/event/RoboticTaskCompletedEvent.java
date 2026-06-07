package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class RoboticTaskCompletedEvent extends EchoBackendGameEvent {
    public final ServerLevel level;
    public final Identifier definitionId;
    public final Identifier taskId;
    public final BlockPos controllerPos;
    public final BlockPos robotPos;
    public final TaskExecutionSnapshot snapshot;
    public final MultiblockRuntimeSnapshot beforeSnapshot;
    public final MultiblockRuntimeSnapshot afterSnapshot;

    public RoboticTaskCompletedEvent(ServerLevel level, Identifier definitionId, Identifier taskId, BlockPos controllerPos, BlockPos robotPos) {
        this(level, definitionId, taskId, controllerPos, robotPos, null);
    }

    public RoboticTaskCompletedEvent(ServerLevel level, Identifier definitionId, Identifier taskId, BlockPos controllerPos,
            BlockPos robotPos, TaskExecutionSnapshot snapshot) {
        this(level, definitionId, taskId, controllerPos, robotPos, snapshot, null, null);
    }

    public RoboticTaskCompletedEvent(ServerLevel level, Identifier definitionId, Identifier taskId, BlockPos controllerPos,
            BlockPos robotPos, TaskExecutionSnapshot snapshot, MultiblockRuntimeSnapshot beforeSnapshot,
            MultiblockRuntimeSnapshot afterSnapshot) {
        this.level = level;
        this.definitionId = definitionId;
        this.taskId = taskId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        this.snapshot = snapshot;
        this.beforeSnapshot = beforeSnapshot;
        this.afterSnapshot = afterSnapshot;
    }
}
