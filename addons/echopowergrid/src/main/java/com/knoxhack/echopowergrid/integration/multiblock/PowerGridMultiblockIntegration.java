package com.knoxhack.echopowergrid.integration.multiblock;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.config.PowerGridConfig;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockPowerProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * Optional integration bridge for ECHO MultiblockCore.
 *
 * <p>Multiblock controllers can call {@link #requestPower(Level, BlockPos, long)} to draw EP
 * from an adjacent ECHO PowerGrid network. This is a soft bridge: if PowerGrid is not present
 * or the position is not on a network, the request safely returns zero.
 */
public final class PowerGridMultiblockIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final long AVAILABILITY_PROBE = Long.MAX_VALUE / 8L;

    private PowerGridMultiblockIntegration() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        MultiblockIntegrationServices.registerPowerProvider(Provider.INSTANCE);
        EchoPowerGrid.LOGGER.info("ECHO PowerGrid MultiblockCore bridge registered.");
    }

    /**
     * Requests EP from the PowerGrid network at the given position.
     *
     * @param level     the level
     * @param pos       block position of the multiblock controller
     * @param epPerTick amount of EP requested per tick
     * @return the amount of EP actually available (0 if no network or insufficient power)
     */
    public static long requestPower(Level level, BlockPos pos, long epPerTick) {
        if (!PowerGridConfig.ENABLED.get()) return 0;
        return EchoPowerGridApi.drawPower(level, pos, epPerTick, false).drawn();
    }

    /**
     * Returns true if the position is connected to a powered network.
     */
    public static boolean isPowered(Level level, BlockPos pos) {
        if (!PowerGridConfig.ENABLED.get()) return false;
        return EchoPowerGridApi.isPowered(level, pos);
    }

    private enum Provider implements MultiblockPowerProvider {
        INSTANCE;

        private final Identifier providerId = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "provider/multiblock_power");

        @Override
        public Identifier providerId() {
            return providerId;
        }

        @Override
        public long availablePower(Level level, BlockPos controllerPos) {
            if (!PowerGridConfig.ENABLED.get()) {
                return 0L;
            }
            return EchoPowerGridApi.drawPower(level, controllerPos, AVAILABILITY_PROBE, true).drawn();
        }

        @Override
        public long drawPower(Level level, BlockPos controllerPos, long ep, boolean simulate) {
            if (!PowerGridConfig.ENABLED.get()) {
                return 0L;
            }
            return EchoPowerGridApi.drawPower(level, controllerPos, ep, simulate).drawn();
        }
    }
}
