package com.knoxhack.echo.scriptcore.validation;

import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoEndingDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapLayerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapMarkerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLensScanDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLootProfileDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoObjective;
import com.knoxhack.echo.scriptcore.model.EchoRecipeUnlockDefinition;
import com.knoxhack.echo.scriptcore.model.EchoReward;
import com.knoxhack.echo.scriptcore.model.EchoTutorialHintDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWeatherEventDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class EchoScriptValidator {
    public static final EchoScriptValidator INSTANCE = new EchoScriptValidator();

    private EchoScriptValidator() {
    }

    public List<EchoScriptDiagnostic> validate(Collection<? extends EchoScriptDefinitionView> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of(EchoScriptDiagnostic.info("SCRIPTCORE_EMPTY", "No ScriptCore definitions were supplied."));
        }

        List<EchoScriptDiagnostic> diagnostics = new ArrayList<>();
        Map<Identifier, EchoScriptDefinitionView> byId = new HashMap<>();
        Set<Identifier> missionIds = new HashSet<>();
        Map<String, Set<Identifier>> idsByType = new HashMap<>();

        for (EchoScriptDefinitionView definition : definitions) {
            if (definition == null) {
                diagnostics.add(error("SCRIPTCORE_NULL_DEFINITION", "A null ScriptCore definition was ignored.",
                        Optional.empty(), Optional.empty(), "$", "Remove the null definition source."));
                continue;
            }
            validateEnvelope(definition, diagnostics);
            if (definition.id() != null) {
                EchoScriptDefinitionView previous = byId.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    diagnostics.add(error("SCRIPTCORE_DUPLICATE_ID", "Duplicate ScriptCore definition id: " + definition.id(),
                            definition.sourceFile(), Optional.of(definition.id()), "$.id",
                            "Give each definition a unique namespaced id."));
                }
                if ("mission".equals(definition.type())) {
                    missionIds.add(definition.id());
                }
                idsByType.computeIfAbsent(definition.type(), ignored -> new HashSet<>()).add(definition.id());
            }
        }

        Map<Identifier, Set<Identifier>> missionPrerequisites = new HashMap<>();
        for (EchoScriptDefinitionView definition : definitions) {
            if (definition == null) {
                continue;
            }
            validateConditions(definition, definition.unlockConditions(), "$.unlock_conditions", diagnostics, missionIds, idsByType);
            validateConditions(definition, definition.conditions(), "$.conditions", diagnostics, missionIds, idsByType);
            validateActions(definition, definition.actions(), "$.actions", diagnostics, missionIds, idsByType);
            validateTypedDefinition(definition, diagnostics, missionIds, idsByType, missionPrerequisites);
        }
        validateMissionCycles(byId, missionPrerequisites, diagnostics);
        return List.copyOf(diagnostics);
    }

    private static void validateEnvelope(EchoScriptDefinitionView definition, List<EchoScriptDiagnostic> diagnostics) {
        if (definition.id() == null) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "A ScriptCore definition is missing id.",
                    definition.sourceFile(), Optional.empty(), "$.id", "Add a namespaced id such as example:repair_radio."));
        }
        if (definition.schemaVersion() <= 0) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Definition " + safeId(definition)
                            + " is missing schema_version.",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), "$.schema_version",
                    "Set schema_version to 1."));
        }
        if (definition.type() == null || definition.type().isBlank()) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Definition " + safeId(definition)
                            + " is missing type.",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), "$.type",
                    "Set type to a known ScriptCore definition type."));
        } else if (!EchoScriptKnownTypes.DEFINITION_TYPES.contains(definition.type())) {
            diagnostics.add(error("SCRIPTCORE_UNKNOWN_TYPE", "Definition " + safeId(definition)
                            + " uses unknown type " + definition.type() + ".",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), "$.type",
                    "Use a supported type or add an adapter for the custom type."));
        }
        if (definition.pack() == null || definition.pack().isBlank() || "unknown".equals(definition.pack())) {
            diagnostics.add(warning("SCRIPTCORE_UNKNOWN_PACK", "Definition " + safeId(definition)
                            + " does not declare a clear pack id.",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), "$.pack",
                    "Add pack to make dashboard grouping predictable."));
        }
    }

    private static void validateTypedDefinition(
            EchoScriptDefinitionView definition,
            List<EchoScriptDiagnostic> diagnostics,
            Set<Identifier> missionIds,
            Map<String, Set<Identifier>> idsByType,
            Map<Identifier, Set<Identifier>> missionPrerequisites) {
        if (definition instanceof EchoMissionDefinition mission) {
            Set<Identifier> prerequisites = new HashSet<>();
            collectMissionReferences(mission.prerequisites(), prerequisites);
            missionPrerequisites.put(mission.id(), prerequisites);
            validateConditions(definition, mission.prerequisites(), "$.prerequisites", diagnostics, missionIds, idsByType);
            validateActions(definition, mission.onStart(), "$.on_start", diagnostics, missionIds, idsByType);
            validateActions(definition, mission.onComplete(), "$.on_complete", diagnostics, missionIds, idsByType);
            validateActions(definition, mission.onFail(), "$.on_fail", diagnostics, missionIds, idsByType);
            validateObjectives(mission, diagnostics);
            validateRewards(mission, diagnostics, idsByType);
        }
        if (definition instanceof EchoWeatherEventDefinition weather && weather.durationTicks() <= 0) {
            diagnostics.add(error("SCRIPTCORE_INVALID_WEATHER_DURATION", "Weather event " + safeId(definition)
                            + " must have duration_ticks greater than zero.",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), "$.duration_ticks",
                    "Set duration_ticks to a positive number."));
        }
        if (definition instanceof EchoWeatherEventDefinition weather) {
            validateActions(definition, weather.effects(), "$.effects", diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoFactionDefinition faction) {
            validateFaction(faction, diagnostics);
        }
        if (definition instanceof EchoArchiveEntryDefinition archive) {
            validateArchive(archive, diagnostics, idsByType);
        }
        if (definition instanceof EchoLensScanDefinition lens) {
            validateLens(lens, diagnostics);
        }
        if (definition instanceof EchoHoloMapLayerDefinition layer) {
            for (int i = 0; i < layer.markers().size(); i++) {
                validateMarker(layer.markers().get(i), "$.markers[" + i + "]", diagnostics, idsByType);
            }
        }
        if (definition instanceof EchoHoloMapMarkerDefinition marker) {
            validateMarker(marker, "$", diagnostics, idsByType);
        }
        if (definition instanceof EchoWorldStateDefinition worldState) {
            validateConditions(definition, worldState.setBy(), "$.set_by", diagnostics, missionIds, idsByType);
            validateActions(definition, worldState.effects(), "$.effects", diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoTutorialHintDefinition tutorial) {
            if (tutorial.message().isBlank()) {
                diagnostics.add(warning("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Tutorial hint " + safeId(definition)
                                + " has no message.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), "$.message",
                        "Add the tutorial hint message."));
            }
            validateConditions(definition, tutorial.triggerConditions(), "$.trigger_conditions", diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoDialogueDefinition dialogue) {
            validateDialogue(dialogue, diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoEndingDefinition ending) {
            if (ending.endingConditions().isEmpty()) {
                diagnostics.add(warning("SCRIPTCORE_BROKEN_CONDITION_REFERENCE", "Ending " + safeId(definition)
                                + " has no conditions and may trigger immediately.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), "$.conditions",
                        "Add ending conditions such as world_state_set."));
            }
            validateConditions(definition, ending.endingConditions(), "$.conditions", diagnostics, missionIds, idsByType);
            validateActions(definition, ending.endingActions(), "$.actions", diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoRecipeUnlockDefinition recipe) {
            if (recipe.recipe().isEmpty()) {
                diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Recipe unlock " + safeId(definition)
                                + " is missing recipe.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), "$.recipe",
                        "Add a recipe id."));
            }
            validateConditions(definition, recipe.recipeUnlockConditions(), "$.unlock_conditions", diagnostics, missionIds, idsByType);
            validateActions(definition, recipe.recipeActions(), "$.actions", diagnostics, missionIds, idsByType);
        }
        if (definition instanceof EchoLootProfileDefinition loot) {
            if (loot.table().isEmpty()) {
                diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Loot profile " + safeId(definition)
                                + " is missing table.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), "$.table",
                        "Add a loot table id."));
            }
            validateConditions(definition, loot.lootUnlockConditions(), "$.unlock_conditions", diagnostics, missionIds, idsByType);
        }
    }

    private static void validateObjectives(EchoMissionDefinition mission, List<EchoScriptDiagnostic> diagnostics) {
        Set<String> ids = new HashSet<>();
        int index = 0;
        for (EchoObjective objective : mission.objectives()) {
            String path = "$.objectives[" + index + "]";
            if (!EchoScriptKnownTypes.OBJECTIVE_TYPES.contains(objective.type())) {
                diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                + " has unknown objective type " + objective.type() + ".",
                        mission.sourceFile(), Optional.of(mission.id()), path + ".type",
                        "Use a supported objective type or custom."));
            }
            switch (objective.type()) {
                case "collect_item", "craft_item", "obtain_item", "scan_item" -> {
                    if (objective.item().isEmpty() && objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " uses type " + objective.type()
                                        + " but is missing item.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".item",
                                "Add \"item\": \"minecraft:iron_ingot\"."));
                    }
                }
                case "scan_block", "interact_block" -> {
                    if (objective.block().isEmpty() && objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " is missing block.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".block",
                                "Add \"block\": \"minecraft:furnace\"."));
                    }
                }
                case "scan_entity", "kill_entity" -> {
                    if (objective.entity().isEmpty() && objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " is missing entity.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".entity",
                                "Add \"entity\": \"minecraft:zombie\"."));
                    }
                }
                case "visit_poi" -> {
                    if (objective.poi().isEmpty() && objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " is missing poi.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".poi",
                                "Add a poi id."));
                    }
                }
                case "enter_region" -> {
                    if (objective.region().isEmpty() && objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " is missing region.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".region",
                                "Add a region id."));
                    }
                }
                case "complete_mission", "set_world_state", "survive_weather" -> {
                    if (objective.target().isEmpty()) {
                        diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "Mission " + mission.id()
                                        + " objective " + objective.id() + " is missing target.",
                                mission.sourceFile(), Optional.of(mission.id()), path + ".target",
                                "Add a target id."));
                    }
                }
                default -> {
                }
            }
            if (!ids.add(objective.id())) {
                diagnostics.add(warning("SCRIPTCORE_DUPLICATE_OBJECTIVE", "Mission " + mission.id()
                                + " has duplicate objective id " + objective.id() + ".",
                        mission.sourceFile(), Optional.of(mission.id()), path + ".id",
                        "Give each objective in the mission a unique id."));
            }
            index++;
        }
    }

    private static void validateRewards(
            EchoMissionDefinition mission,
            List<EchoScriptDiagnostic> diagnostics,
            Map<String, Set<Identifier>> idsByType) {
        int index = 0;
        for (EchoReward reward : mission.rewards()) {
            String path = "$.rewards[" + index + "]";
            switch (reward.type()) {
                case "item", "give_item" -> require(reward.item().isPresent(), diagnostics, mission,
                        "SCRIPTCORE_INVALID_REWARD", path + ".item", "Reward " + reward.type() + " is missing item.",
                        "Add an item id.");
                case "unlock_mission", "start_mission", "complete_mission" -> checkOptionalRef(reward.mission(), "mission", mission,
                        diagnostics, idsByType, path + ".mission", "SCRIPTCORE_BROKEN_MISSION_REFERENCE");
                case "unlock_lore", "unlock_archive_entry" -> checkOptionalRef(reward.entry(), "archive_entry", mission,
                        diagnostics, idsByType, path + ".entry", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "unlock_holomap_layer" -> checkOptionalRef(reward.layer(), "holomap_layer", mission,
                        diagnostics, idsByType, path + ".layer", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "add_holomap_marker" -> checkOptionalRef(reward.marker(), "holomap_marker", mission,
                        diagnostics, idsByType, path + ".marker", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "set_world_state", "clear_world_state" -> checkOptionalRef(reward.state(), "world_state", mission,
                        diagnostics, idsByType, path + ".state", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "play_sound" -> require(reward.sound().isPresent(), diagnostics, mission,
                        "SCRIPTCORE_INVALID_REWARD", path + ".sound", "Reward play_sound is missing sound.",
                        "Add a sound id.");
                default -> {
                }
            }
            index++;
        }
    }

    private static void validateFaction(EchoFactionDefinition faction, List<EchoScriptDiagnostic> diagnostics) {
        if (faction.ranks().isEmpty()) {
            diagnostics.add(warning("SCRIPTCORE_INVALID_FACTION_RANKS", "Faction " + faction.id()
                            + " has no reputation ranks.",
                    faction.sourceFile(), Optional.of(faction.id()), "$.ranks",
                    "Add at least one rank for creator dashboards and runtime UI."));
            return;
        }
        for (int i = 0; i < faction.ranks().size(); i++) {
            var rank = faction.ranks().get(i);
            if (rank.max().isPresent() && rank.max().get() < rank.min()) {
                diagnostics.add(error("SCRIPTCORE_INVALID_FACTION_RANKS", "Faction " + faction.id()
                                + " has a rank with max below min.",
                        faction.sourceFile(), Optional.of(faction.id()), "$.ranks[" + i + "]",
                        "Ensure rank max is greater than or equal to min."));
            }
        }
    }

    private static void validateArchive(
            EchoArchiveEntryDefinition archive,
            List<EchoScriptDiagnostic> diagnostics,
            Map<String, Set<Identifier>> idsByType) {
        if (archive.title().isEmpty()) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Archive entry " + archive.id()
                            + " is missing title.",
                    archive.sourceFile(), Optional.of(archive.id()), "$.title", "Add a readable archive title."));
        }
        if (archive.content().isEmpty()) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Archive entry " + archive.id()
                            + " has no content.",
                    archive.sourceFile(), Optional.of(archive.id()), "$.content", "Add at least one content line."));
        }
        archive.relatedMissions().forEach(mission -> checkRef(mission, "mission", archive, diagnostics,
                idsByType, "$.related_missions", "SCRIPTCORE_BROKEN_MISSION_REFERENCE"));
        archive.relatedScans().forEach(scan -> checkRef(scan, "lens_scan", archive, diagnostics,
                idsByType, "$.related_scans", "SCRIPTCORE_BROKEN_CONDITION_REFERENCE"));
    }

    private static void validateLens(EchoLensScanDefinition lens, List<EchoScriptDiagnostic> diagnostics) {
        if (lens.target().isEmpty()) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Lens scan " + lens.id()
                            + " is missing target.",
                    lens.sourceFile(), Optional.of(lens.id()), "$.target", "Add a target id."));
        }
        if (lens.title().isEmpty()) {
            diagnostics.add(error("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Lens scan " + lens.id()
                            + " is missing title.",
                    lens.sourceFile(), Optional.of(lens.id()), "$.title", "Add a scan title."));
        }
        if (!Set.of("block", "entity", "item", "fluid", "poi", "region", "custom").contains(lens.targetType())) {
            diagnostics.add(warning("SCRIPTCORE_UNKNOWN_TYPE", "Lens scan " + lens.id()
                            + " uses unknown target_type " + lens.targetType() + ".",
                    lens.sourceFile(), Optional.of(lens.id()), "$.target_type",
                    "Use block, entity, item, fluid, poi, region, or custom."));
        }
    }

    private static void validateMarker(
            EchoHoloMapMarkerDefinition marker,
            String path,
            List<EchoScriptDiagnostic> diagnostics,
            Map<String, Set<Identifier>> idsByType) {
        if (!Double.isFinite(marker.x()) || !Double.isFinite(marker.z())
                || Math.abs(marker.x()) > 30_000_000D || Math.abs(marker.z()) > 30_000_000D
                || marker.y().map(y -> !Double.isFinite(y) || y < -2048D || y > 4096D).orElse(false)) {
            diagnostics.add(error("SCRIPTCORE_INVALID_OBJECTIVE", "HoloMap marker " + marker.id()
                            + " has invalid coordinates.",
                    marker.sourceFile(), Optional.of(marker.id()), path,
                    "Use finite coordinates inside the Minecraft world border."));
        }
        if (marker.layer().isEmpty()) {
            diagnostics.add(warning("SCRIPTCORE_BROKEN_ACTION_REFERENCE", "HoloMap marker " + marker.id()
                            + " has no layer.",
                    marker.sourceFile(), Optional.of(marker.id()), path + ".layer",
                    "Add a holomap_layer id."));
        } else {
            checkRef(marker.layer().get(), "holomap_layer", marker, diagnostics, idsByType, path + ".layer",
                    "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
        }
    }

    private static void validateDialogue(
            EchoDialogueDefinition dialogue,
            List<EchoScriptDiagnostic> diagnostics,
            Set<Identifier> missionIds,
            Map<String, Set<Identifier>> idsByType) {
        if (dialogue.lines().isEmpty()) {
            diagnostics.add(warning("SCRIPTCORE_MISSING_REQUIRED_FIELD", "Dialogue " + dialogue.id()
                            + " has no lines.",
                    dialogue.sourceFile(), Optional.of(dialogue.id()), "$.lines", "Add dialogue lines."));
        }
        for (int i = 0; i < dialogue.choices().size(); i++) {
            var choice = dialogue.choices().get(i);
            validateConditions(dialogue, choice.conditions(), "$.choices[" + i + "].conditions", diagnostics, missionIds, idsByType);
            validateActions(dialogue, choice.actions(), "$.choices[" + i + "].actions", diagnostics, missionIds, idsByType);
            String nextPath = "$.choices[" + i + "].next_dialogue";
            choice.nextDialogue().ifPresent(next -> checkRef(next, "dialogue", dialogue, diagnostics,
                    idsByType, nextPath, "SCRIPTCORE_BROKEN_ACTION_REFERENCE"));
        }
    }

    private static void validateConditions(
            EchoScriptDefinitionView definition,
            List<EchoCondition> conditions,
            String path,
            List<EchoScriptDiagnostic> diagnostics,
            Set<Identifier> missionIds,
            Map<String, Set<Identifier>> idsByType) {
        int index = 0;
        for (EchoCondition condition : conditions) {
            String currentPath = path + "[" + index + "]";
            if (!EchoScriptKnownTypes.CONDITION_TYPES.contains(condition.type())) {
                diagnostics.add(warning("SCRIPTCORE_UNKNOWN_CONDITION", "Definition " + safeId(definition)
                                + " uses unknown condition type " + condition.type() + ".",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".type",
                        "Use a supported condition type or custom."));
            }
            if ("all".equals(condition.type()) && condition.all().isEmpty()) {
                diagnostics.add(error("SCRIPTCORE_BROKEN_CONDITION_REFERENCE", "Condition all has no child conditions.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".all",
                        "Add at least one condition to all."));
            }
            if ("any".equals(condition.type()) && condition.any().isEmpty()) {
                diagnostics.add(error("SCRIPTCORE_BROKEN_CONDITION_REFERENCE", "Condition any has no child conditions.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".any",
                        "Add at least one condition to any."));
            }
            if ("not".equals(condition.type()) && condition.all().isEmpty() && condition.any().isEmpty()) {
                diagnostics.add(error("SCRIPTCORE_BROKEN_CONDITION_REFERENCE", "Condition not has no child condition.",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath,
                        "Add one child condition."));
            }
            condition.mission().ifPresent(mission -> {
                if (!missionIds.contains(mission)) {
                    diagnostics.add(error("SCRIPTCORE_BROKEN_MISSION_REFERENCE", "Definition " + safeId(definition)
                                    + " references missing mission " + mission + ".",
                            definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".mission",
                            "Create the mission or fix the reference id."));
                }
            });
            if ("world_state_set".equals(condition.type())) {
                checkOptionalRef(condition.state(), "world_state", definition, diagnostics, idsByType, currentPath + ".state",
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE");
            }
            if ("branch_marker_set".equals(condition.type())) {
                require(condition.value().isPresent() || condition.id().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE", currentPath + ".value",
                        "branch_marker_set is missing a marker value.", "Add value: \"route_a_unlocked\".");
            }
            if ("dialogue_choice_made".equals(condition.type())) {
                require(condition.poi().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE", currentPath + ".poi",
                        "dialogue_choice_made is missing dialogue id.", "Add poi: \"pack:dialogue_id\".");
                require(condition.value().isPresent() || condition.id().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE", currentPath + ".value",
                        "dialogue_choice_made is missing choice value.", "Add value: \"choice_id\".");
            }
            if ("weather_survived".equals(condition.type())) {
                checkOptionalRef(condition.weather(), "weather_event", definition, diagnostics, idsByType, currentPath + ".weather",
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE");
            }
            if (condition.type().startsWith("faction_reputation")) {
                checkOptionalRef(condition.faction(), "faction", definition, diagnostics, idsByType, currentPath + ".faction",
                        "SCRIPTCORE_BROKEN_CONDITION_REFERENCE");
            }
            validateConditions(definition, condition.all(), currentPath + ".all", diagnostics, missionIds, idsByType);
            validateConditions(definition, condition.any(), currentPath + ".any", diagnostics, missionIds, idsByType);
            index++;
        }
    }

    private static void validateActions(
            EchoScriptDefinitionView definition,
            List<EchoAction> actions,
            String path,
            List<EchoScriptDiagnostic> diagnostics,
            Set<Identifier> missionIds,
            Map<String, Set<Identifier>> idsByType) {
        int index = 0;
        for (EchoAction action : actions) {
            String currentPath = path + "[" + index + "]";
            if (!EchoScriptKnownTypes.ACTION_TYPES.contains(action.type())) {
                diagnostics.add(warning("SCRIPTCORE_UNKNOWN_ACTION", "Definition " + safeId(definition)
                                + " uses unknown action type " + action.type() + ".",
                        definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".type",
                        "Use a supported action type or custom."));
            }
            action.mission().ifPresent(mission -> {
                if (!missionIds.contains(mission)) {
                    diagnostics.add(error("SCRIPTCORE_BROKEN_MISSION_REFERENCE", "Definition " + safeId(definition)
                                    + " references missing mission " + mission + ".",
                            definition.sourceFile(), Optional.ofNullable(definition.id()), currentPath + ".mission",
                            "Create the mission or fix the reference id."));
                }
            });
            switch (action.type()) {
                case "unlock_lore", "unlock_archive_entry" -> checkOptionalRef(action.entry(), "archive_entry", definition,
                        diagnostics, idsByType, currentPath + ".entry", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "unlock_holomap_layer" -> checkOptionalRef(action.layer(), "holomap_layer", definition,
                        diagnostics, idsByType, currentPath + ".layer", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "add_holomap_marker" -> checkOptionalRef(action.marker(), "holomap_marker", definition,
                        diagnostics, idsByType, currentPath + ".marker", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "trigger_weather" -> checkOptionalRef(action.weather(), "weather_event", definition,
                        diagnostics, idsByType, currentPath + ".weather", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "show_tutorial_hint" -> checkOptionalRef(action.entry(), "tutorial_hint", definition,
                        diagnostics, idsByType, currentPath + ".entry", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "set_world_state", "clear_world_state" -> checkOptionalRef(action.state(), "world_state", definition,
                        diagnostics, idsByType, currentPath + ".state", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "start_dialogue" -> checkOptionalRef(action.entry(), "dialogue", definition,
                        diagnostics, idsByType, currentPath + ".entry", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "change_reputation" -> checkOptionalRef(action.faction(), "faction", definition,
                        diagnostics, idsByType, currentPath + ".faction", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                case "give_item" -> require(action.item().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_ACTION_REFERENCE", currentPath + ".item", "give_item is missing item.",
                        "Add an item id.");
                case "play_sound" -> require(action.sound().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_ACTION_REFERENCE", currentPath + ".sound", "play_sound is missing sound.",
                        "Add a sound id.");
                case "set_custom_metric", "change_custom_metric" -> require(action.metric().isPresent(), diagnostics, definition,
                        "SCRIPTCORE_BROKEN_ACTION_REFERENCE", currentPath + ".metric", action.type() + " is missing metric.",
                        "Add a metric name.");
                case "set_branch_marker", "clear_branch_marker" -> require(action.value().isPresent() || action.id().isPresent(),
                        diagnostics, definition, "SCRIPTCORE_BROKEN_ACTION_REFERENCE", currentPath + ".value",
                        action.type() + " is missing marker value.", "Add value: \"route_a_unlocked\".");
                case "record_dialogue_choice" -> {
                    checkOptionalRef(action.entry(), "dialogue", definition, diagnostics, idsByType,
                            currentPath + ".entry", "SCRIPTCORE_BROKEN_ACTION_REFERENCE");
                    require(action.value().isPresent() || action.id().isPresent(), diagnostics, definition,
                            "SCRIPTCORE_BROKEN_ACTION_REFERENCE", currentPath + ".value",
                            "record_dialogue_choice is missing choice value.", "Add value: \"choice_id\".");
                }
                default -> {
                }
            }
            index++;
        }
    }

    private static void collectMissionReferences(List<EchoCondition> conditions, Set<Identifier> references) {
        for (EchoCondition condition : conditions) {
            condition.mission().ifPresent(references::add);
            collectMissionReferences(condition.all(), references);
            collectMissionReferences(condition.any(), references);
        }
    }

    private static void validateMissionCycles(
            Map<Identifier, EchoScriptDefinitionView> byId,
            Map<Identifier, Set<Identifier>> missionPrerequisites,
            List<EchoScriptDiagnostic> diagnostics) {
        for (Identifier mission : missionPrerequisites.keySet()) {
            if (hasCycle(mission, mission, missionPrerequisites, new HashSet<>(), new ArrayDeque<>())) {
                EchoScriptDefinitionView definition = byId.get(mission);
                diagnostics.add(error("SCRIPTCORE_CIRCULAR_PREREQUISITE", "Mission " + mission
                                + " has a circular prerequisite chain.",
                        definition == null ? Optional.empty() : definition.sourceFile(), Optional.of(mission),
                        "$.prerequisites", "Remove the circular mission prerequisite reference."));
            }
        }
    }

    private static boolean hasCycle(
            Identifier start,
            Identifier current,
            Map<Identifier, Set<Identifier>> graph,
            Set<Identifier> visited,
            ArrayDeque<Identifier> stack) {
        if (!visited.add(current)) {
            return false;
        }
        stack.push(current);
        for (Identifier next : graph.getOrDefault(current, Set.of())) {
            if (start.equals(next)) {
                return true;
            }
            if (!stack.contains(next) && hasCycle(start, next, graph, visited, stack)) {
                return true;
            }
        }
        stack.pop();
        return false;
    }

    private static void require(
            boolean condition,
            List<EchoScriptDiagnostic> diagnostics,
            EchoScriptDefinitionView definition,
            String code,
            String path,
            String message,
            String suggestion) {
        if (!condition) {
            diagnostics.add(error(code, message, definition.sourceFile(), Optional.ofNullable(definition.id()), path, suggestion));
        }
    }

    private static void checkOptionalRef(
            Optional<Identifier> ref,
            String type,
            EchoScriptDefinitionView definition,
            List<EchoScriptDiagnostic> diagnostics,
            Map<String, Set<Identifier>> idsByType,
            String path,
            String code) {
        if (ref.isEmpty()) {
            diagnostics.add(error(code, "Definition " + safeId(definition) + " is missing reference " + path + ".",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), path, "Add a namespaced id."));
            return;
        }
        checkRef(ref.get(), type, definition, diagnostics, idsByType, path, code);
    }

    private static void checkRef(
            Identifier ref,
            String type,
            EchoScriptDefinitionView definition,
            List<EchoScriptDiagnostic> diagnostics,
            Map<String, Set<Identifier>> idsByType,
            String path,
            String code) {
        if (!idsByType.getOrDefault(type, Set.of()).contains(ref)) {
            EchoScriptDiagnostic.Severity severity = "mission".equals(type)
                    ? EchoScriptDiagnostic.Severity.ERROR
                    : EchoScriptDiagnostic.Severity.WARNING;
            diagnostics.add(diagnostic(severity, code,
                    "Definition " + safeId(definition) + " references missing " + type + " " + ref + ".",
                    definition.sourceFile(), Optional.ofNullable(definition.id()), path,
                    "Create the referenced definition or fix the id."));
        }
    }

    private static EchoScriptDiagnostic warning(
            String code,
            String message,
            Optional<Path> file,
            Optional<Identifier> definitionId,
            String jsonPath,
            String suggestion) {
        return diagnostic(EchoScriptDiagnostic.Severity.WARNING, code, message, file, definitionId, jsonPath, suggestion);
    }

    private static EchoScriptDiagnostic error(
            String code,
            String message,
            Optional<Path> file,
            Optional<Identifier> definitionId,
            String jsonPath,
            String suggestion) {
        return diagnostic(EchoScriptDiagnostic.Severity.ERROR, code, message, file, definitionId, jsonPath, suggestion);
    }

    private static EchoScriptDiagnostic diagnostic(
            EchoScriptDiagnostic.Severity severity,
            String code,
            String message,
            Optional<Path> file,
            Optional<Identifier> definitionId,
            String jsonPath,
            String suggestion) {
        return new EchoScriptDiagnostic(
                severity,
                code,
                message,
                file,
                definitionId,
                Optional.ofNullable(jsonPath),
                Optional.ofNullable(suggestion));
    }

    private static String safeId(EchoScriptDefinitionView definition) {
        return definition.id() == null ? "(missing id)" : definition.id().toString();
    }
}
