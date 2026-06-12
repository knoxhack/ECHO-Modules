package com.knoxhack.echo.scriptcore.runtime;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoActionResult;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoConditionResult;
import com.knoxhack.echo.scriptcore.api.EchoScriptExecutionContext;
import com.knoxhack.echo.scriptcore.api.EchoScriptRegistryView;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeState;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoIntegrations;
import com.echoplatform.echocore.api.IDataKey;
import com.echoplatform.echocore.api.IDataService;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ScriptCoreRuntimeStateService implements EchoScriptRuntimeState {
    public static final ScriptCoreRuntimeStateService INSTANCE = new ScriptCoreRuntimeStateService();

    private ScriptCoreRuntimeStateService() {
    }

    @Override
    public boolean available() {
        try {
            return EchoIntegrations.hasDataCore() && EchoCoreServices.dataService().diagnostics().available();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public String backendName() {
        try {
            String provider = EchoCoreServices.dataService().diagnostics().providerClass();
            return provider == null || provider.isBlank() ? "unavailable" : provider;
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    @Override
    public void registerKeys(EchoScriptRegistryView registry) {
        if (registry == null || !available()) {
            return;
        }
        for (var definition : registry.all()) {
            if (definition instanceof EchoWorldStateDefinition) {
                registerWorldState(definition.id());
            } else if (definition instanceof EchoFactionDefinition) {
                registerFaction(definition.id());
            } else if (definition instanceof EchoDialogueDefinition dialogue) {
                for (var choice : dialogue.choices()) {
                    if (!choice.id().isBlank()) {
                        registerBranch(dialogueChoiceMarker(dialogue.id(), choice.id()));
                    }
                }
            }
            definition.conditions().forEach(this::registerConditionKeys);
            definition.unlockConditions().forEach(this::registerConditionKeys);
            definition.actions().forEach(this::registerActionKeys);
        }
    }

    @Override
    public boolean worldState(Level level, Identifier state) {
        if (level == null || state == null || !available()) {
            return false;
        }
        registerWorldState(state);
        return EchoCoreServices.worldData(level).get(worldStateKey(state));
    }

    @Override
    public boolean setWorldState(Level level, Identifier state, boolean value) {
        if (level == null || state == null || !available()) {
            return false;
        }
        registerWorldState(state);
        return EchoCoreServices.worldData(level).set(worldStateKey(state), value) || worldState(level, state) == value;
    }

    @Override
    public long factionReputation(Player player, Identifier faction) {
        if (player == null || faction == null || !available()) {
            return 0L;
        }
        registerFaction(faction);
        return EchoCoreServices.playerData(player).get(factionKey(faction));
    }

    @Override
    public boolean setFactionReputation(Player player, Identifier faction, long amount) {
        if (player == null || faction == null || !available()) {
            return false;
        }
        registerFaction(faction);
        return EchoCoreServices.playerData(player).set(factionKey(faction), amount) || factionReputation(player, faction) == amount;
    }

    @Override
    public long changeFactionReputation(Player player, Identifier faction, long delta) {
        if (player == null || faction == null || !available()) {
            return 0L;
        }
        long next = factionReputation(player, faction) + delta;
        setFactionReputation(player, faction, next);
        return next;
    }

    @Override
    public long customMetric(Player player, String metric) {
        if (player == null || metric == null || metric.isBlank() || !available()) {
            return 0L;
        }
        registerMetric(metric);
        return EchoCoreServices.playerData(player).get(metricKey(metric));
    }

    @Override
    public boolean setCustomMetric(Player player, String metric, long value) {
        if (player == null || metric == null || metric.isBlank() || !available()) {
            return false;
        }
        registerMetric(metric);
        return EchoCoreServices.playerData(player).set(metricKey(metric), value) || customMetric(player, metric) == value;
    }

    @Override
    public long changeCustomMetric(Player player, String metric, long delta) {
        if (player == null || metric == null || metric.isBlank() || !available()) {
            return 0L;
        }
        long next = customMetric(player, metric) + delta;
        setCustomMetric(player, metric, next);
        return next;
    }

    @Override
    public boolean branchMarker(Player player, String marker) {
        if (player == null || marker == null || marker.isBlank() || !available()) {
            return false;
        }
        registerBranch(marker);
        return EchoCoreServices.playerData(player).get(branchKey(marker));
    }

    @Override
    public boolean setBranchMarker(Player player, String marker, boolean value) {
        if (player == null || marker == null || marker.isBlank() || !available()) {
            return false;
        }
        registerBranch(marker);
        return EchoCoreServices.playerData(player).set(branchKey(marker), value) || branchMarker(player, marker) == value;
    }

    @Override
    public boolean dialogueChoiceMade(Player player, Identifier dialogue, String choice) {
        return branchMarker(player, dialogueChoiceMarker(dialogue, choice));
    }

    @Override
    public boolean recordDialogueChoice(Player player, Identifier dialogue, String choice) {
        return setBranchMarker(player, dialogueChoiceMarker(dialogue, choice), true);
    }

    public EchoActionResult execute(EchoAction action, EchoScriptExecutionContext context) {
        if (!available()) {
            return EchoActionResult.unsupported("DataCore runtime storage is unavailable.");
        }
        ServerPlayer player = context.player().orElse(null);
        Level level = level(context);
        return switch (action.type()) {
            case "set_world_state" -> action.state()
                    .map(state -> setWorldState(level, state, true)
                            ? EchoActionResult.success("World state set: " + state + ".")
                            : EchoActionResult.failure("Could not set world state " + state + "."))
                    .orElseGet(() -> EchoActionResult.failure("set_world_state requires state."));
            case "clear_world_state" -> action.state()
                    .map(state -> setWorldState(level, state, false)
                            ? EchoActionResult.success("World state cleared: " + state + ".")
                            : EchoActionResult.failure("Could not clear world state " + state + "."))
                    .orElseGet(() -> EchoActionResult.failure("clear_world_state requires state."));
            case "change_reputation" -> {
                if (player == null || action.faction().isEmpty()) {
                    yield EchoActionResult.failure("change_reputation requires player and faction.");
                }
                long next = changeFactionReputation(player, action.faction().get(), action.amount().orElse(0));
                yield EchoActionResult.success("Faction reputation is now " + next + ".");
            }
            case "set_custom_metric" -> {
                if (player == null || action.metric().isEmpty()) {
                    yield EchoActionResult.failure("set_custom_metric requires player and metric.");
                }
                boolean ok = setCustomMetric(player, action.metric().get(), longValue(action));
                yield ok ? EchoActionResult.success("Custom metric set.")
                        : EchoActionResult.failure("Could not set custom metric.");
            }
            case "change_custom_metric" -> {
                if (player == null || action.metric().isEmpty()) {
                    yield EchoActionResult.failure("change_custom_metric requires player and metric.");
                }
                long next = changeCustomMetric(player, action.metric().get(), action.amount().orElseGet(() -> (int) longValue(action)));
                yield EchoActionResult.success("Custom metric is now " + next + ".");
            }
            case "set_branch_marker" -> {
                if (player == null || marker(action).isBlank()) {
                    yield EchoActionResult.failure("set_branch_marker requires player and value.");
                }
                yield setBranchMarker(player, marker(action), true)
                        ? EchoActionResult.success("Branch marker set.")
                        : EchoActionResult.failure("Could not set branch marker.");
            }
            case "clear_branch_marker" -> {
                if (player == null || marker(action).isBlank()) {
                    yield EchoActionResult.failure("clear_branch_marker requires player and value.");
                }
                yield setBranchMarker(player, marker(action), false)
                        ? EchoActionResult.success("Branch marker cleared.")
                        : EchoActionResult.failure("Could not clear branch marker.");
            }
            case "record_dialogue_choice" -> {
                if (player == null || action.entry().isEmpty() || marker(action).isBlank()) {
                    yield EchoActionResult.failure("record_dialogue_choice requires player, entry, and value.");
                }
                yield recordDialogueChoice(player, action.entry().get(), marker(action))
                        ? EchoActionResult.success("Dialogue choice recorded.")
                        : EchoActionResult.failure("Could not record dialogue choice.");
            }
            default -> EchoActionResult.unsupported("DataCore runtime does not execute " + action.type() + ".");
        };
    }

    public EchoConditionResult evaluate(EchoCondition condition, EchoScriptExecutionContext context) {
        if (!available()) {
            return EchoConditionResult.unsupported("DataCore runtime storage is unavailable.");
        }
        ServerPlayer player = context.player().orElse(null);
        Level level = level(context);
        return switch (condition.type()) {
            case "world_state_set" -> condition.state()
                    .map(state -> worldState(level, state)
                            ? EchoConditionResult.matched("World state is set.")
                            : EchoConditionResult.unmatched("World state is not set."))
                    .orElseGet(() -> EchoConditionResult.unmatched("world_state_set requires state."));
            case "faction_reputation_at_least" -> {
                if (player == null || condition.faction().isEmpty()) {
                    yield EchoConditionResult.unmatched("faction_reputation_at_least requires player and faction.");
                }
                long have = factionReputation(player, condition.faction().get());
                long need = condition.amount().orElseGet(() -> condition.count().orElse(0));
                yield have >= need ? EchoConditionResult.matched("Faction reputation " + have + " >= " + need + ".")
                        : EchoConditionResult.unmatched("Faction reputation " + have + " < " + need + ".");
            }
            case "faction_reputation_below" -> {
                if (player == null || condition.faction().isEmpty()) {
                    yield EchoConditionResult.unmatched("faction_reputation_below requires player and faction.");
                }
                long have = factionReputation(player, condition.faction().get());
                long limit = condition.amount().orElseGet(() -> condition.count().orElse(0));
                yield have < limit ? EchoConditionResult.matched("Faction reputation " + have + " < " + limit + ".")
                        : EchoConditionResult.unmatched("Faction reputation " + have + " >= " + limit + ".");
            }
            case "custom_metric_at_least" -> {
                if (player == null || condition.metric().isEmpty()) {
                    yield EchoConditionResult.unmatched("custom_metric_at_least requires player and metric.");
                }
                long have = customMetric(player, condition.metric().get());
                long need = condition.amount().orElseGet(() -> condition.count().orElse(0));
                yield have >= need ? EchoConditionResult.matched("Custom metric " + have + " >= " + need + ".")
                        : EchoConditionResult.unmatched("Custom metric " + have + " < " + need + ".");
            }
            case "custom_metric_below" -> {
                if (player == null || condition.metric().isEmpty()) {
                    yield EchoConditionResult.unmatched("custom_metric_below requires player and metric.");
                }
                long have = customMetric(player, condition.metric().get());
                long limit = condition.amount().orElseGet(() -> condition.count().orElse(0));
                yield have < limit ? EchoConditionResult.matched("Custom metric " + have + " < " + limit + ".")
                        : EchoConditionResult.unmatched("Custom metric " + have + " >= " + limit + ".");
            }
            case "branch_marker_set" -> {
                if (player == null || marker(condition).isBlank()) {
                    yield EchoConditionResult.unmatched("branch_marker_set requires player and value.");
                }
                yield branchMarker(player, marker(condition)) ? EchoConditionResult.matched("Branch marker is set.")
                        : EchoConditionResult.unmatched("Branch marker is not set.");
            }
            case "dialogue_choice_made" -> {
                if (player == null || condition.poi().isEmpty()) {
                    yield EchoConditionResult.unmatched("dialogue_choice_made requires player, dialogue, and value.");
                }
                Identifier dialogue = condition.poi().orElse(null);
                yield dialogue != null && dialogueChoiceMade(player, dialogue, marker(condition))
                        ? EchoConditionResult.matched("Dialogue choice was made.")
                        : EchoConditionResult.unmatched("Dialogue choice was not made.");
            }
            default -> EchoConditionResult.unsupported("DataCore runtime does not evaluate " + condition.type() + ".");
        };
    }

    private void registerConditionKeys(EchoCondition condition) {
        if (condition == null) {
            return;
        }
        condition.all().forEach(this::registerConditionKeys);
        condition.any().forEach(this::registerConditionKeys);
        condition.state().ifPresent(this::registerWorldState);
        condition.faction().ifPresent(this::registerFaction);
        condition.metric().ifPresent(this::registerMetric);
        if ("branch_marker_set".equals(condition.type()) && !marker(condition).isBlank()) {
            registerBranch(marker(condition));
        }
        if ("dialogue_choice_made".equals(condition.type())) {
            condition.poi().ifPresent(dialogue -> registerBranch(dialogueChoiceMarker(dialogue, marker(condition))));
        }
    }

    private void registerActionKeys(EchoAction action) {
        if (action == null) {
            return;
        }
        action.state().ifPresent(this::registerWorldState);
        action.faction().ifPresent(this::registerFaction);
        action.metric().ifPresent(this::registerMetric);
        if (("set_branch_marker".equals(action.type()) || "clear_branch_marker".equals(action.type())) && !marker(action).isBlank()) {
            registerBranch(marker(action));
        }
        if ("record_dialogue_choice".equals(action.type())) {
            action.entry().ifPresent(dialogue -> registerBranch(dialogueChoiceMarker(dialogue, marker(action))));
        }
    }

    private void registerWorldState(Identifier state) {
        if (state != null) {
            service().registerKey(worldStateKey(state));
        }
    }

    private void registerFaction(Identifier faction) {
        if (faction != null) {
            service().registerKey(factionKey(faction));
        }
    }

    private void registerMetric(String metric) {
        if (metric != null && !metric.isBlank()) {
            service().registerKey(metricKey(metric));
        }
    }

    private void registerBranch(String marker) {
        if (marker != null && !marker.isBlank()) {
            service().registerKey(branchKey(marker));
        }
    }

    private static IDataKey<Boolean> worldStateKey(Identifier state) {
        return IDataKey.flag(EchoScriptCore.id("world_state/" + keyPath(state)), DataScope.WORLD, false, true);
    }

    private static IDataKey<Long> factionKey(Identifier faction) {
        return IDataKey.counter(EchoScriptCore.id("faction_reputation/" + keyPath(faction)), DataScope.PLAYER, 0L, true);
    }

    private static IDataKey<Long> metricKey(String metric) {
        return IDataKey.counter(EchoScriptCore.id("custom_metric/" + safePath(metric)), DataScope.PLAYER, 0L, true);
    }

    private static IDataKey<Boolean> branchKey(String marker) {
        return IDataKey.flag(EchoScriptCore.id("branch/" + safePath(marker)), DataScope.PLAYER, false, true);
    }

    private static IDataService service() {
        return EchoCoreServices.dataService();
    }

    private static Level level(EchoScriptExecutionContext context) {
        if (context == null) {
            return null;
        }
        if (context.player().isPresent()) {
            return context.player().get().level();
        }
        MinecraftServer server = context.server().orElse(null);
        return server == null ? null : server.overworld();
    }

    private static long longValue(EchoAction action) {
        if (action == null) {
            return 0L;
        }
        if (action.amount().isPresent()) {
            return action.amount().get();
        }
        if (action.count().isPresent()) {
            return action.count().get();
        }
        if (action.value().isPresent()) {
            try {
                return Long.parseLong(action.value().get());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static String marker(EchoAction action) {
        if (action == null) {
            return "";
        }
        return action.value().or(() -> action.id()).orElse("");
    }

    private static String marker(EchoCondition condition) {
        if (condition == null) {
            return "";
        }
        return condition.value().or(() -> condition.id()).orElse("");
    }

    private static String dialogueChoiceMarker(Identifier dialogue, String choice) {
        return "dialogue/" + keyPath(dialogue) + "/" + safePath(choice == null ? "" : choice);
    }

    private static String keyPath(Identifier id) {
        if (id == null) {
            return "unknown";
        }
        return safePath(id.getNamespace()) + "/" + safePath(id.getPath());
    }

    private static String safePath(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(':', '/')
                .replaceAll("[^a-z0-9_./-]", "_")
                .replaceAll("/+", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "unknown" : cleaned;
    }
}
