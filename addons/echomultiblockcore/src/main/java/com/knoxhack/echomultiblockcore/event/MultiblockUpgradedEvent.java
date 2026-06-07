package com.knoxhack.echomultiblockcore.event;

import com.knoxhack.echo.adaptercore.EchoBackendGameEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class MultiblockUpgradedEvent extends EchoBackendGameEvent {
    public final ServerLevel level;
    public final Identifier definitionId;
    public final BlockPos controllerPos;
    public final Identifier upgradeId;

    public MultiblockUpgradedEvent(ServerLevel level, Identifier definitionId, BlockPos controllerPos, Identifier upgradeId) {
        this.level = level;
        this.definitionId = definitionId;
        this.controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        this.upgradeId = upgradeId;
    }
}
