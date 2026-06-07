package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Signal Scanner - locates real nearby POIs and archives route-specific intel.
 */
public class SignalScannerBlockEntity extends BlockEntity {

    public static final int SCAN_COOLDOWN_TICKS = 100;
    public static final int SCAN_POWER_COST = 50;
    public static final int SCAN_WEAR_DELTA = 2;

    private int scanCooldown = 0;
    private MachineWearData wearData;

    public SignalScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_SCANNER.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            wearData = new MachineWearData(level);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SignalScannerBlockEntity entity) {
        if (entity.scanCooldown > 0) {
            entity.scanCooldown--;
        }
    }

    public void triggerScan(ServerPlayer player) {
        AshfallAdapterCoreExplorationRuntime.stationaryScannerUsed(
                player,
                worldPosition,
                "stationary_signal_scanner");
    }

    public boolean isScanCooldownActive() {
        return scanCooldown > 0;
    }

    public int getScanCooldownTicks() {
        return scanCooldown;
    }

    public void startScanCooldown(int ticks) {
        scanCooldown = Math.max(0, ticks);
        setChanged();
    }

    public MachineWearData getWearData() {
        return wearData;
    }
}
