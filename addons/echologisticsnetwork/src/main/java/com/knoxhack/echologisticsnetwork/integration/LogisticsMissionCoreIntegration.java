package com.knoxhack.echologisticsnetwork.integration;

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
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.registry.ModBlocks;
import com.knoxhack.echologisticsnetwork.registry.ModItems;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

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
                safeStack(() -> (ItemLike) ModBlocks.LOGISTICS_TERMINAL.get(), Items.CHEST, 1), 0,
                "Bring a Logistics route online", safeStack(() -> Items.CHEST, Items.CHEST, 1), "network_online/route");
        registerMission(registry, "label_supplies", "label", MissionObjectiveType.CUSTOM,
                "Label Supplies", "Apply a Supply Tag to a Logistics storage node.",
                "Supply rows are now labelled for route planning.",
                safeStack(() -> (ItemLike) ModItems.SUPPLY_TAG.get(), Items.NAME_TAG, 1), 1,
                "Apply a supply label", safeStack(() -> (ItemLike) ModItems.LOGISTICS_CHIP.get(), Items.REDSTONE, 1), null);
        registerMission(registry, "request_loadout", "request", MissionObjectiveType.ESTABLISH_ROUTE,
                "Request Loadout", "Dispatch a loadout from a dashboard, card, or remote request tablet.",
                "Loadout demand is now routable.",
                safeStack(() -> (ItemLike) ModItems.LOADOUT_CARD.get(), Items.MAP, 1), 2,
                "Request a Logistics loadout", safeStack(() -> (ItemLike) ModItems.ROUTE_MANIFEST.get(), Items.PAPER, 2), null);
        registerMission(registry, "courier_delivery", "deliver", MissionObjectiveType.DELIVER_ITEM,
                "Courier Delivery", "Let a courier drone complete a sealed payload delivery.",
                "The courier delivery loop is verified.",
                safeStack(() -> (ItemLike) ModBlocks.DRONE_DELIVERY_DOCK.get(), Items.HOPPER, 1), 3,
                "Complete a courier delivery", safeStack(() -> (ItemLike) ModItems.COURIER_DRONE_MODULE.get(), Items.FEATHER, 1), null);
        registerMission(registry, "depot_exchange", "exchange", MissionObjectiveType.DELIVER_ITEM,
                "Faction Depot Exchange", "Complete any available faction depot exchange.",
                "Depot exchange traffic has been reconciled.",
                safeStack(() -> (ItemLike) ModBlocks.FACTION_TRADE_DEPOT.get(), Items.EMERALD_BLOCK, 1), 4,
                "Complete a depot exchange", safeStack(() -> Items.EMERALD, Items.EMERALD, 2), null);
        registerMission(registry, "industrial_auto_restock", "restock", MissionObjectiveType.ESTABLISH_ROUTE,
                "Industrial Auto-Restock", "Dispatch a configured factory auto-restock courier to an Industrial input depot.",
                "Factory restock traffic is now MissionCore-visible.",
                safeStack(() -> (ItemLike) ModBlocks.AUTO_RESTOCK_STATION.get(), Items.DISPENSER, 1), 5,
                "Dispatch factory auto-restock", safeStack(() -> (ItemLike) ModItems.REMOTE_REQUEST_TABLET.get(), Items.COMPASS, 1), null);
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

    private static ItemStack safeStack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
        if (!EchoCoreServices.itemStackComponentsBound()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemLike value = nativeLoaderActive() || item == null ? fallback : item.get();
            return value == null ? ItemStack.EMPTY : new ItemStack(value, Math.max(1, count));
        } catch (RuntimeException | LinkageError ignored) {
            return fallback == null ? ItemStack.EMPTY : new ItemStack(fallback, Math.max(1, count));
        }
    }

    private static boolean nativeLoaderActive() {
        return Boolean.getBoolean("echo.native.loader")
                || !System.getProperty("echo.native.moduleIds", "").isBlank()
                || !System.getProperty("echo.native.moduleClasspath", "").isBlank()
                || !System.getProperty("echo.native.moduleClasspathFile", "").isBlank();
    }
}
