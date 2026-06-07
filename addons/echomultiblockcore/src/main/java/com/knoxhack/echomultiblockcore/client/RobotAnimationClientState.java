package com.knoxhack.echomultiblockcore.client;

import com.knoxhack.echomultiblockcore.api.RobotAnimationState;
import com.knoxhack.echomultiblockcore.network.RobotAnimationPacket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class RobotAnimationClientState {
    private static final Map<BlockPos, RobotAnimationState> ANIMATIONS = new LinkedHashMap<>();

    private RobotAnimationClientState() {
    }

    public static void accept(RobotAnimationPacket packet) {
        if (packet == null) {
            return;
        }
        Identifier robotId = Identifier.fromNamespaceAndPath(packet.taskId().getNamespace(),
                "client_robot_" + Long.toUnsignedString(packet.robotPos().asLong()));
        ANIMATIONS.put(packet.robotPos(), new RobotAnimationState(robotId, packet.robotPos(), packet.targetPos(),
                packet.animationId(), packet.durationTicks(), 0, packet.pose(), packet.taskId()));
    }

    public static void tick() {
        if (ANIMATIONS.isEmpty()) {
            return;
        }
        List<BlockPos> expired = ANIMATIONS.entrySet().stream()
                .filter(entry -> entry.getValue().elapsedTicks() >= entry.getValue().durationTicks() + 20)
                .map(Map.Entry::getKey)
                .toList();
        expired.forEach(ANIMATIONS::remove);
        ANIMATIONS.replaceAll((pos, state) -> new RobotAnimationState(state.robotId(), state.robotPos(), state.targetPos(),
                state.animationId(), state.durationTicks(), state.elapsedTicks() + 1, state.pose(), state.taskId()));
    }

    public static List<RobotAnimationState> active() {
        return List.copyOf(ANIMATIONS.values());
    }
}
