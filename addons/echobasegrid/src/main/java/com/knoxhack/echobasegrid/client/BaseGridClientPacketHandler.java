package com.knoxhack.echobasegrid.client;

import com.knoxhack.echobasegrid.network.BaseGridSnapshotPacket;

public final class BaseGridClientPacketHandler {
    private BaseGridClientPacketHandler() {
    }

    public static void apply(BaseGridSnapshotPacket packet) {
        BaseGridClientState.apply(packet);
    }
}
