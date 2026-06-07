package com.knoxhack.echoorbitalremnants.progression;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import com.knoxhack.echoorbitalremnants.world.OrbitalDebrisField;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;

public final class EmergencyRocketLaunch {
    private EmergencyRocketLaunch() {
    }

    public static void launchToLowOrbit(Player player) {
        launchToLowOrbit(player, player.getX(), player.getY(), player.getZ(),
                player.level().dimension().identifier().toString());
    }

    public static void launchToLowOrbit(Player player, double returnX, double returnY, double returnZ, String returnDimension) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        progress.setEarthReturnPoint(player, returnX, returnY, returnZ, returnDimension);
        progress.setReturnPoint(player, returnX, returnY, returnZ, returnDimension);
        progress.markLaunchPrepared(player);
        progress.markLowOrbitReached(player);

        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel targetLevel = ModDimensions.resolve(serverPlayer.level().getServer(), ModDimensions.LOW_EARTH_ORBIT, serverPlayer.level());
            double orbitY = targetLevel.dimension() == ModDimensions.LOW_EARTH_ORBIT
                    ? 96.0D
                    : Math.max(serverPlayer.getY() + 80.0D, Config.ORBITAL_ALTITUDE.get());
            BlockPos target = BlockPos.containing(serverPlayer.getX(), orbitY, serverPlayer.getZ());
            if (progress.markRouteArrivalSeeded(player, "low_earth_orbit")) {
                OrbitalDebrisField.seedArrivalField(targetLevel, target);
                Entity dockingAi = ModEntities.CORRUPTED_DOCKING_AI.get().create(targetLevel, EntitySpawnReason.EVENT);
                if (dockingAi != null) {
                    dockingAi.setPos(target.getX() + 6.0D, target.getY() + 1.0D, target.getZ() - 6.0D);
                    targetLevel.addFreshEntity(dockingAi);
                }
            }
            serverPlayer.teleportTo(targetLevel, serverPlayer.getX(), orbitY, serverPlayer.getZ(), Set.of(),
                    serverPlayer.getYRot(), serverPlayer.getXRot(), false);
        }

        player.sendSystemMessage(Component.literal("ECHO-7 // Emergency Rocket ignition. Low Earth orbit acquired."));
        player.sendSystemMessage(Component.literal("\"I was not born in your pod. I fell with it.\""));
    }
}
