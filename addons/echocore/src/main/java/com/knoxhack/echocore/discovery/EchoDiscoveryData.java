package com.knoxhack.echocore.discovery;

import com.echoplatform.echocore.api.EchoCoreServices;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Legacy discovery-data facade retained for modules that predate the EchoCoreServices API.
 */
@Deprecated(forRemoval = false)
public final class EchoDiscoveryData {
    private final Player player;

    private EchoDiscoveryData(Player player) {
        this.player = player;
    }

    public static EchoDiscoveryData get(Player player) {
        return new EchoDiscoveryData(player);
    }

    public boolean discover(Identifier featureId) {
        return EchoCoreServices.discoverFeature(player, featureId);
    }

    public boolean hasDiscovered(Identifier featureId) {
        return EchoCoreServices.hasDiscoveredFeature(player, featureId);
    }
}
