package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.MissionRewardClaimMode;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echocore.api.mission.RewardDefinition;
import com.knoxhack.echoindex.EchoIndex;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndexMissionCoreIntegration {
    private static final Identifier CHAPTER = id("index");

    private IndexMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoIndex.MODID, IndexMissionCoreIntegration::registerContent);
        IndexMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoIndex.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Index Side Ops",
                "Search, track, inspect, and follow shared recipe and source records.",
                76,
                0xFFAEEA6A));
        registerMission(registry, "open_search_entry", "open", MissionObjectiveType.UNLOCK_RESEARCH,
                "Open Search Entry", "Open or search the shared Index feed.",
                "Index search state is now synced.",
                new ItemStack(Items.BOOK), 0, "Open or search Index", new ItemStack(Items.PAPER, 4));
        registerMission(registry, "inspect_recipe_source", "recipe", MissionObjectiveType.UNLOCK_RESEARCH,
                "Inspect Recipe Source", "Pin, transfer, or inspect a tracked recipe card.",
                "The recipe source is now ready for operator use.",
                new ItemStack(Items.CRAFTING_TABLE), 1, "Inspect an Index recipe", new ItemStack(Items.BOOK, 1));
        registerMission(registry, "follow_source_note", "source", MissionObjectiveType.UNLOCK_RESEARCH,
                "Follow Source Note", "Follow an addon source note or read a linked Index entry.",
                "Addon source notes are now reachable from the archive.",
                new ItemStack(Items.WRITABLE_BOOK), 2, "Follow a source note", new ItemStack(Items.EXPERIENCE_BOTTLE, 1));
        registerMission(registry, "bookmark_record", "bookmark", MissionObjectiveType.UNLOCK_RESEARCH,
                "Bookmark Record", "Bookmark an Index item, entry, or source for later planning.",
                "The operator can now maintain a focused research queue.",
                new ItemStack(Items.NAME_TAG), 3, "Bookmark an Index record", new ItemStack(Items.PAPER, 6));
        registerMission(registry, "pin_recipe_plan", "pin", MissionObjectiveType.UNLOCK_RESEARCH,
                "Pin Recipe Plan", "Pin a recipe plan so missing materials stay visible.",
                "Pinned plans now mirror into the operator planning loop.",
                new ItemStack(Items.ITEM_FRAME), 4, "Pin an Index recipe", new ItemStack(Items.STICK, 8));
        registerMission(registry, "transfer_recipe_plan", "transfer", MissionObjectiveType.UNLOCK_RESEARCH,
                "Transfer Crafting Plan", "Use Index transfer on a safe crafting recipe.",
                "Craftable plans can now flow from reference into action.",
                new ItemStack(Items.CRAFTING_TABLE), 5, "Transfer a recipe", new ItemStack(Items.CRAFTING_TABLE, 1));
        registerMission(registry, "read_tutorial_entry", "read", MissionObjectiveType.UNLOCK_RESEARCH,
                "Read Tutorial Entry", "Mark an Index tutorial or overview entry as read.",
                "Tutorial records are now tracked as operator knowledge.",
                new ItemStack(Items.KNOWLEDGE_BOOK), 6, "Read an Index entry", new ItemStack(Items.BOOK, 1));
        registerMission(registry, "use_lens_shortcut", "lens", MissionObjectiveType.UNLOCK_RESEARCH,
                "Use Lens Shortcut", "Reach Index context through a Lens or inspection shortcut.",
                "Lens-to-Index handoff is now part of the shared archive loop.",
                new ItemStack(Items.SPYGLASS), 7, "Use an Index shortcut", new ItemStack(Items.AMETHYST_SHARD, 2));
    }

    private static void registerMission(
            IMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String title,
            String briefing,
            String fieldGuide,
            ItemStack icon,
            int order,
            String objectiveLabel,
            ItemStack reward) {
        Identifier mission = id(missionPath);
        Identifier target = MissionHookTargets.objectiveTarget(EchoIndex.MODID, mission, objectiveKey);
        registry.registerMission(EchoIndex.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("index_side_ops", "Index Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Index", "Side Op")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        id(missionPath + "/" + objectiveKey),
                        type,
                        objectiveLabel,
                        "",
                        icon,
                        1,
                        false,
                        Map.of("target", target.toString())))
                .reward(RewardDefinition.item(id(missionPath + "/reward"), MissionRewardClaimMode.CLAIMABLE, reward))
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndex.MODID, path);
    }
}
