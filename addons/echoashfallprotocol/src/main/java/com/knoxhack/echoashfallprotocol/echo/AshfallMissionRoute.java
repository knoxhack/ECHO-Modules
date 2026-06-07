package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/**
 * Canonical Ashfall route overlay.
 *
 * <p>The mission constructors remain the source of mechanics, rewards, and save
 * IDs. This class owns the lore/progression order and the small prerequisite
 * fixes required to place the same IDs on the P1-P9 modpack route.</p>
 */
public final class AshfallMissionRoute {
    private static final List<List<String>> PHASE_ROUTES = List.of(
            List.of(
                    "secure_crash_outpost",
                    "craft_scrap_knife",
                    "drink_clean_water",
                    "secure_emergency_water_loop",
                    "forage_wasteland_food",
                    "plant_mutated_sapling",
                    "build_rain_collector",
                    "stockpile_rations",
                    "secure_sleep_shelter"
            ),
            List.of(
                    "assemble_wasteland_field_kit",
                    "find_schematic_fragment",
                    "build_hand_recycler",
                    "make_machine_casing",
                    "build_micro_generator",
                    "build_water_purifier",
                    "stockpile_clean_water",
                    "build_battery_bank",
                    "build_scrap_dynamo",
                    "charge_basic_battery",
                    "route_power_cable",
                    "upgrade_power_cable",
                    "install_energy_meter",
                    "set_power_priority"
            ),
            List.of(
                    "build_scrap_press",
                    "overclock_machine",
                    "install_item_pipe",
                    "build_thermal_burner",
                    "base_stability_check",
                    "equip_gas_mask",
                    "fix_mask_filter",
                    "build_filter_workbench",
                    "craft_advanced_filter",
                    "build_research_lab",
                    "first_schematic",
                    "build_factory_controller"
            ),
            List.of(
                    "craft_portable_scanner",
                    "expedition_readiness",
                    "scan_first_poi",
                    "loot_survivor_cache",
                    "first_faction_contact",
                    "complete_first_faction_task",
                    "repair_echo_drone",
                    "recover_drone_intel",
                    "faction_reputation",
                    "first_perk",
                    "poi_explorer"
            ),
            List.of(
                    "enter_bio_lab",
                    "recover_data_log",
                    "survey_reactor_ruin",
                    "build_field_med_bay",
                    "build_radiation_cleanser",
                    "craft_radaway",
                    "scan_mutation_status",
                    "use_field_med_bay",
                    "stabilize_mutation_effects",
                    "scout_radiation_zone",
                    "build_atmospheric_scrubber",
                    "collect_mutated_tissue",
                    "craft_mutagen_vial"
            ),
            List.of(
                    "clear_military_vault",
                    "find_dense_alloy",
                    "build_thermal_array",
                    "build_ore_grinder",
                    "build_isotope_refiner",
                    "forge_alloy_weapon",
                    "equip_alloy_kit",
                    "stockpile_route_supplies",
                    "calibrate_midgame_grid"
            ),
            List.of(
                    "deploy_stationary_scanner",
                    "activate_power_node",
                    "activate_relay_station",
                    "build_scout_drone",
                    "build_nexus_capacitor",
                    "build_workshop",
                    "neutralize_plains_warlord",
                    "neutralize_city_ruin_stalker",
                    "neutralize_industrial_juggernaut",
                    "neutralize_toxic_hive_matriarch",
                    "neutralize_crash_zone_colossus",
                    "neutralize_radiation_behemoth",
                    "enter_cryogenic_ruins",
                    "recover_cryo_sample",
                    "warm_up_after_exposure",
                    "craft_cold_route_supplies",
                    "neutralize_cryogenic_overseer",
                    "neutralize_nexus_scar_avatar"
            ),
            List.of(
                    "find_nexus_core",
                    "awaken_nexus_core",
                    "scan_prime_relays",
                    "resolve_prime_relays",
                    "stabilize_nexus_grid",
                    "survive_core_countermeasure",
                    "reach_decision"
            ),
            List.of(
                    "restore_repair_nodes",
                    "restore_purge_corruption",
                    "restore_enter_archives",
                    "restore_guardian",
                    "restore_world_lattice",
                    "restore_finale",
                    "restore_epilogue",
                    "destroy_scorched_earth",
                    "destroy_survive_storms",
                    "destroy_enter_archives",
                    "destroy_guardian",
                    "destroy_dead_signal",
                    "destroy_finale",
                    "destroy_epilogue",
                    "control_signal_expansion",
                    "control_resource_dominance",
                    "control_enter_archives",
                    "control_guardian",
                    "control_command_lattice",
                    "control_finale",
                    "control_epilogue"
            )
    );

