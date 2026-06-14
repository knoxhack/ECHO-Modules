package com.knoxhack.echo.hazardcore.source;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;

/**
 * Oxygen deprivation when the player's head is submerged in water.
 */
public final class OxygenDeprivationSource implements IHazardSource {
    public static final OxygenDeprivationSource INSTANCE = new OxygenDeprivationSource();

    private OxygenDeprivationSource() {}

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.OXYGEN_DEPRIVATION);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        boolean underwater = player.level().getFluidState(player.blockPosition().above()).is(FluidTags.WATER);
        if (player.canBreatheUnderwater() || !underwater) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "breathing");
        }
        float intensity = 2.0f;
        return new HazardExposure(hazard, intensity, 1.0f, "breathing");
    }
}
