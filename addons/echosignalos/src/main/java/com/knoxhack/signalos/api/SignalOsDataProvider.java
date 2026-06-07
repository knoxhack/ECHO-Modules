package com.knoxhack.signalos.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface SignalOsDataProvider {
    Identifier id();

    List<SignalOsDataRecord> records(Player player);

    default int order() {
        return 0;
    }

    default SignalOsProviderStatus providerStatus(Player player) {
        return SignalOsProviderStatus.online(id(), "Data Provider");
    }
}
