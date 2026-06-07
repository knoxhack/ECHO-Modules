package com.knoxhack.echo.scriptcore.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.model.EchoDialogueChoice;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoEndingDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionRank;
import com.knoxhack.echo.scriptcore.model.EchoFactionReputationEvent;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapLayerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapMarkerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLensScanDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLootProfileDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoObjective;
import com.knoxhack.echo.scriptcore.model.EchoRecipeUnlockDefinition;
import com.knoxhack.echo.scriptcore.model.EchoReward;
import com.knoxhack.echo.scriptcore.model.EchoScriptDefinition;
import com.knoxhack.echo.scriptcore.model.EchoTutorialHintDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWeatherEventDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import com.knoxhack.echo.scriptcore.util.EchoJson;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoScriptParser {
    private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");

    public EchoScriptDefinitionView parse(JsonObject json, Path sourceFile, String inferredPack) {
        int schemaVersion = EchoJson.integer(json, "schema_version", EchoJson.integer(json, "schemaVersion", 0));
        String type = EchoJson.string(json, "type", "generic").trim().toLowerCase(java.util.Locale.ROOT);
        Identifier id = Identifier.tryParse(EchoJson.string(json, "id", ""));
        if (id == null) {
            throw new IllegalArgumentException("Invalid or missing id.");
        }
        String pack = EchoJson.string(json, "pack", inferredPack == null || inferredPack.isBlank() ? "unknown" : inferredPack);
        EchoScriptDefinition base = new EchoScriptDefinition(
                schemaVersion,
                pack,
                id,
                type,
                EchoJson.optionalString(json, "title"),
                EchoJson.optionalString(json, "description"),
                EchoJson.optionalString(json, "source"),
                EchoJson.strings(json, "tags"),
                EchoJson.conditions(json, "unlock_conditions"),
                EchoJson.conditions(json, "conditions"),
                EchoJson.actions(json, "actions"),
                EchoJson.objectMap(EchoJson.object(json, "metadata")),
                json,
                Optional.ofNullable(sourceFile));
        return switch (type) {
            case "mission" -> mission(base, json);
            case "archive_entry", "archive", "lore" -> archive(base, json);
            case "lens_scan" -> lens(base, json);
            case "holomap_layer" -> holomapLayer(base, json);
            case "holomap_marker" -> holomapMarker(base, json);
            case "weather_event" -> weather(base, json);
            case "faction" -> faction(base, json);
            case "world_state" -> worldState(base, json);
            case "tutorial_hint" -> tutorial(base, json);
            case "dialogue" -> dialogue(base, json);
            case "ending" -> ending(base, json);
            case "recipe_unlock" -> recipe(base, json);
            case "loot_profile" -> loot(base, json);
            default -> base;
        };
    }

    private static EchoMissionDefinition mission(EchoScriptDefinition base, JsonObject json) {
        return new EchoMissionDefinition(
                base,
                EchoJson.string(json, "route", ""),
                EchoJson.string(json, "phase", ""),
                EchoJson.string(json, "role", "main"),
                EchoJson.string(json, "briefing", ""),
                objectives(json),
                rewards(json),
                EchoJson.conditions(json, "prerequisites"),
                EchoJson.actions(json, "on_start"),
                EchoJson.actions(json, "on_complete"),
                EchoJson.actions(json, "on_fail"),
                EchoJson.object(json, "terminal"),
                EchoJson.object(json, "lens"),
                EchoJson.object(json, "holomap"));
    }

    private static List<EchoObjective> objectives(JsonObject json) {
        List<EchoObjective> objectives = new ArrayList<>();
        int index = 0;
        for (var element : EchoJson.array(json, "objectives")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            objectives.add(new EchoObjective(
                    EchoJson.string(object, "id", "objective_" + index),
                    EchoJson.string(object, "type", "custom"),
                    EchoJson.string(object, "title", EchoJson.string(object, "label", "Objective " + (index + 1))),
                    EchoJson.string(object, "description", EchoJson.string(object, "detail", "")),
                    EchoJson.id(object, "target"),
                    EchoJson.id(object, "item"),
                    EchoJson.id(object, "block"),
                    EchoJson.id(object, "entity"),
                    EchoJson.id(object, "poi"),
                    EchoJson.id(object, "region"),
                    EchoJson.integer(object, "count", EchoJson.integer(object, "required", 1)),
                    EchoJson.bool(object, "optional", false),
                    EchoJson.bool(object, "hidden", false),
                    EchoJson.conditions(object, "conditions"),
                    EchoJson.objectMap(EchoJson.object(object, "metadata"))));
            index++;
        }
        return List.copyOf(objectives);
    }

    private static List<EchoReward> rewards(JsonObject json) {
        List<EchoReward> rewards = new ArrayList<>();
        for (var element : EchoJson.array(json, "rewards")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            rewards.add(new EchoReward(
                    EchoJson.string(object, "type", "custom"),
                    EchoJson.id(object, "item"),
                    EchoJson.integer(object, "count", 1),
                    EchoJson.integer(object, "experience", EchoJson.integer(object, "xp", 0)),
                    EchoJson.id(object, "mission"),
                    EchoJson.id(object, "tab"),
                    EchoJson.id(object, "entry"),
                    EchoJson.id(object, "layer"),
                    EchoJson.id(object, "marker"),
                    EchoJson.id(object, "faction"),
                    EchoJson.integer(object, "amount", 0),
                    EchoJson.id(object, "state"),
                    EchoJson.id(object, "sound"),
                    EchoJson.objectMap(EchoJson.object(object, "metadata"))));
        }
        return List.copyOf(rewards);
    }

    private static EchoArchiveEntryDefinition archive(EchoScriptDefinition base, JsonObject json) {
        return new EchoArchiveEntryDefinition(
                base,
                EchoJson.string(json, "category", "general"),
                EchoJson.string(json, "subtitle", ""),
                EchoJson.strings(json, "content"),
                EchoJson.ids(json, "related_missions"),
                EchoJson.ids(json, "related_scans"),
                EchoJson.ids(json, "related_pois"),
                EchoJson.string(json, "importance", "common"));
    }

    private static EchoLensScanDefinition lens(EchoScriptDefinition base, JsonObject json) {
        return new EchoLensScanDefinition(
                base,
                EchoJson.id(json, "target"),
                EchoJson.string(json, "target_type", "custom"),
                EchoJson.string(json, "summary", ""),
                EchoJson.strings(json, "details"),
                EchoJson.string(json, "danger", ""));
    }

    private static EchoHoloMapLayerDefinition holomapLayer(EchoScriptDefinition base, JsonObject json) {
        List<EchoHoloMapMarkerDefinition> markers = new ArrayList<>();
        for (var element : EchoJson.array(json, "markers")) {
            if (element.isJsonObject()) {
                JsonObject marker = element.getAsJsonObject();
                if (!marker.has("id")) {
                    marker.addProperty("id", base.id().getNamespace() + ":" + base.id().getPath() + "_marker_" + markers.size());
                }
                if (!marker.has("type")) {
                    marker.addProperty("type", "holomap_marker");
                }
                if (!marker.has("layer")) {
                    marker.addProperty("layer", base.id().toString());
                }
                markers.add((EchoHoloMapMarkerDefinition) holomapMarker(new EchoScriptParser().parseBase(marker, base.pack(), base.sourceFile().orElse(null)), marker));
            }
        }
        return new EchoHoloMapLayerDefinition(base, EchoJson.bool(json, "locked_by_default", false), markers);
    }

    private EchoScriptDefinition parseBase(JsonObject json, String pack, Path sourceFile) {
        Identifier id = Identifier.tryParse(EchoJson.string(json, "id", ""));
        if (id == null) {
            id = Identifier.fromNamespaceAndPath(pack == null || pack.isBlank() ? "echoscriptcore" : pack, "inline_marker");
        }
        return new EchoScriptDefinition(
                EchoJson.integer(json, "schema_version", 1),
                pack,
                id,
                EchoJson.string(json, "type", "generic"),
                EchoJson.optionalString(json, "title"),
                EchoJson.optionalString(json, "description"),
                EchoJson.optionalString(json, "source"),
                EchoJson.strings(json, "tags"),
                EchoJson.conditions(json, "unlock_conditions"),
                EchoJson.conditions(json, "conditions"),
                EchoJson.actions(json, "actions"),
                EchoJson.objectMap(EchoJson.object(json, "metadata")),
                json,
                Optional.ofNullable(sourceFile));
    }

    private static EchoHoloMapMarkerDefinition holomapMarker(EchoScriptDefinition base, JsonObject json) {
        return new EchoHoloMapMarkerDefinition(
                base,
                EchoJson.decimal(json, "x", 0),
                optionalDouble(json, "y"),
                EchoJson.decimal(json, "z", 0),
                EchoJson.id(json, "dimension", OVERWORLD),
                EchoJson.id(json, "icon"),
                EchoJson.string(json, "danger", ""),
                EchoJson.id(json, "layer"));
    }

    private static Optional<Double> optionalDouble(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.get(key).getAsDouble());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static EchoWeatherEventDefinition weather(EchoScriptDefinition base, JsonObject json) {
        return new EchoWeatherEventDefinition(
                base,
                EchoJson.integer(json, "duration_ticks", 0),
                EchoJson.integer(json, "warning_seconds", 0),
                EchoJson.actions(json, "effects"),
                EchoJson.string(json, "terminal_warning", ""),
                EchoJson.id(json, "sound_stinger"));
    }

    private static EchoFactionDefinition faction(EchoScriptDefinition base, JsonObject json) {
        List<EchoFactionRank> ranks = new ArrayList<>();
        for (var element : EchoJson.array(json, "ranks")) {
            if (element.isJsonObject()) {
                JsonObject rank = element.getAsJsonObject();
                ranks.add(new EchoFactionRank(
                        EchoJson.string(rank, "name", "rank"),
                        EchoJson.integer(rank, "min", 0),
                        EchoJson.optionalInt(rank, "max"),
                        EchoJson.optionalString(rank, "color"),
                        EchoJson.objectMap(EchoJson.object(rank, "metadata"))));
            }
        }
        List<EchoFactionReputationEvent> events = new ArrayList<>();
        for (var element : EchoJson.array(json, "reputation_events")) {
            if (element.isJsonObject()) {
                JsonObject event = element.getAsJsonObject();
                events.add(new EchoFactionReputationEvent(
                        EchoJson.string(event, "id", "event"),
                        EchoJson.string(event, "title", ""),
                        EchoJson.integer(event, "amount", 0),
                        EchoJson.objectMap(EchoJson.object(event, "metadata"))));
            }
        }
        return new EchoFactionDefinition(
                base,
                EchoJson.string(json, "display_name", base.title().orElse(base.id().toString())),
                EchoJson.string(json, "description", ""),
                EchoJson.integer(json, "starting_reputation", 0),
                ranks,
                events);
    }

    private static EchoWorldStateDefinition worldState(EchoScriptDefinition base, JsonObject json) {
        return new EchoWorldStateDefinition(base, EchoJson.conditions(json, "set_by"), EchoJson.actions(json, "effects"));
    }

    private static EchoTutorialHintDefinition tutorial(EchoScriptDefinition base, JsonObject json) {
        return new EchoTutorialHintDefinition(
                base,
                EchoJson.string(json, "message", ""),
                EchoJson.conditions(json, "trigger_conditions"),
                EchoJson.integer(json, "priority", 0),
                EchoJson.bool(json, "once", true),
                EchoJson.object(json, "terminal_card"));
    }

    private static EchoDialogueDefinition dialogue(EchoScriptDefinition base, JsonObject json) {
        List<EchoDialogueChoice> choices = new ArrayList<>();
        for (var element : EchoJson.array(json, "choices")) {
            if (element.isJsonObject()) {
                JsonObject choice = element.getAsJsonObject();
                choices.add(new EchoDialogueChoice(
                        EchoJson.string(choice, "id", "choice"),
                        EchoJson.string(choice, "label", ""),
                        EchoJson.conditions(choice, "conditions"),
                        EchoJson.actions(choice, "actions"),
                        EchoJson.id(choice, "next_dialogue"),
                        EchoJson.objectMap(EchoJson.object(choice, "metadata"))));
            }
        }
        return new EchoDialogueDefinition(base, EchoJson.string(json, "speaker", ""), EchoJson.strings(json, "lines"), choices);
    }

    private static EchoEndingDefinition ending(EchoScriptDefinition base, JsonObject json) {
        return new EchoEndingDefinition(
                base,
                EchoJson.integer(json, "priority", 0),
                EchoJson.conditions(json, "conditions"),
                EchoJson.actions(json, "actions"),
                EchoJson.string(json, "terminal_summary", ""));
    }

    private static EchoRecipeUnlockDefinition recipe(EchoScriptDefinition base, JsonObject json) {
        return new EchoRecipeUnlockDefinition(
                base,
                EchoJson.id(json, "recipe"),
                EchoJson.conditions(json, "unlock_conditions"),
                EchoJson.actions(json, "actions"));
    }

    private static EchoLootProfileDefinition loot(EchoScriptDefinition base, JsonObject json) {
        JsonArray entries = EchoJson.array(json, "entries");
        return new EchoLootProfileDefinition(base, EchoJson.id(json, "table"), entries, EchoJson.conditions(json, "unlock_conditions"));
    }
}
