package com.knoxhack.echomultiblockcore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import com.knoxhack.echomultiblockcore.registry.ModItems;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class MultiblockMissionCoreIntegration {
    private static final Identifier CHAPTER = id("multiblock_core");

    private MultiblockMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoMultiblockCore.MODID, MultiblockMissionCoreIntegration::registerContent);
        MultiblockMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoMultiblockCore.MODID, new MissionChapterDefinition(
                CHAPTER,
                "MultiblockCore Side Ops",
                "Structure validation, robotics, automation tasks, repair, and auto-builder support.",
                82,
                0xFF55FFDD));
        registerMission(registry, "validate_first_structure", "validate", MissionObjectiveType.BUILD_MULTIBLOCK,
                "Validate First Structure", "Validate or form a MultiblockCore structure from a controller.",
                "The facility controller accepted a complete structure.",
                safeStack(ModBlocks.MULTIBLOCK_CONTROLLER, 1), 0, "Validate a structure",
                safeStack(ModItems.MACHINE_CASING, 2));
        registerMission(registry, "install_robot_tool", "tool", MissionObjectiveType.REPAIR_MACHINE,
                "Install Robot Tool", "Install any tool head into a robotic arm.",
                "Robotic workcell tooling is online.",
                safeStack(ModBlocks.ROBOTIC_ARM, 1), 1, "Install a robot tool head",
                safeStack(ModItems.WELDER_HEAD, 1));
        registerMission(registry, "complete_automation_task", "task", MissionObjectiveType.CUSTOM,
                "Complete Automation Task", "Let a queued MultiblockCore automation task complete.",
                "Automation completion reached MissionCore.",
                safeStack(ModItems.SUPPLY_MANIFEST, 1), 2, "Complete an automation task",
                safeStack(ModItems.SIGNAL_CIRCUIT, 2));
        registerMission(registry, "repair_integrity", "repair", MissionObjectiveType.REPAIR_MACHINE,
                "Repair Integrity", "Complete an automation task that repairs facility integrity.",
                "Facility integrity repair has been recorded.",
                safeStack(ModItems.INTEGRITY_UPGRADE, 1), 3, "Repair multiblock integrity",
                safeStack(ModItems.INTEGRITY_UPGRADE, 1));
        registerMission(registry, "use_auto_builder", "builder", MissionObjectiveType.BUILD_MULTIBLOCK,
                "Use Auto-Builder", "Run the auto-builder to place missing structure blocks.",
                "Auto-builder placement assistance is online.",
                safeStack(ModBlocks.AUTO_BUILDER, 1), 4, "Use the auto-builder",
                safeStack(ModItems.AUTO_BUILDER_CORE, 1));
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
        Identifier target = MissionHookTargets.objectiveTarget(EchoMultiblockCore.MODID, mission, objectiveKey);
        registry.registerMission(EchoMultiblockCore.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("multiblock_side_ops", "MultiblockCore Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Multiblock", "Side Op")
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
        return Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, path);
    }

    private static ItemStack safeStack(Supplier<? extends ItemLike> item, int count) {
        try {
            ItemLike value = item == null ? null : item.get();
            return value == null ? ItemStack.EMPTY : new ItemStack(value, Math.max(1, count));
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }
}
