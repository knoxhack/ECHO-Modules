package com.knoxhack.echo.scriptcore.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoActionResult;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoConditionResult;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptExecutionContext;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWeatherEventDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echo.scriptcore.util.EchoJson;
import com.knoxhack.echo.scriptcore.validation.EchoScriptKnownTypes;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ScriptCoreUiExecutionService {
    public static final ScriptCoreUiExecutionService INSTANCE = new ScriptCoreUiExecutionService();
    public static final String SCREENCORE_ACTION = "scriptcore.execute";
    public static final String SCREENCORE_PREVIEW_ACTION = "scriptcore.preview";
    public static final String DEFAULT_SLOT = "actions";
    public static final int MAX_ACTIONS_PER_TRIGGER = 16;
    public static final int MAX_PARAMS_PER_TRIGGER = 16;
    public static final int MAX_PARAM_KEY_LENGTH = 40;
    public static final int MAX_PARAM_VALUE_LENGTH = 256;

    private static final Pattern PARAM_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1," + MAX_PARAM_KEY_LENGTH + "}");
    private static final Pattern EXACT_PARAM_PLACEHOLDER = Pattern.compile("\\{param\\.([A-Za-z0-9_.-]+)}");
    private static final String UI_REASON = "screencore_ui";

    private volatile UiExecutionResult lastServerRejection = UiExecutionResult.empty(UiExecutionMode.EXECUTE);

    private ScriptCoreUiExecutionService() {
    }

    public UiExecutionResult preview(
            ServerPlayer player,
            Identifier definitionId,
            String slot,
            String pageId,
            String componentId,
            String actionValue,
            Map<String, String> params) {
        return evaluate(new UiExecutionIntent(UiExecutionMode.PREVIEW, player, definitionId, slot, pageId,
                componentId, actionValue, params));
    }

    public UiExecutionResult execute(
            ServerPlayer player,
            Identifier definitionId,
            String slot,
            String pageId,
            String componentId,
            String actionValue) {
        return execute(player, definitionId, slot, pageId, componentId, actionValue, Map.of());
    }

    public UiExecutionResult execute(
            ServerPlayer player,
            Identifier definitionId,
            String slot,
            String pageId,
            String componentId,
            String actionValue,
            Map<String, String> params) {
        return evaluate(new UiExecutionIntent(UiExecutionMode.EXECUTE, player, definitionId, slot, pageId,
                componentId, actionValue, params));
    }

    public UiExecutionResult evaluate(UiExecutionIntent intent) {
        if (!ScriptCoreConfig.screenCoreUiActionsAllowed()) {
            return rejected(intent, "disabled", "ScreenCore UI action execution is disabled by ScriptCore config.", 0, 0);
        }
        if (!bridgeModulesLoaded()) {
            return rejected(intent, "bridge-unavailable", bridgeStatus(), 0, 0);
        }
        if (intent.player() == null) {
            return rejected(intent, "missing-player", "ScreenCore UI action execution requires a server player.", 0, 0);
        }
        if (intent.definitionId() == null) {
            return rejected(intent, "missing-definition", "ScreenCore UI action execution requires a definition id.", 0, 0);
        }
        Optional<EchoScriptDefinitionView> definition = EchoScriptRegistry.INSTANCE.get(intent.definitionId());
        if (definition.isEmpty()) {
            return rejected(intent, "missing-definition", "No ScriptCore definition is registered for "
                    + intent.definitionId() + ".", 0, 0);
        }
        EchoScriptExecutionContext context = context(intent);
        UiExecutionResult gates = evaluateConditions(definition.get().unlockConditions(), context, "unlock_conditions");
        if (!gates.success()) {
            return rejected(intent, gates.code(), gates.message(), 0, 0);
        }
        gates = evaluateConditions(definition.get().conditions(), context, "conditions");
        if (!gates.success()) {
            return rejected(intent, gates.code(), gates.message(), 0, 0);
        }
        ActionSelection selection = actions(definition.get(), intent.slot());
        if (!selection.valid()) {
            return rejected(intent, "invalid-slot", selection.message(), 0, 0);
        }
        gates = evaluateConditions(selection.conditions(), context, selection.slot() + ".conditions");
        if (!gates.success()) {
            return rejected(intent, gates.code(), gates.message(), 0, 0);
        }
        List<EchoAction> actions = selection.actions();
        if (actions.isEmpty()) {
            return rejected(intent, "empty-actions", "Slot " + selection.slot()
                    + " has no ScriptCore actions to execute.", 0, 0);
        }
        if (actions.size() > MAX_ACTIONS_PER_TRIGGER) {
            return rejected(intent, "too-many-actions", "Slot " + selection.slot() + " has " + actions.size()
                    + " actions; the UI execution cap is " + MAX_ACTIONS_PER_TRIGGER + ".", actions.size(), 0);
        }
        ParamValidation params = validateParams(definition.get(), intent.params());
        if (!params.success()) {
            return rejected(intent, params.code(), params.message(), actions.size(), 0);
        }
        ActionResolution resolved = resolveActionParams(actions, params.values(), selection.slot());
        if (!resolved.success()) {
            return rejected(intent, resolved.code(), resolved.message(), actions.size(), 0);
        }
        UiExecutionResult preflight = preflightActions(resolved.actions(), selection.slot());
        if (!preflight.success()) {
            return rejected(intent, preflight.code(), preflight.message(), resolved.actions().size(), 0);
        }
        if (intent.mode() == UiExecutionMode.PREVIEW) {
            return success(intent, "preview-ok", "Preview accepted " + resolved.actions().size()
                    + " ScriptCore action(s).", resolved.actions().size(), 0);
        }
        for (int i = 0; i < resolved.actions().size(); i++) {
            EchoAction action = resolved.actions().get(i);
            EchoActionResult result = EchoScriptAdapterRegistry.INSTANCE.executeAction(action, context);
            if (!result.supported()) {
                return rejected(intent, "unsupported-action", result.message(), resolved.actions().size(), i);
            }
            if (!result.success()) {
                return rejected(intent, "action-failed", result.message(), resolved.actions().size(), i);
            }
        }
        return success(intent, "ok", "Executed " + resolved.actions().size() + " ScriptCore action(s).",
                resolved.actions().size(), resolved.actions().size());
    }

    public String bridgeStatus() {
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ENABLED, true)) {
            return "disabled: ScriptCore loading is disabled";
        }
        if (ScriptCoreConfig.bool(ScriptCoreConfig.READ_ONLY_MODE, false)) {
            return "disabled: read_only_mode is true";
        }
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ALLOW_SCREENCORE_UI_ACTIONS, false)) {
            return "disabled by config: allow_screencore_ui_actions=false";
        }
        boolean screenCore = EchoRuntimeModules.isLoaded("echoscreencore");
        boolean netCore = EchoRuntimeModules.isLoaded("echonetcore");
        if (!screenCore && !netCore) {
            return "unavailable: missing ScreenCore and NetCore";
        }
        if (!screenCore) {
            return "unavailable: missing ScreenCore";
        }
        if (!netCore) {
            return "unavailable: missing NetCore";
        }
        return "enabled: execute, preview, typed params, result packets";
    }

    public String lastServerRejectionStatus() {
        UiExecutionResult rejection = lastServerRejection;
        if (rejection == null || rejection.success()) {
            return "none";
        }
        return rejection.code() + " " + rejection.definitionId() + " slot=" + rejection.slot()
                + " page=" + rejection.pageId() + " component=" + rejection.componentId()
                + " message=" + rejection.message();
    }

    private boolean bridgeModulesLoaded() {
        return EchoRuntimeModules.isLoaded("echoscreencore") && EchoRuntimeModules.isLoaded("echonetcore");
    }

    private UiExecutionResult preflightActions(List<EchoAction> actions, String slot) {
        for (int i = 0; i < actions.size(); i++) {
            EchoAction action = actions.get(i);
            if (action == null) {
                return reject("unsupported-action", "Slot " + slot + " contains a null action at index " + i + ".", 0);
            }
            String type = action.type();
            if ("custom".equals(type)) {
                return reject("unsupported-action", "UI execution rejects custom action type at index " + i + ".", 0);
            }
            if (!EchoScriptKnownTypes.ACTION_TYPES.contains(type)) {
                return reject("unsupported-action", "UI execution rejects unknown action type " + type
                        + " at index " + i + ".", 0);
            }
        }
        return ok("Actions passed UI preflight.");
    }

    private UiExecutionResult evaluateConditions(
            List<EchoCondition> conditions,
            EchoScriptExecutionContext context,
            String label) {
        for (EchoCondition condition : conditions == null ? List.<EchoCondition>of() : conditions) {
            if (condition == null) {
                return reject("unsupported-condition", label + " contains a null condition.", 0);
            }
            EchoConditionResult result = EchoScriptAdapterRegistry.INSTANCE.evaluateCondition(condition, context);
            if (!result.supported()) {
                return reject("unsupported-condition", label + " contains unsupported condition "
                        + condition.type() + ": " + result.message(), 0);
            }
            if (!result.matched()) {
                return reject("condition-unmet", label + " did not match: " + result.message(), 0);
            }
        }
        return ok("Conditions matched.");
    }

    private ParamValidation validateParams(EchoScriptDefinitionView definition, Map<String, String> rawParams) {
        Map<String, String> supplied = sanitizeParams(rawParams);
        if (supplied.size() > MAX_PARAMS_PER_TRIGGER) {
            return ParamValidation.reject("too-many-params", "ScreenCore UI supplied " + supplied.size()
                    + " params; the UI param cap is " + MAX_PARAMS_PER_TRIGGER + ".");
        }
        Map<String, UiParamSpec> specs = paramSpecs(definition);
        for (String key : supplied.keySet()) {
            if (!specs.containsKey(key)) {
                return ParamValidation.reject("undeclared-param", "ScreenCore UI param " + key
                        + " is not declared by " + definition.id() + ".");
            }
        }
        LinkedHashMap<String, String> validated = new LinkedHashMap<>();
        for (UiParamSpec spec : specs.values()) {
            String raw = supplied.getOrDefault(spec.id(), "");
            if (raw.isBlank()) {
                if (spec.required()) {
                    return ParamValidation.reject("missing-param", "Required ScreenCore UI param "
                            + spec.id() + " is missing.");
                }
                continue;
            }
            ParamValidationResult result = validateParam(spec, raw);
            if (!result.success()) {
                return ParamValidation.reject(result.code(), result.message());
            }
            validated.put(spec.id(), result.value());
        }
        return new ParamValidation(true, "ok", "", Map.copyOf(validated));
    }

    private static Map<String, String> sanitizeParams(Map<String, String> rawParams) {
        LinkedHashMap<String, String> clean = new LinkedHashMap<>();
        if (rawParams == null || rawParams.isEmpty()) {
            return Map.of();
        }
        for (Map.Entry<String, String> entry : rawParams.entrySet()) {
            String key = safe(entry.getKey());
            if (key.isBlank()) {
                continue;
            }
            String value = safe(entry.getValue());
            clean.put(limit(key, MAX_PARAM_KEY_LENGTH), limit(value, MAX_PARAM_VALUE_LENGTH));
        }
        return clean;
    }

    private static ParamValidationResult validateParam(UiParamSpec spec, String rawValue) {
        String value = limit(safe(rawValue), MAX_PARAM_VALUE_LENGTH);
        if (!PARAM_ID_PATTERN.matcher(spec.id()).matches()) {
            return ParamValidationResult.reject("invalid-param-schema", "Invalid ScreenCore UI param id: " + spec.id() + ".");
        }
        return switch (spec.type()) {
            case "string" -> validateStringParam(spec, value);
            case "identifier" -> {
                Identifier id = Identifier.tryParse(value);
                yield id == null
                        ? ParamValidationResult.reject("invalid-param", "Param " + spec.id() + " must be a valid identifier.")
                        : ParamValidationResult.ok(id.toString());
            }
            case "int", "integer" -> validateIntParam(spec, value);
            case "boolean", "bool" -> {
                String normalized = value.toLowerCase(Locale.ROOT);
                yield switch (normalized) {
                    case "true", "false" -> ParamValidationResult.ok(normalized);
                    default -> ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                            + " must be true or false.");
                };
            }
            case "enum" -> spec.values().contains(value)
                    ? ParamValidationResult.ok(value)
                    : ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                            + " must be one of " + spec.values() + ".");
            default -> ParamValidationResult.reject("invalid-param-schema", "Unsupported ScreenCore UI param type "
                    + spec.type() + " for " + spec.id() + ".");
        };
    }

    private static ParamValidationResult validateStringParam(UiParamSpec spec, String value) {
        if (spec.minLength().isPresent() && value.length() < spec.minLength().get()) {
            return ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                    + " is shorter than " + spec.minLength().get() + " characters.");
        }
        if (spec.maxLength().isPresent() && value.length() > spec.maxLength().get()) {
            return ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                    + " is longer than " + spec.maxLength().get() + " characters.");
        }
        if (spec.pattern().isPresent() && !Pattern.matches(spec.pattern().get(), value)) {
            return ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                    + " does not match its required pattern.");
        }
        return ParamValidationResult.ok(value);
    }

    private static ParamValidationResult validateIntParam(UiParamSpec spec, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (spec.min().isPresent() && parsed < spec.min().get()) {
                return ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                        + " must be at least " + spec.min().get() + ".");
            }
            if (spec.max().isPresent() && parsed > spec.max().get()) {
                return ParamValidationResult.reject("invalid-param", "Param " + spec.id()
                        + " must be at most " + spec.max().get() + ".");
            }
            return ParamValidationResult.ok(Integer.toString(parsed));
        } catch (NumberFormatException exception) {
            return ParamValidationResult.reject("invalid-param", "Param " + spec.id() + " must be an integer.");
        }
    }

    private static Map<String, UiParamSpec> paramSpecs(EchoScriptDefinitionView definition) {
        LinkedHashMap<String, UiParamSpec> specs = new LinkedHashMap<>();
        JsonObject metadata = EchoJson.object(definition.rawJson(), "metadata");
        JsonObject ui = EchoJson.object(metadata, "screencore_ui");
        JsonElement params = ui.get("params");
        if (params == null || params.isJsonNull()) {
            return Map.of();
        }
        if (params.isJsonArray()) {
            for (JsonElement element : params.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    UiParamSpec spec = paramSpec(EchoJson.string(element.getAsJsonObject(), "id", ""),
                            element.getAsJsonObject());
                    if (!spec.id().isBlank()) {
                        specs.put(spec.id(), spec);
                    }
                }
            }
        } else if (params.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    UiParamSpec spec = paramSpec(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (!spec.id().isBlank()) {
                        specs.put(spec.id(), spec);
                    }
                } else if (entry.getValue().isJsonPrimitive()) {
                    specs.put(entry.getKey(), new UiParamSpec(entry.getKey(), entry.getValue().getAsString(),
                            false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                            Optional.empty(), List.of()));
                }
            }
        }
        return Map.copyOf(specs);
    }

    private static UiParamSpec paramSpec(String id, JsonObject json) {
        return new UiParamSpec(
                safe(id),
                EchoJson.string(json, "type", "string").trim().toLowerCase(Locale.ROOT),
                EchoJson.bool(json, "required", false),
                optionalInt(json, "min"),
                optionalInt(json, "max"),
                optionalInt(json, "min_length").or(() -> optionalInt(json, "minLength")),
                optionalInt(json, "max_length").or(() -> optionalInt(json, "maxLength")),
                EchoJson.optionalString(json, "pattern"),
                EchoJson.strings(json, "values"));
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return EchoJson.optionalInt(json, key);
    }

    private ActionResolution resolveActionParams(List<EchoAction> actions, Map<String, String> params, String slot) {
        ArrayList<EchoAction> resolved = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            EchoAction action = actions.get(i);
            if (action == null) {
                return ActionResolution.reject("unsupported-action", "Slot " + slot
                        + " contains a null action at index " + i + ".");
            }
            JsonElement resolvedJson = resolveElement(action.rawJson(), params);
            if (resolvedJson == null || !resolvedJson.isJsonObject()) {
                return ActionResolution.reject("invalid-placeholder", "Could not resolve UI params for action "
                        + i + " in slot " + slot + ".");
            }
            PlaceholderScan scan = scanPlaceholders(resolvedJson);
            if (!scan.success()) {
                return ActionResolution.reject(scan.code(), "Action " + i + " in slot " + slot + ": " + scan.message());
            }
            try {
                resolved.add(EchoJson.action(resolvedJson.getAsJsonObject()));
            } catch (RuntimeException exception) {
                return ActionResolution.reject("invalid-action", "Action " + i + " could not be parsed after UI param resolution.");
            }
        }
        return new ActionResolution(true, "ok", "", List.copyOf(resolved));
    }

    private static JsonElement resolveElement(JsonElement element, Map<String, String> params) {
        if (element == null || element.isJsonNull()) {
            return element;
        }
        if (element.isJsonObject()) {
            JsonObject object = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                object.add(entry.getKey(), resolveElement(entry.getValue(), params));
            }
            return object;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                array.add(resolveElement(child, params));
            }
            return array;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString();
            java.util.regex.Matcher matcher = EXACT_PARAM_PLACEHOLDER.matcher(raw);
            if (matcher.matches()) {
                String value = params.get(matcher.group(1));
                return value == null ? new JsonPrimitive(raw) : new JsonPrimitive(value);
            }
        }
        return element.deepCopy();
    }

    private static PlaceholderScan scanPlaceholders(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return PlaceholderScan.ok();
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                PlaceholderScan scan = scanPlaceholders(entry.getValue());
                if (!scan.success()) {
                    return scan;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                PlaceholderScan scan = scanPlaceholders(child);
                if (!scan.success()) {
                    return scan;
                }
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.contains("{param.")) {
                return EXACT_PARAM_PLACEHOLDER.matcher(value).matches()
                        ? PlaceholderScan.reject("unresolved-param", "Unresolved UI param placeholder " + value + ".")
                        : PlaceholderScan.reject("invalid-placeholder", "Embedded UI param placeholders are not allowed.");
            }
        }
        return PlaceholderScan.ok();
    }

    private static EchoScriptExecutionContext context(UiExecutionIntent intent) {
        return new EchoScriptExecutionContext(
                Optional.of(intent.player()),
                Optional.ofNullable(intent.player().level().getServer()),
                UI_REASON,
                contextData(intent));
    }

    private static Map<String, Object> contextData(UiExecutionIntent intent) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("definition_id", intent.definitionId() == null ? "" : intent.definitionId().toString());
        data.put("slot", normalizeSlot(intent.slot()));
        data.put("page_id", safe(intent.pageId()));
        data.put("component_id", safe(intent.componentId()));
        data.put("action_value", safe(intent.actionValue()));
        data.put("mode", intent.mode().wireName());
        data.put("params", sanitizeParams(intent.params()));
        return Map.copyOf(data);
    }

    private static ActionSelection actions(EchoScriptDefinitionView definition, String rawSlot) {
        String slot = normalizeSlot(rawSlot);
        String normalized = slot.toLowerCase(Locale.ROOT);
        if (DEFAULT_SLOT.equals(normalized)) {
            return ActionSelection.valid(DEFAULT_SLOT, definition.actions(), List.of());
        }
        if (definition instanceof EchoMissionDefinition mission) {
            return switch (normalized) {
                case "on_start" -> ActionSelection.valid("on_start", mission.onStart(), List.of());
                case "on_complete" -> ActionSelection.valid("on_complete", mission.onComplete(), List.of());
                case "on_fail" -> ActionSelection.valid("on_fail", mission.onFail(), List.of());
                default -> ActionSelection.invalid("Unsupported mission action slot: " + slot + ".");
            };
        }
        if ("effects".equals(normalized) && definition instanceof EchoWeatherEventDefinition weather) {
            return ActionSelection.valid("effects", weather.effects(), List.of());
        }
        if ("effects".equals(normalized) && definition instanceof EchoWorldStateDefinition worldState) {
            return ActionSelection.valid("effects", worldState.effects(), List.of());
        }
        if (normalized.startsWith("choice:") && definition instanceof EchoDialogueDefinition dialogue) {
            String choiceId = slot.substring(slot.indexOf(':') + 1).trim();
            String choiceSlot = slot;
            return dialogue.choices().stream()
                    .filter(choice -> choice.id().equals(choiceId))
                    .findFirst()
                    .map(choice -> ActionSelection.valid(choiceSlot, choice.actions(), choice.conditions()))
                    .orElseGet(() -> ActionSelection.invalid("Dialogue choice not found: " + choiceId + "."));
        }
        return ActionSelection.invalid("Unsupported ScriptCore UI action slot: " + slot + ".");
    }

    private UiExecutionResult success(UiExecutionIntent intent, String code, String message, int actionCount, int executed) {
        return new UiExecutionResult(intent.mode(), intent.definitionId(), normalizeSlot(intent.slot()),
                safe(intent.pageId()), safe(intent.componentId()), true, code, message, actionCount, executed);
    }

    private UiExecutionResult rejected(UiExecutionIntent intent, String code, String message, int actionCount, int executed) {
        UiExecutionResult result = new UiExecutionResult(intent.mode(), intent.definitionId(), normalizeSlot(intent.slot()),
                safe(intent.pageId()), safe(intent.componentId()), false, code, message, actionCount, executed);
        lastServerRejection = result;
        return result;
    }

    private static UiExecutionResult reject(String code, String message, int executed) {
        return new UiExecutionResult(UiExecutionMode.EXECUTE, null, DEFAULT_SLOT, "", "", false, code,
                message == null ? "" : message, 0, Math.max(0, executed));
    }

    private static UiExecutionResult ok(String message) {
        return new UiExecutionResult(UiExecutionMode.EXECUTE, null, DEFAULT_SLOT, "", "", true, "ok",
                message == null ? "" : message, 0, 0);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeSlot(String value) {
        String slot = clean(value);
        return slot.isBlank() ? DEFAULT_SLOT : slot;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int maxLength) {
        String clean = safe(value);
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    public enum UiExecutionMode {
        EXECUTE("execute"),
        PREVIEW("preview");

        private final String wireName;

        UiExecutionMode(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        public static UiExecutionMode fromWire(String value) {
            String clean = safe(value).toLowerCase(Locale.ROOT);
            return "preview".equals(clean) ? PREVIEW : EXECUTE;
        }
    }

    public record UiExecutionIntent(
            UiExecutionMode mode,
            ServerPlayer player,
            Identifier definitionId,
            String slot,
            String pageId,
            String componentId,
            String actionValue,
            Map<String, String> params) {
        public UiExecutionIntent {
            mode = mode == null ? UiExecutionMode.EXECUTE : mode;
            slot = normalizeSlot(slot);
            pageId = safe(pageId);
            componentId = safe(componentId);
            actionValue = safe(actionValue);
            params = sanitizeParams(params);
        }
    }

    public record UiExecutionResult(
            UiExecutionMode mode,
            Identifier definitionId,
            String slot,
            String pageId,
            String componentId,
            boolean success,
            String code,
            String message,
            int actionCount,
            int executedActions) {
        public UiExecutionResult {
            mode = mode == null ? UiExecutionMode.EXECUTE : mode;
            slot = normalizeSlot(slot);
            pageId = safe(pageId);
            componentId = safe(componentId);
            code = code == null || code.isBlank() ? (success ? "ok" : "rejected") : code;
            message = message == null ? "" : message;
            actionCount = Math.max(0, actionCount);
            executedActions = Math.max(0, executedActions);
        }

        static UiExecutionResult empty(UiExecutionMode mode) {
            return new UiExecutionResult(mode, null, DEFAULT_SLOT, "", "", true, "none", "", 0, 0);
        }
    }

    private record ActionSelection(
            boolean valid,
            String slot,
            List<EchoAction> actions,
            List<EchoCondition> conditions,
            String message) {
        private ActionSelection {
            slot = slot == null || slot.isBlank() ? DEFAULT_SLOT : slot;
            actions = List.copyOf(actions == null ? List.of() : actions);
            conditions = List.copyOf(conditions == null ? List.of() : conditions);
            message = message == null ? "" : message;
        }

        static ActionSelection valid(String slot, List<EchoAction> actions, List<EchoCondition> conditions) {
            return new ActionSelection(true, slot, actions, conditions, "");
        }

        static ActionSelection invalid(String message) {
            return new ActionSelection(false, "", List.of(), List.of(), message);
        }
    }

    private record UiParamSpec(
            String id,
            String type,
            boolean required,
            Optional<Integer> min,
            Optional<Integer> max,
            Optional<Integer> minLength,
            Optional<Integer> maxLength,
            Optional<String> pattern,
            List<String> values) {
        private UiParamSpec {
            id = safe(id);
            type = type == null || type.isBlank() ? "string" : type.trim().toLowerCase(Locale.ROOT);
            min = min == null ? Optional.empty() : min;
            max = max == null ? Optional.empty() : max;
            minLength = minLength == null ? Optional.empty() : minLength;
            maxLength = maxLength == null ? Optional.empty() : maxLength;
            pattern = pattern == null ? Optional.empty() : pattern;
            values = List.copyOf(values == null ? List.of() : values);
        }
    }

    private record ParamValidation(boolean success, String code, String message, Map<String, String> values) {
        private ParamValidation {
            code = code == null || code.isBlank() ? (success ? "ok" : "invalid-param") : code;
            message = message == null ? "" : message;
            values = Map.copyOf(values == null ? Map.of() : values);
        }

        static ParamValidation reject(String code, String message) {
            return new ParamValidation(false, code, message, Map.of());
        }
    }

    private record ParamValidationResult(boolean success, String code, String message, String value) {
        private ParamValidationResult {
            code = code == null || code.isBlank() ? (success ? "ok" : "invalid-param") : code;
            message = message == null ? "" : message;
            value = value == null ? "" : value;
        }

        static ParamValidationResult ok(String value) {
            return new ParamValidationResult(true, "ok", "", value);
        }

        static ParamValidationResult reject(String code, String message) {
            return new ParamValidationResult(false, code, message, "");
        }
    }

    private record ActionResolution(boolean success, String code, String message, List<EchoAction> actions) {
        private ActionResolution {
            code = code == null || code.isBlank() ? (success ? "ok" : "invalid-action") : code;
            message = message == null ? "" : message;
            actions = List.copyOf(actions == null ? List.of() : actions);
        }

        static ActionResolution reject(String code, String message) {
            return new ActionResolution(false, code, message, List.of());
        }
    }

    private record PlaceholderScan(boolean success, String code, String message) {
        static PlaceholderScan ok() {
            return new PlaceholderScan(true, "ok", "");
        }

        static PlaceholderScan reject(String code, String message) {
            return new PlaceholderScan(false, code, message);
        }
    }
}
