package com.knoxhack.signalos.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Optional provider for addons that publish curated SignalNet sites.
 */
public interface SignalOsNetProvider {
    Identifier id();

    List<SignalOsNetSite> sites(Player player);

    default int order() {
        return 0;
    }

    default SignalOsProviderStatus providerStatus(Player player) {
        return SignalOsProviderStatus.online(id(), "SignalNet Provider");
    }
}
