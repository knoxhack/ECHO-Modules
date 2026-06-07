package com.knoxhack.echoterminal.api;

import com.knoxhack.echoterminal.EchoTerminal;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class TerminalVisualAssets {
    private static final Set<String> MISSION_ICON_IDS = Set.of(
            "echoashfallprotocol:acquire_mutagen",
            "echoashfallprotocol:activate_power_node",
            "echoashfallprotocol:activate_relay_station",
            "echoashfallprotocol:awaken_nexus_core",
            "echoashfallprotocol:base_stability_check",
            "echoashfallprotocol:build_atmospheric_scrubber",
            "echoashfallprotocol:build_battery_bank",
            "echoashfallprotocol:build_factory_controller",
            "echoashfallprotocol:build_field_med_bay",
            "echoashfallprotocol:build_filter_workbench",
            "echoashfallprotocol:build_hand_recycler",
            "echoashfallprotocol:build_isotope_refiner",
            "echoashfallprotocol:build_micro_generator",
            "echoashfallprotocol:build_nexus_capacitor",
            "echoashfallprotocol:build_ore_grinder",
            "echoashfallprotocol:build_radiation_cleanser",
            "echoashfallprotocol:build_rain_collector",
            "echoashfallprotocol:build_research_lab",
            "echoashfallprotocol:build_scout_drone",
            "echoashfallprotocol:build_scrap_dynamo",
            "echoashfallprotocol:build_scrap_press",
            "echoashfallprotocol:build_thermal_array",
            "echoashfallprotocol:build_thermal_burner",
            "echoashfallprotocol:build_water_purifier",
            "echoashfallprotocol:build_workshop",
            "echoashfallprotocol:calibrate_midgame_grid",
            "echoashfallprotocol:charge_basic_battery",
            "echoashfallprotocol:clear_military_vault",
            "echoashfallprotocol:collect_mutated_tissue",
            "echoashfallprotocol:complete_crashbreak_contract",
            "echoashfallprotocol:complete_first_faction_task",
            "echoashfallprotocol:complete_radwarden_contract",
            "echoashfallprotocol:complete_sporebound_contract",
            "echoashfallprotocol:contact_crashbreak_salvage",
            "echoashfallprotocol:contact_radwarden_compact",
            "echoashfallprotocol:contact_sporebound_sanctum",
            "echoashfallprotocol:control_command_lattice",
            "echoashfallprotocol:control_enter_archives",
            "echoashfallprotocol:control_epilogue",
            "echoashfallprotocol:control_finale",
            "echoashfallprotocol:control_guardian",
            "echoashfallprotocol:control_resource_dominance",
            "echoashfallprotocol:control_signal_expansion",
            "echoashfallprotocol:craft_advanced_filter",
            "echoashfallprotocol:craft_bone_knife",
            "echoashfallprotocol:craft_cold_route_supplies",
            "echoashfallprotocol:craft_crude_spear",
            "echoashfallprotocol:craft_hide_wrap",
            "echoashfallprotocol:craft_mutagen_vial",
            "echoashfallprotocol:craft_portable_scanner",
            "echoashfallprotocol:craft_radaway",
            "echoashfallprotocol:craft_scrap_knife",
            "echoashfallprotocol:crash_blackbox_signal",
            "echoashfallprotocol:deploy_scout_drone",
            "echoashfallprotocol:deploy_stationary_scanner",
            "echoashfallprotocol:destroy_dead_signal",
            "echoashfallprotocol:destroy_enter_archives",
            "echoashfallprotocol:destroy_epilogue",
            "echoashfallprotocol:destroy_finale",
            "echoashfallprotocol:destroy_guardian",
            "echoashfallprotocol:destroy_scorched_earth",
            "echoashfallprotocol:destroy_survive_storms",
            "echoashfallprotocol:drink_clean_water",
            "echoashfallprotocol:drone_memory_sweep",
            "echoashfallprotocol:emergency_filter_water",
            "echoashfallprotocol:enter_bio_lab",
            "echoashfallprotocol:enter_cryogenic_ruins",
            "echoashfallprotocol:equip_alloy_kit",
            "echoashfallprotocol:equip_gas_mask",
            "echoashfallprotocol:expedition_readiness",
            "echoashfallprotocol:faction_crossband",
            "echoashfallprotocol:faction_reputation",
            "echoashfallprotocol:find_dense_alloy",
            "echoashfallprotocol:find_nexus_core",
            "echoashfallprotocol:find_schematic_fragment",
            "echoashfallprotocol:first_faction_contact",
            "echoashfallprotocol:first_perk",
            "echoashfallprotocol:first_ruin_signature",
            "echoashfallprotocol:first_schematic",
            "echoashfallprotocol:fix_mask_filter",
            "echoashfallprotocol:forage_wasteland_food",
            "echoashfallprotocol:forge_alloy_weapon",
            "echoashfallprotocol:get_dirty_water",
            "echoashfallprotocol:guardian_signal_lattice",
            "echoashfallprotocol:install_energy_meter",
            "echoashfallprotocol:install_item_pipe",
            "echoashfallprotocol:loot_survivor_cache",
            "echoashfallprotocol:make_machine_casing",
            "echoashfallprotocol:nexus_choice_record",
            "echoashfallprotocol:orbital_quarantine_echo",
            "echoashfallprotocol:overclock_machine",
            "echoashfallprotocol:plant_mutated_sapling",
            "echoashfallprotocol:poi_explorer",
            "echoashfallprotocol:poi_field_atlas",
            "echoashfallprotocol:reach_decision",
            "echoashfallprotocol:recover_cryo_sample",
            "echoashfallprotocol:recover_data_log",
            "echoashfallprotocol:recover_drone_intel",
            "echoashfallprotocol:repair_echo_drone",
            "echoashfallprotocol:resolve_prime_relays",
            "echoashfallprotocol:restore_enter_archives",
            "echoashfallprotocol:restore_epilogue",
            "echoashfallprotocol:restore_finale",
            "echoashfallprotocol:restore_guardian",
            "echoashfallprotocol:restore_purge_corruption",
            "echoashfallprotocol:restore_repair_nodes",
            "echoashfallprotocol:restore_world_lattice",
            "echoashfallprotocol:route_power_cable",
            "echoashfallprotocol:scan_first_poi",
            "echoashfallprotocol:scan_mutation_status",
            "echoashfallprotocol:scan_prime_relays",
            "echoashfallprotocol:scout_radiation_zone",
            "echoashfallprotocol:secure_crash_outpost",
            "echoashfallprotocol:secure_emergency_water_loop",
            "echoashfallprotocol:secure_sleep_shelter",
            "echoashfallprotocol:set_drone_scout_mode",
            "echoashfallprotocol:set_power_priority",
            "echoashfallprotocol:stabilize_mutation_effects",
            "echoashfallprotocol:stabilize_nexus_grid",
            "echoashfallprotocol:stockpile_clean_water",
            "echoashfallprotocol:stockpile_rations",
            "echoashfallprotocol:stockpile_route_supplies",
            "echoashfallprotocol:survey_reactor_ruin",
            "echoashfallprotocol:survive_core_countermeasure",
            "echoashfallprotocol:upgrade_drone_support",
            "echoashfallprotocol:upgrade_power_cable",
            "echoashfallprotocol:use_field_med_bay",
            "echoashfallprotocol:warm_up_after_exposure",
            "echoashfallprotocol:wasteland_surface_report",
            "echoblackboxprotocol:blackbox_vault",
            "echoblackboxprotocol:core_key",
            "echoblackboxprotocol:decode_memories",
            "echoblackboxprotocol:memory_bosses",
            "echoblackboxprotocol:nexus_guardian",
            "echoblackboxprotocol:truth_engine",
            "echoindustrialnexus:mission/clean_camp",
            "echoindustrialnexus:mission/control_heat",
            "echoindustrialnexus:mission/dense_alloy",
            "echoindustrialnexus:mission/factory_controller",
            "echoindustrialnexus:mission/filters_survival",
            "echoindustrialnexus:mission/grind_wasteland",
            "echoindustrialnexus:mission/hybrid_warning",
            "echoindustrialnexus:mission/production_survived",
            "echoindustrialnexus:mission/reactor_waste",
            "echoindustrialnexus:mission/reclaim_power",
            "echonexusprotocol:deleted_history",
            "echonexusprotocol:dirty_charge",
            "echonexusprotocol:quarantine_failed",
            "echonexusprotocol:reality_forge",
            "echonexusprotocol:stabilize_the_camp",
            "echonexusprotocol:the_core_door",
            "echonexusprotocol:the_monolith_remembers",
            "echonexusprotocol:the_signal_beneath",
            "echonexusprotocol:the_tower_still_speaks",
            "echonexusprotocol:what_rebuilds_the_world",
            "echoorbitalremnants:deep_space_protocol",
            "echoorbitalremnants:earth_calibration",
            "echoorbitalremnants:echo_zero",
            "echoorbitalremnants:europa_route",
            "echoorbitalremnants:faction_contract",
            "echoorbitalremnants:final_seal",
            "echoorbitalremnants:launch_chain",
            "echoorbitalremnants:low_orbit",
            "echoorbitalremnants:lunar_signal",
            "echoorbitalremnants:mars_route",
            "echoorbitalremnants:saturn_route",
            "echoorbitalremnants:station_network",
            "echoorbitalremnants:survey_network",
            "echoorbitalremnants:titan_route",
            "echostationfall:ai_override",
            "echostationfall:blackbox",
            "echostationfall:board_station",
            "echostationfall:decode_logs",
            "echostationfall:restore_power",
            "echostationfall:stabilize_sections",
            "echostationfall:station_mother",
            "minecraft:adventure/hero_of_the_village",
            "minecraft:adventure/kill_a_mob",
            "minecraft:adventure/kill_all_mobs",
            "minecraft:adventure/root",
            "minecraft:adventure/shoot_arrow",
            "minecraft:adventure/sleep_in_bed",
            "minecraft:adventure/trade",
            "minecraft:end/dragon_egg",
            "minecraft:end/elytra",
            "minecraft:end/enter_end_gateway",
            "minecraft:end/kill_dragon",
            "minecraft:end/root",
            "minecraft:husbandry/balanced_diet",
            "minecraft:husbandry/breed_an_animal",
            "minecraft:husbandry/plant_seed",
            "minecraft:husbandry/root",
            "minecraft:husbandry/safely_harvest_honey",
            "minecraft:husbandry/tame_an_animal",
            "minecraft:nether/all_effects",
            "minecraft:nether/all_potions",
            "minecraft:nether/brew_potion",
            "minecraft:nether/create_beacon",
            "minecraft:nether/find_bastion",
            "minecraft:nether/obtain_ancient_debris",
            "minecraft:nether/obtain_blaze_rod",
            "minecraft:nether/return_to_sender",
            "minecraft:nether/root",
            "minecraft:nether/summon_wither",
            "minecraft:story/enchant_item",
            "minecraft:story/enter_the_end",
            "minecraft:story/enter_the_nether",
            "minecraft:story/follow_ender_eye",
            "minecraft:story/iron_tools",
            "minecraft:story/lava_bucket",
            "minecraft:story/mine_diamond",
            "minecraft:story/mine_stone",
            "minecraft:story/root",
            "minecraft:story/smelt_iron",
            "minecraft:story/upgrade_tools");
    private static final Set<String> MISSION_HERO_IDS = Set.of(
            "echoashfallprotocol:acquire_mutagen",
            "echoashfallprotocol:activate_power_node",
            "echoashfallprotocol:activate_relay_station",
            "echoashfallprotocol:awaken_nexus_core",
            "echoashfallprotocol:base_stability_check",
            "echoashfallprotocol:build_atmospheric_scrubber",
            "echoashfallprotocol:build_battery_bank",
            "echoashfallprotocol:build_factory_controller",
            "echoashfallprotocol:build_field_med_bay",
            "echoashfallprotocol:build_filter_workbench",
            "echoashfallprotocol:build_hand_recycler",
            "echoashfallprotocol:build_isotope_refiner",
            "echoashfallprotocol:build_micro_generator",
            "echoashfallprotocol:build_nexus_capacitor",
            "echoashfallprotocol:build_ore_grinder",
            "echoashfallprotocol:build_radiation_cleanser",
            "echoashfallprotocol:build_rain_collector",
            "echoashfallprotocol:build_research_lab",
            "echoashfallprotocol:build_scout_drone",
            "echoashfallprotocol:build_scrap_dynamo",
            "echoashfallprotocol:build_scrap_press",
            "echoashfallprotocol:build_thermal_array",
            "echoashfallprotocol:build_thermal_burner",
            "echoashfallprotocol:build_water_purifier",
            "echoashfallprotocol:build_workshop",
            "echoashfallprotocol:calibrate_midgame_grid",
            "echoashfallprotocol:charge_basic_battery",
            "echoashfallprotocol:clear_military_vault",
            "echoashfallprotocol:collect_mutated_tissue",
            "echoashfallprotocol:complete_crashbreak_contract",
            "echoashfallprotocol:complete_first_faction_task",
            "echoashfallprotocol:complete_radwarden_contract",
            "echoashfallprotocol:complete_sporebound_contract",
            "echoashfallprotocol:contact_crashbreak_salvage",
            "echoashfallprotocol:contact_radwarden_compact",
            "echoashfallprotocol:contact_sporebound_sanctum",
            "echoashfallprotocol:control_command_lattice",
            "echoashfallprotocol:control_enter_archives",
            "echoashfallprotocol:control_epilogue",
            "echoashfallprotocol:control_finale",
            "echoashfallprotocol:control_guardian",
            "echoashfallprotocol:control_resource_dominance",
            "echoashfallprotocol:control_signal_expansion",
            "echoashfallprotocol:craft_advanced_filter",
            "echoashfallprotocol:craft_bone_knife",
            "echoashfallprotocol:craft_cold_route_supplies",
            "echoashfallprotocol:craft_crude_spear",
            "echoashfallprotocol:craft_hide_wrap",
            "echoashfallprotocol:craft_mutagen_vial",
            "echoashfallprotocol:craft_portable_scanner",
            "echoashfallprotocol:craft_radaway",
            "echoashfallprotocol:craft_scrap_knife",
            "echoashfallprotocol:crash_blackbox_signal",
            "echoashfallprotocol:deploy_scout_drone",
            "echoashfallprotocol:deploy_stationary_scanner",
            "echoashfallprotocol:destroy_dead_signal",
            "echoashfallprotocol:destroy_enter_archives",
            "echoashfallprotocol:destroy_epilogue",
            "echoashfallprotocol:destroy_finale",
            "echoashfallprotocol:destroy_guardian",
            "echoashfallprotocol:destroy_scorched_earth",
            "echoashfallprotocol:destroy_survive_storms",
            "echoashfallprotocol:drink_clean_water",
            "echoashfallprotocol:drone_memory_sweep",
            "echoashfallprotocol:emergency_filter_water",
            "echoashfallprotocol:enter_bio_lab",
            "echoashfallprotocol:enter_cryogenic_ruins",
            "echoashfallprotocol:equip_alloy_kit",
            "echoashfallprotocol:equip_gas_mask",
            "echoashfallprotocol:expedition_readiness",
            "echoashfallprotocol:faction_crossband",
            "echoashfallprotocol:faction_reputation",
            "echoashfallprotocol:find_dense_alloy",
            "echoashfallprotocol:find_nexus_core",
            "echoashfallprotocol:find_schematic_fragment",
            "echoashfallprotocol:first_faction_contact",
            "echoashfallprotocol:first_perk",
            "echoashfallprotocol:first_ruin_signature",
            "echoashfallprotocol:first_schematic",
            "echoashfallprotocol:fix_mask_filter",
            "echoashfallprotocol:forage_wasteland_food",
            "echoashfallprotocol:forge_alloy_weapon",
            "echoashfallprotocol:get_dirty_water",
            "echoashfallprotocol:guardian_signal_lattice",
            "echoashfallprotocol:install_energy_meter",
            "echoashfallprotocol:install_item_pipe",
            "echoashfallprotocol:loot_survivor_cache",
            "echoashfallprotocol:make_machine_casing",
            "echoashfallprotocol:nexus_choice_record",
            "echoashfallprotocol:orbital_quarantine_echo",
            "echoashfallprotocol:overclock_machine",
            "echoashfallprotocol:plant_mutated_sapling",
            "echoashfallprotocol:poi_explorer",
            "echoashfallprotocol:poi_field_atlas",
            "echoashfallprotocol:reach_decision",
            "echoashfallprotocol:recover_cryo_sample",
            "echoashfallprotocol:recover_data_log",
            "echoashfallprotocol:recover_drone_intel",
            "echoashfallprotocol:repair_echo_drone",
            "echoashfallprotocol:resolve_prime_relays",
            "echoashfallprotocol:restore_enter_archives",
            "echoashfallprotocol:restore_epilogue",
            "echoashfallprotocol:restore_finale",
            "echoashfallprotocol:restore_guardian",
            "echoashfallprotocol:restore_purge_corruption",
            "echoashfallprotocol:restore_repair_nodes",
            "echoashfallprotocol:restore_world_lattice",
            "echoashfallprotocol:route_power_cable",
            "echoashfallprotocol:scan_first_poi",
            "echoashfallprotocol:scan_mutation_status",
            "echoashfallprotocol:scan_prime_relays",
            "echoashfallprotocol:scout_radiation_zone",
            "echoashfallprotocol:secure_crash_outpost",
            "echoashfallprotocol:secure_emergency_water_loop",
            "echoashfallprotocol:secure_sleep_shelter",
            "echoashfallprotocol:set_drone_scout_mode",
            "echoashfallprotocol:set_power_priority",
            "echoashfallprotocol:stabilize_mutation_effects",
            "echoashfallprotocol:stabilize_nexus_grid",
            "echoashfallprotocol:stockpile_clean_water",
            "echoashfallprotocol:stockpile_rations",
            "echoashfallprotocol:stockpile_route_supplies",
            "echoashfallprotocol:survey_reactor_ruin",
            "echoashfallprotocol:survive_core_countermeasure",
            "echoashfallprotocol:upgrade_drone_support",
            "echoashfallprotocol:upgrade_power_cable",
            "echoashfallprotocol:use_field_med_bay",
            "echoashfallprotocol:warm_up_after_exposure",
            "echoashfallprotocol:wasteland_surface_report",
            "echoblackboxprotocol:blackbox_vault",
            "echoblackboxprotocol:core_key",
            "echoblackboxprotocol:decode_memories",
            "echoblackboxprotocol:memory_bosses",
            "echoblackboxprotocol:nexus_guardian",
            "echoblackboxprotocol:truth_engine",
            "echoindustrialnexus:mission/clean_camp",
            "echoindustrialnexus:mission/control_heat",
            "echoindustrialnexus:mission/dense_alloy",
            "echoindustrialnexus:mission/factory_controller",
            "echoindustrialnexus:mission/filters_survival",
            "echoindustrialnexus:mission/grind_wasteland",
            "echoindustrialnexus:mission/hybrid_warning",
            "echoindustrialnexus:mission/production_survived",
            "echoindustrialnexus:mission/reactor_waste",
            "echoindustrialnexus:mission/reclaim_power",
            "echonexusprotocol:deleted_history",
            "echonexusprotocol:dirty_charge",
            "echonexusprotocol:quarantine_failed",
            "echonexusprotocol:reality_forge",
            "echonexusprotocol:stabilize_the_camp",
            "echonexusprotocol:the_core_door",
            "echonexusprotocol:the_monolith_remembers",
            "echonexusprotocol:the_signal_beneath",
            "echonexusprotocol:the_tower_still_speaks",
            "echonexusprotocol:what_rebuilds_the_world",
            "echoorbitalremnants:deep_space_protocol",
            "echoorbitalremnants:earth_calibration",
            "echoorbitalremnants:echo_zero",
            "echoorbitalremnants:europa_route",
            "echoorbitalremnants:faction_contract",
            "echoorbitalremnants:final_seal",
            "echoorbitalremnants:launch_chain",
            "echoorbitalremnants:low_orbit",
            "echoorbitalremnants:lunar_signal",
            "echoorbitalremnants:mars_route",
            "echoorbitalremnants:saturn_route",
            "echoorbitalremnants:station_network",
            "echoorbitalremnants:survey_network",
            "echoorbitalremnants:titan_route",
            "echostationfall:ai_override",
            "echostationfall:blackbox",
            "echostationfall:board_station",
            "echostationfall:decode_logs",
            "echostationfall:restore_power",
            "echostationfall:stabilize_sections",
            "echostationfall:station_mother",
            "minecraft:adventure/hero_of_the_village",
            "minecraft:adventure/kill_a_mob",
            "minecraft:adventure/kill_all_mobs",
            "minecraft:adventure/root",
            "minecraft:adventure/shoot_arrow",
            "minecraft:adventure/sleep_in_bed",
            "minecraft:adventure/trade",
            "minecraft:end/dragon_egg",
            "minecraft:end/elytra",
            "minecraft:end/enter_end_gateway",
            "minecraft:end/kill_dragon",
            "minecraft:end/root",
            "minecraft:husbandry/balanced_diet",
            "minecraft:husbandry/breed_an_animal",
            "minecraft:husbandry/plant_seed",
            "minecraft:husbandry/root",
            "minecraft:husbandry/safely_harvest_honey",
            "minecraft:husbandry/tame_an_animal",
            "minecraft:nether/all_effects",
            "minecraft:nether/all_potions",
            "minecraft:nether/brew_potion",
            "minecraft:nether/create_beacon",
            "minecraft:nether/find_bastion",
            "minecraft:nether/obtain_ancient_debris",
            "minecraft:nether/obtain_blaze_rod",
            "minecraft:nether/return_to_sender",
            "minecraft:nether/root",
            "minecraft:nether/summon_wither",
            "minecraft:story/enchant_item",
            "minecraft:story/enter_the_end",
            "minecraft:story/enter_the_nether",
            "minecraft:story/follow_ender_eye",
            "minecraft:story/iron_tools",
            "minecraft:story/lava_bucket",
            "minecraft:story/mine_diamond",
            "minecraft:story/mine_stone",
            "minecraft:story/root",
            "minecraft:story/smelt_iron",
            "minecraft:story/upgrade_tools");

    public static final Identifier TERMINAL_FRAME_BACKDROP = terminal("terminal_frame_backdrop");
    public static final Identifier OVERVIEW_PROTOCOL_DASHBOARD = terminal("overview_protocol_dashboard");
    public static final Identifier MISSIONS_VISUAL_HERO = terminal("missions_visual_hero");
    public static final Identifier MISSION_SURVIVAL = terminal("mission_survival");
    public static final Identifier MISSION_CRAFTING = terminal("mission_crafting");
    public static final Identifier MISSION_TECH = terminal("mission_tech");
    public static final Identifier MISSION_EXPLORATION = terminal("mission_exploration");
    public static final Identifier MISSION_COMBAT = terminal("mission_combat");
    public static final Identifier MISSION_STORY = terminal("mission_story");
    public static final Identifier MISSION_SIDE_OPS = terminal("mission_side_ops");
    public static final Identifier MISSION_ICON_SURVIVAL = icon("mission_survival");
    public static final Identifier MISSION_ICON_CRAFTING = icon("mission_crafting");
    public static final Identifier MISSION_ICON_TECH = icon("mission_tech");
    public static final Identifier MISSION_ICON_EXPLORATION = icon("mission_exploration");
    public static final Identifier MISSION_ICON_COMBAT = icon("mission_combat");
    public static final Identifier MISSION_ICON_STORY = icon("mission_story");
    public static final Identifier MISSION_ICON_SIDE_OPS = icon("mission_side_ops");
    public static final Identifier MISSION_ICON_HAZARD = icon("mission_hazard");
    public static final Identifier STATUS_HAZARD_SCAN = terminal("status_hazard_scan");
    public static final Identifier DRONE_COMMAND_LINK = terminal("drone_command_link");
    public static final Identifier ARCHIVES_DOSSIER_WALL = terminal("archives_dossier_wall");
    public static final Identifier CODEX_FIELD_MANUAL = terminal("codex_field_manual");
    public static final Identifier WORLD_ROUTE_MAP = terminal("world_route_map");
    public static final Identifier NEXUS_CORE_INTERFACE = terminal("nexus_core_interface");
    public static final Identifier ORBITAL_ROUTE_TELEMETRY = terminal("orbital_route_telemetry");
    public static final Identifier ADDONS_MODULE_GRID = terminal("addons_module_grid");
    public static final Identifier ICON_GROUP_PROTOCOL = terminalIcon("group_protocol");
    public static final Identifier ICON_GROUP_FIELD = terminalIcon("group_field");
    public static final Identifier ICON_GROUP_SYSTEMS = terminalIcon("group_systems");
    public static final Identifier ICON_GROUP_NEXUS = terminalIcon("group_nexus");
    public static final Identifier ICON_GROUP_ORBITAL = terminalIcon("group_orbital");
    public static final Identifier ICON_GROUP_CHAPTERS = terminalIcon("group_chapters");
    public static final Identifier ICON_PAGE_COMMAND_DECK = terminalIcon("page_command_deck");
    public static final Identifier ICON_PAGE_PROTOCOL_ROADMAP = terminalIcon("page_protocol_roadmap");
    public static final Identifier ICON_PAGE_SIGNAL_LEADS = terminalIcon("page_signal_leads");
    public static final Identifier ICON_PAGE_VITALS_SCAN = terminalIcon("page_vitals_scan");
    public static final Identifier ICON_PAGE_COMPANION_LINK = terminalIcon("page_companion_link");
    public static final Identifier ICON_PAGE_ROUTE_MAP = terminalIcon("page_route_map");
    public static final Identifier ICON_PAGE_FIELD_ARCHIVE = terminalIcon("page_field_archive");
    public static final Identifier ICON_PAGE_SURVIVAL_INDEX = terminalIcon("page_survival_index");
    public static final Identifier ICON_PAGE_BASELINE = terminalIcon("page_baseline");
    public static final Identifier ICON_PAGE_ORBITAL_COMMAND = terminalIcon("page_orbital_command");
    public static final Identifier ICON_PAGE_ROUTE_SURVEY = terminalIcon("page_route_survey");
    public static final Identifier ICON_PAGE_ECHO0_RECORDS = terminalIcon("page_echo0_records");
    public static final Identifier ICON_PAGE_NEXUS_CORE = terminalIcon("page_nexus_core");
    public static final Identifier ICON_PAGE_CHAPTERS = terminalIcon("page_chapters");
    public static final Identifier ICON_ACTION_VIEW = terminalIcon("action_view");
    public static final Identifier ICON_ACTION_TURN_IN = terminalIcon("action_turn_in");
    public static final Identifier ICON_ACTION_CLAIM = terminalIcon("action_claim");
    public static final Identifier ICON_ACTION_SCAN = terminalIcon("action_scan");
    public static final Identifier ICON_ACTION_OPEN_ROADMAP = terminalIcon("action_open_roadmap");
    public static final Identifier ICON_STATE_LOCKED = terminalIcon("state_locked");
    public static final Identifier ICON_STATE_ACTIVE = terminalIcon("state_active");
    public static final Identifier ICON_STATE_NEEDED = terminalIcon("state_needed");
    public static final Identifier ICON_STATE_OPEN = terminalIcon("state_open");
    public static final Identifier ICON_STATE_AVAILABLE = terminalIcon("state_available");
    public static final Identifier ICON_STATE_ONLINE = terminalIcon("state_online");
    public static final Identifier ICON_BRAND_ECHO = terminalIcon("brand_echo");
    public static final Identifier CARD_ACTIVE_PROTOCOL_HERO = card("active_protocol_hero");
    public static final Identifier CARD_MISSION_DETAIL_HEADER = card("mission_detail_header");
    public static final Identifier CARD_SIGNAL_DETAIL_HEADER = card("signal_detail_header");
    public static final Identifier CARD_ROUTE_STATUS_PANEL = card("route_status_panel");
    public static final Identifier CARD_NEXT_ACTION_PANEL = card("next_action_panel");
    public static final Identifier CARD_METRIC_TILE_PLATE = card("metric_tile_plate");
    public static final Identifier CARD_PANEL_LIST_COMPACT = card("panel_list_compact");
    public static final Identifier CARD_PANEL_DETAIL_STANDARD = card("panel_detail_standard");
    public static final Identifier CARD_PANEL_STATUS_HEALTH = card("panel_status_health");
    public static final Identifier CARD_PANEL_STATUS_SYNC = card("panel_status_sync");
    public static final Identifier CARD_PANEL_DRONE_COMMAND = card("panel_drone_command");
    public static final Identifier CARD_PANEL_ROUTE_MAP = card("panel_route_map");
    public static final Identifier CARD_PANEL_ARCHIVE_CODEX = card("panel_archive_codex");
    public static final Identifier CARD_PANEL_NEXUS_PATH = card("panel_nexus_path");
    public static final Identifier CARD_PANEL_CHAPTER_STATUS = card("panel_chapter_status");
    public static final Identifier CARD_PANEL_ORBITAL_COMMAND = card("panel_orbital_command");
    public static final Identifier CARD_FILTER_TOOLBAR_PLATE = card("filter_toolbar_plate");
    public static final Identifier CARD_EMPTY_STATE_PLATE = card("empty_state_plate");
    public static final Identifier CARD_ACTION_BAR_PLATE = card("action_bar_plate");

    private TerminalVisualAssets() {
    }

    public static Identifier terminal(String name) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "textures/gui/terminal/" + name + ".png");
    }

    public static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "textures/gui/icons/" + name + ".png");
    }

    public static Identifier terminalIcon(String name) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "textures/gui/icons/terminal/" + name + ".png");
    }

    public static Identifier card(String name) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "textures/gui/terminal/cards/" + name + ".png");
    }

    public static Identifier terminalGroupIcon(String group) {
        if (TerminalNavigationSection.COMMAND.key().equals(group)) {
            return ICON_GROUP_PROTOCOL;
        }
        if (TerminalNavigationSection.INTEL.key().equals(group)) {
            return ICON_GROUP_FIELD;
        }
        if (TerminalNavigationSection.SYSTEM.key().equals(group)) {
            return ICON_GROUP_SYSTEMS;
        }
        if (TerminalNavigationSection.CHAPTERS.key().equals(group)) {
            return ICON_GROUP_CHAPTERS;
        }
        if (TerminalNavigationSection.TERMINAL.key().equals(group)) {
            return ICON_GROUP_PROTOCOL;
        }
        if (TerminalNavigationSection.CORE.key().equals(group)) {
            return ICON_GROUP_FIELD;
        }
        if (TerminalTabChrome.GROUP_PROTOCOL.equals(group) || TerminalTabChrome.GROUP_CORE.equals(group)) {
            return ICON_GROUP_PROTOCOL;
        }
        if (TerminalTabChrome.GROUP_FIELD.equals(group)) {
            return ICON_GROUP_FIELD;
        }
        if (TerminalTabChrome.GROUP_SYSTEMS.equals(group)) {
            return ICON_GROUP_SYSTEMS;
        }
        if (TerminalTabChrome.GROUP_NEXUS.equals(group) || TerminalTabChrome.GROUP_ENDGAME.equals(group)) {
            return ICON_GROUP_NEXUS;
        }
        if (TerminalTabChrome.GROUP_ORBITAL.equals(group)) {
            return ICON_GROUP_ORBITAL;
        }
        if (TerminalTabChrome.GROUP_ADDONS.equals(group)) {
            return ICON_GROUP_CHAPTERS;
        }
        return ICON_BRAND_ECHO;
    }

    public static Identifier terminalPageIcon(String title) {
        String value = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (value.contains("command deck") || value.contains("overview")) {
            return ICON_PAGE_COMMAND_DECK;
        }
        if (value.contains("what now")) {
            return ICON_PAGE_SIGNAL_LEADS;
        }
        if (value.contains("protocol roadmap") || value.contains("mission")) {
            return ICON_PAGE_PROTOCOL_ROADMAP;
        }
        if (value.contains("signal lead")) {
            return ICON_PAGE_SIGNAL_LEADS;
        }
        if (value.contains("vitals") || value.contains("hazard")) {
            return ICON_PAGE_VITALS_SCAN;
        }
        if (value.contains("companion") || value.contains("drone")) {
            return ICON_PAGE_COMPANION_LINK;
        }
        if (value.contains("route map") || value.contains("poi")) {
            return ICON_PAGE_ROUTE_MAP;
        }
        if (value.contains("field archive") || value.contains("archive")) {
            return ICON_PAGE_FIELD_ARCHIVE;
        }
        if (value.contains("survival index") || value.contains("codex") || value.contains("recipe")) {
            return ICON_PAGE_SURVIVAL_INDEX;
        }
        if (value.contains("baseline") || value.contains("minecraft")) {
            return ICON_PAGE_BASELINE;
        }
        if (value.contains("orbital command")) {
            return ICON_PAGE_ORBITAL_COMMAND;
        }
        if (value.contains("route survey")) {
            return ICON_PAGE_ROUTE_SURVEY;
        }
        if (value.contains("echo-0") || value.contains("echo 0")) {
            return ICON_PAGE_ECHO0_RECORDS;
        }
        if (value.contains("nexus core") || value.contains("final path")) {
            return ICON_PAGE_NEXUS_CORE;
        }
        if (value.contains("chapter") || value.contains("addon")) {
            return ICON_PAGE_CHAPTERS;
        }
        if (value.contains("settings")) {
            return ICON_GROUP_SYSTEMS;
        }
        return null;
    }

    public static Identifier missionIconArt(Identifier missionId, String category) {
        if (missionId != null && hasBundledMissionIcon(missionId)) {
            return Identifier.fromNamespaceAndPath(EchoTerminal.MODID,
                    "textures/gui/mission_icons/" + missionId.getNamespace() + "/" + missionId.getPath() + ".png");
        }
        return missionCategoryIcon(category);
    }

    public static Identifier missionHeroArt(Identifier missionId, String category) {
        if (missionId != null && MISSION_HERO_IDS.contains(missionId.toString())) {
            return Identifier.fromNamespaceAndPath(EchoTerminal.MODID,
                    "textures/gui/mission_heroes/" + missionId.getNamespace() + "/" + missionId.getPath() + ".png");
        }
        return missionCategoryArt(category);
    }

    private static boolean hasBundledMissionIcon(Identifier missionId) {
        return missionId != null && MISSION_ICON_IDS.contains(missionId.toString());
    }

    public static Identifier missionCategoryArt(String category) {
        String key = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (key.contains("survival") || key.contains("water") || key.contains("radiation")) {
            return MISSION_SURVIVAL;
        }
        if (key.contains("craft") || key.contains("machine") || key.contains("recipe")) {
            return MISSION_CRAFTING;
        }
        if (key.contains("tech") || key.contains("research") || key.contains("power") || key.contains("grid")) {
            return MISSION_TECH;
        }
        if (key.contains("explor") || key.contains("world") || key.contains("route") || key.contains("poi")) {
            return MISSION_EXPLORATION;
        }
        if (key.contains("combat") || key.contains("guardian") || key.contains("warden") || key.contains("boss")) {
            return MISSION_COMBAT;
        }
        if (key.contains("story") || key.contains("nexus") || key.contains("archive")) {
            return MISSION_STORY;
        }
        return MISSION_SIDE_OPS;
    }

    public static Identifier missionCategoryIcon(String category) {
        String key = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (key.contains("hazard") || key.contains("weather") || key.contains("storm") || key.contains("biome")) {
            return MISSION_ICON_HAZARD;
        }
        if (key.contains("survival") || key.contains("water") || key.contains("radiation")) {
            return MISSION_ICON_SURVIVAL;
        }
        if (key.contains("craft") || key.contains("machine") || key.contains("recipe")) {
            return MISSION_ICON_CRAFTING;
        }
        if (key.contains("tech") || key.contains("research") || key.contains("power") || key.contains("grid")) {
            return MISSION_ICON_TECH;
        }
        if (key.contains("explor") || key.contains("world") || key.contains("route") || key.contains("poi")) {
            return MISSION_ICON_EXPLORATION;
        }
        if (key.contains("combat") || key.contains("guardian") || key.contains("warden") || key.contains("boss")) {
            return MISSION_ICON_COMBAT;
        }
        if (key.contains("story") || key.contains("nexus") || key.contains("archive")) {
            return MISSION_ICON_STORY;
        }
        return MISSION_ICON_SIDE_OPS;
    }
}
