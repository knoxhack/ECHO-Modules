package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class IndexLensMissionBridge {
    private static final String LENS_ID = "echolens";

    private IndexLensMissionBridge() {
    }

    public static void recordIndexShortcut(ServerPlayer player, String action) {
        if (player == null) {
            return;
        }
        IndexMissionHooks.recordLensShortcut(player, action);
        Identifier mission = Identifier.fromNamespaceAndPath(LENS_ID, "index_shortcut");
        EchoCoreServices.recordMissionObjective(
                player,
                MissionObjectiveType.UNLOCK_RESEARCH,
                MissionHookTargets.objectiveTarget(LENS_ID, mission, "shortcut"),
                1,
                MissionHookTargets.context(LENS_ID, mission, "action", action));
    }
}
