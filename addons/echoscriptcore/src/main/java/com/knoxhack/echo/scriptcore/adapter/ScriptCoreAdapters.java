package com.knoxhack.echo.scriptcore.adapter;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoActionResult;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoConditionResult;
import com.knoxhack.echo.scriptcore.api.EchoDiagnosticSink;
import com.knoxhack.echo.scriptcore.api.EchoScriptAdapter;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptExecutionContext;
import com.knoxhack.echo.scriptcore.api.EchoScriptRegistryView;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapLayerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapMarkerDefinition;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreRuntimeStateService;
import com.knoxhack.echo.scriptcore.validation.EchoScriptKnownTypes;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoIntegrations;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.mission.MissionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

abstract class BaseScriptAdapter implements EchoScriptAdapter {
    private final Identifier id;
    private final String modId;
    private final Set<String> definitions;
    private final Set<String> actions;
    private final Set<String> conditions;

    BaseScriptAdapter(String path, String modId, Set<String> definitions, Set<String> actions, Set<String> conditions) {
        this.id = EchoScriptCore.id(path);
        this.modId = modId;
        this.definitions = Set.copyOf(definitions == null ? Set.of() : definitions);
        this.actions = Set.copyOf(actions == null ? Set.of() : actions);
        this.conditions = Set.copyOf(conditions == null ? Set.of() : conditions);
    }

    @Override public Identifier id() { return id; }
    @Override public boolean isAvailable() { return modId == null || EchoIntegrations.has(modId); }
    @Override public Set<String> supportedDefinitionTypes() { return definitions; }
    @Override public Set<String> supportedActions() { return actions; }
    @Override public Set<String> supportedConditions() { return conditions; }
    @Override public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        if (!isAvailable() && diagnostics != null) {
            diagnostics.report(new EchoScriptDiagnostic(EchoScriptDiagnostic.Severity.INFO, "SCRIPTCORE_ADAPTER_UNAVAILABLE",
                    id + " is unavailable; matching definitions remain registered in ScriptCore only.",
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("Install " + modId + " to enable this adapter.")));
        }
    }
    @Override public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        return EchoActionResult.unsupported(id + " does not execute " + action.type()
                + " in the current ScriptCore release.");
    }
    @Override public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        return EchoConditionResult.unsupported(id + " does not evaluate " + condition.type()
                + " in the current ScriptCore release.");
    }
}

final class InternalFallbackAdapter extends BaseScriptAdapter {
    InternalFallbackAdapter() {
        super("internal_fallback", null, EchoScriptKnownTypes.DEFINITION_TYPES, Set.of("noop", "give_item"), Set.of("always", "never", "all", "any", "not"));
    }

    @Override
    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        if ("noop".equals(action.type())) {
            return EchoActionResult.success("noop");
        }
        if ("give_item".equals(action.type())) {
            Optional<ServerPlayer> player = context.player();
            if (player.isEmpty() || action.item().isEmpty()) {
                return EchoActionResult.failure("give_item requires a server player and item.");
            }
            Item item = BuiltInRegistries.ITEM.getValue(action.item().get());
            if (item == null || item == Items.AIR) {
                return EchoActionResult.failure("Unknown item " + action.item().get() + ".");
            }
            ItemStack stack = new ItemStack(item, Math.max(1, action.count().orElse(1)));
            if (!player.get().getInventory().add(stack)) {
                player.get().drop(stack, false);
            }
            return EchoActionResult.success("Gave " + stack.getCount() + "x " + action.item().get() + ".");
        }
        return EchoActionResult.unsupported("Internal fallback does not execute " + action.type() + ".");
    }

    @Override
    public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        return switch (condition.type()) {
            case "always" -> EchoConditionResult.matched("always");
            case "never" -> EchoConditionResult.unmatched("never");
            case "all" -> {
                for (EchoCondition child : condition.all()) {
                    EchoConditionResult result = EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(child, context);
                    if (!result.supported() || !result.matched()) {
                        yield EchoConditionResult.unmatched("all condition failed: " + result.message());
                    }
                }
                yield EchoConditionResult.matched("all");
            }
            case "any" -> {
                for (EchoCondition child : condition.any()) {
                    EchoConditionResult result = EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(child, context);
                    if (result.supported() && result.matched()) {
                        yield EchoConditionResult.matched("any");
                    }
                }
                yield EchoConditionResult.unmatched("no any condition matched");
            }
            case "not" -> {
                EchoCondition child = condition.all().isEmpty() ? null : condition.all().get(0);
                if (child == null) {
                    yield EchoConditionResult.unmatched("not has no child");
                }
                EchoConditionResult result = EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(child, context);
                yield new EchoConditionResult(result.supported(), !result.matched(), "not " + result.message());
            }
            default -> EchoConditionResult.unsupported("Internal fallback does not evaluate " + condition.type() + ".");
        };
    }
}

