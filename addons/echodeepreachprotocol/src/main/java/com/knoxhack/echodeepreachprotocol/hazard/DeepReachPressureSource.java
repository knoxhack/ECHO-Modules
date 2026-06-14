package com.knoxhack.echodeepreachprotocol.hazard;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Deep Reach depth-zone pressure curve.
 *
 * <p>The flooded underworld is split into five pressure regimes. Intensity is the
 * raw pressure value that suits must resist; resistance providers in
 * {@code echoequipmentcore} and {@code echosettlementcore} reduce the final damage.
 */
public final class DeepReachPressureSource implements IHazardSource {
    public static final DeepReachPressureSource INSTANCE = new DeepReachPressureSource();

    private DeepReachPressureSource() {
    }

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.PRESSURE);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        double y = player.getY();
        float intensity;
        if (y >= 40.0) {
            // Shoals — surface shallows and dry cave mouths are safe.
            intensity = 0.0f;
        } else if (y >= 10.0) {
            // Twilight Caverns
            intensity = 1.0f;
        } else if (y >= -30.0) {
            // Abyssal Rifts
            intensity = 2.5f;
        } else if (y >= -80.0) {
            // The Lattice
            intensity = 4.0f;
        } else {
            // Hadal Trenches
            intensity = 6.0f;
        }
        float multiplier = DeepReachSeasonManager.INSTANCE.getMultiplier(hazard);
        return new HazardExposure(hazard, intensity * multiplier, 1.0f, "deep_reach_depth");
    }
}
