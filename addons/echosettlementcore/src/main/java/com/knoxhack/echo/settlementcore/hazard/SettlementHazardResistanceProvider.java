package com.knoxhack.echo.settlementcore.hazard;

import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardResistanceProvider;
import com.knoxhack.echo.settlementcore.api.SettlementService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Provides full hazard immunity when a player is inside a safe sealed habitat.
 */
public final class SettlementHazardResistanceProvider implements IHazardResistanceProvider {
    @Override
    public float getResistance(ServerPlayer player, HazardType hazard) {
        if (player == null || hazard == null) {
            return 0.0f;
        }
        if (!SettlementService.find().isPlayerInSafeHabitat(player)) {
            return 0.0f;
        }
        return switch (hazard.id().getPath()) {
            case "pressure",
                 "oxygen_deprivation",
                 "cold",
                 "heat",
                 "corruption" -> 1.0f;
            default -> 0.0f;
        };
    }
}
