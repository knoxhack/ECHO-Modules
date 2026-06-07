package com.knoxhack.echogrimoire.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echogrimoire.EchoGrimoire;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GrimoireMissionIntegration {
    private static final Identifier CHAPTER = id("grimoire");

    private GrimoireMissionIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoGrimoire.MODID, GrimoireMissionIntegration::registerContent);
        EchoCoreServices.registerMissionHookCoverage(EchoGrimoire.MODID, id("read_first_entry"),
                MissionHookTargets.objectiveTarget(EchoGrimoire.MODID, id("read_first_entry"), "read"));
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoGrimoire.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Grimoire",
                "Terminal archive route for Arcana lore, warnings, and progression records.",
                92,
                0xB78DFF));
        Identifier mission = id("read_first_entry");
        Identifier target = MissionHookTargets.objectiveTarget(EchoGrimoire.MODID, mission, "read");
        registry.registerMission(EchoGrimoire.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("grimoire", "Grimoire", 0, 10)
                .text("Read First Grimoire Entry", "Open a Grimoire archive record in Terminal.", "The Grimoire is lore/progression archive, not a recipe replacement for Arcane Index.")
                .category("Arcana Division", "Archive")
                .icon(new ItemStack(Items.WRITABLE_BOOK))
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(id("read_first_entry/objective"),
                        MissionObjectiveType.UNLOCK_RESEARCH,
                        "Read a Grimoire entry",
                        "",
                        new ItemStack(Items.WRITABLE_BOOK),
                        1,
                        false,
                        Map.of("target", target.toString())))
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoGrimoire.MODID, path);
    }
}
