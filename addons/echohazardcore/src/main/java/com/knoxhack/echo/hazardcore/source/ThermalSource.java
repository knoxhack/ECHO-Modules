package com.knoxhack.echo.hazardcore.source;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hot/cold exposure driven by biome temperature. Simplified: very cold biomes
 * cause cold, very hot biomes cause heat.
 */
public final class ThermalSource implements IHazardSource {
    public static final ThermalSource INSTANCE = new ThermalSource();

    private ThermalSource() {}

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.COLD) || hazard.equals(HazardType.HEAT);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        float temp = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
        if (hazard.equals(HazardType.COLD)) {
            if (temp > 0.1f) {
                return new HazardExposure(hazard, 0.0f, 1.0f, "biome_temp");
            }
            float intensity = (0.1f - temp) * 4.0f;
            return new HazardExposure(hazard, intensity, 1.0f, "biome_temp");
        }
        if (hazard.equals(HazardType.HEAT)) {
            if (temp < 1.5f) {
                return new HazardExposure(hazard, 0.0f, 1.0f, "biome_temp");
            }
            float intensity = (temp - 1.5f) * 3.0f;
            return new HazardExposure(hazard, intensity, 1.0f, "biome_temp");
        }
        return new HazardExposure(hazard, 0.0f, 1.0f, "biome_temp");
    }
}
