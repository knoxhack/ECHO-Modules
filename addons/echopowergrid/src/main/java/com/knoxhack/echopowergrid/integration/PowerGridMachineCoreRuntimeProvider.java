package com.knoxhack.echopowergrid.integration;

import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeProvider;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PowerGridMachineCoreRuntimeProvider implements EchoMachineRuntimeProvider {
    private static final PowerGridMachineCoreRuntimeProvider INSTANCE = new PowerGridMachineCoreRuntimeProvider();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int RADIUS = 32;
    private static final int Y_RADIUS = 8;
    private static final int LIMIT = 128;

    private PowerGridMachineCoreRuntimeProvider() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoMachineRuntimeRegistry.register(INSTANCE);
            EchoPowerGrid.LOGGER.info("ECHO: PowerGrid MachineCore runtime provider registered.");
        }
    }

    @Override
    public Identifier providerId() {
        return EchoPowerGrid.id("machinecore_runtime");
    }

    @Override
    public Optional<EchoMachineRuntimeSnapshot> snapshot(Level level, BlockPos pos) {
        BlockEntity blockEntity = level == null || pos == null ? null : level.getBlockEntity(pos);
        if (PowerGridMachineCoreAdapter.supports(blockEntity)) {
            return Optional.of(PowerGridMachineCoreAdapter.runtimeSnapshot(blockEntity));
        }
        return Optional.empty();
    }

    @Override
    public List<EchoMachineRuntimeSnapshot> snapshots(Player player) {
        return machines(player).stream()
                .map(PowerGridMachineCoreAdapter::runtimeSnapshot)
                .toList();
    }

    @Override
    public List<EchoMachineProfile> profiles(Player player) {
        return machines(player).stream()
                .map(PowerGridMachineCoreAdapter::profile)
                .toList();
    }

    private static List<BlockEntity> machines(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return List.of();
        }
        Level level = serverPlayer.level();
        BlockPos origin = serverPlayer.blockPosition();
        List<BlockEntity> machines = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -Y_RADIUS, -RADIUS), origin.offset(RADIUS, Y_RADIUS, RADIUS))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (PowerGridMachineCoreAdapter.supports(blockEntity)) {
                machines.add(blockEntity);
                if (machines.size() >= LIMIT) {
                    break;
                }
            }
        }
        return List.copyOf(machines);
    }
}
