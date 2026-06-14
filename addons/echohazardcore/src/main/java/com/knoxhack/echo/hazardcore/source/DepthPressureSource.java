package com.knoxhack.echo.hazardcore.source;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pressure increases with depth (lower Y). Calibrated so sea level (~Y 63) is
 * safe and deep ocean / cavern depths become dangerous without a pressure suit.
 */
public final class DepthPressureSource implements IHazardSource {
    public static final DepthPressureSource INSTANCE = new DepthPressureSource();

    private DepthPressureSource() {}

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.PRESSURE);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        double y = player.getY();
        // Sea level and above: no pressure hazard.
        if (y >= 60) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "depth");
        }
        // Below sea level, intensity grows roughly every 30 meters.
        float depth = (float) (60 - y);
        float intensity = depth / 30.0f;
        return new HazardExposure(hazard, intensity, 1.0f, "depth");
    }
}
