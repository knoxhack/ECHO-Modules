package com.knoxhack.signalos.integration;

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
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.registry.ModBlocks;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class SignalOsMissionCoreIntegration {
    private static final Identifier CHAPTER = id("signalos");

    private SignalOsMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(SignalOS.MODID, SignalOsMissionCoreIntegration::registerContent);
        SignalOsMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(SignalOS.MODID, new MissionChapterDefinition(
                CHAPTER,
                "SignalOS Side Ops",
                "Computer access, rack networking, and drive record workflows.",
                84,
                0xFF38DFF4));
        registerMission(registry, "boot_terminal", "boot", MissionObjectiveType.SCAN_BLOCK,
                "Boot Terminal", "Open a SignalOS terminal or workstation.",
                "SignalOS shell boot verified. Native app missions remain owned by SignalOS.",
                new ItemStack(ModBlocks.TERMINAL_ITEM.get()), 0, "Boot a SignalOS terminal", new ItemStack(ModBlocks.WORKSTATION_ITEM.get(), 1));
        registerMission(registry, "rack_network_online", "rack", MissionObjectiveType.ESTABLISH_ROUTE,
                "Rack Network Online", "Open or populate a Server Rack with a data drive.",
                "Rack and drive network state is online.",
                new ItemStack(ModBlocks.SERVER_RACK_ITEM.get()), 1, "Bring a rack network online", new ItemStack(ModBlocks.NETWORK_RELAY_ITEM.get(), 1));
        registerMission(registry, "drive_record_flow", "record", MissionObjectiveType.UNLOCK_RESEARCH,
                "Drive Record Flow", "Copy a network record to a drive or apply a drive template.",
                "Drive record mutation reached the server-side rack workflow.",
                new ItemStack(ModBlocks.DATA_DRIVE.get()), 2, "Copy or apply a drive record", new ItemStack(ModBlocks.DATA_DRIVE.get(), 1));
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
        Identifier target = MissionHookTargets.objectiveTarget(SignalOS.MODID, mission, objectiveKey);
        registry.registerMission(SignalOS.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("signalos_side_ops", "SignalOS Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("SignalOS", "Side Op")
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
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }
}
