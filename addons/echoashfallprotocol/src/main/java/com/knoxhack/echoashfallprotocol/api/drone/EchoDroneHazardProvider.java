package com.knoxhack.echoashfallprotocol.api.drone;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public interface EchoDroneHazardProvider {
    String id();

    List<String> warnings(ServerPlayer owner, EchoDroneOwnerData drone);
}
