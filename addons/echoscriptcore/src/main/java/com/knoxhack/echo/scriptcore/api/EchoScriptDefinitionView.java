package com.knoxhack.echo.scriptcore.api;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface EchoScriptDefinitionView {
    int schemaVersion();

    String pack();

    Identifier id();

    String type();

    Optional<String> title();

    Optional<String> description();

    Optional<String> source();

    List<String> tags();

    List<EchoCondition> unlockConditions();

    List<EchoCondition> conditions();

    List<EchoAction> actions();

    Map<String, Object> metadata();

    JsonObject rawJson();

    Optional<Path> sourceFile();
}
