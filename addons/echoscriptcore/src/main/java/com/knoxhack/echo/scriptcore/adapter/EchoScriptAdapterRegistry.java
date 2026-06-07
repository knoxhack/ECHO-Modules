package com.knoxhack.echo.scriptcore.adapter;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoActionResult;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoConditionResult;
import com.knoxhack.echo.scriptcore.api.EchoDiagnosticSink;
import com.knoxhack.echo.scriptcore.api.EchoScriptAdapter;
import com.knoxhack.echo.scriptcore.api.EchoScriptExecutionContext;
import com.knoxhack.echo.scriptcore.api.EchoScriptRegistryView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class EchoScriptAdapterRegistry {
    public static final EchoScriptAdapterRegistry INSTANCE = new EchoScriptAdapterRegistry();

    private final Map<Identifier, EchoScriptAdapter> adapters = new LinkedHashMap<>();

    private EchoScriptAdapterRegistry() {
    }

    public synchronized void registerDefaults() {
        register(new InternalFallbackAdapter());
        register(new MissionCoreAdapter());
        register(new TerminalAdapter());
        register(new LensAdapter());
        register(new HoloMapAdapter());
        register(new WeatherCoreAdapter());
        register(new TutorialCoreAdapter());
        register(new SoundCoreAdapter());
        register(new IndexAdapter());
        register(new DataCoreAdapter());
        register(new WorldCoreAdapter());
    }

    public synchronized void register(EchoScriptAdapter adapter) {
        if (adapter == null || adapter.id() == null) {
            return;
        }
        adapters.putIfAbsent(adapter.id(), adapter);
    }

    public synchronized List<EchoScriptAdapter> adapters() {
        return adapters.values().stream()
                .sorted(Comparator.comparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink sink) {
        for (EchoScriptAdapter adapter : adapters()) {
            try {
                adapter.registerDefinitions(registry, sink);
            } catch (RuntimeException exception) {
                EchoScriptCore.LOGGER.warn("ScriptCore adapter {} failed while registering definitions.", adapter.id(), exception);
            }
        }
    }

    public EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context) {
        if (action == null) {
            return EchoActionResult.unsupported("No action supplied.");
        }
        List<String> unavailable = new ArrayList<>();
        for (EchoScriptAdapter adapter : adapters()) {
            if (!adapter.supportedActions().contains(action.type())) {
                continue;
            }
            if (!adapter.isAvailable()) {
                unavailable.add(adapter.id().toString());
                continue;
            }
            try {
                EchoActionResult result = adapter.executeAction(action, context == null ? EchoScriptExecutionContext.empty() : context);
                if (result.supported()) {
                    return result;
                }
            } catch (RuntimeException exception) {
                EchoScriptCore.LOGGER.warn("ScriptCore adapter {} failed action {}.", adapter.id(), action.type(), exception);
                return EchoActionResult.failure("Adapter " + adapter.id() + " failed: " + exception.getMessage());
            }
        }
        String suffix = unavailable.isEmpty() ? "" : " Unavailable adapters: " + unavailable + ".";
        return EchoActionResult.unsupported("No available ScriptCore adapter supports action " + action.type() + "." + suffix);
    }

    public EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context) {
        if (condition == null) {
            return EchoConditionResult.unsupported("No condition supplied.");
        }
        List<String> unavailable = new ArrayList<>();
        for (EchoScriptAdapter adapter : adapters()) {
            if (!adapter.supportedConditions().contains(condition.type())) {
                continue;
            }
            if (!adapter.isAvailable()) {
                unavailable.add(adapter.id().toString());
                continue;
            }
            try {
                EchoConditionResult result = adapter.evaluateCondition(condition, context == null ? EchoScriptExecutionContext.empty() : context);
                if (result.supported()) {
                    return condition.not()
                            ? new EchoConditionResult(true, !result.matched(), "not(" + result.message() + ")")
                            : result;
                }
            } catch (RuntimeException exception) {
                EchoScriptCore.LOGGER.warn("ScriptCore adapter {} failed condition {}.", adapter.id(), condition.type(), exception);
                return EchoConditionResult.unsupported("Adapter " + adapter.id() + " failed: " + exception.getMessage());
            }
        }
        String suffix = unavailable.isEmpty() ? "" : " Unavailable adapters: " + unavailable + ".";
        return EchoConditionResult.unsupported("No available ScriptCore adapter supports condition " + condition.type() + "." + suffix);
    }
}
