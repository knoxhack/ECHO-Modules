package com.knoxhack.echoashfallprotocol.api.drone;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public interface EchoDroneScanProvider {
    String id();

    List<EchoDroneScanResult> scan(ServerPlayer owner, EchoDroneOwnerData drone, int radius, int maxResults);
}
