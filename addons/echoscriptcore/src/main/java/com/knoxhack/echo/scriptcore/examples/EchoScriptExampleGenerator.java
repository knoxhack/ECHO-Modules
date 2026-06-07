package com.knoxhack.echo.scriptcore.examples;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoScriptExampleGenerator {
    private EchoScriptExampleGenerator() {
    }

    public static void generateIfEnabled(Path scriptsRoot) {
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.GENERATE_EXAMPLES, true)) {
            return;
        }
        generate(scriptsRoot, false, true);
    }

    public static void generateOfficialExamples(Path scriptsRoot, boolean overwriteExisting) {
        generate(scriptsRoot, overwriteExisting, false);
    }

    private static void generate(Path scriptsRoot, boolean overwriteExisting, boolean respectConfig) {
        Map<String, String> files = new LinkedHashMap<>();
        if (!respectConfig || ScriptCoreConfig.bool(ScriptCoreConfig.GENERATE_PUBLIC_EXAMPLES, true)) {
            generic(files);
            tech(files);
        }
        if (!respectConfig || ScriptCoreConfig.bool(ScriptCoreConfig.GENERATE_ASHFALL_EXAMPLES, true)) {
            ashfall(files);
        }
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path target = scriptsRoot.resolve(entry.getKey()).toAbsolutePath().normalize();
            if (!target.startsWith(scriptsRoot.toAbsolutePath().normalize())) {
                EchoScriptCore.LOGGER.warn("Skipped unsafe ScriptCore example path {}.", target);
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                if (overwriteExisting || !Files.exists(target)) {
                    Files.writeString(target, entry.getValue());
                }
            } catch (IOException exception) {
                EchoScriptCore.LOGGER.warn("Could not write ScriptCore example {}.", target, exception);
            }
        }
    }

    private static void generic(Map<String, String> files) {
        String base = "examples/generic_survival/";
        files.put(base + "missions/repair_radio.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:repair_radio",
                  "type": "mission",
                  "title": "Repair the Radio",
                  "route": "survival",
                  "phase": "first_contact",
                  "role": "main",
                  "briefing": "Restore a broken radio with basic salvage.",
                  "objectives": [
                    { "id": "iron", "type": "collect_item", "title": "Collect iron ingots", "item": "minecraft:iron_ingot", "count": 4 },
                    { "id": "redstone", "type": "collect_item", "title": "Collect redstone dust", "item": "minecraft:redstone", "count": 2 }
                  ],
                  "rewards": [
                    { "type": "unlock_archive_entry", "entry": "generic_survival:first_signal" },
                    { "type": "add_holomap_marker", "marker": "generic_survival:shelter_marker" },
                    { "type": "set_world_state", "state": "generic_survival:radio_restored" }
                  ],
                  "on_complete": [
                    { "type": "unlock_archive_entry", "entry": "generic_survival:first_signal" },
                    { "type": "set_world_state", "state": "generic_survival:radio_restored" }
                  ]
                }
                """);
        files.put(base + "actions/radio_choice.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:radio_choice",
                  "type": "generic",
                  "title": "Radio Choice UI Action",
                  "metadata": {
                    "screencore_ui": {
                      "params": [
                        { "id": "choice_id", "type": "string", "required": true, "pattern": "[a-z0-9_]+" }
                      ]
                    }
                  },
                  "actions": [
                    { "type": "set_branch_marker", "value": "{param.choice_id}" }
                  ]
                }
                """);
        files.put(base + "archive/first_signal.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:first_signal",
                  "type": "archive_entry",
                  "category": "signals",
                  "title": "First Signal",
                  "subtitle": "A faint broadcast repeats at dusk.",
                  "content": ["The repaired radio catches a short emergency loop.", "Someone marked a shelter beyond the ridge."],
                  "importance": "important"
                }
                """);
        files.put(base + "lens/broken_generator_scan.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:broken_generator_scan",
                  "type": "lens_scan",
                  "target": "minecraft:furnace",
                  "target_type": "block",
                  "title": "Broken Generator",
                  "summary": "A placeholder scan for a damaged power source.",
                  "details": ["Needs conductive material and a small redstone signal."]
                }
                """);
        files.put(base + "holomap/shelter_marker.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:shelter_marker",
                  "type": "holomap_marker",
                  "title": "Emergency Shelter",
                  "x": 128,
                  "y": 72,
                  "z": -96,
                  "dimension": "minecraft:overworld",
                  "icon": "minecraft:bread",
                  "layer": "generic_survival:shelters"
                }
                """);
        files.put(base + "weather/electrical_storm.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:electrical_storm",
                  "type": "weather_event",
                  "title": "Electrical Storm",
                  "duration_ticks": 2400,
                  "warning_seconds": 30,
                  "terminal_warning": "Electrical storm approaching. Shelter recommended.",
                  "effects": [{ "type": "play_sound", "sound": "minecraft:entity.lightning_bolt.thunder" }]
                }
                """);
        files.put(base + "factions/settlers.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:settlers",
                  "type": "faction",
                  "display_name": "Settlers",
                  "description": "A practical group trying to make the valley livable.",
                  "starting_reputation": 0,
                  "ranks": [
                    { "name": "Stranger", "min": -100, "max": 9 },
                    { "name": "Neighbor", "min": 10, "max": 49 },
                    { "name": "Trusted", "min": 50 }
                  ]
                }
                """);
        files.put(base + "world_state/radio_restored.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:radio_restored",
                  "type": "world_state",
                  "title": "Radio Restored",
                  "set_by": [{ "type": "mission_complete", "mission": "generic_survival:repair_radio" }]
                }
                """);
        files.put(base + "tutorials/first_storm_warning.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:first_storm_warning",
                  "type": "tutorial_hint",
                  "title": "Storm Warning",
                  "message": "Storms can change routes and visibility. Find shelter before nightfall.",
                  "priority": 10,
                  "once": true
                }
                """);
        files.put(base + "endings/safe_haven_ending.json", """
                {
                  "schema_version": 1,
                  "pack": "generic_survival",
                  "id": "generic_survival:safe_haven_ending",
                  "type": "ending",
                  "title": "Safe Haven",
                  "description": "The shelter network is online.",
                  "priority": 10,
                  "conditions": [{ "type": "world_state_set", "state": "generic_survival:radio_restored" }],
                  "terminal_summary": "A stable settlement route is possible."
                }
                """);
    }

    private static void tech(Map<String, String> files) {
        String base = "examples/tech_progression/";
        files.put(base + "missions/build_first_machine.json", """
                {
                  "schema_version": 1,
                  "pack": "tech_progression",
                  "id": "tech_progression:build_first_machine",
                  "type": "mission",
                  "title": "Build the First Machine",
                  "route": "automation",
                  "phase": "power_intro",
                  "role": "main",
                  "objectives": [
                    { "id": "craft_furnace", "type": "craft_item", "title": "Craft a furnace", "item": "minecraft:furnace", "count": 1 },
                    { "id": "obtain_redstone", "type": "obtain_item", "title": "Obtain redstone", "item": "minecraft:redstone", "count": 4 }
                  ],
                  "on_complete": [{ "type": "set_world_state", "state": "tech_progression:grid_online" }]
                }
                """);
        files.put(base + "archive/machine_manual.json", """
                {
                  "schema_version": 1,
                  "pack": "tech_progression",
                  "id": "tech_progression:machine_manual",
                  "type": "archive_entry",
                  "category": "manuals",
                  "title": "Basic Machine Manual",
                  "content": ["Use this as a placeholder for your own tech mod IDs.", "Keep optional mod IDs in metadata until your pack requires them."]
                }
                """);
        files.put(base + "recipes/basic_generator_unlock.json", """
                {
                  "schema_version": 1,
                  "pack": "tech_progression",
                  "id": "tech_progression:basic_generator_unlock",
                  "type": "recipe_unlock",
                  "recipe": "minecraft:furnace",
                  "title": "Basic Generator Placeholder",
                  "unlock_conditions": [{ "type": "mission_complete", "mission": "tech_progression:build_first_machine" }],
                  "metadata": { "intended_mod": "exampletech" }
                }
                """);
        files.put(base + "tutorials/power_intro.json", """
                {
                  "schema_version": 1,
                  "pack": "tech_progression",
                  "id": "tech_progression:power_intro",
                  "type": "tutorial_hint",
                  "title": "Power Intro",
                  "message": "Machines become clearer when a pack defines recipe unlocks and world states together.",
                  "priority": 5,
                  "once": true
                }
                """);
        files.put(base + "world_state/grid_online.json", """
                {
                  "schema_version": 1,
                  "pack": "tech_progression",
                  "id": "tech_progression:grid_online",
                  "type": "world_state",
                  "title": "Grid Online"
                }
                """);
    }

    private static void ashfall(Map<String, String> files) {
        String base = "examples/ashfall/";
        files.put(base + "missions/repair_field_terminal.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:repair_field_terminal",
                  "type": "mission",
                  "title": "Repair Field Terminal",
                  "route": "ashfall_example",
                  "phase": "field_recovery",
                  "role": "main",
                  "objectives": [
                    { "id": "scrap_circuit", "type": "collect_item", "title": "Find a scrap circuit", "item": "echo:scrap_circuit", "count": 1 },
                    { "id": "field_battery", "type": "collect_item", "title": "Find a field battery", "item": "echo:field_battery", "count": 1 }
                  ],
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "archive/first_boot_log.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:first_boot_log",
                  "type": "archive_entry",
                  "category": "example_logs",
                  "title": "First Boot Log",
                  "content": ["Example-only Ashfall-flavored archive entry.", "Do not treat this as required ScriptCore content."],
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "lens/broken_cryo_pod_scan.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:broken_cryo_pod_scan",
                  "type": "lens_scan",
                  "target": "echo:broken_cryo_pod",
                  "target_type": "block",
                  "title": "Broken Cryo Pod",
                  "summary": "Example-only Ashfall scan.",
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "holomap/hazard_layer.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:hazard_layer",
                  "type": "holomap_layer",
                  "title": "Hazards",
                  "locked_by_default": true,
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "weather/em_blackout.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:em_blackout",
                  "type": "weather_event",
                  "title": "EM Blackout",
                  "duration_ticks": 1800,
                  "warning_seconds": 20,
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "factions/reclaimers.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:reclaimers",
                  "type": "faction",
                  "display_name": "Reclaimers",
                  "starting_reputation": 0,
                  "ranks": [{ "name": "Unknown", "min": 0 }],
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "world_state/relay_alpha_restored.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:relay_alpha_restored",
                  "type": "world_state",
                  "title": "Relay Alpha Restored",
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "tutorials/first_radiation_warning.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:first_radiation_warning",
                  "type": "tutorial_hint",
                  "title": "Radiation Warning",
                  "message": "Example-only warning for an Ashfall pack.",
                  "metadata": { "example_only": true }
                }
                """);
        files.put(base + "endings/reclamation_ending.json", """
                {
                  "schema_version": 1,
                  "pack": "ashfall",
                  "id": "ashfall:reclamation_ending",
                  "type": "ending",
                  "title": "Reclamation",
                  "conditions": [{ "type": "world_state_set", "state": "ashfall:relay_alpha_restored" }],
                  "metadata": { "example_only": true }
                }
                """);
    }
}
