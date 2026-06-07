package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeProvider;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
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

public final class AshfallMachineCoreRuntimeProvider implements EchoMachineRuntimeProvider {
    private static final AshfallMachineCoreRuntimeProvider INSTANCE = new AshfallMachineCoreRuntimeProvider();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int RADIUS = 32;
    private static final int Y_RADIUS = 8;
    private static final int LIMIT = 96;

    private AshfallMachineCoreRuntimeProvider() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoMachineRuntimeRegistry.register(INSTANCE);
            EchoAshfallProtocol.LOGGER.info("ECHO Ashfall MachineCore runtime provider registered.");
        }
    }

    @Override
    public Identifier providerId() {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "machinecore_runtime");
    }

    @Override
    public Optional<EchoMachineRuntimeSnapshot> snapshot(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!ashfallBlockEntity(blockEntity)) {
            return Optional.empty();
        }
        try {
            return Optional.of(AshfallMachineCoreAdapter.runtimeSnapshot(blockEntity));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<EchoMachineRuntimeSnapshot> snapshots(Player player) {
        return machines(player).stream()
                .map(AshfallMachineCoreAdapter::runtimeSnapshot)
                .toList();
    }

    @Override
    public List<EchoMachineProfile> profiles(Player player) {
        return machines(player).stream()
                .map(AshfallMachineCoreAdapter::profile)
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
            if (ashfallBlockEntity(blockEntity)) {
                machines.add(blockEntity);
                if (machines.size() >= LIMIT) {
                    break;
                }
            }
        }
        return List.copyOf(machines);
    }

    private static boolean ashfallBlockEntity(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        Identifier id = blockEntity.getBlockState() == null
                ? null
                : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        return id != null && EchoAshfallProtocol.MODID.equals(id.getNamespace());
    }
}
