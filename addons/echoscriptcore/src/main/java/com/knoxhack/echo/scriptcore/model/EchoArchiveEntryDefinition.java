package com.knoxhack.echo.scriptcore.model;

import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoArchiveEntryDefinition(
        EchoScriptDefinition base,
        String category,
        String subtitle,
        List<String> content,
        List<Identifier> relatedMissions,
        List<Identifier> relatedScans,
        List<Identifier> relatedPois,
        String importance) implements EchoScriptDefinitionView {
    public EchoArchiveEntryDefinition {
        category = category == null || category.isBlank() ? "general" : category;
        subtitle = subtitle == null ? "" : subtitle;
        content = List.copyOf(content == null ? List.of() : content);
        relatedMissions = List.copyOf(relatedMissions == null ? List.of() : relatedMissions);
        relatedScans = List.copyOf(relatedScans == null ? List.of() : relatedScans);
        relatedPois = List.copyOf(relatedPois == null ? List.of() : relatedPois);
        importance = importance == null || importance.isBlank() ? "common" : importance.trim().toLowerCase(java.util.Locale.ROOT);
    }
    @Override public int schemaVersion() { return base.schemaVersion(); }
    @Override public String pack() { return base.pack(); }
    @Override public Identifier id() { return base.id(); }
    @Override public String type() { return base.type(); }
    @Override public Optional<String> title() { return base.title(); }
    @Override public Optional<String> description() { return base.description(); }
    @Override public Optional<String> source() { return base.source(); }
    @Override public List<String> tags() { return base.tags(); }
    @Override public List<EchoCondition> unlockConditions() { return base.unlockConditions(); }
    @Override public List<EchoCondition> conditions() { return base.conditions(); }
    @Override public List<EchoAction> actions() { return base.actions(); }
    @Override public Map<String, Object> metadata() { return base.metadata(); }
    @Override public JsonObject rawJson() { return base.rawJson(); }
    @Override public Optional<Path> sourceFile() { return base.sourceFile(); }
}
