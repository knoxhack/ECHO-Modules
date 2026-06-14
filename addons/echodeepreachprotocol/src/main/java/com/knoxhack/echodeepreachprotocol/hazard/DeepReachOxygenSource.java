package com.knoxhack.echodeepreachprotocol.hazard;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;

/**
 * Deep Reach oxygen deprivation.
 *
 * <p>Submersion becomes truly punishing below the Shoals threshold (Y 60). Players
 * without rebreathers or suit oxygen bonuses begin taking deprivation damage.
 */
public final class DeepReachOxygenSource implements IHazardSource {
    public static final DeepReachOxygenSource INSTANCE = new DeepReachOxygenSource();

    private DeepReachOxygenSource() {
    }

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.OXYGEN_DEPRIVATION);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        if (player.canBreatheUnderwater()) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "deep_reach_breathing");
        }
        boolean underwater = player.level().getFluidState(player.blockPosition().above()).is(FluidTags.WATER);
        if (!underwater || player.getY() >= 60.0) {
            return new HazardExposure(hazard, 0.0f, 1.0f, "deep_reach_breathing");
        }
        float multiplier = DeepReachSeasonManager.INSTANCE.getMultiplier(hazard);
        return new HazardExposure(hazard, 3.0f * multiplier, 1.0f, "deep_reach_breathing");
    }
}