final class MissionCoreAdapter extends BaseScriptAdapter {
    private boolean registered;

    MissionCoreAdapter() {
        super("missioncore", EchoIntegrations.MISSION_CORE, Set.of("mission"), Set.of("start_mission", "complete_mission", "complete_objective", "unlock_mission"), Set.of("mission_complete", "mission_active", "mission_started", "objective_complete"));
    }

    @Override
    public boolean isAvailable() {
        return EchoIntegrations.hasMissionCore() && EchoCoreServices.missionCoreAvailable();
    }

    @Override
    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        super.registerDefinitions(registry, diagnostics);
        if (!isAvailable()) {
            return;
        }
        if (!registered) {
            registered = true;
            EchoCoreServices.registerMissionContent(ScriptCoreMissionContent.SOURCE, ScriptCoreMissionContent::register);
        } else {
            ScriptCoreMissionContent.register(EchoCoreServices.missionService());
        }
    }

    @Override
    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        if (context.player().isEmpty() || action.mission().isEmpty()) {
            return EchoActionResult.failure(action.type() + " requires a player and mission id.");
        }
        ServerPlayer player = context.player().get();
        boolean ok = switch (action.type()) {
            case "start_mission", "unlock_mission" -> EchoCoreServices.startMission(player, action.mission().get());
            case "complete_mission" -> EchoCoreServices.completeMission(player, action.mission().get());
            case "complete_objective" -> EchoCoreServices.recordMissionObjective(player,
                    com.knoxhack.echocore.api.mission.MissionObjectiveType.CUSTOM,
                    action.mission().get(), 1, java.util.Map.of("scriptcore_objective", action.objective().orElse("")));
            default -> false;
        };
        return ok ? EchoActionResult.success("MissionCore handled " + action.type() + ".")
                : EchoActionResult.failure("MissionCore did not apply " + action.type() + ".");
    }

    @Override
    public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        if (context.player().isEmpty() || condition.mission().isEmpty()) {
            return EchoConditionResult.unmatched(condition.type() + " requires a player and mission id.");
        }
        var view = EchoCoreServices.missionService().mission(context.player().get(), condition.mission().get()).orElse(null);
        if (view == null) {
            return EchoConditionResult.unmatched("Mission " + condition.mission().get() + " is not registered.");
        }
        MissionStatus status = view.status();
        boolean complete = status == MissionStatus.COMPLETED || status == MissionStatus.CLAIMABLE || status == MissionStatus.CLAIMED;
        return switch (condition.type()) {
            case "mission_complete" -> complete
                    ? EchoConditionResult.matched("Mission is complete.")
                    : EchoConditionResult.unmatched("Mission is not complete.");
            case "mission_active", "mission_started" -> status == MissionStatus.ACTIVE || complete
                    ? EchoConditionResult.matched("Mission has started.")
                    : EchoConditionResult.unmatched("Mission has not started.");
            case "objective_complete" -> {
                String objective = condition.objective().orElse("");
                boolean objectiveComplete = !objective.isBlank() && view.objectives().stream()
                        .anyMatch(row -> (row.id().toString().equals(objective) || row.id().getPath().endsWith("/" + objective))
                                && row.complete());
                yield objectiveComplete ? EchoConditionResult.matched("Objective is complete.")
                        : EchoConditionResult.unmatched("Objective is not complete.");
            }
            default -> EchoConditionResult.unsupported("MissionCore does not evaluate " + condition.type() + ".");
        };
    }
}

