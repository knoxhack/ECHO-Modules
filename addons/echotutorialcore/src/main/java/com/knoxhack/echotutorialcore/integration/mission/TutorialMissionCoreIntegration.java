package com.knoxhack.echotutorialcore.integration.mission;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TutorialMissionCoreIntegration {
    private static final Identifier CHAPTER = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "onboarding");

    private TutorialMissionCoreIntegration() {}

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoTutorialCore.MODID, registry -> {
            registry.registerChapter(EchoTutorialCore.MODID, new MissionChapterDefinition(
                    CHAPTER,
                    "ECHO-7 Onboarding",
                    "Non-authoritative mirrors of TutorialCore first-hour flow.",
                    5,
                    0x92F7A6));
            registry.registerMission(EchoTutorialCore.MODID, MissionDefinition.builder(id("first_hour"), CHAPTER)
                    .phase("tutorial", "First Hour", 0, 0)
                    .text("Read the Field", "Let ECHO-7 track your first stabilizing steps.",
                            "This mirrors TutorialCore flow state. Ashfall remains the authority for campaign progression.")
                    .category("tutorial", "onboarding")
                    .kind(MissionKind.SIDE_OP)
                    .icon(new ItemStack(Items.COMPASS))
                    .objective(new ObjectiveDefinition(id("first_hour_objective"), MissionObjectiveType.CUSTOM,
                            "Complete first-hour guidance", "Open Terminal, find water, build power, scan a route.",
                            ItemStack.EMPTY, 1, false, Map.of("flow", id("welcome_flow").toString())))
                    .completionRule((player, mission) -> TutorialPlayerData.get(player).isFlowCompleted(id("welcome_flow")))
                    .build());
        });
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with MissionCore. Onboarding mirror mission registered.");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }
}
