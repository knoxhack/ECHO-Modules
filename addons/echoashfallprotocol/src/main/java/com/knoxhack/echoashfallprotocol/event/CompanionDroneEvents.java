package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CompanionDroneEvents {
    private CompanionDroneEvents() {
    }

    public static void onPlayerLoggedIn(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        CompanionDroneStateStore.hydrateFromDataCore(player);
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        if (drone != null) {
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Drone link restored.")
                    .withStyle(ChatFormatting.AQUA), true);
        } else if (data.isDeployed()) {
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Drone signal lost. Use recall to reconstruct local link.")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player) || player.tickCount % 100 != 0) {
            return;
        }
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        if (!data.isDeployed()) {
            return;
        }
        EchoCompanionDrone drone = CompanionDroneStateStore.ensureDrone(player, false, false);
        if (drone == null && Config.LOG_DRONE_STATE_CHANGES.get()) {
            EchoAshfallProtocol.LOGGER.debug("Companion Drone state is deployed for {}, but entity is not loaded.",
                    player.getScoreboardName());
        }
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
