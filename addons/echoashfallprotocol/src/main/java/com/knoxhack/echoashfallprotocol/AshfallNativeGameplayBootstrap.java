package com.knoxhack.echoashfallprotocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AshfallNativeGameplayBootstrap {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final String FIRST_CHAPTER_ID = "echoashfallprotocol:ashfall_crash_landing";
    private static final String FIRST_MISSION_ID = "echoashfallprotocol:secure_crash_outpost";
    private static final String FIRST_OBJECTIVE_ID = "echoashfallprotocol:secure_crash_outpost/place_ash_campfire";
    private static final String FIRST_OBJECTIVE_TARGET = "echoashfallprotocol:ash_campfire";
    private static final String SECOND_MISSION_ID = "echoashfallprotocol:craft_scrap_knife";
    private static final String SECOND_OBJECTIVE_ID = "echoashfallprotocol:craft_scrap_knife/obtain_scrap_knife";
    private static final String SECOND_OBJECTIVE_TARGET = "echoashfallprotocol:scrap_knife";
    private static final String THIRD_MISSION_ID = "echoashfallprotocol:drink_clean_water";
    private static final String THIRD_OBJECTIVE_ID = "echoashfallprotocol:drink_clean_water/confirm_clean_water";
    private static final String THIRD_OBJECTIVE_TARGET = "echoashfallprotocol:clean_water_bottle";
    private static final String THIRD_CONSUME_MARKER = "water:clean_consumed";
    private static final String FOURTH_MISSION_ID = "echoashfallprotocol:secure_emergency_water_loop";
    private static final String FOURTH_DIRTY_OBJECTIVE_ID = "echoashfallprotocol:secure_emergency_water_loop/dirty_water_collected";
    private static final String FOURTH_DIRTY_OBJECTIVE_TARGET = "water:dirty_collected";
    private static final String FOURTH_FILTER_OBJECTIVE_ID = "echoashfallprotocol:secure_emergency_water_loop/emergency_filtration_proven";
    private static final String FOURTH_FILTER_OBJECTIVE_TARGET = "water:emergency_filtered";
    private static final String FIFTH_MISSION_ID = "echoashfallprotocol:forage_wasteland_food";
    private static final String FIFTH_OBJECTIVE_ID = "echoashfallprotocol:forage_wasteland_food/confirm_food_buffer";
    private static final String FIFTH_OBJECTIVE_TARGET = "ashfall:food_buffer";
    private static final String SIXTH_MISSION_ID = "echoashfallprotocol:plant_mutated_sapling";
    private static final String SIXTH_OBJECTIVE_ID = "echoashfallprotocol:plant_mutated_sapling/place_mutated_sapling";
    private static final String SIXTH_OBJECTIVE_TARGET = "echoashfallprotocol:mutated_sapling";
    private static final String SEVENTH_MISSION_ID = "echoashfallprotocol:build_rain_collector";
    private static final String SEVENTH_OBJECTIVE_ID = "echoashfallprotocol:build_rain_collector/place_rain_collector";
    private static final String SEVENTH_OBJECTIVE_TARGET = "echoashfallprotocol:rain_collector";
    private static final String EIGHTH_MISSION_ID = "echoashfallprotocol:stockpile_rations";
    private static final String EIGHTH_OBJECTIVE_ID = "echoashfallprotocol:stockpile_rations/confirm_ration_buffer";
    private static final String EIGHTH_OBJECTIVE_TARGET = "ashfall:ration_buffer";
    private static final String NINTH_MISSION_ID = "echoashfallprotocol:secure_sleep_shelter";
    private static final String NINTH_OBJECTIVE_ID = "echoashfallprotocol:secure_sleep_shelter/confirm_sleep_shelter";
    private static final String NINTH_OBJECTIVE_TARGET = "ashfall:sleep_shelter";
    private static final String TENTH_MISSION_ID = "echoashfallprotocol:assemble_wasteland_field_kit";
    private static final String TENTH_OBJECTIVE_ID = "echoashfallprotocol:assemble_wasteland_field_kit/confirm_field_kit";
    private static final String TENTH_OBJECTIVE_TARGET = "ashfall:wasteland_field_kit";
    private static final String ELEVENTH_MISSION_ID = "echoashfallprotocol:find_schematic_fragment";
    private static final String ELEVENTH_OBJECTIVE_ID = "echoashfallprotocol:find_schematic_fragment/authenticate_schematic_fragment";
    private static final String ELEVENTH_OBJECTIVE_TARGET = "echoashfallprotocol:schematic_fragment";
    private static final String TWELFTH_MISSION_ID = "echoashfallprotocol:build_hand_recycler";
    private static final String TWELFTH_OBJECTIVE_ID = "echoashfallprotocol:build_hand_recycler/place_hand_recycler";
    private static final String TWELFTH_OBJECTIVE_TARGET = "echoashfallprotocol:hand_recycler";
    private static final String THIRTEENTH_MISSION_ID = "echoashfallprotocol:make_machine_casing";
    private static final String THIRTEENTH_OBJECTIVE_ID = "echoashfallprotocol:make_machine_casing/confirm_machine_casing";
    private static final String THIRTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:machine_casing";
    private static final String FOURTEENTH_MISSION_ID = "echoashfallprotocol:build_micro_generator";
    private static final String FOURTEENTH_OBJECTIVE_ID = "echoashfallprotocol:build_micro_generator/place_micro_generator";
    private static final String FOURTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:micro_generator";
    private static final String FIFTEENTH_MISSION_ID = "echoashfallprotocol:build_water_purifier";
    private static final String FIFTEENTH_OBJECTIVE_ID = "echoashfallprotocol:build_water_purifier/place_water_purifier";
    private static final String FIFTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:water_purifier";
    private static final String SIXTEENTH_MISSION_ID = "echoashfallprotocol:stockpile_clean_water";
    private static final String SIXTEENTH_OBJECTIVE_ID = "echoashfallprotocol:stockpile_clean_water/confirm_clean_water_reserve";
    private static final String SIXTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:clean_water_bottle";
    private static final String SEVENTEENTH_MISSION_ID = "echoashfallprotocol:build_battery_bank";
    private static final String SEVENTEENTH_OBJECTIVE_ID = "echoashfallprotocol:build_battery_bank/place_battery_bank";
    private static final String SEVENTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:battery_bank";
    private static final String EIGHTEENTH_MISSION_ID = "echoashfallprotocol:build_scrap_dynamo";
    private static final String EIGHTEENTH_OBJECTIVE_ID = "echoashfallprotocol:build_scrap_dynamo/place_scrap_dynamo";
    private static final String EIGHTEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:scrap_dynamo";
    private static final String NINETEENTH_MISSION_ID = "echoashfallprotocol:charge_basic_battery";
    private static final String NINETEENTH_OBJECTIVE_ID = "echoashfallprotocol:charge_basic_battery/confirm_charged_battery";
    private static final String NINETEENTH_OBJECTIVE_TARGET = "echoashfallprotocol:basic_battery";
    private static final String TWENTIETH_MISSION_ID = "echoashfallprotocol:route_power_cable";
    private static final String TWENTIETH_OBJECTIVE_ID = "echoashfallprotocol:route_power_cable/place_power_cable";
    private static final String TWENTIETH_OBJECTIVE_TARGET = "echoashfallprotocol:power_cable";
    private static final String TWENTY_FIRST_MISSION_ID = "echoashfallprotocol:upgrade_power_cable";
    private static final String TWENTY_FIRST_OBJECTIVE_ID = "echoashfallprotocol:upgrade_power_cable/place_reinforced_power_cable";
    private static final String TWENTY_FIRST_OBJECTIVE_TARGET = "echoashfallprotocol:reinforced_power_cable";
    private static final String TWENTY_SECOND_MISSION_ID = "echoashfallprotocol:install_energy_meter";
    private static final String TWENTY_SECOND_OBJECTIVE_ID = "echoashfallprotocol:install_energy_meter/place_energy_meter";
    private static final String TWENTY_SECOND_OBJECTIVE_TARGET = "echoashfallprotocol:energy_meter";
    private static final String TWENTY_THIRD_MISSION_ID = "echoashfallprotocol:set_power_priority";
    private static final String TWENTY_THIRD_BLOCK_OBJECTIVE_ID = "echoashfallprotocol:set_power_priority/place_load_distributor";
    private static final String TWENTY_THIRD_MARKER_OBJECTIVE_ID = "echoashfallprotocol:set_power_priority/set_survival_first";
    private static final String TWENTY_THIRD_OBJECTIVE_TARGET = "echoashfallprotocol:load_distributor";
    private static final String TWENTY_THIRD_PRIORITY_MARKER = "power:priority_set";
    private static final String TWENTY_FOURTH_MISSION_ID = "echoashfallprotocol:build_scrap_press";
    private static final String TWENTY_FOURTH_OBJECTIVE_ID = "echoashfallprotocol:build_scrap_press/place_scrap_press";
    private static final String TWENTY_FOURTH_OBJECTIVE_TARGET = "echoashfallprotocol:scrap_press";
    private static final String TWENTY_FIFTH_MISSION_ID = "echoashfallprotocol:overclock_machine";
    private static final String TWENTY_FIFTH_OBJECTIVE_ID = "echoashfallprotocol:overclock_machine/obtain_overclock_module";
    private static final String TWENTY_FIFTH_OBJECTIVE_TARGET = "echoashfallprotocol:machine_upgrade_overclock";
    private static final String TWENTY_SIXTH_MISSION_ID = "echoashfallprotocol:install_item_pipe";
    private static final String TWENTY_SIXTH_OBJECTIVE_ID = "echoashfallprotocol:install_item_pipe/place_item_pipe";
    private static final String TWENTY_SIXTH_OBJECTIVE_TARGET = "echoashfallprotocol:item_pipe";
    private static final String TWENTY_SEVENTH_MISSION_ID = "echoashfallprotocol:build_thermal_burner";
    private static final String TWENTY_SEVENTH_OBJECTIVE_ID = "echoashfallprotocol:build_thermal_burner/place_thermal_burner";
    private static final String TWENTY_SEVENTH_OBJECTIVE_TARGET = "echoashfallprotocol:thermal_burner";
    private static final String TWENTY_EIGHTH_MISSION_ID = "echoashfallprotocol:base_stability_check";
    private static final String TWENTY_EIGHTH_OBJECTIVE_ID = "echoashfallprotocol:base_stability_check/confirm_stable_outpost";
    private static final String TWENTY_EIGHTH_OBJECTIVE_TARGET = "ashfall:stable_outpost";
    private static final String TWENTY_NINTH_MISSION_ID = "echoashfallprotocol:equip_gas_mask";
    private static final String TWENTY_NINTH_OBJECTIVE_ID = "echoashfallprotocol:equip_gas_mask/equip_head_slot";
    private static final String TWENTY_NINTH_OBJECTIVE_TARGET = "echoashfallprotocol:gas_mask";
    private static final String THIRTIETH_MISSION_ID = "echoashfallprotocol:fix_mask_filter";
    private static final String THIRTIETH_OBJECTIVE_ID = "echoashfallprotocol:fix_mask_filter/confirm_basic_filter";
    private static final String THIRTIETH_OBJECTIVE_TARGET = "echoashfallprotocol:filter_cartridge_basic";
    private static final String THIRTY_FIRST_MISSION_ID = "echoashfallprotocol:build_filter_workbench";
    private static final String THIRTY_FIRST_OBJECTIVE_ID = "echoashfallprotocol:build_filter_workbench/place_filter_workbench";
    private static final String THIRTY_FIRST_OBJECTIVE_TARGET = "echoashfallprotocol:filter_workbench";
    private static final String THIRTY_SECOND_MISSION_ID = "echoashfallprotocol:craft_advanced_filter";
    private static final String THIRTY_SECOND_OBJECTIVE_ID = "echoashfallprotocol:craft_advanced_filter/confirm_advanced_filter";
    private static final String THIRTY_SECOND_OBJECTIVE_TARGET = "echoashfallprotocol:filter_cartridge_advanced";
    private static final String THIRTY_THIRD_MISSION_ID = "echoashfallprotocol:build_research_lab";
    private static final String THIRTY_THIRD_OBJECTIVE_ID = "echoashfallprotocol:build_research_lab/place_research_lab";
    private static final String THIRTY_THIRD_OBJECTIVE_TARGET = "echoashfallprotocol:research_lab";
    private static final String THIRTY_FOURTH_MISSION_ID = "echoashfallprotocol:first_schematic";
    private static final String THIRTY_FOURTH_OBJECTIVE_ID = "echoashfallprotocol:first_schematic/unlock_first_schematic";
    private static final String THIRTY_FOURTH_OBJECTIVE_TARGET = "ashfall:schematic_unlocked";
    private static final String THIRTY_FIFTH_MISSION_ID = "echoashfallprotocol:build_factory_controller";
    private static final String THIRTY_FIFTH_OBJECTIVE_ID = "echoashfallprotocol:build_factory_controller/place_factory_controller";
    private static final String THIRTY_FIFTH_OBJECTIVE_TARGET = "echoashfallprotocol:factory_controller";
    private static final String THIRTY_SIXTH_MISSION_ID = "echoashfallprotocol:craft_portable_scanner";
    private static final String THIRTY_SIXTH_OBJECTIVE_ID = "echoashfallprotocol:craft_portable_scanner/obtain_portable_signal_scanner";
    private static final String THIRTY_SIXTH_OBJECTIVE_TARGET = "echoashfallprotocol:portable_signal_scanner";
    private static final String CRASH_REGION_ID = "echoashfallprotocol:crash_zone_wasteland";
    private static final String CRASH_HAZARD_ID = "echoworldcore:hazard/salvage_debris";

    private AshfallNativeGameplayBootstrap() {
    }

    static Map<String, Object> initialize(Map<String, String> context) {
        Map<String, Object> firstMission = firstMission();
        Map<String, Object> secondMission = secondMission();
        Map<String, Object> thirdMission = thirdMission();
        Map<String, Object> fourthMission = fourthMission();
        Map<String, Object> fifthMission = fifthMission();
        Map<String, Object> sixthMission = sixthMission();
        Map<String, Object> seventhMission = seventhMission();
        Map<String, Object> eighthMission = eighthMission();
        Map<String, Object> ninthMission = ninthMission();
        Map<String, Object> tenthMission = tenthMission();
        Map<String, Object> eleventhMission = eleventhMission();
        Map<String, Object> twelfthMission = twelfthMission();
        Map<String, Object> thirteenthMission = thirteenthMission();
        Map<String, Object> fourteenthMission = fourteenthMission();
        Map<String, Object> fifteenthMission = fifteenthMission();
        Map<String, Object> sixteenthMission = sixteenthMission();
        Map<String, Object> seventeenthMission = seventeenthMission();
        Map<String, Object> eighteenthMission = eighteenthMission();
        Map<String, Object> nineteenthMission = nineteenthMission();
        Map<String, Object> twentiethMission = twentiethMission();
        Map<String, Object> twentyFirstMission = twentyFirstMission();
        Map<String, Object> twentySecondMission = twentySecondMission();
        Map<String, Object> twentyThirdMission = twentyThirdMission();
        Map<String, Object> twentyFourthMission = twentyFourthMission();
        Map<String, Object> twentyFifthMission = twentyFifthMission();
        Map<String, Object> twentySixthMission = twentySixthMission();
        Map<String, Object> twentySeventhMission = twentySeventhMission();
        Map<String, Object> twentyEighthMission = twentyEighthMission();
        Map<String, Object> twentyNinthMission = twentyNinthMission();
        Map<String, Object> thirtiethMission = thirtiethMission();
        Map<String, Object> thirtyFirstMission = thirtyFirstMission();
        Map<String, Object> thirtySecondMission = thirtySecondMission();
        Map<String, Object> thirtyThirdMission = thirtyThirdMission();
        Map<String, Object> thirtyFourthMission = thirtyFourthMission();
        Map<String, Object> thirtyFifthMission = thirtyFifthMission();
        Map<String, Object> thirtySixthMission = thirtySixthMission();
        Map<String, Object> worldAnchor = worldAnchor();
        Map<String, Object> progressionHook = progressionHook(List.of(
                firstMission, secondMission, thirdMission, fourthMission, fifthMission, sixthMission, seventhMission, eighthMission, ninthMission, tenthMission, eleventhMission, twelfthMission, thirteenthMission, fourteenthMission, fifteenthMission, sixteenthMission, seventeenthMission, eighteenthMission, nineteenthMission, twentiethMission, twentyFirstMission, twentySecondMission, twentyThirdMission, twentyFourthMission, twentyFifthMission, twentySixthMission, twentySeventhMission, twentyEighthMission, twentyNinthMission, thirtiethMission, thirtyFirstMission, thirtySecondMission, thirtyThirdMission, thirtyFourthMission, thirtyFifthMission, thirtySixthMission));
        List<String> diagnostics = validate(
                firstMission, secondMission, thirdMission, fourthMission, fifthMission, sixthMission, seventhMission, eighthMission, ninthMission, tenthMission, eleventhMission, twelfthMission, thirteenthMission, fourteenthMission, fifteenthMission, sixteenthMission, seventeenthMission, eighteenthMission, nineteenthMission, twentiethMission, twentyFirstMission, twentySecondMission, twentyThirdMission, twentyFourthMission, twentyFifthMission, twentySixthMission, twentySeventhMission, twentyEighthMission, twentyNinthMission, thirtiethMission, thirtyFirstMission, thirtySecondMission, thirtyThirdMission, thirtyFourthMission, thirtyFifthMission, thirtySixthMission, worldAnchor, progressionHook);
        boolean verified = diagnostics.isEmpty();

        List<Map<String, Object>> verifiedHooks = verified
                ? List.of(firstGameplayHook(), secondGameplayHook(), thirdGameplayHook(), fourthGameplayHook(), fifthGameplayHook(), sixthGameplayHook(), seventhGameplayHook(), eighthGameplayHook(), ninthGameplayHook(), tenthGameplayHook(), eleventhGameplayHook(), twelfthGameplayHook(), thirteenthGameplayHook(), fourteenthGameplayHook(), fifteenthGameplayHook(), sixteenthGameplayHook(), seventeenthGameplayHook(), eighteenthGameplayHook(), nineteenthGameplayHook(), twentiethGameplayHook(), twentyFirstGameplayHook(), twentySecondGameplayHook(), twentyThirdGameplayHook(), twentyFourthGameplayHook(), twentyFifthGameplayHook(), twentySixthGameplayHook(), twentySeventhGameplayHook(), twentyEighthGameplayHook(), twentyNinthGameplayHook(), thirtiethGameplayHook(), thirtyFirstGameplayHook(), thirtySecondGameplayHook(), thirtyThirdGameplayHook(), thirtyFourthGameplayHook(), thirtyFifthGameplayHook(), thirtySixthGameplayHook())
                : List.of();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", MODULE_ID);
        result.put("packId", context == null ? "unknown" : context.getOrDefault("packId", "unknown"));
        result.put("serviceId", "echoashfallprotocol:first_route_bootstrap");
        result.put("adapterCoreBridge", true);
        result.put("implementationTarget", "AdapterCore native service bridge");
        result.put("standaloneDuplicateGameplaySystem", false);
        result.put("serviceCodeExecuted", true);
        result.put("runtimeStateInitialized", true);
        result.put("minecraftRuntimeAccessed", false);
        result.put("registryMutated", false);
        result.put("bytecodeMutated", false);
        result.put("unsafeRuntimeWorkStarted", false);
        result.put("verificationMode", "jdk_only_adaptercore_contract_validation");
        result.put("routeStepCount", 36);
        result.put("routeMissions", List.of(firstMission, secondMission, thirdMission, fourthMission, fifthMission, sixthMission, seventhMission, eighthMission, ninthMission, tenthMission, eleventhMission, twelfthMission, thirteenthMission, fourteenthMission, fifteenthMission, sixteenthMission, seventeenthMission, eighteenthMission, nineteenthMission, twentiethMission, twentyFirstMission, twentySecondMission, twentyThirdMission, twentyFourthMission, twentyFifthMission, twentySixthMission, twentySeventhMission, twentyEighthMission, twentyNinthMission, thirtiethMission, thirtyFirstMission, thirtySecondMission, thirtyThirdMission, thirtyFourthMission, thirtyFifthMission, thirtySixthMission));
        result.put("firstMission", firstMission);
        result.put("secondMission", secondMission);
        result.put("thirdMission", thirdMission);
        result.put("fourthMission", fourthMission);
        result.put("fifthMission", fifthMission);
        result.put("sixthMission", sixthMission);
        result.put("seventhMission", seventhMission);
        result.put("eighthMission", eighthMission);
        result.put("ninthMission", ninthMission);
        result.put("tenthMission", tenthMission);
        result.put("eleventhMission", eleventhMission);
        result.put("twelfthMission", twelfthMission);
        result.put("thirteenthMission", thirteenthMission);
        result.put("fourteenthMission", fourteenthMission);
        result.put("fifteenthMission", fifteenthMission);
        result.put("sixteenthMission", sixteenthMission);
        result.put("seventeenthMission", seventeenthMission);
        result.put("eighteenthMission", eighteenthMission);
        result.put("nineteenthMission", nineteenthMission);
        result.put("twentiethMission", twentiethMission);
        result.put("twentyFirstMission", twentyFirstMission);
        result.put("twentySecondMission", twentySecondMission);
        result.put("twentyThirdMission", twentyThirdMission);
        result.put("twentyFourthMission", twentyFourthMission);
        result.put("twentyFifthMission", twentyFifthMission);
        result.put("twentySixthMission", twentySixthMission);
        result.put("twentySeventhMission", twentySeventhMission);
        result.put("twentyEighthMission", twentyEighthMission);
        result.put("twentyNinthMission", twentyNinthMission);
        result.put("thirtiethMission", thirtiethMission);
        result.put("thirtyFirstMission", thirtyFirstMission);
        result.put("thirtySecondMission", thirtySecondMission);
        result.put("thirtyThirdMission", thirtyThirdMission);
        result.put("thirtyFourthMission", thirtyFourthMission);
        result.put("thirtyFifthMission", thirtyFifthMission);
        result.put("thirtySixthMission", thirtySixthMission);
        result.put("worldAnchor", worldAnchor);
        result.put("progressionHook", progressionHook);
        result.put("verifiedGameplayHooks", verifiedHooks);
        result.put("verifiedHookCount", verifiedHooks.size());
        result.put("diagnostics", diagnostics);
        result.put("status", verified ? "PASS" : "FAIL");
        result.put("summary", verified
                ? "Ashfall opening route gameplay content was validated as a native MissionCore/WorldCore data path without touching Minecraft runtime state."
                : "Ashfall opening route gameplay content failed native contract validation.");
        return result;
    }

    private static Map<String, Object> firstMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", FIRST_MISSION_ID);
        mission.put("neoForgeMissionId", "secure_crash_outpost");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("chapterDataPath", "data/echoashfallprotocol/missioncore/chapters/ashfall_crash_landing.json");
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/secure_crash_outpost.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_0");
        mission.put("objectiveId", FIRST_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", FIRST_OBJECTIVE_TARGET);
        mission.put("runtimeEvent", "player.block_placed");
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 4);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> secondMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", SECOND_MISSION_ID);
        mission.put("neoForgeMissionId", "craft_scrap_knife");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/craft_scrap_knife.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_0");
        mission.put("prerequisite", FIRST_MISSION_ID);
        mission.put("objectiveId", SECOND_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", SECOND_OBJECTIVE_TARGET);
        mission.put("runtimeEvent", "player.inventory_changed");
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirdMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRD_MISSION_ID);
        mission.put("neoForgeMissionId", "drink_clean_water");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/drink_clean_water.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_0");
        mission.put("prerequisite", SECOND_MISSION_ID);
        mission.put("objectiveId", THIRD_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", THIRD_OBJECTIVE_TARGET);
        mission.put("consumeMarker", THIRD_CONSUME_MARKER);
        mission.put("consumeEventRecordsTarget", THIRD_OBJECTIVE_TARGET);
        mission.put("runtimeEvents", List.of("player.inventory_changed", "player.consume_item"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> fourthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", FOURTH_MISSION_ID);
        mission.put("neoForgeMissionId", "secure_emergency_water_loop");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/secure_emergency_water_loop.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", THIRD_MISSION_ID);
        mission.put("objectiveIds", List.of(FOURTH_DIRTY_OBJECTIVE_ID, FOURTH_FILTER_OBJECTIVE_ID));
        mission.put("objectiveType", "enter_region");
        mission.put("objectiveTargets", List.of(FOURTH_DIRTY_OBJECTIVE_TARGET, FOURTH_FILTER_OBJECTIVE_TARGET));
        mission.put("runtimeEvents", List.of("ashfall.special_marker", "player.inventory_changed", "player.craft_item"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("replacesDeprecatedlegacy runtimeMissions", List.of("get_dirty_water", "emergency_filter_water"));
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> fifthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", FIFTH_MISSION_ID);
        mission.put("neoForgeMissionId", "forage_wasteland_food");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/forage_wasteland_food.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", FOURTH_MISSION_ID);
        mission.put("objectiveId", FIFTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", FIFTH_OBJECTIVE_TARGET);
        mission.put("acceptedItems", List.of("echoashfallprotocol:wild_berry", "echoashfallprotocol:emergency_ration"));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> sixthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", SIXTH_MISSION_ID);
        mission.put("neoForgeMissionId", "plant_mutated_sapling");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/plant_mutated_sapling.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", FIFTH_MISSION_ID);
        mission.put("objectiveId", SIXTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", SIXTH_OBJECTIVE_TARGET);
        mission.put("runtimeEvent", "player.block_placed");
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 4);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> seventhMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", SEVENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_rain_collector");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_rain_collector.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", SIXTH_MISSION_ID);
        mission.put("objectiveId", SEVENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", SEVENTH_OBJECTIVE_TARGET);
        mission.put("runtimeEvent", "player.block_placed");
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> eighthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", EIGHTH_MISSION_ID);
        mission.put("neoForgeMissionId", "stockpile_rations");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/stockpile_rations.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", SEVENTH_MISSION_ID);
        mission.put("objectiveId", EIGHTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", EIGHTH_OBJECTIVE_TARGET);
        mission.put("acceptedItemCounts", Map.of(
                "echoashfallprotocol:emergency_ration", 4,
                "echoashfallprotocol:wild_berry", 12));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> ninthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", NINTH_MISSION_ID);
        mission.put("neoForgeMissionId", "secure_sleep_shelter");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/secure_sleep_shelter.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", EIGHTH_MISSION_ID);
        mission.put("objectiveId", NINTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", NINTH_OBJECTIVE_TARGET);
        mission.put("acceptedTargets", List.of("echoashfallprotocol:emergency_bunk", "minecraft:bed", "shelter:slept"));
        mission.put("runtimeEvents", List.of("player.block_placed", "player.sleep", "ashfall.special_marker"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 5);
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> tenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "assemble_wasteland_field_kit");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/assemble_wasteland_field_kit.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", NINTH_MISSION_ID);
        mission.put("objectiveId", TENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", TENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:bone_knife",
                "echoashfallprotocol:crude_spear",
                "echoashfallprotocol:hide_wrap"));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "player.craft_item", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 6);
        mission.put("replacesDeprecatedlegacy runtimeMissions", List.of("craft_bone_knife", "craft_crude_spear", "craft_hide_wrap"));
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> eleventhMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", ELEVENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "find_schematic_fragment");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/find_schematic_fragment.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_1");
        mission.put("prerequisite", TENTH_MISSION_ID);
        mission.put("objectiveId", ELEVENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", ELEVENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(ELEVENTH_OBJECTIVE_TARGET));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "player.terminal_opened", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("machineProgressionBridge", "ashfall.machine.hand_recycler.casing_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twelfthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWELFTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_hand_recycler");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_hand_recycler.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", ELEVENTH_MISSION_ID);
        mission.put("objectiveId", TWELFTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWELFTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire"));
        mission.put("blockRequirements", Map.of(TWELFTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("starterBatteryItem", "echoashfallprotocol:basic_battery");
        mission.put("starterBatteryEnergy", 1000);
        mission.put("machineProgressionBridge", "ashfall.machine.hand_recycler.online");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "make_machine_casing");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/make_machine_casing.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", TWELFTH_MISSION_ID);
        mission.put("objectiveId", THIRTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", THIRTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(THIRTEENTH_OBJECTIVE_TARGET));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 4);
        mission.put("machineProgressionBridge", "ashfall.machine.micro_generator.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> fourteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", FOURTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_micro_generator");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_micro_generator.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", THIRTEENTH_MISSION_ID);
        mission.put("objectiveId", FOURTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", FOURTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_metal"));
        mission.put("blockRequirements", Map.of(FOURTEENTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 5);
        mission.put("machineProgressionBridge", "ashfall.machine.water_purifier.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> fifteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", FIFTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_water_purifier");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_water_purifier.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", FOURTEENTH_MISSION_ID);
        mission.put("objectiveId", FIFTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", FIFTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:circuit_board"));
        mission.put("blockRequirements", Map.of(FIFTEENTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 4);
        mission.put("machineProgressionBridge", "ashfall.survival.clean_water_reserves_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> sixteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", SIXTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "stockpile_clean_water");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/stockpile_clean_water.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", FIFTEENTH_MISSION_ID);
        mission.put("objectiveId", SIXTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", SIXTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItemCounts", Map.of(SIXTEENTH_OBJECTIVE_TARGET, 3));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.battery_bank.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> seventeenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", SEVENTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_battery_bank");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_battery_bank.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", SIXTEENTH_MISSION_ID);
        mission.put("objectiveId", SEVENTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", SEVENTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:circuit_board"));
        mission.put("blockRequirements", Map.of(SEVENTEENTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.scrap_dynamo.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> eighteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", EIGHTEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_scrap_dynamo");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_scrap_dynamo.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", SEVENTEENTH_MISSION_ID);
        mission.put("objectiveId", EIGHTEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", EIGHTEENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:energy_cell"));
        mission.put("blockRequirements", Map.of(EIGHTEENTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.basic_battery.charge_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> nineteenthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", NINETEENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "charge_basic_battery");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/charge_basic_battery.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", EIGHTEENTH_MISSION_ID);
        mission.put("objectiveId", NINETEENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "custom");
        mission.put("objectiveTarget", NINETEENTH_OBJECTIVE_TARGET);
        mission.put("chargedItemPredicate", Map.of(
                "item", NINETEENTH_OBJECTIVE_TARGET,
                "minimumEnergy", 1_000));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.energy_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.power_cable.route_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentiethMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTIETH_MISSION_ID);
        mission.put("neoForgeMissionId", "route_power_cable");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/route_power_cable.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", NINETEENTH_MISSION_ID);
        mission.put("objectiveId", TWENTIETH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTIETH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "minecraft:copper_ingot",
                "echoashfallprotocol:circuit_board",
                "minecraft:redstone"));
        mission.put("blockRequirements", Map.of(TWENTIETH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.reinforced_power_cable.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyFirstMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_FIRST_MISSION_ID);
        mission.put("neoForgeMissionId", "upgrade_power_cable");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/upgrade_power_cable.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", TWENTIETH_MISSION_ID);
        mission.put("objectiveId", TWENTY_FIRST_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTY_FIRST_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:power_cable",
                "echoashfallprotocol:scrap_wire"));
        mission.put("blockRequirements", Map.of(TWENTY_FIRST_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.energy_meter.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentySecondMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_SECOND_MISSION_ID);
        mission.put("neoForgeMissionId", "install_energy_meter");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/install_energy_meter.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", TWENTY_FIRST_MISSION_ID);
        mission.put("objectiveId", TWENTY_SECOND_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTY_SECOND_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:circuit_board",
                "minecraft:glass_pane",
                "minecraft:redstone"));
        mission.put("blockRequirements", Map.of(TWENTY_SECOND_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 1);
        mission.put("machineProgressionBridge", "ashfall.machine.load_distributor.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyThirdMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_THIRD_MISSION_ID);
        mission.put("neoForgeMissionId", "set_power_priority");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/set_power_priority.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_2");
        mission.put("prerequisite", TWENTY_SECOND_MISSION_ID);
        mission.put("objectiveIds", List.of(TWENTY_THIRD_BLOCK_OBJECTIVE_ID, TWENTY_THIRD_MARKER_OBJECTIVE_ID));
        mission.put("objectiveType", "composite");
        mission.put("objectiveTarget", TWENTY_THIRD_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:battery_bank",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_wire"));
        mission.put("blockRequirements", Map.of(TWENTY_THIRD_OBJECTIVE_TARGET, 1));
        mission.put("specialMarkers", List.of(TWENTY_THIRD_PRIORITY_MARKER));
        mission.put("runtimeEvents", List.of("player.block_placed", "player.use_block", "ashfall.block_requirement", "ashfall.special_marker"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.scrap_press.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyFourthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_FOURTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_scrap_press");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_scrap_press.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_THIRD_MISSION_ID);
        mission.put("objectiveId", TWENTY_FOURTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTY_FOURTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:circuit_board"));
        mission.put("blockRequirements", Map.of(TWENTY_FOURTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.overclock_module.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyFifthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_FIFTH_MISSION_ID);
        mission.put("neoForgeMissionId", "overclock_machine");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/overclock_machine.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_FOURTH_MISSION_ID);
        mission.put("objectiveId", TWENTY_FIFTH_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", TWENTY_FIFTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(TWENTY_FIFTH_OBJECTIVE_TARGET));
        mission.put("requiredItemCounts", Map.of(TWENTY_FIFTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.logistics.item_pipe.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentySixthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_SIXTH_MISSION_ID);
        mission.put("neoForgeMissionId", "install_item_pipe");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/install_item_pipe.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_FIFTH_MISSION_ID);
        mission.put("objectiveId", TWENTY_SIXTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTY_SIXTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "minecraft:iron_ingot",
                "echoashfallprotocol:circuit_board"));
        mission.put("blockRequirements", Map.of(TWENTY_SIXTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.machine.thermal_burner.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentySeventhMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_SEVENTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_thermal_burner");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_thermal_burner.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_SIXTH_MISSION_ID);
        mission.put("objectiveId", TWENTY_SEVENTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", TWENTY_SEVENTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:ash"));
        mission.put("blockRequirements", Map.of(TWENTY_SEVENTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 1);
        mission.put("machineProgressionBridge", "ashfall.survival.base_stability_check.bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyEighthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_EIGHTH_MISSION_ID);
        mission.put("neoForgeMissionId", "base_stability_check");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/base_stability_check.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_SEVENTH_MISSION_ID);
        mission.put("objectiveId", TWENTY_EIGHTH_OBJECTIVE_ID);
        mission.put("objectiveType", "composite_predicate");
        mission.put("objectiveTarget", TWENTY_EIGHTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:clean_water_bottle",
                "echoashfallprotocol:bandage",
                "echoashfallprotocol:energy_cell"));
        mission.put("requiredItemCounts", Map.of(
                "echoashfallprotocol:clean_water_bottle", 3,
                "echoashfallprotocol:bandage", 2,
                "echoashfallprotocol:energy_cell", 1));
        mission.put("blockRequirements", Map.of(
                "minecraft:chest", 1,
                "echoashfallprotocol:water_purifier", 1,
                "echoashfallprotocol:battery_bank", 1));
        mission.put("alternateCompletionSignals", List.of("echoashfallprotocol:first_faction_contact:completed"));
        mission.put("runtimeEvents", List.of("ashfall.block_requirement", "ashfall.inventory_predicate", "ashfall.mission_completed"));
        mission.put("terminalAction", "auto_complete_when_predicate_true");
        mission.put("rewardCount", 4);
        mission.put("machineProgressionBridge", "ashfall.survival.hazard_prep.bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> twentyNinthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", TWENTY_NINTH_MISSION_ID);
        mission.put("neoForgeMissionId", "equip_gas_mask");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/equip_gas_mask.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_EIGHTH_MISSION_ID);
        mission.put("objectiveId", TWENTY_NINTH_OBJECTIVE_ID);
        mission.put("objectiveType", "equipment_predicate");
        mission.put("objectiveTarget", TWENTY_NINTH_OBJECTIVE_TARGET);
        mission.put("equipmentSlot", "head");
        mission.put("requiredEquipment", Map.of("head", TWENTY_NINTH_OBJECTIVE_TARGET));
        mission.put("runtimeEvents", List.of("player.equipment_changed", "ashfall.equipment_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.survival.filter_reserve.bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtiethMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTIETH_MISSION_ID);
        mission.put("neoForgeMissionId", "fix_mask_filter");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/fix_mask_filter.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", TWENTY_NINTH_MISSION_ID);
        mission.put("objectiveId", THIRTIETH_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", THIRTIETH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(THIRTIETH_OBJECTIVE_TARGET));
        mission.put("requiredItemCounts", Map.of(THIRTIETH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.survival.filter_workbench.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtyFirstMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_FIRST_MISSION_ID);
        mission.put("neoForgeMissionId", "build_filter_workbench");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_filter_workbench.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", THIRTIETH_MISSION_ID);
        mission.put("objectiveId", THIRTY_FIRST_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", THIRTY_FIRST_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_wire"));
        mission.put("blockRequirements", Map.of(THIRTY_FIRST_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 1);
        mission.put("machineProgressionBridge", "ashfall.survival.advanced_filter.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtySecondMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_SECOND_MISSION_ID);
        mission.put("neoForgeMissionId", "craft_advanced_filter");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/craft_advanced_filter.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_3");
        mission.put("prerequisite", THIRTY_FIRST_MISSION_ID);
        mission.put("objectiveId", THIRTY_SECOND_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", THIRTY_SECOND_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(THIRTY_SECOND_OBJECTIVE_TARGET));
        mission.put("requiredItemCounts", Map.of(THIRTY_SECOND_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 2);
        mission.put("machineProgressionBridge", "ashfall.survival.research_lab.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtyThirdMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_THIRD_MISSION_ID);
        mission.put("neoForgeMissionId", "build_research_lab");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_research_lab.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_4");
        mission.put("prerequisite", THIRTY_SECOND_MISSION_ID);
        mission.put("neoForgeRouteOverridePrerequisite", true);
        mission.put("objectiveId", THIRTY_THIRD_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", THIRTY_THIRD_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:machine_casing"));
        mission.put("blockRequirements", Map.of(THIRTY_THIRD_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 1);
        mission.put("machineProgressionBridge", "ashfall.research.first_schematic.bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtyFourthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_FOURTH_MISSION_ID);
        mission.put("neoForgeMissionId", "first_schematic");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/first_schematic.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_4");
        mission.put("prerequisite", THIRTY_THIRD_MISSION_ID);
        mission.put("objectiveId", THIRTY_FOURTH_OBJECTIVE_ID);
        mission.put("objectiveType", "research_predicate");
        mission.put("objectiveTarget", THIRTY_FOURTH_OBJECTIVE_TARGET);
        mission.put("requiredResearch", Map.of("unlockedSchematicsAtLeast", 1));
        mission.put("runtimeEvents", List.of("ashfall.research_updated", "ashfall.schematic_unlocked"));
        mission.put("terminalAction", "auto_complete_when_predicate_true");
        mission.put("rewardCount", 1);
        mission.put("machineProgressionBridge", "ashfall.machine.factory_controller.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtyFifthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_FIFTH_MISSION_ID);
        mission.put("neoForgeMissionId", "build_factory_controller");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/build_factory_controller.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_4");
        mission.put("prerequisite", THIRTY_FOURTH_MISSION_ID);
        mission.put("objectiveId", THIRTY_FIFTH_OBJECTIVE_ID);
        mission.put("objectiveType", "place_block");
        mission.put("objectiveTarget", THIRTY_FIFTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:energy_cell",
                "minecraft:redstone_block"));
        mission.put("requiredItemCounts", Map.of(
                "echoashfallprotocol:circuit_board", 5,
                "echoashfallprotocol:machine_casing", 2,
                "echoashfallprotocol:energy_cell", 1,
                "minecraft:redstone_block", 1));
        mission.put("blockRequirements", Map.of(THIRTY_FIFTH_OBJECTIVE_TARGET, 1));
        mission.put("runtimeEvents", List.of("player.block_placed", "ashfall.block_requirement"));
        mission.put("terminalAction", "turn_in");
        mission.put("rewardCount", 3);
        mission.put("machineProgressionBridge", "ashfall.poi.portable_scanner.components_bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> thirtySixthMission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", THIRTY_SIXTH_MISSION_ID);
        mission.put("neoForgeMissionId", "craft_portable_scanner");
        mission.put("chapterId", FIRST_CHAPTER_ID);
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/craft_portable_scanner.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "phase_5");
        mission.put("prerequisite", THIRTY_FIFTH_MISSION_ID);
        mission.put("objectiveId", THIRTY_SIXTH_OBJECTIVE_ID);
        mission.put("objectiveType", "obtain_item");
        mission.put("objectiveTarget", THIRTY_SIXTH_OBJECTIVE_TARGET);
        mission.put("requiredItems", List.of(THIRTY_SIXTH_OBJECTIVE_TARGET));
        mission.put("requiredItemCounts", Map.of(THIRTY_SIXTH_OBJECTIVE_TARGET, 1));
        mission.put("recipeDataPath", "data/echoashfallprotocol/recipe/portable_signal_scanner.json");
        mission.put("recipeIngredients", Map.of(
                "echoashfallprotocol:scrap_circuit", 2,
                "echoashfallprotocol:scrap_wire", 1,
                "echoashfallprotocol:scrap_metal", 2,
                "echoashfallprotocol:circuit_board", 1,
                "echoashfallprotocol:energy_cell", 1));
        mission.put("runtimeEvents", List.of("player.craft_item", "player.inventory_changed", "ashfall.inventory_predicate"));
        mission.put("terminalAction", "auto_complete_on_pickup");
        mission.put("rewardCount", 2);
        mission.put("explorationProgressionBridge", "ashfall.poi.first_ruin_signature.bridge");
        mission.put("mirrorslegacy runtimeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> worldAnchor() {
        Map<String, Object> world = new LinkedHashMap<>();
        world.put("regionId", CRASH_REGION_ID);
        world.put("regionDataPath", "data/echoashfallprotocol/echoworldcore/world_regions/ashfall_crash_zone_wasteland.json");
        world.put("nativeProvider", "echoworldcore");
        world.put("spawnStructureId", "echoashfallprotocol:drop_pod");
        world.put("hazardId", CRASH_HAZARD_ID);
        world.put("hazardProvider", "echoworldcore");
        world.put("biomeId", "echoashfallprotocol:crash_zone_wasteland");
        return world;
    }

    private static Map<String, Object> progressionHook(List<Map<String, Object>> routeMissions) {
        Map<String, Object> progression = new LinkedHashMap<>();
        progression.put("id", "ashfall.opening_route.phase_0");
        progression.put("nativeProvider", "echoprogressioncore");
        progression.put("routeMissions", routeMissions.stream()
                .map(mission -> mission.get("id"))
                .toList());
        progression.put("unlocks", List.of(
                "ashfall.survival.first_outpost",
                "ashfall.survival.scrap_tooling",
                "ashfall.survival.hydration_buffer",
                "ashfall.survival.emergency_water_loop",
                "ashfall.survival.food_buffer",
                "ashfall.survival.renewable_materials",
                "ashfall.survival.rain_catchment",
                "ashfall.survival.ration_buffer",
                "ashfall.survival.sleep_shelter",
                "ashfall.survival.wasteland_field_kit",
                "ashfall.survival.schematic_authentication",
                "ashfall.machine.hand_recycler.casing_bridge",
                "ashfall.machine.hand_recycler.online",
                "ashfall.machine.micro_generator.components_bridge",
                "ashfall.machine.water_purifier.components_bridge",
                "ashfall.survival.clean_water_reserves_bridge",
                "ashfall.machine.battery_bank.components_bridge",
                "ashfall.machine.scrap_dynamo.components_bridge",
                "ashfall.machine.basic_battery.charge_bridge",
                "ashfall.machine.power_cable.route_bridge",
                "ashfall.machine.reinforced_power_cable.components_bridge",
                "ashfall.machine.energy_meter.components_bridge",
                "ashfall.machine.load_distributor.components_bridge",
                "ashfall.machine.scrap_press.components_bridge",
                "ashfall.machine.overclock_module.components_bridge",
                "ashfall.logistics.item_pipe.components_bridge",
                "ashfall.machine.thermal_burner.components_bridge",
                "ashfall.survival.base_stability_check.bridge",
                "ashfall.survival.hazard_prep.bridge",
                "ashfall.survival.filter_reserve.bridge",
                "ashfall.survival.filter_workbench.components_bridge",
                "ashfall.survival.advanced_filter.components_bridge",
                "ashfall.survival.research_lab.components_bridge",
                "ashfall.research.first_schematic.bridge",
                "ashfall.machine.factory_controller.components_bridge",
                "ashfall.poi.portable_scanner.components_bridge",
                "ashfall.poi.first_ruin_signature.bridge",
                "ashfall.world.crash_route",
                "ashfall.missions.phase_0",
                "ashfall.missions.phase_1"));
        progression.put("completionSignals", List.of(
                FIRST_MISSION_ID + ":completed",
                SECOND_MISSION_ID + ":completed",
                THIRD_MISSION_ID + ":completed",
                FOURTH_MISSION_ID + ":completed",
                FIFTH_MISSION_ID + ":completed",
                SIXTH_MISSION_ID + ":completed",
                SEVENTH_MISSION_ID + ":completed",
                EIGHTH_MISSION_ID + ":completed",
                NINTH_MISSION_ID + ":completed",
                TENTH_MISSION_ID + ":completed",
                ELEVENTH_MISSION_ID + ":completed",
                TWELFTH_MISSION_ID + ":completed",
                THIRTEENTH_MISSION_ID + ":completed",
                FOURTEENTH_MISSION_ID + ":completed",
                FIFTEENTH_MISSION_ID + ":completed",
                SIXTEENTH_MISSION_ID + ":completed",
                SEVENTEENTH_MISSION_ID + ":completed",
                EIGHTEENTH_MISSION_ID + ":completed",
                NINETEENTH_MISSION_ID + ":completed",
                TWENTIETH_MISSION_ID + ":completed",
                TWENTY_FIRST_MISSION_ID + ":completed",
                TWENTY_SECOND_MISSION_ID + ":completed",
                TWENTY_THIRD_MISSION_ID + ":completed",
                TWENTY_FOURTH_MISSION_ID + ":completed",
                TWENTY_FIFTH_MISSION_ID + ":completed",
                TWENTY_SIXTH_MISSION_ID + ":completed",
                TWENTY_SEVENTH_MISSION_ID + ":completed",
                TWENTY_EIGHTH_MISSION_ID + ":completed",
                TWENTY_NINTH_MISSION_ID + ":completed",
                THIRTIETH_MISSION_ID + ":completed",
                THIRTY_FIRST_MISSION_ID + ":completed",
                THIRTY_SECOND_MISSION_ID + ":completed",
                THIRTY_THIRD_MISSION_ID + ":completed",
                THIRTY_FOURTH_MISSION_ID + ":completed",
                THIRTY_FIFTH_MISSION_ID + ":completed",
                THIRTY_SIXTH_MISSION_ID + ":completed"));
        progression.put("requiresLivePlayerState", true);
        progression.put("livePlayerStateBridgeReady", false);
        return progression;
    }

    private static Map<String, Object> firstGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.secure_crash_outpost");
        hook.put("moduleId", MODULE_ID);
        hook.put("event", "player.block_placed");
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", FIRST_MISSION_ID);
        hook.put("objectiveId", FIRST_OBJECTIVE_ID);
        hook.put("target", FIRST_OBJECTIVE_TARGET);
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "First Ashfall outpost mission has a native MissionCore JSON data path and a deterministic AdapterCore hook target.");
        return hook;
    }

    private static Map<String, Object> secondGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.craft_scrap_knife");
        hook.put("moduleId", MODULE_ID);
        hook.put("event", "player.inventory_changed");
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", SECOND_MISSION_ID);
        hook.put("objectiveId", SECOND_OBJECTIVE_ID);
        hook.put("target", SECOND_OBJECTIVE_TARGET);
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Scrap Knife route mission has a native MissionCore JSON data path and deterministic AdapterCore inventory target.");
        return hook;
    }

    private static Map<String, Object> thirdGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.drink_clean_water");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "player.consume_item"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRD_MISSION_ID);
        hook.put("objectiveId", THIRD_OBJECTIVE_ID);
        hook.put("target", THIRD_OBJECTIVE_TARGET);
        hook.put("consumeMarker", THIRD_CONSUME_MARKER);
        hook.put("consumeEventRecordsTarget", THIRD_OBJECTIVE_TARGET);
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Clean Water route mission has native MissionCore JSON data for both inventory and consumed-water confirmation paths.");
        return hook;
    }

    private static Map<String, Object> fourthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.secure_emergency_water_loop");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("ashfall.special_marker", "player.inventory_changed", "player.craft_item"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", FOURTH_MISSION_ID);
        hook.put("objectiveIds", List.of(FOURTH_DIRTY_OBJECTIVE_ID, FOURTH_FILTER_OBJECTIVE_ID));
        hook.put("targets", List.of(FOURTH_DIRTY_OBJECTIVE_TARGET, FOURTH_FILTER_OBJECTIVE_TARGET));
        hook.put("objectiveType", "enter_region");
        hook.put("locationType", "special");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore special-marker objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Emergency Water Loop has native MissionCore JSON data for the canonical route step replacing deprecated dirty-water and filter-water aliases.");
        return hook;
    }

    private static Map<String, Object> fifthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.forage_wasteland_food");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", FIFTH_MISSION_ID);
        hook.put("objectiveId", FIFTH_OBJECTIVE_ID);
        hook.put("target", FIFTH_OBJECTIVE_TARGET);
        hook.put("acceptedItems", List.of("echoashfallprotocol:wild_berry", "echoashfallprotocol:emergency_ration"));
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore inventory-predicate objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Food Buffer has native MissionCore JSON data that preserves legacy runtime OR semantics for Wild Berry or Emergency Ration.");
        return hook;
    }

    private static Map<String, Object> sixthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.plant_mutated_sapling");
        hook.put("moduleId", MODULE_ID);
        hook.put("event", "player.block_placed");
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", SIXTH_MISSION_ID);
        hook.put("objectiveId", SIXTH_OBJECTIVE_ID);
        hook.put("target", SIXTH_OBJECTIVE_TARGET);
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore block-placement objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Mutated Sapling has native MissionCore JSON data and a deterministic AdapterCore place-block target.");
        return hook;
    }

    private static Map<String, Object> seventhGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_rain_collector");
        hook.put("moduleId", MODULE_ID);
        hook.put("event", "player.block_placed");
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", SEVENTH_MISSION_ID);
        hook.put("objectiveId", SEVENTH_OBJECTIVE_ID);
        hook.put("target", SEVENTH_OBJECTIVE_TARGET);
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore block-placement objective hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Rain Collector has native MissionCore JSON data and a deterministic AdapterCore place-block target.");
        return hook;
    }

    private static Map<String, Object> eighthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.stockpile_rations");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", EIGHTH_MISSION_ID);
        hook.put("objectiveId", EIGHTH_OBJECTIVE_ID);
        hook.put("target", EIGHTH_OBJECTIVE_TARGET);
        hook.put("acceptedItemCounts", Map.of(
                "echoashfallprotocol:emergency_ration", 4,
                "echoashfallprotocol:wild_berry", 12));
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore ration-buffer predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Ration Buffer has native MissionCore JSON data preserving legacy runtime OR-count semantics for Emergency Rations or Wild Berries.");
        return hook;
    }

    private static Map<String, Object> ninthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.secure_sleep_shelter");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "player.sleep", "ashfall.special_marker"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", NINTH_MISSION_ID);
        hook.put("objectiveId", NINTH_OBJECTIVE_ID);
        hook.put("target", NINTH_OBJECTIVE_TARGET);
        hook.put("acceptedTargets", List.of("echoashfallprotocol:emergency_bunk", "minecraft:bed", "shelter:slept"));
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore shelter/sleep predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Sleep Shelter has native MissionCore JSON data for Emergency Bunk, vanilla bed, or shelter slept marker confirmation.");
        return hook;
    }

    private static Map<String, Object> tenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.assemble_wasteland_field_kit");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "player.craft_item", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TENTH_MISSION_ID);
        hook.put("objectiveId", TENTH_OBJECTIVE_ID);
        hook.put("target", TENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:bone_knife",
                "echoashfallprotocol:crude_spear",
                "echoashfallprotocol:hide_wrap"));
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore three-tool field-kit predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Wasteland Field Kit has native MissionCore JSON data preserving the Bone Knife, Crude Spear, and Hide Wrap completion contract while replacing deprecated primitive-craft aliases.");
        return hook;
    }

    private static Map<String, Object> eleventhGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.find_schematic_fragment");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "player.terminal_opened", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", ELEVENTH_MISSION_ID);
        hook.put("objectiveId", ELEVENTH_OBJECTIVE_ID);
        hook.put("target", ELEVENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(ELEVENTH_OBJECTIVE_TARGET));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:emergency_ration",
                "echoashfallprotocol:clean_water_bottle"));
        hook.put("machineProgressionBridge", "ashfall.machine.hand_recycler.casing_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore schematic-fragment inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Schematic Fragment authentication has native MissionCore JSON data and bridges the first machine-casing reward into the Hand Recycler route.");
        return hook;
    }

    private static Map<String, Object> twelfthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_hand_recycler");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWELFTH_MISSION_ID);
        hook.put("objectiveId", TWELFTH_OBJECTIVE_ID);
        hook.put("target", TWELFTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire"));
        hook.put("blockRequirements", Map.of(TWELFTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire"));
        hook.put("machineProgressionBridge", "ashfall.machine.hand_recycler.online");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Hand Recycler block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Hand Recycler construction has native MissionCore JSON data and preserves legacy runtime block requirement plus starter machine-loop rewards.");
        return hook;
    }

    private static Map<String, Object> thirteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.make_machine_casing");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTEENTH_MISSION_ID);
        hook.put("objectiveId", THIRTEENTH_OBJECTIVE_ID);
        hook.put("target", THIRTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(THIRTEENTH_OBJECTIVE_TARGET));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:machine_casing"));
        hook.put("machineProgressionBridge", "ashfall.machine.micro_generator.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore machine-casing inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Machine Casing production has native MissionCore JSON data and bridges the first generator component rewards into the machine route.");
        return hook;
    }

    private static Map<String, Object> fourteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_micro_generator");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", FOURTEENTH_MISSION_ID);
        hook.put("objectiveId", FOURTEENTH_OBJECTIVE_ID);
        hook.put("target", FOURTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_metal"));
        hook.put("blockRequirements", Map.of(FOURTEENTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:circuit_board"));
        hook.put("machineProgressionBridge", "ashfall.machine.water_purifier.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Micro Generator block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Micro Generator construction has native MissionCore JSON data and preserves legacy runtime block requirement plus water-purifier bridge rewards.");
        return hook;
    }

    private static Map<String, Object> fifteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_water_purifier");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", FIFTEENTH_MISSION_ID);
        hook.put("objectiveId", FIFTEENTH_OBJECTIVE_ID);
        hook.put("target", FIFTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:circuit_board"));
        hook.put("blockRequirements", Map.of(FIFTEENTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:dirty_water_bottle",
                "minecraft:glass_bottle"));
        hook.put("machineProgressionBridge", "ashfall.survival.clean_water_reserves_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Water Purifier block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Water Purifier construction has native MissionCore JSON data and preserves legacy runtime block requirement plus clean-water-loop starter rewards.");
        return hook;
    }

    private static Map<String, Object> sixteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.stockpile_clean_water");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", SIXTEENTH_MISSION_ID);
        hook.put("objectiveId", SIXTEENTH_OBJECTIVE_ID);
        hook.put("target", SIXTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItemCounts", Map.of(SIXTEENTH_OBJECTIVE_TARGET, 3));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:ash",
                "echoashfallprotocol:bandage"));
        hook.put("machineProgressionBridge", "ashfall.machine.battery_bank.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore clean-water stockpile predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Clean Water stockpiling has native MissionCore JSON data and preserves legacy runtime 3-bottle reserve semantics before battery-bank expansion.");
        return hook;
    }

    private static Map<String, Object> seventeenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_battery_bank");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", SEVENTEENTH_MISSION_ID);
        hook.put("objectiveId", SEVENTEENTH_OBJECTIVE_ID);
        hook.put("target", SEVENTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:circuit_board"));
        hook.put("blockRequirements", Map.of(SEVENTEENTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:scrap_circuit"));
        hook.put("machineProgressionBridge", "ashfall.machine.scrap_dynamo.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Battery Bank block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Battery Bank construction has native MissionCore JSON data and preserves legacy runtime block requirement plus Scrap Dynamo bridge rewards.");
        return hook;
    }

    private static Map<String, Object> eighteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_scrap_dynamo");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", EIGHTEENTH_MISSION_ID);
        hook.put("objectiveId", EIGHTEENTH_OBJECTIVE_ID);
        hook.put("target", EIGHTEENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:energy_cell"));
        hook.put("blockRequirements", Map.of(EIGHTEENTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:scrap_wire"));
        hook.put("machineProgressionBridge", "ashfall.machine.basic_battery.charge_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Scrap Dynamo block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Scrap Dynamo construction has native MissionCore JSON data and preserves legacy runtime block requirement plus charged-battery bridge rewards.");
        return hook;
    }

    private static Map<String, Object> nineteenthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.charge_basic_battery");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.energy_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", NINETEENTH_MISSION_ID);
        hook.put("objectiveId", NINETEENTH_OBJECTIVE_ID);
        hook.put("target", NINETEENTH_OBJECTIVE_TARGET);
        hook.put("chargedItemPredicate", Map.of(
                "item", NINETEENTH_OBJECTIVE_TARGET,
                "minimumEnergy", 1_000));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_wire",
                "minecraft:redstone"));
        hook.put("machineProgressionBridge", "ashfall.machine.power_cable.route_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore charged-battery predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Basic Battery charging has native MissionCore JSON data and preserves legacy runtime BatteryItem >= 1000 FE semantics before cable routing.");
        return hook;
    }

    private static Map<String, Object> twentiethGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.route_power_cable");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTIETH_MISSION_ID);
        hook.put("objectiveId", TWENTIETH_OBJECTIVE_ID);
        hook.put("target", TWENTIETH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "minecraft:copper_ingot",
                "echoashfallprotocol:circuit_board",
                "minecraft:redstone"));
        hook.put("blockRequirements", Map.of(TWENTIETH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:circuit_board",
                "minecraft:redstone"));
        hook.put("machineProgressionBridge", "ashfall.machine.reinforced_power_cable.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Power Cable block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Power Cable routing has native MissionCore JSON data and preserves the legacy runtime one-block cable placement requirement before reinforced cable progression.");
        return hook;
    }

    private static Map<String, Object> twentyFirstGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.upgrade_power_cable");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_FIRST_MISSION_ID);
        hook.put("objectiveId", TWENTY_FIRST_OBJECTIVE_ID);
        hook.put("target", TWENTY_FIRST_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:power_cable",
                "echoashfallprotocol:scrap_wire"));
        hook.put("blockRequirements", Map.of(TWENTY_FIRST_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_wire",
                "minecraft:redstone"));
        hook.put("machineProgressionBridge", "ashfall.machine.energy_meter.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Reinforced Power Cable block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Reinforced Power Cable placement has native MissionCore JSON data and preserves the legacy runtime upgrade requirement before energy meter progression.");
        return hook;
    }

    private static Map<String, Object> twentySecondGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.install_energy_meter");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_SECOND_MISSION_ID);
        hook.put("objectiveId", TWENTY_SECOND_OBJECTIVE_ID);
        hook.put("target", TWENTY_SECOND_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:circuit_board",
                "minecraft:glass_pane",
                "minecraft:redstone"));
        hook.put("blockRequirements", Map.of(TWENTY_SECOND_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of("echoashfallprotocol:circuit_board"));
        hook.put("machineProgressionBridge", "ashfall.machine.load_distributor.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Energy Meter block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Energy Meter installation has native MissionCore JSON data and preserves the legacy runtime diagnostics block requirement before priority routing.");
        return hook;
    }

    private static Map<String, Object> twentyThirdGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.set_power_priority");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "player.use_block", "ashfall.block_requirement", "ashfall.special_marker"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_THIRD_MISSION_ID);
        hook.put("objectiveIds", List.of(TWENTY_THIRD_BLOCK_OBJECTIVE_ID, TWENTY_THIRD_MARKER_OBJECTIVE_ID));
        hook.put("target", TWENTY_THIRD_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:battery_bank",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_wire"));
        hook.put("blockRequirements", Map.of(TWENTY_THIRD_OBJECTIVE_TARGET, 1));
        hook.put("specialMarkers", List.of(TWENTY_THIRD_PRIORITY_MARKER));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:scrap_wire"));
        hook.put("machineProgressionBridge", "ashfall.machine.scrap_press.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Load Distributor placement and priority marker hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Power Priority setup has native MissionCore JSON data and preserves the legacy runtime Load Distributor plus power:priority_set marker requirement.");
        return hook;
    }

    private static Map<String, Object> twentyFourthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_scrap_press");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_FOURTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_FOURTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_FOURTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:circuit_board"));
        hook.put("blockRequirements", Map.of(TWENTY_FOURTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_plastic"));
        hook.put("machineProgressionBridge", "ashfall.machine.overclock_module.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Scrap Press block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Scrap Press construction has native MissionCore JSON data and preserves the legacy runtime powered-machine placement requirement before overclock progression.");
        return hook;
    }

    private static Map<String, Object> twentyFifthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.overclock_machine");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_FIFTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_FIFTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_FIFTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(TWENTY_FIFTH_OBJECTIVE_TARGET));
        hook.put("requiredItemCounts", Map.of(TWENTY_FIFTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:energy_cell",
                "minecraft:redstone"));
        hook.put("machineProgressionBridge", "ashfall.logistics.item_pipe.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Overclock Module inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Overclock Module crafting has native MissionCore JSON data and preserves the legacy runtime inventory predicate before item-pipe progression.");
        return hook;
    }

    private static Map<String, Object> twentySixthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.install_item_pipe");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_SIXTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_SIXTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_SIXTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "minecraft:iron_ingot",
                "echoashfallprotocol:circuit_board"));
        hook.put("blockRequirements", Map.of(TWENTY_SIXTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:energy_cell"));
        hook.put("machineProgressionBridge", "ashfall.machine.thermal_burner.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Item Pipe block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Item Pipe installation has native MissionCore JSON data and preserves the legacy runtime logistics block placement requirement before thermal burner progression.");
        return hook;
    }

    private static Map<String, Object> twentySeventhGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_thermal_burner");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_SEVENTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_SEVENTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_SEVENTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:ash"));
        hook.put("blockRequirements", Map.of(TWENTY_SEVENTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of("echoashfallprotocol:filtration_membrane"));
        hook.put("machineProgressionBridge", "ashfall.survival.base_stability_check.bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Thermal Burner block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Thermal Burner construction has native MissionCore JSON data and preserves the legacy runtime heat-machine placement requirement before base-stability progression.");
        return hook;
    }

    private static Map<String, Object> twentyEighthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.base_stability_check");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("ashfall.block_requirement", "ashfall.inventory_predicate", "ashfall.mission_completed"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_EIGHTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_EIGHTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_EIGHTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:clean_water_bottle",
                "echoashfallprotocol:bandage",
                "echoashfallprotocol:energy_cell"));
        hook.put("requiredItemCounts", Map.of(
                "echoashfallprotocol:clean_water_bottle", 3,
                "echoashfallprotocol:bandage", 2,
                "echoashfallprotocol:energy_cell", 1));
        hook.put("blockRequirements", Map.of(
                "minecraft:chest", 1,
                "echoashfallprotocol:water_purifier", 1,
                "echoashfallprotocol:battery_bank", 1));
        hook.put("alternateCompletionSignals", List.of("echoashfallprotocol:first_faction_contact:completed"));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:clean_water_bottle",
                "echoashfallprotocol:bandage"));
        hook.put("machineProgressionBridge", "ashfall.survival.hazard_prep.bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Base Stability composite predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Base Stability Check has native MissionCore JSON data for the legacy runtime storage, purifier, battery, medicine, water, and power-buffer predicate.");
        return hook;
    }

    private static Map<String, Object> twentyNinthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.equip_gas_mask");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.equipment_changed", "ashfall.equipment_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", TWENTY_NINTH_MISSION_ID);
        hook.put("objectiveId", TWENTY_NINTH_OBJECTIVE_ID);
        hook.put("target", TWENTY_NINTH_OBJECTIVE_TARGET);
        hook.put("equipmentSlot", "head");
        hook.put("requiredEquipment", Map.of("head", TWENTY_NINTH_OBJECTIVE_TARGET));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:bandage"));
        hook.put("machineProgressionBridge", "ashfall.survival.filter_reserve.bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Gas Mask equipment predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Gas Mask equipment has native MissionCore JSON data and preserves the legacy runtime head-slot predicate before filter reserve progression.");
        return hook;
    }

    private static Map<String, Object> thirtiethGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.fix_mask_filter");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTIETH_MISSION_ID);
        hook.put("objectiveId", THIRTIETH_OBJECTIVE_ID);
        hook.put("target", THIRTIETH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(THIRTIETH_OBJECTIVE_TARGET));
        hook.put("requiredItemCounts", Map.of(THIRTIETH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:clean_water_bottle"));
        hook.put("machineProgressionBridge", "ashfall.survival.filter_workbench.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Basic Filter Cartridge inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Basic Filter Cartridge reserve has native MissionCore JSON data and preserves the legacy runtime inventory predicate before Filter Workbench progression.");
        return hook;
    }

    private static Map<String, Object> thirtyFirstGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_filter_workbench");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_FIRST_MISSION_ID);
        hook.put("objectiveId", THIRTY_FIRST_OBJECTIVE_ID);
        hook.put("target", THIRTY_FIRST_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:filtration_membrane",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_wire"));
        hook.put("blockRequirements", Map.of(THIRTY_FIRST_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of("echoashfallprotocol:filter_cartridge_basic"));
        hook.put("rewardItemCounts", Map.of("echoashfallprotocol:filter_cartridge_basic", 2));
        hook.put("machineProgressionBridge", "ashfall.survival.advanced_filter.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Filter Workbench block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Filter Workbench construction has native MissionCore JSON data and preserves the legacy runtime block-placement predicate before advanced filter progression.");
        return hook;
    }

    private static Map<String, Object> thirtySecondGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.craft_advanced_filter");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_SECOND_MISSION_ID);
        hook.put("objectiveId", THIRTY_SECOND_OBJECTIVE_ID);
        hook.put("target", THIRTY_SECOND_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(THIRTY_SECOND_OBJECTIVE_TARGET));
        hook.put("requiredItemCounts", Map.of(THIRTY_SECOND_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:clean_water_bottle",
                "echoashfallprotocol:filtration_membrane"));
        hook.put("machineProgressionBridge", "ashfall.survival.research_lab.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Advanced Filter Cartridge inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Advanced Filter Cartridge crafting has native MissionCore JSON data and preserves the legacy runtime inventory predicate before research-lab progression.");
        return hook;
    }

    private static Map<String, Object> thirtyThirdGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_research_lab");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_THIRD_MISSION_ID);
        hook.put("objectiveId", THIRTY_THIRD_OBJECTIVE_ID);
        hook.put("target", THIRTY_THIRD_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:machine_casing"));
        hook.put("blockRequirements", Map.of(THIRTY_THIRD_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of("echoashfallprotocol:schematic_fragment"));
        hook.put("machineProgressionBridge", "ashfall.research.first_schematic.bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Research Lab block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Research Lab construction has native MissionCore JSON data and preserves the route override from Advanced Filter into research progression.");
        return hook;
    }

    private static Map<String, Object> thirtyFourthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.first_schematic");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("ashfall.research_updated", "ashfall.schematic_unlocked"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_FOURTH_MISSION_ID);
        hook.put("objectiveId", THIRTY_FOURTH_OBJECTIVE_ID);
        hook.put("target", THIRTY_FOURTH_OBJECTIVE_TARGET);
        hook.put("requiredResearch", Map.of("unlockedSchematicsAtLeast", 1));
        hook.put("rewardItems", List.of("echoashfallprotocol:scrap_circuit"));
        hook.put("rewardItemCounts", Map.of("echoashfallprotocol:scrap_circuit", 6));
        hook.put("machineProgressionBridge", "ashfall.machine.factory_controller.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore First Schematic research-predicate hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "First Schematic unlock has native MissionCore JSON data and preserves the research predicate before factory-controller progression.");
        return hook;
    }

    private static Map<String, Object> thirtyFifthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.build_factory_controller");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.block_placed", "ashfall.block_requirement"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_FIFTH_MISSION_ID);
        hook.put("objectiveId", THIRTY_FIFTH_OBJECTIVE_ID);
        hook.put("target", THIRTY_FIFTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:machine_casing",
                "echoashfallprotocol:energy_cell",
                "minecraft:redstone_block"));
        hook.put("requiredItemCounts", Map.of(
                "echoashfallprotocol:circuit_board", 5,
                "echoashfallprotocol:machine_casing", 2,
                "echoashfallprotocol:energy_cell", 1,
                "minecraft:redstone_block", 1));
        hook.put("blockRequirements", Map.of(THIRTY_FIFTH_OBJECTIVE_TARGET, 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:energy_cell",
                "minecraft:redstone"));
        hook.put("rewardItemCounts", Map.of(
                "echoashfallprotocol:circuit_board", 2,
                "echoashfallprotocol:energy_cell", 1,
                "minecraft:redstone", 6));
        hook.put("machineProgressionBridge", "ashfall.poi.portable_scanner.components_bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Factory Controller block-placement hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Factory Controller construction has native MissionCore JSON data and preserves the legacy runtime block requirement before scanner-led POI progression.");
        return hook;
    }

    private static Map<String, Object> thirtySixthGameplayHook() {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.first_route.craft_portable_scanner");
        hook.put("moduleId", MODULE_ID);
        hook.put("events", List.of("player.craft_item", "player.inventory_changed", "ashfall.inventory_predicate"));
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", THIRTY_SIXTH_MISSION_ID);
        hook.put("objectiveId", THIRTY_SIXTH_OBJECTIVE_ID);
        hook.put("target", THIRTY_SIXTH_OBJECTIVE_TARGET);
        hook.put("requiredItems", List.of(THIRTY_SIXTH_OBJECTIVE_TARGET));
        hook.put("requiredItemCounts", Map.of(THIRTY_SIXTH_OBJECTIVE_TARGET, 1));
        hook.put("recipeIngredients", Map.of(
                "echoashfallprotocol:scrap_circuit", 2,
                "echoashfallprotocol:scrap_wire", 1,
                "echoashfallprotocol:scrap_metal", 2,
                "echoashfallprotocol:circuit_board", 1,
                "echoashfallprotocol:energy_cell", 1));
        hook.put("rewardItems", List.of(
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:scrap_circuit"));
        hook.put("rewardItemCounts", Map.of(
                "echoashfallprotocol:circuit_board", 2,
                "echoashfallprotocol:scrap_circuit", 4));
        hook.put("explorationProgressionBridge", "ashfall.poi.first_ruin_signature.bridge");
        hook.put("worldRegion", CRASH_REGION_ID);
        hook.put("hazardContext", CRASH_HAZARD_ID);
        hook.put("verificationMode", "data_path_and_contract");
        hook.put("adapterCoreBridge", true);
        hook.put("implementationTarget", "MissionCore Portable Signal Scanner inventory hook through AdapterCore native event/service bridges");
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("liveGameplayHookVerified", false);
        hook.put("gameplayHookEvidence", true);
        hook.put("summary", "Portable Signal Scanner crafting has native MissionCore JSON data and preserves the legacy runtime pickup predicate before scanner-led POI routing.");
        return hook;
    }

    private static List<String> validate(
            Map<String, Object> firstMission,
            Map<String, Object> secondMission,
            Map<String, Object> thirdMission,
            Map<String, Object> fourthMission,
            Map<String, Object> fifthMission,
            Map<String, Object> sixthMission,
            Map<String, Object> seventhMission,
            Map<String, Object> eighthMission,
            Map<String, Object> ninthMission,
            Map<String, Object> tenthMission,
            Map<String, Object> eleventhMission,
            Map<String, Object> twelfthMission,
            Map<String, Object> thirteenthMission,
            Map<String, Object> fourteenthMission,
            Map<String, Object> fifteenthMission,
            Map<String, Object> sixteenthMission,
            Map<String, Object> seventeenthMission,
            Map<String, Object> eighteenthMission,
            Map<String, Object> nineteenthMission,
            Map<String, Object> twentiethMission,
            Map<String, Object> twentyFirstMission,
            Map<String, Object> twentySecondMission,
            Map<String, Object> twentyThirdMission,
            Map<String, Object> twentyFourthMission,
            Map<String, Object> twentyFifthMission,
            Map<String, Object> twentySixthMission,
            Map<String, Object> twentySeventhMission,
            Map<String, Object> twentyEighthMission,
            Map<String, Object> twentyNinthMission,
            Map<String, Object> thirtiethMission,
            Map<String, Object> thirtyFirstMission,
            Map<String, Object> thirtySecondMission,
            Map<String, Object> thirtyThirdMission,
            Map<String, Object> thirtyFourthMission,
            Map<String, Object> thirtyFifthMission,
            Map<String, Object> thirtySixthMission,
            Map<String, Object> worldAnchor,
            Map<String, Object> progressionHook) {
        List<String> diagnostics = new ArrayList<>();
        require(firstMission, "id", FIRST_MISSION_ID, diagnostics);
        require(firstMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(firstMission, "objectiveTarget", FIRST_OBJECTIVE_TARGET, diagnostics);
        require(firstMission, "nativeProvider", "echomissioncore", diagnostics);
        require(secondMission, "id", SECOND_MISSION_ID, diagnostics);
        require(secondMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(secondMission, "prerequisite", FIRST_MISSION_ID, diagnostics);
        require(secondMission, "objectiveTarget", SECOND_OBJECTIVE_TARGET, diagnostics);
        require(secondMission, "nativeProvider", "echomissioncore", diagnostics);
        require(thirdMission, "id", THIRD_MISSION_ID, diagnostics);
        require(thirdMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirdMission, "prerequisite", SECOND_MISSION_ID, diagnostics);
        require(thirdMission, "objectiveTarget", THIRD_OBJECTIVE_TARGET, diagnostics);
        require(thirdMission, "consumeMarker", THIRD_CONSUME_MARKER, diagnostics);
        require(thirdMission, "consumeEventRecordsTarget", THIRD_OBJECTIVE_TARGET, diagnostics);
        require(thirdMission, "nativeProvider", "echomissioncore", diagnostics);
        require(fourthMission, "id", FOURTH_MISSION_ID, diagnostics);
        require(fourthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(fourthMission, "prerequisite", THIRD_MISSION_ID, diagnostics);
        require(fourthMission, "objectiveType", "enter_region", diagnostics);
        require(fourthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object fourthTargets = fourthMission.get("objectiveTargets");
        if (!(fourthTargets instanceof List<?> list)
                || !list.contains(FOURTH_DIRTY_OBJECTIVE_TARGET)
                || !list.contains(FOURTH_FILTER_OBJECTIVE_TARGET)) {
            diagnostics.add("Expected Emergency Water Loop targets for dirty-water collection and emergency filtration.");
        }
        require(fifthMission, "id", FIFTH_MISSION_ID, diagnostics);
        require(fifthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(fifthMission, "prerequisite", FOURTH_MISSION_ID, diagnostics);
        require(fifthMission, "objectiveType", "custom", diagnostics);
        require(fifthMission, "objectiveTarget", FIFTH_OBJECTIVE_TARGET, diagnostics);
        require(fifthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object fifthAcceptedItems = fifthMission.get("acceptedItems");
        if (!(fifthAcceptedItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:wild_berry")
                || !list.contains("echoashfallprotocol:emergency_ration")) {
            diagnostics.add("Expected Food Buffer accepted items for Wild Berry and Emergency Ration.");
        }
        require(sixthMission, "id", SIXTH_MISSION_ID, diagnostics);
        require(sixthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(sixthMission, "prerequisite", FIFTH_MISSION_ID, diagnostics);
        require(sixthMission, "objectiveType", "place_block", diagnostics);
        require(sixthMission, "objectiveTarget", SIXTH_OBJECTIVE_TARGET, diagnostics);
        require(sixthMission, "nativeProvider", "echomissioncore", diagnostics);
        require(seventhMission, "id", SEVENTH_MISSION_ID, diagnostics);
        require(seventhMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(seventhMission, "prerequisite", SIXTH_MISSION_ID, diagnostics);
        require(seventhMission, "objectiveType", "place_block", diagnostics);
        require(seventhMission, "objectiveTarget", SEVENTH_OBJECTIVE_TARGET, diagnostics);
        require(seventhMission, "nativeProvider", "echomissioncore", diagnostics);
        require(eighthMission, "id", EIGHTH_MISSION_ID, diagnostics);
        require(eighthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(eighthMission, "prerequisite", SEVENTH_MISSION_ID, diagnostics);
        require(eighthMission, "objectiveType", "custom", diagnostics);
        require(eighthMission, "objectiveTarget", EIGHTH_OBJECTIVE_TARGET, diagnostics);
        require(eighthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object rationCounts = eighthMission.get("acceptedItemCounts");
        if (!(rationCounts instanceof Map<?, ?> map)
                || !Integer.valueOf(4).equals(map.get("echoashfallprotocol:emergency_ration"))
                || !Integer.valueOf(12).equals(map.get("echoashfallprotocol:wild_berry"))) {
            diagnostics.add("Expected Ration Buffer accepted item counts for 4 Emergency Rations or 12 Wild Berries.");
        }
        require(ninthMission, "id", NINTH_MISSION_ID, diagnostics);
        require(ninthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(ninthMission, "prerequisite", EIGHTH_MISSION_ID, diagnostics);
        require(ninthMission, "objectiveType", "custom", diagnostics);
        require(ninthMission, "objectiveTarget", NINTH_OBJECTIVE_TARGET, diagnostics);
        require(ninthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object shelterTargets = ninthMission.get("acceptedTargets");
        if (!(shelterTargets instanceof List<?> list)
                || !list.contains("echoashfallprotocol:emergency_bunk")
                || !list.contains("minecraft:bed")
                || !list.contains("shelter:slept")) {
            diagnostics.add("Expected Sleep Shelter targets for Emergency Bunk, vanilla bed, or shelter slept marker.");
        }
        require(tenthMission, "id", TENTH_MISSION_ID, diagnostics);
        require(tenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(tenthMission, "prerequisite", NINTH_MISSION_ID, diagnostics);
        require(tenthMission, "objectiveType", "custom", diagnostics);
        require(tenthMission, "objectiveTarget", TENTH_OBJECTIVE_TARGET, diagnostics);
        require(tenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object requiredItems = tenthMission.get("requiredItems");
        if (!(requiredItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:bone_knife")
                || !list.contains("echoashfallprotocol:crude_spear")
                || !list.contains("echoashfallprotocol:hide_wrap")) {
            diagnostics.add("Expected Wasteland Field Kit requirements for Bone Knife, Crude Spear, and Hide Wrap.");
        }
        require(eleventhMission, "id", ELEVENTH_MISSION_ID, diagnostics);
        require(eleventhMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(eleventhMission, "prerequisite", TENTH_MISSION_ID, diagnostics);
        require(eleventhMission, "objectiveType", "obtain_item", diagnostics);
        require(eleventhMission, "objectiveTarget", ELEVENTH_OBJECTIVE_TARGET, diagnostics);
        require(eleventhMission, "nativeProvider", "echomissioncore", diagnostics);
        Object schematicItems = eleventhMission.get("requiredItems");
        if (!(schematicItems instanceof List<?> list)
                || !list.contains(ELEVENTH_OBJECTIVE_TARGET)) {
            diagnostics.add("Expected Schematic Fragment authentication to require echoashfallprotocol:schematic_fragment.");
        }
        require(twelfthMission, "id", TWELFTH_MISSION_ID, diagnostics);
        require(twelfthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twelfthMission, "prerequisite", ELEVENTH_MISSION_ID, diagnostics);
        require(twelfthMission, "objectiveType", "place_block", diagnostics);
        require(twelfthMission, "objectiveTarget", TWELFTH_OBJECTIVE_TARGET, diagnostics);
        require(twelfthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object recyclerItems = twelfthMission.get("requiredItems");
        if (!(recyclerItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:machine_casing")
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:scrap_wire")) {
            diagnostics.add("Expected Hand Recycler requirements for Machine Casing, Scrap Metal, and Scrap Wire.");
        }
        Object recyclerBlocks = twelfthMission.get("blockRequirements");
        if (!(recyclerBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWELFTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Hand Recycler block requirement for one echoashfallprotocol:hand_recycler.");
        }
        require(twelfthMission, "starterBatteryItem", "echoashfallprotocol:basic_battery", diagnostics);
        if (!Integer.valueOf(1000).equals(twelfthMission.get("starterBatteryEnergy"))) {
            diagnostics.add("Expected Hand Recycler starter battery bridge to provide 1000 FE.");
        }
        require(thirteenthMission, "id", THIRTEENTH_MISSION_ID, diagnostics);
        require(thirteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirteenthMission, "prerequisite", TWELFTH_MISSION_ID, diagnostics);
        require(thirteenthMission, "objectiveType", "obtain_item", diagnostics);
        require(thirteenthMission, "objectiveTarget", THIRTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(thirteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object casingItems = thirteenthMission.get("requiredItems");
        if (!(casingItems instanceof List<?> list)
                || !list.contains(THIRTEENTH_OBJECTIVE_TARGET)) {
            diagnostics.add("Expected Machine Casing mission to require echoashfallprotocol:machine_casing.");
        }
        require(fourteenthMission, "id", FOURTEENTH_MISSION_ID, diagnostics);
        require(fourteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(fourteenthMission, "prerequisite", THIRTEENTH_MISSION_ID, diagnostics);
        require(fourteenthMission, "objectiveType", "place_block", diagnostics);
        require(fourteenthMission, "objectiveTarget", FOURTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(fourteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object generatorItems = fourteenthMission.get("requiredItems");
        if (!(generatorItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:machine_casing")
                || !list.contains("echoashfallprotocol:scrap_wire")
                || !list.contains("echoashfallprotocol:energy_cell")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("echoashfallprotocol:scrap_metal")) {
            diagnostics.add("Expected Micro Generator requirements for casing, wire, energy cell, circuit board, and scrap metal.");
        }
        Object generatorBlocks = fourteenthMission.get("blockRequirements");
        if (!(generatorBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(FOURTEENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Micro Generator block requirement for one echoashfallprotocol:micro_generator.");
        }
        require(fifteenthMission, "id", FIFTEENTH_MISSION_ID, diagnostics);
        require(fifteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(fifteenthMission, "prerequisite", FOURTEENTH_MISSION_ID, diagnostics);
        require(fifteenthMission, "objectiveType", "place_block", diagnostics);
        require(fifteenthMission, "objectiveTarget", FIFTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(fifteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object purifierItems = fifteenthMission.get("requiredItems");
        if (!(purifierItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_plastic")
                || !list.contains("echoashfallprotocol:filtration_membrane")
                || !list.contains("echoashfallprotocol:machine_casing")
                || !list.contains("echoashfallprotocol:circuit_board")) {
            diagnostics.add("Expected Water Purifier requirements for plastic, membrane, casings, and circuit board.");
        }
        Object purifierBlocks = fifteenthMission.get("blockRequirements");
        if (!(purifierBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(FIFTEENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Water Purifier block requirement for one echoashfallprotocol:water_purifier.");
        }
        require(sixteenthMission, "id", SIXTEENTH_MISSION_ID, diagnostics);
        require(sixteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(sixteenthMission, "prerequisite", FIFTEENTH_MISSION_ID, diagnostics);
        require(sixteenthMission, "objectiveType", "custom", diagnostics);
        require(sixteenthMission, "objectiveTarget", SIXTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(sixteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object cleanWaterCounts = sixteenthMission.get("requiredItemCounts");
        if (!(cleanWaterCounts instanceof Map<?, ?> map)
                || !Integer.valueOf(3).equals(map.get(SIXTEENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Clean Water stockpile requirement for 3 echoashfallprotocol:clean_water_bottle.");
        }
        require(seventeenthMission, "id", SEVENTEENTH_MISSION_ID, diagnostics);
        require(seventeenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(seventeenthMission, "prerequisite", SIXTEENTH_MISSION_ID, diagnostics);
        require(seventeenthMission, "objectiveType", "place_block", diagnostics);
        require(seventeenthMission, "objectiveTarget", SEVENTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(seventeenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object batteryItems = seventeenthMission.get("requiredItems");
        if (!(batteryItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:energy_cell")
                || !list.contains("echoashfallprotocol:circuit_board")) {
            diagnostics.add("Expected Battery Bank requirements for scrap metal, energy cells, and circuit board.");
        }
        Object batteryBlocks = seventeenthMission.get("blockRequirements");
        if (!(batteryBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(SEVENTEENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Battery Bank block requirement for one echoashfallprotocol:battery_bank.");
        }
        require(eighteenthMission, "id", EIGHTEENTH_MISSION_ID, diagnostics);
        require(eighteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(eighteenthMission, "prerequisite", SEVENTEENTH_MISSION_ID, diagnostics);
        require(eighteenthMission, "objectiveType", "place_block", diagnostics);
        require(eighteenthMission, "objectiveTarget", EIGHTEENTH_OBJECTIVE_TARGET, diagnostics);
        require(eighteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object dynamoItems = eighteenthMission.get("requiredItems");
        if (!(dynamoItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:scrap_wire")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("echoashfallprotocol:energy_cell")) {
            diagnostics.add("Expected Scrap Dynamo requirements for scrap metal, wire, circuit board, and energy cell.");
        }
        Object dynamoBlocks = eighteenthMission.get("blockRequirements");
        if (!(dynamoBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(EIGHTEENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Scrap Dynamo block requirement for one echoashfallprotocol:scrap_dynamo.");
        }
        require(nineteenthMission, "id", NINETEENTH_MISSION_ID, diagnostics);
        require(nineteenthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(nineteenthMission, "prerequisite", EIGHTEENTH_MISSION_ID, diagnostics);
        require(nineteenthMission, "objectiveType", "custom", diagnostics);
        require(nineteenthMission, "objectiveTarget", NINETEENTH_OBJECTIVE_TARGET, diagnostics);
        require(nineteenthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object batteryPredicate = nineteenthMission.get("chargedItemPredicate");
        if (!(batteryPredicate instanceof Map<?, ?> map)
                || !NINETEENTH_OBJECTIVE_TARGET.equals(map.get("item"))
                || !Integer.valueOf(1_000).equals(map.get("minimumEnergy"))) {
            diagnostics.add("Expected Basic Battery charge predicate for echoashfallprotocol:basic_battery with at least 1000 FE.");
        }
        require(twentiethMission, "id", TWENTIETH_MISSION_ID, diagnostics);
        require(twentiethMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentiethMission, "prerequisite", NINETEENTH_MISSION_ID, diagnostics);
        require(twentiethMission, "objectiveType", "place_block", diagnostics);
        require(twentiethMission, "objectiveTarget", TWENTIETH_OBJECTIVE_TARGET, diagnostics);
        require(twentiethMission, "nativeProvider", "echomissioncore", diagnostics);
        Object powerCableItems = twentiethMission.get("requiredItems");
        if (!(powerCableItems instanceof List<?> list)
                || !list.contains("minecraft:copper_ingot")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("minecraft:redstone")) {
            diagnostics.add("Expected Power Cable requirements for copper ingots, circuit boards, and redstone.");
        }
        Object powerCableBlocks = twentiethMission.get("blockRequirements");
        if (!(powerCableBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTIETH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Power Cable block requirement for one echoashfallprotocol:power_cable.");
        }
        require(twentyFirstMission, "id", TWENTY_FIRST_MISSION_ID, diagnostics);
        require(twentyFirstMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyFirstMission, "prerequisite", TWENTIETH_MISSION_ID, diagnostics);
        require(twentyFirstMission, "objectiveType", "place_block", diagnostics);
        require(twentyFirstMission, "objectiveTarget", TWENTY_FIRST_OBJECTIVE_TARGET, diagnostics);
        require(twentyFirstMission, "nativeProvider", "echomissioncore", diagnostics);
        Object reinforcedCableItems = twentyFirstMission.get("requiredItems");
        if (!(reinforcedCableItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:power_cable")
                || !list.contains("echoashfallprotocol:scrap_wire")) {
            diagnostics.add("Expected Reinforced Power Cable requirements for Power Cable and Scrap Wire.");
        }
        Object reinforcedCableBlocks = twentyFirstMission.get("blockRequirements");
        if (!(reinforcedCableBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_FIRST_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Reinforced Power Cable block requirement for one echoashfallprotocol:reinforced_power_cable.");
        }
        require(twentySecondMission, "id", TWENTY_SECOND_MISSION_ID, diagnostics);
        require(twentySecondMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentySecondMission, "prerequisite", TWENTY_FIRST_MISSION_ID, diagnostics);
        require(twentySecondMission, "objectiveType", "place_block", diagnostics);
        require(twentySecondMission, "objectiveTarget", TWENTY_SECOND_OBJECTIVE_TARGET, diagnostics);
        require(twentySecondMission, "nativeProvider", "echomissioncore", diagnostics);
        Object energyMeterItems = twentySecondMission.get("requiredItems");
        if (!(energyMeterItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("minecraft:glass_pane")
                || !list.contains("minecraft:redstone")) {
            diagnostics.add("Expected Energy Meter requirements for circuit boards, glass pane, and redstone.");
        }
        Object energyMeterBlocks = twentySecondMission.get("blockRequirements");
        if (!(energyMeterBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_SECOND_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Energy Meter block requirement for one echoashfallprotocol:energy_meter.");
        }
        require(twentyThirdMission, "id", TWENTY_THIRD_MISSION_ID, diagnostics);
        require(twentyThirdMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyThirdMission, "prerequisite", TWENTY_SECOND_MISSION_ID, diagnostics);
        require(twentyThirdMission, "objectiveType", "composite", diagnostics);
        require(twentyThirdMission, "objectiveTarget", TWENTY_THIRD_OBJECTIVE_TARGET, diagnostics);
        require(twentyThirdMission, "nativeProvider", "echomissioncore", diagnostics);
        Object priorityItems = twentyThirdMission.get("requiredItems");
        if (!(priorityItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:battery_bank")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("echoashfallprotocol:scrap_wire")) {
            diagnostics.add("Expected Load Distributor priority requirements for Battery Bank, Circuit Board, and Scrap Wire.");
        }
        Object priorityBlocks = twentyThirdMission.get("blockRequirements");
        if (!(priorityBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_THIRD_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Load Distributor block requirement for one echoashfallprotocol:load_distributor.");
        }
        Object priorityMarkers = twentyThirdMission.get("specialMarkers");
        if (!(priorityMarkers instanceof List<?> list)
                || !list.contains(TWENTY_THIRD_PRIORITY_MARKER)) {
            diagnostics.add("Expected Load Distributor priority marker power:priority_set.");
        }
        require(twentyFourthMission, "id", TWENTY_FOURTH_MISSION_ID, diagnostics);
        require(twentyFourthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyFourthMission, "prerequisite", TWENTY_THIRD_MISSION_ID, diagnostics);
        require(twentyFourthMission, "objectiveType", "place_block", diagnostics);
        require(twentyFourthMission, "objectiveTarget", TWENTY_FOURTH_OBJECTIVE_TARGET, diagnostics);
        require(twentyFourthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object scrapPressItems = twentyFourthMission.get("requiredItems");
        if (!(scrapPressItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:scrap_plastic")
                || !list.contains("echoashfallprotocol:circuit_board")) {
            diagnostics.add("Expected Scrap Press requirements for Scrap Metal, Scrap Plastic, and Circuit Board.");
        }
        Object scrapPressBlocks = twentyFourthMission.get("blockRequirements");
        if (!(scrapPressBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_FOURTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Scrap Press block requirement for one echoashfallprotocol:scrap_press.");
        }
        require(twentyFifthMission, "id", TWENTY_FIFTH_MISSION_ID, diagnostics);
        require(twentyFifthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyFifthMission, "prerequisite", TWENTY_FOURTH_MISSION_ID, diagnostics);
        require(twentyFifthMission, "objectiveType", "obtain_item", diagnostics);
        require(twentyFifthMission, "objectiveTarget", TWENTY_FIFTH_OBJECTIVE_TARGET, diagnostics);
        require(twentyFifthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object overclockItems = twentyFifthMission.get("requiredItemCounts");
        if (!(overclockItems instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_FIFTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Overclock Module inventory requirement for one echoashfallprotocol:machine_upgrade_overclock.");
        }
        require(twentySixthMission, "id", TWENTY_SIXTH_MISSION_ID, diagnostics);
        require(twentySixthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentySixthMission, "prerequisite", TWENTY_FIFTH_MISSION_ID, diagnostics);
        require(twentySixthMission, "objectiveType", "place_block", diagnostics);
        require(twentySixthMission, "objectiveTarget", TWENTY_SIXTH_OBJECTIVE_TARGET, diagnostics);
        require(twentySixthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object itemPipeItems = twentySixthMission.get("requiredItems");
        if (!(itemPipeItems instanceof List<?> list)
                || !list.contains("minecraft:iron_ingot")
                || !list.contains("echoashfallprotocol:circuit_board")) {
            diagnostics.add("Expected Item Pipe requirements for Iron Ingots and Circuit Boards.");
        }
        Object itemPipeBlocks = twentySixthMission.get("blockRequirements");
        if (!(itemPipeBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_SIXTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Item Pipe block requirement for one echoashfallprotocol:item_pipe.");
        }
        require(twentySeventhMission, "id", TWENTY_SEVENTH_MISSION_ID, diagnostics);
        require(twentySeventhMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentySeventhMission, "prerequisite", TWENTY_SIXTH_MISSION_ID, diagnostics);
        require(twentySeventhMission, "objectiveType", "place_block", diagnostics);
        require(twentySeventhMission, "objectiveTarget", TWENTY_SEVENTH_OBJECTIVE_TARGET, diagnostics);
        require(twentySeventhMission, "nativeProvider", "echomissioncore", diagnostics);
        Object thermalBurnerItems = twentySeventhMission.get("requiredItems");
        if (!(thermalBurnerItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:machine_casing")
                || !list.contains("echoashfallprotocol:ash")) {
            diagnostics.add("Expected Thermal Burner requirements for Scrap Metal, Machine Casing, and Ash.");
        }
        Object thermalBurnerBlocks = twentySeventhMission.get("blockRequirements");
        if (!(thermalBurnerBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(TWENTY_SEVENTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Thermal Burner block requirement for one echoashfallprotocol:thermal_burner.");
        }
        require(twentyEighthMission, "id", TWENTY_EIGHTH_MISSION_ID, diagnostics);
        require(twentyEighthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyEighthMission, "prerequisite", TWENTY_SEVENTH_MISSION_ID, diagnostics);
        require(twentyEighthMission, "objectiveType", "composite_predicate", diagnostics);
        require(twentyEighthMission, "objectiveTarget", TWENTY_EIGHTH_OBJECTIVE_TARGET, diagnostics);
        require(twentyEighthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object stabilityItems = twentyEighthMission.get("requiredItemCounts");
        if (!(stabilityItems instanceof Map<?, ?> map)
                || !Integer.valueOf(3).equals(map.get("echoashfallprotocol:clean_water_bottle"))
                || !Integer.valueOf(2).equals(map.get("echoashfallprotocol:bandage"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:energy_cell"))) {
            diagnostics.add("Expected Base Stability item requirements for clean water, bandages, and energy cell.");
        }
        Object stabilityBlocks = twentyEighthMission.get("blockRequirements");
        if (!(stabilityBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get("minecraft:chest"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:water_purifier"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:battery_bank"))) {
            diagnostics.add("Expected Base Stability block requirements for chest, Water Purifier, and Battery Bank.");
        }
        Object stabilityAlternates = twentyEighthMission.get("alternateCompletionSignals");
        if (!(stabilityAlternates instanceof List<?> list)
                || !list.contains("echoashfallprotocol:first_faction_contact:completed")) {
            diagnostics.add("Expected Base Stability alternate completion signal for first_faction_contact.");
        }
        require(twentyNinthMission, "id", TWENTY_NINTH_MISSION_ID, diagnostics);
        require(twentyNinthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(twentyNinthMission, "prerequisite", TWENTY_EIGHTH_MISSION_ID, diagnostics);
        require(twentyNinthMission, "objectiveType", "equipment_predicate", diagnostics);
        require(twentyNinthMission, "objectiveTarget", TWENTY_NINTH_OBJECTIVE_TARGET, diagnostics);
        require(twentyNinthMission, "equipmentSlot", "head", diagnostics);
        require(twentyNinthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object gasMaskEquipment = twentyNinthMission.get("requiredEquipment");
        if (!(gasMaskEquipment instanceof Map<?, ?> map)
                || !TWENTY_NINTH_OBJECTIVE_TARGET.equals(map.get("head"))) {
            diagnostics.add("Expected Gas Mask equipment requirement for echoashfallprotocol:gas_mask in the head slot.");
        }
        Object gasMaskEvents = twentyNinthMission.get("runtimeEvents");
        if (!(gasMaskEvents instanceof List<?> list)
                || !list.contains("player.equipment_changed")
                || !list.contains("ashfall.equipment_predicate")) {
            diagnostics.add("Expected Gas Mask runtime events for equipment predicate handling.");
        }
        require(thirtiethMission, "id", THIRTIETH_MISSION_ID, diagnostics);
        require(thirtiethMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtiethMission, "prerequisite", TWENTY_NINTH_MISSION_ID, diagnostics);
        require(thirtiethMission, "objectiveType", "obtain_item", diagnostics);
        require(thirtiethMission, "objectiveTarget", THIRTIETH_OBJECTIVE_TARGET, diagnostics);
        require(thirtiethMission, "nativeProvider", "echomissioncore", diagnostics);
        Object filterItems = thirtiethMission.get("requiredItemCounts");
        if (!(filterItems instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTIETH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Basic Filter Cartridge inventory requirement for one echoashfallprotocol:filter_cartridge_basic.");
        }
        Object filterEvents = thirtiethMission.get("runtimeEvents");
        if (!(filterEvents instanceof List<?> list)
                || !list.contains("player.inventory_changed")
                || !list.contains("ashfall.inventory_predicate")) {
            diagnostics.add("Expected Basic Filter Cartridge runtime events for inventory predicate handling.");
        }
        require(thirtyFirstMission, "id", THIRTY_FIRST_MISSION_ID, diagnostics);
        require(thirtyFirstMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtyFirstMission, "prerequisite", THIRTIETH_MISSION_ID, diagnostics);
        require(thirtyFirstMission, "objectiveType", "place_block", diagnostics);
        require(thirtyFirstMission, "objectiveTarget", THIRTY_FIRST_OBJECTIVE_TARGET, diagnostics);
        require(thirtyFirstMission, "nativeProvider", "echomissioncore", diagnostics);
        Object filterWorkbenchItems = thirtyFirstMission.get("requiredItems");
        if (!(filterWorkbenchItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:filtration_membrane")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("echoashfallprotocol:scrap_wire")) {
            diagnostics.add("Expected Filter Workbench requirements for Scrap Metal, Filtration Membrane, Circuit Board, and Scrap Wire.");
        }
        Object filterWorkbenchBlocks = thirtyFirstMission.get("blockRequirements");
        if (!(filterWorkbenchBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTY_FIRST_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Filter Workbench block requirement for one echoashfallprotocol:filter_workbench.");
        }
        require(thirtySecondMission, "id", THIRTY_SECOND_MISSION_ID, diagnostics);
        require(thirtySecondMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtySecondMission, "prerequisite", THIRTY_FIRST_MISSION_ID, diagnostics);
        require(thirtySecondMission, "objectiveType", "obtain_item", diagnostics);
        require(thirtySecondMission, "objectiveTarget", THIRTY_SECOND_OBJECTIVE_TARGET, diagnostics);
        require(thirtySecondMission, "nativeProvider", "echomissioncore", diagnostics);
        Object advancedFilterItems = thirtySecondMission.get("requiredItemCounts");
        if (!(advancedFilterItems instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTY_SECOND_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Advanced Filter Cartridge inventory requirement for one echoashfallprotocol:filter_cartridge_advanced.");
        }
        Object advancedFilterEvents = thirtySecondMission.get("runtimeEvents");
        if (!(advancedFilterEvents instanceof List<?> list)
                || !list.contains("player.inventory_changed")
                || !list.contains("ashfall.inventory_predicate")) {
            diagnostics.add("Expected Advanced Filter Cartridge runtime events for inventory predicate handling.");
        }
        require(thirtyThirdMission, "id", THIRTY_THIRD_MISSION_ID, diagnostics);
        require(thirtyThirdMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtyThirdMission, "prerequisite", THIRTY_SECOND_MISSION_ID, diagnostics);
        require(thirtyThirdMission, "objectiveType", "place_block", diagnostics);
        require(thirtyThirdMission, "objectiveTarget", THIRTY_THIRD_OBJECTIVE_TARGET, diagnostics);
        require(thirtyThirdMission, "nativeProvider", "echomissioncore", diagnostics);
        Object researchLabItems = thirtyThirdMission.get("requiredItems");
        if (!(researchLabItems instanceof List<?> list)
                || !list.contains("echoashfallprotocol:scrap_metal")
                || !list.contains("echoashfallprotocol:circuit_board")
                || !list.contains("echoashfallprotocol:machine_casing")) {
            diagnostics.add("Expected Research Lab requirements for Scrap Metal, Circuit Board, and Machine Casing.");
        }
        Object researchLabBlocks = thirtyThirdMission.get("blockRequirements");
        if (!(researchLabBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTY_THIRD_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Research Lab block requirement for one echoashfallprotocol:research_lab.");
        }
        require(thirtyFourthMission, "id", THIRTY_FOURTH_MISSION_ID, diagnostics);
        require(thirtyFourthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtyFourthMission, "prerequisite", THIRTY_THIRD_MISSION_ID, diagnostics);
        require(thirtyFourthMission, "objectiveType", "research_predicate", diagnostics);
        require(thirtyFourthMission, "objectiveTarget", THIRTY_FOURTH_OBJECTIVE_TARGET, diagnostics);
        require(thirtyFourthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object firstSchematicResearch = thirtyFourthMission.get("requiredResearch");
        if (!(firstSchematicResearch instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get("unlockedSchematicsAtLeast"))) {
            diagnostics.add("Expected First Schematic research requirement for at least one unlocked schematic.");
        }
        Object firstSchematicEvents = thirtyFourthMission.get("runtimeEvents");
        if (!(firstSchematicEvents instanceof List<?> list)
                || !list.contains("ashfall.research_updated")
                || !list.contains("ashfall.schematic_unlocked")) {
            diagnostics.add("Expected First Schematic runtime events for research and schematic unlock predicates.");
        }
        require(thirtyFifthMission, "id", THIRTY_FIFTH_MISSION_ID, diagnostics);
        require(thirtyFifthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtyFifthMission, "prerequisite", THIRTY_FOURTH_MISSION_ID, diagnostics);
        require(thirtyFifthMission, "objectiveType", "place_block", diagnostics);
        require(thirtyFifthMission, "objectiveTarget", THIRTY_FIFTH_OBJECTIVE_TARGET, diagnostics);
        require(thirtyFifthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object factoryControllerItems = thirtyFifthMission.get("requiredItemCounts");
        if (!(factoryControllerItems instanceof Map<?, ?> map)
                || !Integer.valueOf(5).equals(map.get("echoashfallprotocol:circuit_board"))
                || !Integer.valueOf(2).equals(map.get("echoashfallprotocol:machine_casing"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:energy_cell"))
                || !Integer.valueOf(1).equals(map.get("minecraft:redstone_block"))) {
            diagnostics.add("Expected Factory Controller requirements for Circuit Board x5, Machine Casing x2, Energy Cell x1, and Redstone Block x1.");
        }
        Object factoryControllerBlocks = thirtyFifthMission.get("blockRequirements");
        if (!(factoryControllerBlocks instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTY_FIFTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Factory Controller block requirement for one echoashfallprotocol:factory_controller.");
        }
        Object factoryControllerEvents = thirtyFifthMission.get("runtimeEvents");
        if (!(factoryControllerEvents instanceof List<?> list)
                || !list.contains("player.block_placed")
                || !list.contains("ashfall.block_requirement")) {
            diagnostics.add("Expected Factory Controller runtime events for block-placement predicate handling.");
        }
        require(thirtySixthMission, "id", THIRTY_SIXTH_MISSION_ID, diagnostics);
        require(thirtySixthMission, "chapterId", FIRST_CHAPTER_ID, diagnostics);
        require(thirtySixthMission, "prerequisite", THIRTY_FIFTH_MISSION_ID, diagnostics);
        require(thirtySixthMission, "objectiveType", "obtain_item", diagnostics);
        require(thirtySixthMission, "objectiveTarget", THIRTY_SIXTH_OBJECTIVE_TARGET, diagnostics);
        require(thirtySixthMission, "nativeProvider", "echomissioncore", diagnostics);
        Object scannerItems = thirtySixthMission.get("requiredItemCounts");
        if (!(scannerItems instanceof Map<?, ?> map)
                || !Integer.valueOf(1).equals(map.get(THIRTY_SIXTH_OBJECTIVE_TARGET))) {
            diagnostics.add("Expected Portable Signal Scanner inventory requirement for one echoashfallprotocol:portable_signal_scanner.");
        }
        Object scannerRecipe = thirtySixthMission.get("recipeIngredients");
        if (!(scannerRecipe instanceof Map<?, ?> map)
                || !Integer.valueOf(2).equals(map.get("echoashfallprotocol:scrap_circuit"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:scrap_wire"))
                || !Integer.valueOf(2).equals(map.get("echoashfallprotocol:scrap_metal"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:circuit_board"))
                || !Integer.valueOf(1).equals(map.get("echoashfallprotocol:energy_cell"))) {
            diagnostics.add("Expected Portable Signal Scanner recipe contract to mirror the shaped crafting recipe.");
        }
        Object scannerEvents = thirtySixthMission.get("runtimeEvents");
        if (!(scannerEvents instanceof List<?> list)
                || !list.contains("player.craft_item")
                || !list.contains("player.inventory_changed")
                || !list.contains("ashfall.inventory_predicate")) {
            diagnostics.add("Expected Portable Signal Scanner runtime events for crafting and pickup predicate handling.");
        }
        require(worldAnchor, "regionId", CRASH_REGION_ID, diagnostics);
        require(worldAnchor, "hazardId", CRASH_HAZARD_ID, diagnostics);
        Object signals = progressionHook.get("completionSignals");
        if (!(signals instanceof List<?> list)
                || !list.contains(FIRST_MISSION_ID + ":completed")
                || !list.contains(SECOND_MISSION_ID + ":completed")
                || !list.contains(THIRD_MISSION_ID + ":completed")
                || !list.contains(FOURTH_MISSION_ID + ":completed")
                || !list.contains(FIFTH_MISSION_ID + ":completed")
                || !list.contains(SIXTH_MISSION_ID + ":completed")
                || !list.contains(SEVENTH_MISSION_ID + ":completed")
                || !list.contains(EIGHTH_MISSION_ID + ":completed")
                || !list.contains(NINTH_MISSION_ID + ":completed")
                || !list.contains(TENTH_MISSION_ID + ":completed")
                || !list.contains(ELEVENTH_MISSION_ID + ":completed")
                || !list.contains(TWELFTH_MISSION_ID + ":completed")
                || !list.contains(THIRTEENTH_MISSION_ID + ":completed")
                || !list.contains(FOURTEENTH_MISSION_ID + ":completed")
                || !list.contains(FIFTEENTH_MISSION_ID + ":completed")
                || !list.contains(SIXTEENTH_MISSION_ID + ":completed")
                || !list.contains(SEVENTEENTH_MISSION_ID + ":completed")
                || !list.contains(EIGHTEENTH_MISSION_ID + ":completed")
                || !list.contains(NINETEENTH_MISSION_ID + ":completed")
                || !list.contains(TWENTIETH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_FIRST_MISSION_ID + ":completed")
                || !list.contains(TWENTY_SECOND_MISSION_ID + ":completed")
                || !list.contains(TWENTY_THIRD_MISSION_ID + ":completed")
                || !list.contains(TWENTY_FOURTH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_FIFTH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_SIXTH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_SEVENTH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_EIGHTH_MISSION_ID + ":completed")
                || !list.contains(TWENTY_NINTH_MISSION_ID + ":completed")
                || !list.contains(THIRTIETH_MISSION_ID + ":completed")
                || !list.contains(THIRTY_FIRST_MISSION_ID + ":completed")
                || !list.contains(THIRTY_SECOND_MISSION_ID + ":completed")
                || !list.contains(THIRTY_THIRD_MISSION_ID + ":completed")
                || !list.contains(THIRTY_FOURTH_MISSION_ID + ":completed")
                || !list.contains(THIRTY_FIFTH_MISSION_ID + ":completed")
                || !list.contains(THIRTY_SIXTH_MISSION_ID + ":completed")) {
            diagnostics.add("Expected opening route completion signals for all native opening-route missions.");
        }
        return List.copyOf(diagnostics);
    }

    private static void require(Map<String, Object> data, String key, String expected, List<String> diagnostics) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            diagnostics.add("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }
}
