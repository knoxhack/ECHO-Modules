package com.knoxhack.echostationfall.world;

import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.integration.StationfallOrbitalCompat;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class StationfallRouteService {
    private StationfallRouteService() {
    }

    public static boolean board(ServerPlayer player, String source) {
        StationfallProgress progress = StationfallProgress.get(player);
        if (StationfallDimensions.isStation(player.level())) {
            player.sendSystemMessage(Component.literal(
                    "ECHO-7 // Already docked inside Stationfall. Sneak-use the access card or use RETURN STATION to leave."
            ));
            return false;
        }
        if (!progress.canBoard(player)) {
            player.sendSystemMessage(Component.literal(
                    "ECHO-7 // Stationfall route locked. Recover Stationfall coordinates or restore Station Network first."
            ));
            return false;
        }
        if (StationfallOrbitalCompat.requiresOrbitalStaging()
                && !StationfallOrbitalCompat.isOrbitalExposure(player)
                && !player.hasInfiniteMaterials()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Stationfall boarding requires orbital staging."));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        ServerLevel target = StationfallDimensions.resolve(server, player.level());
        StationfallStationState station = StationfallStationState.get(target);
        station.ensureSeeded(target);
        station.ensureLighting(target);
        station.mergeFromProgress(progress);

        progress.setReturnPoint(player);
        progress.markBoarded(player);

        BlockPos dock = StationSection.DOCKING_RING.center();
        player.teleportTo(
                target,
                dock.getX() + 0.5,
                dock.getY() + 1,
                dock.getZ() + 0.5,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
        player.sendSystemMessage(Component.literal(
                "ECHO-7 // Stationfall docking vector accepted. The station is still broadcasting."
        ));
        SignalPanicState.get(player).gain(player, "terminal".equals(source) ? 2 : 4);
        return true;
    }

    public static boolean returnFromStation(ServerPlayer player) {
        StationfallProgress progress = StationfallProgress.get(player);
        if (!StationfallDimensions.isStation(player.level())) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Return vector unavailable outside Stationfall."));
            return false;
        }
        if (!progress.hasReturnPoint()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // No Stationfall return vector is saved."));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        ServerLevel target = server.getLevel(progress.returnLevelKey());
        if (target == null) {
            target = server.overworld();
        }
        player.teleportTo(
                target,
                progress.returnX(),
                progress.returnY(),
                progress.returnZ(),
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
        player.sendSystemMessage(Component.literal("ECHO-7 // Return vector complete. Station signal still attached."));
        return true;
    }
}