    private static final Set<String> MAIN_SPINE = Set.of(
            "secure_crash_outpost",
            "craft_scrap_knife",
            "drink_clean_water",
            "forage_wasteland_food",
            "build_rain_collector",
            "secure_sleep_shelter",
            "assemble_wasteland_field_kit",
            "find_schematic_fragment",
            "build_hand_recycler",
            "make_machine_casing",
            "build_micro_generator",
            "build_water_purifier",
            "stockpile_clean_water",
            "build_battery_bank",
            "build_scrap_dynamo",
            "route_power_cable",
            "build_thermal_burner",
            "base_stability_check",
            "equip_gas_mask",
            "build_filter_workbench",
            "craft_portable_scanner",
            "scan_first_poi",
            "first_faction_contact",
            "repair_echo_drone",
            "expedition_readiness",
            "build_research_lab",
            "build_factory_controller",
            "enter_bio_lab",
            "find_dense_alloy",
            "build_field_med_bay",
            "build_radiation_cleanser",
            "craft_radaway",
            "scan_mutation_status",
            "use_field_med_bay",
            "build_ore_grinder",
            "build_isotope_refiner",
            "build_thermal_array",
            "forge_alloy_weapon",
            "equip_alloy_kit",
            "calibrate_midgame_grid",
            "build_workshop",
            "deploy_stationary_scanner",
            "activate_power_node",
            "activate_relay_station",
            "build_scout_drone",
            "enter_cryogenic_ruins",
            "recover_cryo_sample",
            "build_nexus_capacitor",
            "find_nexus_core",
            "awaken_nexus_core",
            "scan_prime_relays",
            "resolve_prime_relays",
            "stabilize_nexus_grid",
            "survive_core_countermeasure",
            "reach_decision",
            "restore_enter_archives",
            "restore_guardian",
            "restore_finale",
            "restore_epilogue",
            "destroy_enter_archives",
            "destroy_guardian",
            "destroy_finale",
            "destroy_epilogue",
            "control_enter_archives",
            "control_guardian",
            "control_finale",
            "control_epilogue"
    );

    private static final Map<String, List<String>> PREREQUISITE_OVERRIDES = Map.ofEntries(
            entry("forage_wasteland_food", List.of("secure_emergency_water_loop")),
            entry("find_schematic_fragment", List.of("assemble_wasteland_field_kit")),
            entry("build_research_lab", List.of("craft_advanced_filter")),
            entry("first_faction_contact", List.of("scan_first_poi")),
            entry("complete_first_faction_task", List.of("first_faction_contact")),
            entry("repair_echo_drone", List.of("complete_first_faction_task")),
            entry("recover_drone_intel", List.of("repair_echo_drone")),
            entry("faction_reputation", List.of("recover_drone_intel")),
            entry("use_field_med_bay", List.of("build_field_med_bay")),
            entry("survey_reactor_ruin", List.of("recover_data_log")),
            entry("clear_military_vault", List.of("survey_reactor_ruin")),
            entry("find_dense_alloy", List.of("clear_military_vault")),
            entry("activate_power_node", List.of("deploy_stationary_scanner"))
    );

    private static final Map<String, List<String>> DEPRECATED_ALIASES_BY_REPLACEMENT = Map.ofEntries(
            entry("secure_emergency_water_loop", List.of(
                    "get_dirty_water",
                    "emergency_filter_water")),
            entry("assemble_wasteland_field_kit", List.of(
                    "craft_bone_knife",
                    "craft_crude_spear",
                    "craft_hide_wrap")),
            entry("complete_first_faction_task", List.of(
                    "contact_radwarden_compact",
                    "contact_crashbreak_salvage",
                    "contact_sporebound_sanctum",
                    "complete_radwarden_contract",
                    "complete_crashbreak_contract",
                    "complete_sporebound_contract")),
            entry("recover_drone_intel", List.of(
                    "upgrade_drone_support",
                    "set_drone_scout_mode",
                    "deploy_scout_drone")),
            entry("craft_mutagen_vial", List.of("acquire_mutagen"))
    );

    private static final Set<String> DEPRECATED_MISSION_IDS = collectDeprecatedMissionIds();

    private AshfallMissionRoute() {
    }

    static int phaseCount() {
        return PHASE_ROUTES.size();
    }

    static List<String> phaseRoute(int phase) {
        if (phase < 0 || phase >= PHASE_ROUTES.size()) {
            return List.of();
        }
        return PHASE_ROUTES.get(phase);
    }

