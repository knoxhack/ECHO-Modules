package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class LogisticsMissionHooks {
    private LogisticsMissionHooks() {
    }

    public static void registerCoverage() {
        register("network_online", "route");
        register("label_supplies", "label");
        register("request_loadout", "request");
        register("courier_delivery", "deliver");
        register("depot_exchange", "exchange");
        register("industrial_auto_restock", "restock");
    }

    public static void recordNetworkOnline(Player player, String networkId, String source) {
        record(player, "network_online", "route", MissionObjectiveType.ESTABLISH_ROUTE, "network", networkId, source);
    }

    public static void recordLabelSupplies(Player player, String category) {
        record(player, "label_supplies", "label", MissionObjectiveType.CUSTOM, "category", category, "supply_tag");
    }

    public static void recordRequestLoadout(Player player, String presetId) {
        record(player, "request_loadout", "request", MissionObjectiveType.ESTABLISH_ROUTE, "loadout", presetId, "request_loadout");
    }

    public static void recordCourierDelivery(Player player, String presetId) {
        record(player, "courier_delivery", "deliver", MissionObjectiveType.DELIVER_ITEM, "loadout", presetId, "courier_delivery");
    }

    public static void recordDepotExchange(Player player, String offerId) {
        record(player, "depot_exchange", "exchange", MissionObjectiveType.DELIVER_ITEM, "offer", offerId, "depot_exchange");
    }

    public static void recordIndustrialAutoRestock(Player player, String presetId) {
        record(player, "industrial_auto_restock", "restock", MissionObjectiveType.ESTABLISH_ROUTE, "loadout", presetId, "industrial_auto_restock");
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoLogisticsNetwork.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoLogisticsNetwork.MODID, mission, objectiveKey));
    }

    private static void record(
            Player player,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String detailKey,
            String detail,
            String action) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Identifier mission = mission(missionPath);
        java.util.Map<String, String> context = new java.util.LinkedHashMap<>(
                MissionHookTargets.context(EchoLogisticsNetwork.MODID, mission, detailKey, detail));
        if (action != null && !action.isBlank()) {
            context.put("action", action);
        }
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoLogisticsNetwork.MODID, mission, objectiveKey),
                1,
                java.util.Map.copyOf(context));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, path);
    }
}
