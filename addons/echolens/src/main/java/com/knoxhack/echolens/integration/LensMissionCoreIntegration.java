package com.knoxhack.echolens.integration;

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
import com.knoxhack.echolens.EchoLens;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LensMissionCoreIntegration {
    private static final Identifier CHAPTER = id("lens");

    private LensMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoLens.MODID, LensMissionCoreIntegration::registerContent);
        LensMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoLens.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Lens Side Ops",
                "Deep scans, machine diagnostics, and Index shortcut verification.",
                78,
                0xFF66D9EF));
        registerMission(registry, "verified_deep_scan", "scan", MissionObjectiveType.SCAN_BLOCK,
                "Verified Deep Scan", "Complete a server-assisted Deep Scan.",
                "The Lens target was verified server-side.",
                new ItemStack(Items.SPYGLASS), 0, "Complete a verified Deep Scan", new ItemStack(Items.AMETHYST_SHARD, 2));
        registerMission(registry, "machine_diagnostic", "diagnostic", MissionObjectiveType.SCAN_BLOCK,
                "Machine Diagnostic", "Deep-scan a block target for machine or container diagnostics.",
                "Machine diagnostic context was accepted.",
                new ItemStack(Items.REDSTONE_TORCH), 1, "Deep-scan a machine", new ItemStack(Items.REDSTONE, 4));
        registerMission(registry, "index_shortcut", "shortcut", MissionObjectiveType.UNLOCK_RESEARCH,
                "Index Shortcut", "Use a Lens-to-Index recipe, use, or track shortcut.",
                "Lens shortcut telemetry reached the Index.",
                new ItemStack(Items.BOOK), 2, "Use an Index shortcut", new ItemStack(Items.EXPERIENCE_BOTTLE, 1));
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
        Identifier target = MissionHookTargets.objectiveTarget(EchoLens.MODID, mission, objectiveKey);
        registry.registerMission(EchoLens.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("lens_side_ops", "Lens Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Lens", "Side Op")
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
        return Identifier.fromNamespaceAndPath(EchoLens.MODID, path);
    }
}
