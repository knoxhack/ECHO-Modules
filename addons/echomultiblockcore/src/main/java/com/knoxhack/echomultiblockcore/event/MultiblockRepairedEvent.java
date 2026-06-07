package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class MultiblockRepairedEvent extends EchoBackendGameEvent {
    public final ServerLevel level;
    public final Identifier definitionId;
    public final BlockPos controllerPos;
    public final float integrity;

    public MultiblockRepairedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, float integrity) {
        this.level = level;
        this.definitionId = definitionId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.integrity = integrity;
    }
}
