package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.knoxhack.echoblockworks.registry.ModItems;
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
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BlockworksMissionCoreIntegration {
    private static final Identifier CHAPTER = id("blockworks");

    private BlockworksMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoBlockworks.MODID, BlockworksMissionCoreIntegration::registerContent);
        BlockworksMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoBlockworks.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Blockworks Side Ops",
                "Blockworks table conversion, pattern cutting, and showcase discovery.",
                80,
                0xFF9CC7D8));
        registerMission(registry, "use_table", "table", MissionObjectiveType.CUSTOM,
                "Use Blockworks Table", "Craft or use a Blockworks Table conversion workflow.",
                "The table conversion loop is online.",
                new ItemStack(ModBlocks.BLOCKWORKS_TABLE.get()), 0, "Use the Blockworks Table", new ItemStack(Items.STONECUTTER, 1));
        registerMission(registry, "convert_variant", "convert", MissionObjectiveType.CRAFT_ITEM,
                "Convert Variant", "Convert any Blockworks variant through the table.",
                "A Blockworks palette conversion has been recorded.",
                new ItemStack(ModBlocks.BLOCKWORKS_TABLE.get()), 1, "Convert a Blockworks variant", new ItemStack(Items.BRICKS, 4));
        registerMission(registry, "use_pattern_cutter", "cutter", MissionObjectiveType.PLACE_BLOCK,
                "Use Pattern Cutter", "Cycle a placed Blockworks block with the Echo Pattern Cutter.",
                "Pattern cutter state transfer verified.",
                new ItemStack(ModItems.ECHO_PATTERN_CUTTER.get()), 2, "Use the Pattern Cutter", new ItemStack(ModItems.ECHO_PATTERN_CUTTER.get()));
        registerMission(registry, "discover_showcase_site", "showcase", MissionObjectiveType.DISCOVER_STRUCTURE,
                "Discover Showcase Site", "Find or interact with a generated Blockworks showcase site.",
                "Showcase discovery route recorded.",
                new ItemStack(Items.LODESTONE), 3, "Discover a showcase site", new ItemStack(Items.LANTERN, 2));
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
        Identifier target = MissionHookTargets.objectiveTarget(EchoBlockworks.MODID, mission, objectiveKey);
        registry.registerMission(EchoBlockworks.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("blockworks_side_ops", "Blockworks Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Blockworks", "Side Op")
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
        return Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, path);
    }
}