    static Set<String> allRouteIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (List<String> phase : PHASE_ROUTES) {
            ids.addAll(phase);
        }
        return ids;
    }

    public static boolean isMainSpine(String missionId) {
        return missionId != null && MAIN_SPINE.contains(missionId);
    }

    public static boolean blocksPhase(Mission mission) {
        return mission != null && isMainSpine(mission.id()) && !isDeprecated(mission.id());
    }

    public static boolean canStartMainSpine(Mission mission, Set<String> completedMissionIds) {
        return completedMissionIds != null && completedMissionIds.containsAll(mainlinePrerequisites(mission));
    }

    public static List<String> mainlinePrerequisites(Mission mission) {
        if (mission == null || mission.getPrerequisites().isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> prerequisites = new LinkedHashSet<>();
        for (String prerequisite : mission.getPrerequisites()) {
            collectMainlinePrerequisites(prerequisite, prerequisites, new LinkedHashSet<>());
        }
        return List.copyOf(prerequisites);
    }

    public static String routeAnchor(String missionId) {
        if (missionId == null || missionId.isBlank() || isMainSpine(missionId)) {
            return "";
        }
        String anchor = routeAnchor(missionId, new LinkedHashSet<>());
        return anchor == null ? "" : anchor;
    }

    public static int routePhase(String missionId, int fallbackPhase) {
        return switch (missionId) {
            case "secure_crash_outpost", "craft_scrap_knife", "drink_clean_water" -> 0;
            case "secure_emergency_water_loop", "forage_wasteland_food", "plant_mutated_sapling",
                    "build_rain_collector", "stockpile_rations", "secure_sleep_shelter" -> 1;
            case "build_water_purifier", "stockpile_clean_water" -> 2;
            case "assemble_wasteland_field_kit", "find_schematic_fragment" -> 3;
            case "build_hand_recycler", "make_machine_casing", "build_micro_generator",
                    "build_battery_bank", "build_scrap_dynamo", "charge_basic_battery",
                    "route_power_cable", "upgrade_power_cable", "install_energy_meter",
                    "set_power_priority" -> 4;
            case "build_scrap_press", "overclock_machine", "install_item_pipe",
                    "build_thermal_burner", "base_stability_check", "build_research_lab",
                    "first_schematic", "build_factory_controller" -> 5;
            case "equip_gas_mask", "fix_mask_filter", "build_filter_workbench",
                    "craft_advanced_filter" -> 6;
            case "craft_portable_scanner", "expedition_readiness", "scan_first_poi",
                    "loot_survivor_cache", "poi_explorer" -> 7;
            case "first_faction_contact", "complete_first_faction_task", "repair_echo_drone",
                    "recover_drone_intel", "faction_reputation", "first_perk" -> 8;
            case "enter_bio_lab", "recover_data_log", "survey_reactor_ruin",
                    "build_field_med_bay", "use_field_med_bay", "craft_radaway",
                    "scan_mutation_status", "stabilize_mutation_effects", "scout_radiation_zone",
                    "build_atmospheric_scrubber", "build_radiation_cleanser",
                    "collect_mutated_tissue", "craft_mutagen_vial" -> 9;
            case "clear_military_vault", "find_dense_alloy", "build_thermal_array",
                    "build_ore_grinder", "build_isotope_refiner", "forge_alloy_weapon",
                    "equip_alloy_kit", "stockpile_route_supplies", "calibrate_midgame_grid" -> 10;
            case "deploy_stationary_scanner", "activate_power_node", "build_nexus_capacitor",
                    "build_workshop", "activate_relay_station", "build_scout_drone" -> 11;
            case "neutralize_plains_warlord", "neutralize_city_ruin_stalker",
                    "neutralize_industrial_juggernaut", "neutralize_toxic_hive_matriarch",
                    "neutralize_crash_zone_colossus", "neutralize_radiation_behemoth" -> 12;
            case "enter_cryogenic_ruins", "recover_cryo_sample", "warm_up_after_exposure",
                    "craft_cold_route_supplies", "neutralize_cryogenic_overseer" -> 13;
            case "neutralize_nexus_scar_avatar", "find_nexus_core", "awaken_nexus_core",
                    "scan_prime_relays", "resolve_prime_relays", "stabilize_nexus_grid",
                    "survive_core_countermeasure", "reach_decision" -> 14;
            case "restore_repair_nodes", "restore_purge_corruption", "restore_enter_archives",
                    "restore_guardian", "restore_world_lattice", "restore_finale",
                    "restore_epilogue", "destroy_scorched_earth", "destroy_survive_storms",
                    "destroy_enter_archives", "destroy_guardian", "destroy_dead_signal",
                    "destroy_finale", "destroy_epilogue", "control_signal_expansion",
                    "control_resource_dominance", "control_enter_archives", "control_guardian",
                    "control_command_lattice", "control_finale", "control_epilogue" -> 15;
            default -> Math.max(0, Math.min(15, fallbackPhase));
        };
    }

    public static int routeOrder(String missionId, int fallbackOrder) {
        int order = 0;
        for (List<String> phase : PHASE_ROUTES) {
            for (String id : phase) {
                order++;
                if (id.equals(missionId)) {
                    return order;
                }
            }
        }
        return fallbackOrder;
    }

    static boolean isDeprecated(String missionId) {
        return DEPRECATED_MISSION_IDS.contains(missionId);
    }

    static Set<String> deprecatedMissionIds() {
        return DEPRECATED_MISSION_IDS;
    }

    public static Set<String> replacementMissionIds() {
        return DEPRECATED_ALIASES_BY_REPLACEMENT.keySet();
    }

    public static List<String> deprecatedAliasesFor(String replacementMissionId) {
        return DEPRECATED_ALIASES_BY_REPLACEMENT.getOrDefault(replacementMissionId, List.of());
    }

    static String replacementForDeprecated(String deprecatedMissionId) {
        for (Map.Entry<String, List<String>> entry : DEPRECATED_ALIASES_BY_REPLACEMENT.entrySet()) {
            if (entry.getValue().contains(deprecatedMissionId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static boolean allDeprecatedAliasesComplete(Set<String> completedMissionIds, String replacementMissionId) {
        List<String> aliases = deprecatedAliasesFor(replacementMissionId);
        return !aliases.isEmpty() && completedMissionIds.containsAll(aliases);
    }

    private static void collectMainlinePrerequisites(
            String missionId,
            LinkedHashSet<String> prerequisites,
            LinkedHashSet<String> seen) {
        if (missionId == null || missionId.isBlank() || !seen.add(missionId)) {
            return;
        }
        if (isMainSpine(missionId)) {
            prerequisites.add(missionId);
            return;
        }
        Mission mission = MissionRegistry.getMissionById(missionId);
        if (mission == null || mission.getPrerequisites().isEmpty()) {
            String fallback = previousMainSpineMission(missionId);
            if (!fallback.isBlank()) {
                prerequisites.add(fallback);
            }
            return;
        }
        for (String prerequisite : mission.getPrerequisites()) {
            collectMainlinePrerequisites(prerequisite, prerequisites, seen);
        }
    }

    private static String routeAnchor(String missionId, LinkedHashSet<String> seen) {
        if (missionId == null || missionId.isBlank() || !seen.add(missionId)) {
            return "";
        }
        Mission mission = MissionRegistry.getMissionById(missionId);
        if (mission != null) {
            for (String prerequisite : mission.getPrerequisites()) {
                if (isMainSpine(prerequisite)) {
                    return prerequisite;
                }
                String anchor = routeAnchor(prerequisite, new LinkedHashSet<>(seen));
                if (!anchor.isBlank()) {
                    return anchor;
                }
            }
        }
        return previousMainSpineMission(missionId);
    }

    private static String previousMainSpineMission(String missionId) {
        String previous = "";
        for (List<String> phase : PHASE_ROUTES) {
            for (String id : phase) {
                if (id.equals(missionId)) {
                    return previous;
                }
                if (isMainSpine(id)) {
                    previous = id;
                }
            }
        }
        return previous.isBlank() ? "secure_crash_outpost" : previous;
    }

    private static Set<String> collectDeprecatedMissionIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (List<String> aliases : DEPRECATED_ALIASES_BY_REPLACEMENT.values()) {
            ids.addAll(aliases);
        }
        return Set.copyOf(ids);
    }

    static String phaseTitle(int phase) {
        return switch (phase) {
            case 0 -> "PODFALL";
            case 1 -> "OUTPOST SURVIVAL";
            case 2 -> "LIFE SUPPORT";
            case 3 -> "SIGNAL CONTACT";
            case 4 -> "BIOHAZARD ADAPTATION";
            case 5 -> "DEEP EXTRACTION";
            case 6 -> "GRID RESTORATION";
            case 7 -> "NEXUS DECISION";
            case 8 -> "AFTERMATH PROTOCOL";
            default -> "PHASE " + (phase + 1);
        };
    }

    static Mission adjust(Mission mission) {
        List<String> prerequisites = PREREQUISITE_OVERRIDES.get(mission.id());
        if (prerequisites == null || prerequisites.equals(mission.getPrerequisites())) {
            return mission;
        }
        return rebuild(mission, prerequisites);
    }

    private static Mission rebuild(Mission mission, List<String> prerequisites) {
        return new Mission(
                mission.id(),
                mission.echoMessage(),
                mission.objectiveText(),
                mission.completionMessage(),
                mission.rewards(),
                mission.completionCheck(),
                mission.requiredItems(),
                mission.objectiveIcon(),
                mission.category(),
                mission.difficulty(),
                List.copyOf(prerequisites),
                mission.isTurnInMission(),
                mission.craftingRecipeId(),
                mission.requiredBlocks(),
                mission.requiredEntityKills(),
                mission.requiredLocations(),
                mission.requiredEquipment(),
                mission.requiredPath() == null ? PostNexusData.NexusPath.NONE : mission.requiredPath());
    }
}