final class TerminalAdapter extends BaseScriptAdapter {
    TerminalAdapter() {
        super("terminal", EchoIntegrations.TERMINAL, Set.of("archive_entry", "mission"), Set.of("unlock_terminal_tab", "unlock_archive_entry", "add_terminal_alert"), Set.of());
    }

    @Override
    public boolean isAvailable() {
        return EchoIntegrations.hasTerminal();
    }

    @Override
    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        super.registerDefinitions(registry, diagnostics);
        if (!isAvailable()) {
            return;
        }
        EchoCoreServices.terminalService().registerDashboardCard(EchoScriptCore.id("scriptcore_status"));
        ScriptCoreTerminalContent.registerArchives(registry);
    }
}

final class LensAdapter extends BaseScriptAdapter {
    LensAdapter() {
        super("lens", EchoIntegrations.LENS, Set.of("lens_scan"), Set.of(), Set.of("block_scanned", "entity_scanned", "item_scanned"));
    }

    @Override
    public boolean isAvailable() {
        return EchoIntegrations.hasLens() && EchoCoreServices.lensService().available();
    }

    @Override
    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        super.registerDefinitions(registry, diagnostics);
        if (!isAvailable()) {
            return;
        }
        for (var definition : registry.getByType("lens_scan")) {
            EchoCoreServices.lensService().registerScanType(definition.id(), definition.title().orElse(definition.id().toString()));
        }
    }
}

final class HoloMapAdapter extends BaseScriptAdapter {
    private boolean providerRegistered;

    HoloMapAdapter() {
        super("holomap", EchoIntegrations.HOLO_MAP, Set.of("holomap_layer", "holomap_marker"), Set.of("unlock_holomap_layer", "add_holomap_marker"), Set.of());
    }

    @Override
    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        super.registerDefinitions(registry, diagnostics);
        if (!providerRegistered) {
            providerRegistered = true;
            EchoCoreServices.registerMapDataProvider(new ScriptCoreMapProvider());
        }
    }
}

final class WeatherCoreAdapter extends BaseScriptAdapter {
    WeatherCoreAdapter() {
        super("weathercore", "echoweathercore", Set.of("weather_event"), Set.of("trigger_weather"), Set.of("weather_survived"));
    }
}

final class TutorialCoreAdapter extends BaseScriptAdapter {
    TutorialCoreAdapter() {
        super("tutorialcore", "echotutorialcore", Set.of("tutorial_hint"), Set.of("show_tutorial_hint"), Set.of());
    }
}

final class SoundCoreAdapter extends BaseScriptAdapter {
    SoundCoreAdapter() {
        super("soundcore", EchoIntegrations.SOUND_CORE, Set.of(), Set.of("play_sound"), Set.of());
    }

    @Override
    public boolean isAvailable() {
        return EchoIntegrations.hasSoundCore() && EchoCoreServices.soundService().available();
    }

    @Override
    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        Identifier sound = action.sound().orElse(null);
        if (sound == null) {
            return EchoActionResult.failure("play_sound requires sound.");
        }
        boolean ok = context.player().map(player -> EchoCoreServices.soundService().playEvent(player, sound))
                .orElseGet(() -> EchoCoreServices.soundService().playEvent(sound));
        return ok ? EchoActionResult.success("SoundCore played " + sound + ".")
                : EchoActionResult.failure("SoundCore did not play " + sound + ".");
    }
}

final class IndexAdapter extends BaseScriptAdapter {
    IndexAdapter() {
        super("index", EchoIntegrations.INDEX, Set.of("recipe_unlock", "archive_entry"), Set.of("unlock_index_recipe"), Set.of());
    }
}

final class DataCoreAdapter extends BaseScriptAdapter {
    DataCoreAdapter() {
        super("datacore", EchoIntegrations.DATA_CORE, Set.of("world_state", "faction", "dialogue"),
                Set.of("set_custom_metric", "change_custom_metric", "set_world_state", "clear_world_state",
                        "change_reputation", "set_branch_marker", "clear_branch_marker", "record_dialogue_choice"),
                Set.of("custom_metric_at_least", "custom_metric_below", "world_state_set",
                        "faction_reputation_at_least", "faction_reputation_below",
                        "branch_marker_set", "dialogue_choice_made"));
    }

