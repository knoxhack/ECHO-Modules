package com.knoxhack.echoholomap.integration;

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
import com.knoxhack.echoholomap.EchoHoloMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class HoloMapMissionCoreIntegration {
    private static final Identifier CHAPTER = id("holomap");

    private HoloMapMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoHoloMap.MODID, HoloMapMissionCoreIntegration::registerContent);
        HoloMapMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoHoloMap.MODID, new MissionChapterDefinition(
                CHAPTER,
                "HoloMap Side Ops",
                "Terrain discovery, waypoint authoring, marker reveals, and route synchronization.",
                74,
                0xFF38DFF4));
        registerMission(registry, "discover_terrain", "terrain", MissionObjectiveType.ENTER_REGION,
                "Discover Terrain", "Let HoloMap sample new terrain around the operator.",
                "Terrain tiles are now cached for the map surface.",
                new ItemStack(Items.FILLED_MAP), 0, 3, "Sample terrain chunks", new ItemStack(Items.PAPER, 4));
        registerMission(registry, "create_waypoint", "waypoint", MissionObjectiveType.CUSTOM,
                "Create Waypoint", "Save a personal or shared waypoint.",
                "Waypoint storage is now linked to the command map.",
                new ItemStack(Items.COMPASS), 1, 1, "Create a waypoint", new ItemStack(Items.COMPASS, 1));
        registerMission(registry, "reveal_marker", "marker", MissionObjectiveType.DISCOVER_STRUCTURE,
                "Reveal Marker", "Reveal a server-side map marker such as a debug, route, or recovery point.",
                "Marker visibility is confirmed.",
                new ItemStack(Items.SPYGLASS), 2, 1, "Reveal a map marker", new ItemStack(Items.GLOWSTONE_DUST, 2));
        registerMission(registry, "sync_route", "sync", MissionObjectiveType.ESTABLISH_ROUTE,
                "Sync Route", "Open HoloMap after route or recovery markers are available.",
                "Route and recovery overlays are synced to the client.",
                new ItemStack(Items.RECOVERY_COMPASS), 3, 1, "Sync route markers", new ItemStack(Items.REDSTONE, 4));
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
            int required,
            String objectiveLabel,
            ItemStack reward) {
        Identifier mission = id(missionPath);
        Identifier target = MissionHookTargets.objectiveTarget(EchoHoloMap.MODID, mission, objectiveKey);
        registry.registerMission(EchoHoloMap.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("holomap_side_ops", "HoloMap Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("HoloMap", "Side Op")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        id(missionPath + "/" + objectiveKey),
                        type,
                        objectiveLabel,
                        "",
                        icon,
                        required,
                        false,
                        Map.of("target", target.toString())))
                .reward(RewardDefinition.item(id(missionPath + "/reward"), MissionRewardClaimMode.CLAIMABLE, reward))
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, path);
    }
}
