package com.knoxhack.echo.hazardcore.source;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import com.knoxhack.echo.hazardcore.player.HazardPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Decompression sickness is triggered by ascending too quickly from depth.
 * Severity grows with the distance ascended per tick while under pressure.
 */
public final class DecompressionSicknessSource implements IHazardSource {
    public static final DecompressionSicknessSource INSTANCE = new DecompressionSicknessSource();
    private static final double MAX_SAFE_ASCENT = 0.8;

    private DecompressionSicknessSource() {}

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.DECOMPRESSION_SICKNESS);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        HazardPlayerData data = HazardPlayerData.get(player);
        double lastDepth = data.getLastDepth();
        double currentDepth = player.getY();
        if (lastDepth == Double.MAX_VALUE || currentDepth >= lastDepth) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "ascent_rate");
        }
        double ascent = lastDepth - currentDepth;
        if (ascent <= MAX_SAFE_ASCENT) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "ascent_rate");
        }
        // Only dangerous if the player was deep enough to be under pressure.
        if (currentDepth > 40 || lastDepth > 40) {
            float intensity = (float) ((ascent - MAX_SAFE_ASCENT) * 2.0);
            return new HazardExposure(hazard, intensity, 1.0f, "ascent_rate");
        }
        return new HazardExposure(hazard, 0.0f, 1.0f, "ascent_rate");
    }
}
