package com.knoxhack.echoashfallprotocol.api.drone;

import net.minecraft.server.level.ServerPlayer;

public interface EchoDroneMissionHintProvider {
    String id();

    String missionHint(ServerPlayer owner, EchoDroneOwnerData drone);
}