    @Override
    public boolean isAvailable() {
        return ScriptCoreRuntimeStateService.INSTANCE.available();
    }

    @Override
    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics) {
        super.registerDefinitions(registry, diagnostics);
        ScriptCoreRuntimeStateService.INSTANCE.registerKeys(registry);
    }

    @Override
    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        return ScriptCoreRuntimeStateService.INSTANCE.execute(action, context);
    }

    @Override
    public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        return ScriptCoreRuntimeStateService.INSTANCE.evaluate(condition, context);
    }
}

final class WorldCoreAdapter extends BaseScriptAdapter {
    WorldCoreAdapter() {
        super("worldcore", EchoIntegrations.WORLD_CORE, Set.of(), Set.of("spawn_poi"), Set.of("poi_discovered", "region_entered", "dimension_entered", "biome_entered"));
    }
}

final class ScriptCoreMapProvider implements IMapDataProvider {
    @Override
    public Identifier providerId() {
        return EchoScriptCore.id("scriptcore_map_provider");
    }

    @Override
    public List<IMapLayer> layers(net.minecraft.world.entity.player.Player player) {
        return com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry.INSTANCE.getByType("holomap_layer").stream()
                .filter(EchoHoloMapLayerDefinition.class::isInstance)
                .map(EchoHoloMapLayerDefinition.class::cast)
                .<IMapLayer>map(layer -> new ScriptCoreLayer(layer.id(), layer.title().orElse(layer.id().toString()), !layer.lockedByDefault()))
                .toList();
    }

    @Override
    public List<IMapMarker> markers(net.minecraft.world.entity.player.Player player) {
        List<EchoHoloMapMarkerDefinition> markers = new ArrayList<>();
        markers.addAll(com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry.INSTANCE.getByType("holomap_marker").stream()
                .filter(EchoHoloMapMarkerDefinition.class::isInstance)
                .map(EchoHoloMapMarkerDefinition.class::cast)
                .toList());
        com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry.INSTANCE.getByType("holomap_layer").stream()
                .filter(EchoHoloMapLayerDefinition.class::isInstance)
                .map(EchoHoloMapLayerDefinition.class::cast)
                .flatMap(layer -> layer.markers().stream())
                .forEach(markers::add);
        return markers.stream()
                .<IMapMarker>map(ScriptCoreMarker::new)
                .toList();
    }

    private record ScriptCoreLayer(Identifier id, String title, boolean visibleByDefault) implements IMapLayer {
        @Override public int sortOrder() { return 500; }
        @Override public int color() { return 0x66D9EF; }
    }

    private record ScriptCoreMarker(EchoHoloMapMarkerDefinition definition) implements IMapMarker {
        @Override public Identifier id() { return definition.id(); }
        @Override public Identifier layerId() { return definition.layer().orElse(EchoScriptCore.id("scriptcore")); }
        @Override public Identifier sourceId() { return EchoScriptCore.id("scriptcore_map_provider"); }
        @Override public MarkerKind kind() { return MarkerKind.GENERIC; }
        @Override public MarkerState state() { return MarkerState.DISCOVERED; }
        @Override public String title() { return definition.title().orElse(definition.id().toString()); }
        @Override public String summary() { return definition.description().orElse(""); }
        @Override public ResourceKey<Level> dimension() { return ResourceKey.create(Registries.DIMENSION, definition.dimension()); }
        @Override public double x() { return definition.x(); }
        @Override public double y() { return definition.y().orElse(64.0D); }
        @Override public double z() { return definition.z(); }
        @Override public float radius() { return 0.0F; }
        @Override public Identifier icon() { return definition.icon().orElse(EchoScriptCore.id("marker")); }
        @Override public Identifier routeId() { return definition.layer().orElse(EchoScriptCore.id("scriptcore")); }
        @Override public int routeOrder() { return 0; }
        @Override public boolean precise() { return true; }
    }
}
