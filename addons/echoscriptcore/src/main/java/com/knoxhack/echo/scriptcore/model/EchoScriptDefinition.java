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

public record EchoScriptDefinition(
        int schemaVersion,
        String pack,
        Identifier id,
        String type,
        Optional<String> title,
        Optional<String> description,
        Optional<String> source,
        List<String> tags,
        List<EchoCondition> unlockConditions,
        List<EchoCondition> conditions,
        List<EchoAction> actions,
        Map<String, Object> metadata,
        JsonObject rawJson,
        Optional<Path> sourceFile) implements EchoScriptDefinitionView {
    public EchoScriptDefinition {
        pack = pack == null || pack.isBlank() ? "unknown" : pack;
        type = type == null || type.isBlank() ? "generic" : type.trim().toLowerCase(java.util.Locale.ROOT);
        title = title == null ? Optional.empty() : title;
        description = description == null ? Optional.empty() : description;
        source = source == null ? Optional.empty() : source;
        tags = List.copyOf(tags == null ? List.of() : tags);
        unlockConditions = List.copyOf(unlockConditions == null ? List.of() : unlockConditions);
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
        actions = List.copyOf(actions == null ? List.of() : actions);
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        rawJson = rawJson == null ? new JsonObject() : rawJson.deepCopy();
        sourceFile = sourceFile == null ? Optional.empty() : sourceFile;
    }
}
