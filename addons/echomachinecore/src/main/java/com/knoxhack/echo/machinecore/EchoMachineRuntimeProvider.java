package com.knoxhack.echo.machinecore;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Publishes live machine state through the neutral MachineCore shape.
 */
public interface EchoMachineRuntimeProvider {
    Identifier providerId();

    default Optional<EchoMachineRuntimeSnapshot> snapshot(Level level, BlockPos pos) {
        return Optional.empty();
    }

    default List<EchoMachineRuntimeSnapshot> snapshots(Player player) {
        return List.of();
    }

    default List<EchoMachineProfile> profiles(Player player) {
        return List.of();
    }
}
