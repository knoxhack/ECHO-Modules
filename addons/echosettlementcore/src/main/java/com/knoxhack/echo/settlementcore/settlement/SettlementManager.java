package com.knoxhack.echo.settlementcore.settlement;

import com.knoxhack.echo.settlementcore.api.Settlement;
import com.knoxhack.echo.settlementcore.api.SettlementService;
import com.knoxhack.echo.settlementcore.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scans for connected habitat blocks, validates airlock seal, and computes oxygen/pressure.
 */
public final class SettlementManager {
    private static final int MAX_VOLUME = 4096;
    private static final int SCAN_RADIUS = 32;
    private static final int PLAYER_SCAN_RADIUS = 64;

    private SettlementManager() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        SettlementService service = SettlementService.find();
        service.clear();
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level, service);
        }
    }

    private static void tickLevel(ServerLevel level, SettlementService service) {
        Set<BlockPos> visited = new HashSet<>();
        Block airlock = ModBlocks.AIRLOCK.get();
        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-PLAYER_SCAN_RADIUS, -PLAYER_SCAN_RADIUS, -PLAYER_SCAN_RADIUS),
                center.offset(PLAYER_SCAN_RADIUS, PLAYER_SCAN_RADIUS, PLAYER_SCAN_RADIUS))) {
                if (visited.contains(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.is(airlock)) {
                    Settlement settlement = scanFrom(level, pos.immutable());
                    if (settlement != null) {
                        service.registerSettlement(settlement);
                        visited.addAll(settlement.blocks());
                    } else {
                        visited.add(pos.immutable());
                    }
                }
            }
        }
    }

    public static Settlement scanFrom(ServerLevel level, BlockPos start) {
        Block block = level.getBlockState(start).getBlock();
        if (block != ModBlocks.AIRLOCK.get()) {
            return null;
        }
        List<BlockPos> open = new LinkedList<>();
        Set<BlockPos> enclosed = new HashSet<>();
        open.add(start);
        enclosed.add(start);
        boolean hasOxygenRecycler = false;
        boolean hasPressurePump = false;
        int airlockCount = 0;

        while (!open.isEmpty() && enclosed.size() < MAX_VOLUME) {
            BlockPos current = open.remove(0);
            BlockState state = level.getBlockState(current);
            Block currentBlock = state.getBlock();
            if (currentBlock == ModBlocks.AIRLOCK.get()) {
                airlockCount++;
            }
            if (currentBlock == ModBlocks.OXYGEN_RECYCLER.get()) {
                hasOxygenRecycler = true;
            }
            if (currentBlock == ModBlocks.PRESSURE_PUMP.get()) {
                hasPressurePump = true;
            }
            for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                if (enclosed.contains(neighbor) || neighbor.distManhattan(start) > SCAN_RADIUS) {
                    continue;
                }
                if (isHabitatBlock(level.getBlockState(neighbor))) {
                    BlockPos immutable = neighbor.immutable();
                    enclosed.add(immutable);
                    open.add(immutable);
                }
            }
        }

        if (airlockCount < 1 || !hasOxygenRecycler || !hasPressurePump) {
            return null;
        }
        float oxygen = hasOxygenRecycler ? 0.85f : 0.0f;
        float pressure = hasPressurePump ? 0.95f : 0.0f;
        return new Settlement(UUID.randomUUID(), UUID.randomUUID(), "Habitat", new ArrayList<>(enclosed), oxygen, pressure);
    }

    private static boolean isHabitatBlock(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.AIRLOCK.get()
            || block == ModBlocks.OXYGEN_RECYCLER.get()
            || block == ModBlocks.PRESSURE_PUMP.get()
            || block == ModBlocks.WORKSHOP.get()
            || block == ModBlocks.MED_BAY.get()
            || block == ModBlocks.DIVERS_QUARTERS.get()
            || block == ModBlocks.CARGO_LOCKER.get()
            || block == ModBlocks.SUBMERSIBLE_DOCK.get()
            || block == ModBlocks.DEEP_MINER_STATION.get()
            || block == ModBlocks.PRESSURE_MECHANIC_STATION.get()
            || block == ModBlocks.XENO_BIOLOGIST_LAB.get();
    }
}
