package com.knoxhack.echo.hazardcore.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Implemented by modules that can reduce or negate hazard exposure.
 * EchoEquipmentCore implements this via suit/rebreather stats.
 * EchoSettlementCore implements this when the player is inside a safe habitat.
 */
public interface IHazardResistanceProvider {
    /**
     * @return a value in [0, +∞). 0 means no protection; 1.0 means full immunity.
     * Values above 1.0 may be used for over-pressure or hyper-resistance.
     */
    float getResistance(ServerPlayer player, HazardType hazard);
}
