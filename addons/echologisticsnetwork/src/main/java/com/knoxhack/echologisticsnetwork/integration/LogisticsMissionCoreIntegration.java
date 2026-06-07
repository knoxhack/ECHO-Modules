package com.knoxhack.echologisticsnetwork.integration;

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
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.registry.ModBlocks;
import com.knoxhack.echologisticsnetwork.registry.ModItems;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LogisticsMissionCoreIntegration {
    private static final Identifier CHAPTER = id("logistics");

    private LogisticsMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoLogisticsNetwork.MODID, LogisticsMissionCoreIntegration::registerContent);
        LogisticsMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoLogisticsNetwork.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Logistics Side Ops",
                "Supply labels, route scanning, courier dispatch, depot exchange, and factory restock support.",
                72,
                0xFF8EF7C2));
        registerMission(registry, "network_online", "route", MissionObjectiveType.ESTABLISH_ROUTE,
                "Network Online", "Scan a ready logistics network or active courier route.",
                "The network route is now visible to MissionCore.",
                new ItemStack(ModBlocks.LOGISTICS_TERMINAL.get()), 0,
                "Bring a Logistics route online", new ItemStack(Items.CHEST, 1), "network_online/route");
        registerMission(registry, "label_supplies", "label", MissionObjectiveType.CUSTOM,
                "Label Supplies", "Apply a Supply Tag to a Logistics storage node.",
                "Supply rows are now labelled for route planning.",
                new ItemStack(ModItems.SUPPLY_TAG.get()), 1,
                "Apply a supply label", new ItemStack(ModItems.LOGISTICS_CHIP.get(), 1), null);
        registerMission(registry, "request_loadout", "request", MissionObjectiveType.ESTABLISH_ROUTE,
                "Request Loadout", "Dispatch a loadout from a dashboard, card, or remote request tablet.",
                "Loadout demand is now routable.",
                new ItemStack(ModItems.LOADOUT_CARD.get()), 2,
                "Request a Logistics loadout", new ItemStack(ModItems.ROUTE_MANIFEST.get(), 2), null);
        registerMission(registry, "courier_delivery", "deliver", MissionObjectiveType.DELIVER_ITEM,
                "Courier Delivery", "Let a courier drone complete a sealed payload delivery.",
                "The courier delivery loop is verified.",
                new ItemStack(ModBlocks.DRONE_DELIVERY_DOCK.get()), 3,
                "Complete a courier delivery", new ItemStack(ModItems.COURIER_DRONE_MODULE.get(), 1), null);
        registerMission(registry, "depot_exchange", "exchange", MissionObjectiveType.DELIVER_ITEM,
                "Faction Depot Exchange", "Complete any available faction depot exchange.",
                "Depot exchange traffic has been reconciled.",
                new ItemStack(ModBlocks.FACTION_TRADE_DEPOT.get()), 4,
                "Complete a depot exchange", new ItemStack(Items.EMERALD, 2), null);
        registerMission(registry, "industrial_auto_restock", "restock", MissionObjectiveType.ESTABLISH_ROUTE,
                "Industrial Auto-Restock", "Dispatch a configured factory auto-restock courier to an Industrial input depot.",
                "Factory restock traffic is now MissionCore-visible.",
                new ItemStack(ModBlocks.AUTO_RESTOCK_STATION.get()), 5,
                "Dispatch factory auto-restock", new ItemStack(ModItems.REMOTE_REQUEST_TABLET.get(), 1), null);
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
            ItemStack reward,
            String objectivePathOverride) {
        Identifier mission = id(missionPath);
        Identifier target = MissionHookTargets.objectiveTarget(EchoLogisticsNetwork.MODID, mission, objectiveKey);
        Identifier objectiveId = id(objectivePathOverride == null ? missionPath + "/" + objectiveKey : objectivePathOverride);
        registry.registerMission(EchoLogisticsNetwork.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("logistics_side_ops", "Logistics Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Logistics", "Side Op")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        objectiveId,
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
        return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, path);
    }
}
