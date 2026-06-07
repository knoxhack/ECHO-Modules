package com.knoxhack.echo.adaptercore;

import java.util.Locale;
import java.util.Map;

public final class EchoCanonicalContentIds {
    public static final String ASHFALL_NAMESPACE = "echoashfallprotocol";
    public static final String ADAPTERCORE_NAMESPACE = "echoadaptercore";

    public static final String EVENT_PLAYER_SPAWNED = "player.spawned";
    public static final String EVENT_PLAYER_ITEM_USED = "player.item_used";
    public static final String EVENT_PLAYER_ITEM_COLLECTED = "player.item_collected";
    public static final String EVENT_PLAYER_RECIPE_CRAFTED = "player.recipe_crafted";
    public static final String EVENT_PLAYER_BLOCK_PLACED = "player.block_placed";
    public static final String EVENT_PLAYER_REGION_ENTERED = "player.region_entered";
    public static final String EVENT_PLAYER_TERMINAL_OPENED = "player.terminal_opened";
    public static final String EVENT_PLAYER_SCANNER_USED = "player.scanner_used";
    public static final String EVENT_PLAYER_MACHINE_POWERED = "player.machine_powered";
    public static final String EVENT_MACHINE_OUTPUT_CREATED = "machine.output_created";
    public static final String EVENT_HAZARD_SURVIVED = "hazard.survived";
    public static final String EVENT_MISSION_OBJECTIVE_COMPLETED = "mission.objective_completed";
    public static final String EVENT_MISSION_COMPLETED = "mission.completed";
    public static final String EVENT_PLAYER_ITEM_OBTAINED = EVENT_PLAYER_ITEM_COLLECTED;
    public static final String EVENT_PLAYER_ITEM_CONSUMED = EVENT_PLAYER_ITEM_USED;
    public static final String EVENT_ASHFALL_SPECIAL_MARKER = "ashfall.special_marker";
    public static final String EVENT_ASHFALL_SCANNER_USED = EVENT_PLAYER_SCANNER_USED;
    public static final String EVENT_ASHFALL_POI_DISCOVERED = "ashfall.poi_discovered";
    public static final String EVENT_ASHFALL_CACHE_OPENED = EVENT_PLAYER_TERMINAL_OPENED;
    public static final String EVENT_ASHFALL_DATA_LOG_RECOVERED = "ashfall.data_log_recovered";
    public static final String EVENT_ASHFALL_FACTION_ACTION = "ashfall.faction_action";
    public static final String EVENT_ASHFALL_REPUTATION_UPDATED = "ashfall.reputation_updated";
    public static final String EVENT_ASHFALL_DRONE_STATE = "ashfall.drone_state";
    public static final String EVENT_ASHFALL_PERK_UNLOCKED = "ashfall.perk_unlocked";
    public static final String EVENT_ASHFALL_RESEARCH_UPDATED = "ashfall.research_updated";
    public static final String EVENT_ASHFALL_SCHEMATIC_UNLOCKED = "ashfall.schematic_unlocked";
    public static final String EVENT_ASHFALL_RADIATION_CHANGED = "ashfall.radiation_changed";
    public static final String EVENT_ASHFALL_MUTATION_GAINED = "ashfall.mutation_gained";
    public static final String EVENT_ASHFALL_TREATMENT_APPLIED = "ashfall.treatment_applied";
    public static final String EVENT_ASHFALL_MED_BAY_USED = "ashfall.med_bay_used";
    public static final String EVENT_ASHFALL_CLEANSER_USED = "ashfall.cleanser_used";
    public static final String EVENT_ASHFALL_SCRUBBER_USED = "ashfall.scrubber_used";
    public static final String EVENT_ASHFALL_LAB_OBJECTIVE = "ashfall.lab_objective";
    public static final String EVENT_ASHFALL_VAULT_OBJECTIVE = "ashfall.vault_objective";
    public static final String EVENT_ASHFALL_HAZARD_ROUTE_CHECK = "ashfall.hazard_route_check";
    public static final String EVENT_ASHFALL_BOSS_DEFEATED = "ashfall.boss_defeated";
    public static final String EVENT_ASHFALL_RELAY_ACTIVATED = "ashfall.relay_activated";
    public static final String EVENT_ASHFALL_POWER_NODE_STATE = EVENT_PLAYER_MACHINE_POWERED;
    public static final String EVENT_ASHFALL_SCOUT_DRONE_ROUTE = "ashfall.scout_drone_route";
    public static final String EVENT_ASHFALL_NEXUS_CAPACITOR_STATE = "ashfall.nexus_capacitor_state";
    public static final String EVENT_ASHFALL_NEXUS_STATE = "ashfall.nexus_state";
    public static final String EVENT_ASHFALL_PRIME_RELAY_RESOLVED = "ashfall.prime_relay_resolved";
    public static final String EVENT_ASHFALL_ENDING_CHOICE = "ashfall.ending_choice";
    public static final String EVENT_ASHFALL_POST_NEXUS_PERSISTED = "ashfall.post_nexus_persisted";

