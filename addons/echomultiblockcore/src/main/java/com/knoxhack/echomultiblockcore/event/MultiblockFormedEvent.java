package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class MultiblockFormedEvent extends EchoBackendGameEvent {
    private final ServerLevel level;
    private final Identifier definitionId;
    private final BlockPos controllerPos;
    private final MultiblockRuntime runtime;
    private final MultiblockRuntimeSnapshot snapshot;

    public MultiblockFormedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, MultiblockRuntime runtime) {
        this(level, definitionId, controllerPos, runtime, MultiblockRuntimeSnapshot.from(runtime, MultiblockState.FORMED, 1.0D));
    }

    public MultiblockFormedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, MultiblockRuntime runtime,
            MultiblockRuntimeSnapshot snapshot) {
        this.level = level;
        this.definitionId = definitionId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.runtime = runtime;
        this.snapshot = snapshot;
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

    public MultiblockRuntime runtime() {
        return runtime;
    }

    public MultiblockRuntimeSnapshot snapshot() {
        return snapshot;
    }
}
