package com.knoxhack.echoashfallprotocol.echo;

import java.util.List;
import java.util.Map;

/**
 * Reference contract for the first Ashfall playable loop.
 */
public final class AshfallBetaRouteContract {
    public static final String CONTRACT_ID = "echoashfallprotocol:ashfall_first_playable_loop";
    public static final String FIRST_MISSION_ID = "echoashfallprotocol:secure_crash_outpost";
    public static final String NEXT_MISSION_ID = "echoashfallprotocol:craft_scrap_knife";

    private AshfallBetaRouteContract() {
    }

    public enum EchoObjectiveTrigger {
        PLAYER_SPAWNED("player.spawned"),
        ITEM_USED("player.item_used"),
        ITEM_COLLECTED("player.item_collected"),
        RECIPE_CRAFTED("player.recipe_crafted"),
        BLOCK_PLACED("player.block_placed"),
        BLOCK_BROKEN("player.block_broken"),
        TERMINAL_OPENED("player.terminal_opened"),
        LENS_SCANNED("player.scanner_used"),
        SCANNER_USED("player.scanner_used"),
        REGION_ENTERED("player.region_entered"),
        HAZARD_SURVIVED("hazard.survived"),
        ENTITY_DEFEATED("entity.defeated"),
        MACHINE_POWERED("player.machine_powered"),
        MACHINE_OUTPUT_CREATED("machine.output_created"),
        MISSION_OBJECTIVE_COMPLETED("mission.objective_completed"),
        MISSION_COMPLETED("mission.completed"),
        SAVE_RESTORED("save.restored");

        private final String id;

        EchoObjectiveTrigger(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record EchoObjective(
            String id,
            String label,
            EchoObjectiveTrigger trigger,
            String target,
            int requiredProgress) {
    }

    public record EchoMissionReward(String id, String rewardType, String target, int count) {
    }

    public record EchoProgressionUnlock(String id, String unlockType, String target) {
    }

    public record EchoTutorialStep(String id, String surface, String messageKey) {
    }

    public record EchoLoreUnlock(String id, String codexEntry, String loreKey) {
    }

    public record EchoMission(
            String id,
            String title,
            List<EchoObjective> objectives,
            List<EchoMissionReward> rewards,
            List<EchoProgressionUnlock> progressionUnlocks,
            List<EchoTutorialStep> tutorialSteps,
            List<EchoLoreUnlock> loreUnlocks) {
    }

    public record EchoMissionState(
            String missionId,
            Map<String, Integer> objectiveProgress,
            boolean rewardGranted,
            List<String> unlockedProgression,
            List<String> unlockedLore) {
    }

    public static EchoMission firstPlayableMission() {
        return new EchoMission(
                FIRST_MISSION_ID,
                "Secure the Crash Outpost",
                betaObjectives(),
                List.of(new EchoMissionReward(
                        "echoashfallprotocol:reward/starter_recovery_cache",
                        "item",
                        "echoashfallprotocol:recovery_cache",
                        1)),
                List.of(new EchoProgressionUnlock(
                        "echoashfallprotocol:unlock/craft_scrap_knife",
                        "mission",
                        NEXT_MISSION_ID)),
                List.of(
                        new EchoTutorialStep(
                                "echoashfallprotocol:tutorial/read_field_manual",
                                "hud",
                                "tutorial.EchoAshfallProtocol.read_field_manual"),
                        new EchoTutorialStep(
                                "echoashfallprotocol:tutorial/use_scanner",
                                "scanner",
                                "tutorial.EchoAshfallProtocol.use_scanner")),
                List.of(new EchoLoreUnlock(
                        "echoashfallprotocol:lore/crash_site_intro",
                        "echoashfallprotocol:crash_site_intro",
                        "lore.EchoAshfallProtocol.crash_site_intro")));
    }

    public static List<EchoObjective> betaObjectives() {
        return List.of(
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/spawn_in_drop_pod",
                        "Spawn in the drop pod",
                        EchoObjectiveTrigger.PLAYER_SPAWNED,
                        "echoashfallprotocol:drop_pod",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/starter_kit",
                        "Receive the starter kit",
                        EchoObjectiveTrigger.ITEM_COLLECTED,
                        "echoashfallprotocol:starter_kit",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/read_field_manual",
                        "Read the field manual",
                        EchoObjectiveTrigger.ITEM_USED,
                        "echoashfallprotocol:field_manual",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/collect_scrap_metal",
                        "Collect scrap metal",
                        EchoObjectiveTrigger.ITEM_COLLECTED,
                        "echoashfallprotocol:scrap_metal",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/place_ash_campfire",
                        "Place the ash campfire",
                        EchoObjectiveTrigger.BLOCK_PLACED,
                        "echoashfallprotocol:ash_campfire",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/craft_basic_tool",
                        "Craft the basic tool",
                        EchoObjectiveTrigger.RECIPE_CRAFTED,
                        "echoashfallprotocol:scrap_knife",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/use_basic_tool",
                        "Use the basic tool on debris",
                        EchoObjectiveTrigger.BLOCK_BROKEN,
                        "echoashfallprotocol:rusted_metal_debris",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/place_water_purifier",
                        "Place the water purifier",
                        EchoObjectiveTrigger.BLOCK_PLACED,
                        "echoashfallprotocol:water_purifier",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/insert_dirty_water",
                        "Insert dirty water",
                        EchoObjectiveTrigger.ITEM_USED,
                        "echoashfallprotocol:dirty_water_bottle",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/power_water_purifier",
                        "Power the water purifier",
                        EchoObjectiveTrigger.MACHINE_POWERED,
                        "echoashfallprotocol:water_purifier",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/receive_clean_water",
                        "Receive clean water",
                        EchoObjectiveTrigger.MACHINE_OUTPUT_CREATED,
                        "echoashfallprotocol:clean_water_bottle",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/use_scanner",
                        "Use the scanner",
                        EchoObjectiveTrigger.SCANNER_USED,
                        "echoashfallprotocol:portable_signal_scanner",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/discover_recovery_cache",
                        "Discover the recovery cache",
                        EchoObjectiveTrigger.REGION_ENTERED,
                        "echoashfallprotocol:recovery_cache",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/open_recovery_cache",
                        "Open the recovery cache",
                        EchoObjectiveTrigger.TERMINAL_OPENED,
                        "echoashfallprotocol:recovery_cache",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/complete_first_mission",
                        "Secure the outpost",
                        EchoObjectiveTrigger.MISSION_COMPLETED,
                        "echoashfallprotocol:secure_crash_outpost",
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/unlock_next_objective",
                        "Unlock the next objective",
                        EchoObjectiveTrigger.MISSION_OBJECTIVE_COMPLETED,
                        NEXT_MISSION_ID,
                        1),
                new EchoObjective(
                        "echoashfallprotocol:secure_crash_outpost/save_load_continue",
                        "Save, reload, and continue",
                        EchoObjectiveTrigger.SAVE_RESTORED,
                        CONTRACT_ID,
                        1));
    }

    public static List<String> betaRoute() {
        return List.of(
                "new_game",
                "spawn_in_drop_pod",
                "starter_kit",
                "read_field_manual",
                "collect_scrap_metal",
                "place_ash_campfire",
                "craft_basic_tool",
                "use_basic_tool",
                "place_water_purifier",
                "insert_dirty_water",
                "power_water_purifier",
                "receive_clean_water",
                "use_scanner",
                "discover_recovery_cache",
                "open_recovery_cache",
                "complete_first_mission",
                "unlock_next_objective",
                "save_load_continue");
    }
}
