package com.knoxhack.echo.hazardcore.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Implemented by modules or packs that introduce environmental hazard sources.
 * Deep Reach registers sources for depth, biome, and structure hazards.
 */
public interface IHazardSource {
    /**
     * Compute the base exposure intensity for the given hazard on this player.
     * Resistance providers are applied afterwards.
     */
    HazardExposure computeExposure(ServerPlayer player, HazardType hazard);

    /**
     * @return true if this source can produce the given hazard at all.
     */
    boolean produces(HazardType hazard);
}
