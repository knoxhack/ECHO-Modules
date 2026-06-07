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

public record EchoLensScanDefinition(
        EchoScriptDefinition base,
        Optional<Identifier> target,
        String targetType,
        String summary,
        List<String> details,
        String danger) implements EchoScriptDefinitionView {
    public EchoLensScanDefinition {
        target = target == null ? Optional.empty() : target;
        targetType = targetType == null || targetType.isBlank() ? "custom" : targetType.trim().toLowerCase(java.util.Locale.ROOT);
        summary = summary == null ? "" : summary;
        details = List.copyOf(details == null ? List.of() : details);
        danger = danger == null ? "" : danger;
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
