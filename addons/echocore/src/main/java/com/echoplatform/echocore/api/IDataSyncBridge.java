package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface IDataSyncBridge {
    void requestFullSync(ServerPlayer player);

    void markDirty(DataScope scope, String ownerId, Identifier keyId);

    long revision();
}
