package com.knoxhack.echoarcaneindex.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echoarcaneindex.EchoArcaneIndex;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArcaneIndexMissionIntegration {
    private static final Identifier CHAPTER = id("arcane_index");

    private ArcaneIndexMissionIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoArcaneIndex.MODID, ArcaneIndexMissionIntegration::registerContent);
        EchoCoreServices.registerMissionHookCoverage(EchoArcaneIndex.MODID, id("open_arcane_index"),
                MissionHookTargets.objectiveTarget(EchoArcaneIndex.MODID, id("open_arcane_index"), "open"));
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoArcaneIndex.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Arcane Index",
                "Use the official magic knowledge browser instead of relying on JEI.",
                91,
                0x7DE6D1));
        Identifier mission = id("open_arcane_index");
        Identifier target = MissionHookTargets.objectiveTarget(EchoArcaneIndex.MODID, mission, "open");
        registry.registerMission(EchoArcaneIndex.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("arcane_index", "Arcane Index", 0, 10)
                .text("Open Arcane Index", "Find an Arcana page through ECHO: Index.", "Arcane knowledge is official through Index and Grimoire; JEI is optional.")
                .category("Arcana Division", "Knowledge")
                .icon(new ItemStack(Items.KNOWLEDGE_BOOK))
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(id("open_arcane_index/objective"),
                        MissionObjectiveType.UNLOCK_RESEARCH,
                        "Open an Arcane Index page",
                        "",
                        new ItemStack(Items.KNOWLEDGE_BOOK),
                        1,
                        false,
                        Map.of("target", target.toString())))
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoArcaneIndex.MODID, path);
    }
}
