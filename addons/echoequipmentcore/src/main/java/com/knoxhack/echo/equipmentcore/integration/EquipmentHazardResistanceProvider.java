package com.knoxhack.echo.equipmentcore.integration;

import com.knoxhack.echo.equipmentcore.api.EquipmentStats;
import com.knoxhack.echo.equipmentcore.api.EquipmentService;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardResistanceProvider;
import net.minecraft.server.level.ServerPlayer;

public final class EquipmentHazardResistanceProvider implements IHazardResistanceProvider {
    public static final EquipmentHazardResistanceProvider INSTANCE = new EquipmentHazardResistanceProvider();

    private EquipmentHazardResistanceProvider() {
    }

    @Override
    public float getResistance(ServerPlayer player, HazardType hazard) {
        if (player == null || hazard == null) {
            return 0.0F;
        }
        EquipmentStats stats = EquipmentService.find().getTotalStats(player);
        if (HazardType.PRESSURE.equals(hazard)) {
            return stats.pressureResistance();
        }
        if (HazardType.OXYGEN_DEPRIVATION.equals(hazard)) {
            return stats.oxygenBonus();
        }
        if (HazardType.COLD.equals(hazard)) {
            return stats.coldResistance();
        }
        if (HazardType.HEAT.equals(hazard)) {
            return stats.heatResistance();
        }
        if (HazardType.CORRUPTION.equals(hazard)) {
            return stats.corruptionResistance();
        }
        return 0.0F;
    }
}
