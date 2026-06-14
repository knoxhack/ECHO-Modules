package com.knoxhack.echo.settlementcore.api;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Record for a habitat claim.
 */
public record Settlement(
    UUID id,
    UUID owner,
    String name,
    List<BlockPos> blocks,
    float oxygenLevel,
    float pressureLevel
) {
    public Settlement {
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
    }

    public boolean contains(BlockPos pos) {
        return blocks.contains(pos);
    }
}
