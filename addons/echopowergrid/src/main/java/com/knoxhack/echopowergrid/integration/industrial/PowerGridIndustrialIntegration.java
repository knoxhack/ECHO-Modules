package com.knoxhack.echopowergrid.integration.industrial;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.config.PowerGridConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Optional integration bridge for ECHO Industrial Nexus.
 *
 * <p>Provides a soft bridge so Industrial Nexus machines can query PowerGrid status
 * and optionally draw EP as a fallback or supplement to Thermal Flux.
 */
public final class PowerGridIndustrialIntegration {
    private PowerGridIndustrialIntegration() {}

    public static void register() {
        EchoPowerGrid.LOGGER.info("ECHO PowerGrid Industrial Nexus bridge registered.");
    }

    /**
     * Queries available EP at the machine position.
     */
    public static long availablePower(Level level, BlockPos pos) {
        if (!PowerGridConfig.ENABLED.get()) return 0;
        return EchoPowerGridApi.getAvailablePower(level, pos);
    }

    /**
     * Returns true if the machine position is on a powered network.
     */
    public static boolean isPowered(Level level, BlockPos pos) {
        if (!PowerGridConfig.ENABLED.get()) return false;
        return EchoPowerGridApi.isPowered(level, pos);
    }
}
