package com.knoxhack.echo.scriptcore.registry;

import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptRegistryView;
import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.model.EchoDialogueDefinition;
import com.knoxhack.echo.scriptcore.model.EchoEndingDefinition;
import com.knoxhack.echo.scriptcore.model.EchoFactionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapLayerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoHoloMapMarkerDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLensScanDefinition;
import com.knoxhack.echo.scriptcore.model.EchoLootProfileDefinition;
import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoRecipeUnlockDefinition;
import com.knoxhack.echo.scriptcore.model.EchoTutorialHintDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWeatherEventDefinition;
import com.knoxhack.echo.scriptcore.model.EchoWorldStateDefinition;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoScriptRegistry implements EchoScriptRegistryView {
    public static final EchoScriptRegistry INSTANCE = new EchoScriptRegistry();

    private final Map<Identifier, EchoScriptDefinitionView> definitions = new LinkedHashMap<>();

    private EchoScriptRegistry() {
    }

    public synchronized void clear() {
        definitions.clear();
    }

    public synchronized void register(EchoScriptDefinitionView definition) {
        if (definition == null || definition.id() == null) {
            return;
        }
        definitions.put(definition.id(), definition);
    }

    public synchronized void replaceAll(Collection<? extends EchoScriptDefinitionView> nextDefinitions) {
        definitions.clear();
        if (nextDefinitions == null) {
            return;
        }
        nextDefinitions.stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(this::register);
    }

    @Override
    public synchronized List<EchoScriptDefinitionView> all() {
        return List.copyOf(definitions.values());
    }

    @Override
    public synchronized Optional<EchoScriptDefinitionView> get(Identifier id) {
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public synchronized List<EchoScriptDefinitionView> getByType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        return definitions.values().stream()
                .filter(definition -> definition.type().equals(normalized))
                .toList();
    }

    @Override
    public synchronized List<EchoScriptDefinitionView> getByPack(String pack) {
        String normalized = pack == null ? "" : pack.trim();
        return definitions.values().stream()
                .filter(definition -> definition.pack().equals(normalized))
                .toList();
    }

    @Override
    public Optional<EchoMissionDefinition> getMission(Identifier id) {
        return typed(id, EchoMissionDefinition.class);
    }

    @Override
    public Optional<EchoArchiveEntryDefinition> getArchiveEntry(Identifier id) {
        return typed(id, EchoArchiveEntryDefinition.class);
    }

    @Override
    public Optional<EchoLensScanDefinition> getLensScan(Identifier id) {
        return typed(id, EchoLensScanDefinition.class);
    }

    @Override
    public Optional<EchoHoloMapLayerDefinition> getHoloMapLayer(Identifier id) {
        return typed(id, EchoHoloMapLayerDefinition.class);
    }

    @Override
    public Optional<EchoHoloMapMarkerDefinition> getHoloMapMarker(Identifier id) {
        return typed(id, EchoHoloMapMarkerDefinition.class);
    }

    @Override
    public Optional<EchoWeatherEventDefinition> getWeatherEvent(Identifier id) {
        return typed(id, EchoWeatherEventDefinition.class);
    }

    @Override
    public Optional<EchoFactionDefinition> getFaction(Identifier id) {
        return typed(id, EchoFactionDefinition.class);
    }

    @Override
    public Optional<EchoWorldStateDefinition> getWorldState(Identifier id) {
        return typed(id, EchoWorldStateDefinition.class);
    }

    @Override
    public Optional<EchoTutorialHintDefinition> getTutorialHint(Identifier id) {
        return typed(id, EchoTutorialHintDefinition.class);
    }

    @Override
    public Optional<EchoDialogueDefinition> getDialogue(Identifier id) {
        return typed(id, EchoDialogueDefinition.class);
    }

    @Override
    public Optional<EchoEndingDefinition> getEnding(Identifier id) {
        return typed(id, EchoEndingDefinition.class);
    }

    @Override
    public Optional<EchoRecipeUnlockDefinition> getRecipeUnlock(Identifier id) {
        return typed(id, EchoRecipeUnlockDefinition.class);
    }

    @Override
    public Optional<EchoLootProfileDefinition> getLootProfile(Identifier id) {
        return typed(id, EchoLootProfileDefinition.class);
    }

    @Override
    public synchronized Map<String, Integer> countByType() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EchoScriptDefinitionView definition : definitions.values()) {
            counts.merge(definition.type(), 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }

    @Override
    public synchronized Map<String, Integer> countByPack() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EchoScriptDefinitionView definition : definitions.values()) {
            counts.merge(definition.pack(), 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }

    private synchronized <T extends EchoScriptDefinitionView> Optional<T> typed(Identifier id, Class<T> type) {
        EchoScriptDefinitionView definition = definitions.get(id);
        return type.isInstance(definition) ? Optional.of(type.cast(definition)) : Optional.empty();
    }
}
