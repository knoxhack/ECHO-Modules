package com.knoxhack.echo.creatorcore.adapter;

import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorAdapterRegistry {
    private final Map<Identifier, CreatorAdapter> adapters = new LinkedHashMap<>();

    public synchronized void register(CreatorAdapter adapter) {
        if (adapter != null) {
            adapters.put(adapter.id(), adapter);
        }
    }

    public synchronized void registerDefaults() {
        if (!adapters.isEmpty()) {
            return;
        }
        register(new InternalFallbackCreatorAdapter());
        register(new ScriptCoreCreatorAdapter());
        register(new ScreenCoreCreatorAdapter());
        register(new TerminalCreatorAdapter());
        register(new MissionCoreCreatorAdapter());
        register(new LensCreatorAdapter());
        register(new HoloMapCreatorAdapter());
        register(new WeatherCoreCreatorAdapter());
        register(new TutorialCoreCreatorAdapter());
        register(new ThemeCoreCreatorAdapter());
        register(new TextureForgeCreatorAdapter());
        register(new WikiCreatorAdapter());
    }

    public synchronized List<CreatorAdapter> adapters() {
        return List.copyOf(adapters.values());
    }

    public synchronized Optional<CreatorAdapter> get(Identifier id) {
        return Optional.ofNullable(adapters.get(id));
    }

    public List<CreatorDefinitionSummary> listDefinitions() {
        List<CreatorDefinitionSummary> definitions = new ArrayList<>();
        for (CreatorAdapter adapter : adapters()) {
            definitions.addAll(adapter.listDefinitions());
        }
        return List.copyOf(definitions);
    }

    public List<CreatorDiagnostic> diagnostics() {
        List<CreatorDiagnostic> diagnostics = new ArrayList<>();
        for (CreatorAdapter adapter : adapters()) {
            diagnostics.addAll(adapter.diagnostics());
        }
        return List.copyOf(diagnostics);
    }

    public long availableCount() {
        return adapters().stream().filter(CreatorAdapter::isAvailable).count();
    }
}
