package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class IndexMissionHooks {
    private IndexMissionHooks() {
    }

    public static void registerCoverage() {
        register("open_search_entry", "open");
        register("inspect_recipe_source", "recipe");
        register("follow_source_note", "source");
        register("bookmark_record", "bookmark");
        register("pin_recipe_plan", "pin");
        register("transfer_recipe_plan", "transfer");
        register("read_tutorial_entry", "read");
        register("use_lens_shortcut", "lens");
    }

    public static void recordOpenSearch(ServerPlayer player, Identifier entryId) {
        record(player, "open_search_entry", "open", MissionObjectiveType.UNLOCK_RESEARCH, "entry", detail(entryId, "sync"));
    }

    public static void recordRecipeInspect(ServerPlayer player, Identifier recipeId) {
        record(player, "inspect_recipe_source", "recipe", MissionObjectiveType.UNLOCK_RESEARCH, "recipe", detail(recipeId, "recipe"));
    }

    public static void recordSourceNote(ServerPlayer player, Identifier sourceId) {
        record(player, "follow_source_note", "source", MissionObjectiveType.UNLOCK_RESEARCH, "source", detail(sourceId, "source"));
    }

    public static void recordBookmark(ServerPlayer player, Identifier recordId) {
        record(player, "bookmark_record", "bookmark", MissionObjectiveType.UNLOCK_RESEARCH, "record", detail(recordId, "bookmark"));
    }

    public static void recordRecipePin(ServerPlayer player, Identifier recipeId) {
        record(player, "pin_recipe_plan", "pin", MissionObjectiveType.UNLOCK_RESEARCH, "recipe", detail(recipeId, "pin"));
    }

    public static void recordRecipeTransfer(ServerPlayer player, Identifier recipeId) {
        record(player, "transfer_recipe_plan", "transfer", MissionObjectiveType.UNLOCK_RESEARCH, "recipe", detail(recipeId, "transfer"));
    }

    public static void recordReadEntry(ServerPlayer player, Identifier entryId) {
        record(player, "read_tutorial_entry", "read", MissionObjectiveType.UNLOCK_RESEARCH, "entry", detail(entryId, "read"));
    }

    public static void recordLensShortcut(ServerPlayer player, String action) {
        record(player, "use_lens_shortcut", "lens", MissionObjectiveType.UNLOCK_RESEARCH, "action",
                action == null || action.isBlank() ? "shortcut" : action);
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoIndex.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoIndex.MODID, mission, objectiveKey));
    }

    private static void record(
            ServerPlayer player,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String detailKey,
            String detail) {
        if (player == null) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                player,
                type,
                MissionHookTargets.objectiveTarget(EchoIndex.MODID, mission, objectiveKey),
                1,
                MissionHookTargets.context(EchoIndex.MODID, mission, detailKey, detail));
    }

    private static String detail(Identifier id, String fallback) {
        return id == null ? fallback : id.toString();
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndex.MODID, path);
    }
}
