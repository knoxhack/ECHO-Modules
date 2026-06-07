package com.knoxhack.echorelictech.integration.worldcore;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echorelictech.EchoRelicTech;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class RelicTechWorldCoreIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO WorldCore integration loaded for RelicTech.");
    }

    public static void emitVaultDiscovery(ServerPlayer player, Identifier vaultId, BlockPos pos) {
        EchoCoreServices.structureDiscoveryService().recordStructureScan(
                player, vaultId, pos, "Relic Vault", "Pre-Gridfall research vault discovered.");
    }

    public static void emitRelicFailureHazard(ServerPlayer player, BlockPos pos) {
        if (player == null) {
            return;
        }
        WorldHazardSnapshot previous = EchoCoreServices.hazardService().hazardSnapshot(player);
        WorldHazardSnapshot current = new WorldHazardSnapshot(
                List.of(),
                List.of(Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, "hazard/relic_failure")),
                40,
                false,
                "Relic containment failure detected near "
                        + (pos == null ? "unknown coordinates" : pos.toShortString()) + ".");
        EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player, previous, current));
    }
}
