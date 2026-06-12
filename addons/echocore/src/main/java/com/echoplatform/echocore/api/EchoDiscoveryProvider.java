package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.world.entity.player.Player;

public interface EchoDiscoveryProvider {
    List<EchoDiscoveryEntry> entries(Player player);

    default EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
        return EchoDiscoveryState.LOCKED;
    }
}