    public static final String ITEM_CLEAN_WATER_BOTTLE = ASHFALL_NAMESPACE + ":clean_water_bottle";
    public static final String ITEM_DIRTY_WATER_BOTTLE = ASHFALL_NAMESPACE + ":dirty_water_bottle";
    public static final String ITEM_FILTERED_WATER_BOTTLE = ASHFALL_NAMESPACE + ":filtered_water_bottle";
    public static final String ITEM_BOILED_WATER_BOTTLE = ASHFALL_NAMESPACE + ":boiled_water_bottle";
    public static final String ITEM_RAD_AWAY = ASHFALL_NAMESPACE + ":rad_away";
    public static final String ITEM_BANDAGE = ASHFALL_NAMESPACE + ":bandage";
    public static final String ITEM_STIM_PACK = ASHFALL_NAMESPACE + ":stim_pack";
    public static final String ITEM_MUTAGEN_VIAL = ASHFALL_NAMESPACE + ":mutagen_vial";
    public static final String ITEM_INSTABILITY_DAMPENER = ASHFALL_NAMESPACE + ":instability_dampener";
    public static final String ITEM_RETURN_BEACON = ASHFALL_NAMESPACE + ":return_beacon";
    public static final String ITEM_HAND_WARMER = ASHFALL_NAMESPACE + ":hand_warmer";
    public static final String ITEM_CRUDE_FILTER = ASHFALL_NAMESPACE + ":crude_filter";
    public static final String ITEM_FIELD_MANUAL = ASHFALL_NAMESPACE + ":field_manual";
    public static final String ITEM_PORTABLE_SIGNAL_SCANNER = ASHFALL_NAMESPACE + ":portable_signal_scanner";
    public static final String ITEM_SCOUT_DRONE_ITEM = ASHFALL_NAMESPACE + ":scout_drone_item";
    public static final String ITEM_RARE_TECH_SCHEMATIC = ASHFALL_NAMESPACE + ":rare_tech_schematic";
    public static final String ITEM_FILTER_CARTRIDGE_BASIC = ASHFALL_NAMESPACE + ":filter_cartridge_basic";
    public static final String ITEM_FILTER_CARTRIDGE_ADVANCED = ASHFALL_NAMESPACE + ":filter_cartridge_advanced";
    public static final String ITEM_FILTER_CARTRIDGE_ELITE = ASHFALL_NAMESPACE + ":filter_cartridge_elite";
    public static final String ITEM_GAS_MASK = ASHFALL_NAMESPACE + ":gas_mask";
    public static final String BLOCK_SIGNAL_SCANNER = ASHFALL_NAMESPACE + ":signal_scanner";
    public static final String BLOCK_RELAY_STATION = ASHFALL_NAMESPACE + ":relay_station";
    public static final String BLOCK_POWER_NODE = ASHFALL_NAMESPACE + ":power_node";
    public static final String BLOCK_FIELD_MED_BAY = ASHFALL_NAMESPACE + ":field_med_bay";
    public static final String BLOCK_RADIATION_CLEANSER = ASHFALL_NAMESPACE + ":radiation_cleanser";
    public static final String BLOCK_ATMOSPHERIC_SCRUBBER = ASHFALL_NAMESPACE + ":atmospheric_scrubber";

    public static final Map<String, String> PLAYER_ACTION_EVENTS = Map.of(
            "item_used", EVENT_PLAYER_ITEM_USED,
            "item_collected", EVENT_PLAYER_ITEM_COLLECTED,
            "recipe_crafted", EVENT_PLAYER_RECIPE_CRAFTED,
            "block_placed", EVENT_PLAYER_BLOCK_PLACED,
            "region_entered", EVENT_PLAYER_REGION_ENTERED,
            "terminal_opened", EVENT_PLAYER_TERMINAL_OPENED,
            "scanner_used", EVENT_PLAYER_SCANNER_USED,
            "machine_powered", EVENT_PLAYER_MACHINE_POWERED);

    private EchoCanonicalContentIds() {
    }

    public static String id(String namespace, String path) {
        namespace = normalizePart(namespace, "content namespace");
        path = normalizePart(path, "content path");
        return namespace + ":" + path;
    }

    public static String normalizeContentId(String value) {
        value = AdapterContractGuards.requireText(value, "canonical content id").toLowerCase(Locale.ROOT);
        return value.contains(":") ? value : ASHFALL_NAMESPACE + ":" + value;
    }

    public static String normalizeEventName(String value) {
        return AdapterContractGuards.requireText(value, "canonical event name").toLowerCase(Locale.ROOT);
    }

    private static String normalizePart(String value, String fieldName) {
        return AdapterContractGuards.requireText(value, fieldName)
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');
    }
}
