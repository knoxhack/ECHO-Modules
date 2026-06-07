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

public interface DelegatingScriptDefinition extends EchoScriptDefinitionView {
    EchoScriptDefinition base();

    @Override default int schemaVersion() { return base().schemaVersion(); }
    @Override default String pack() { return base().pack(); }
    @Override default Identifier id() { return base().id(); }
    @Override default String type() { return base().type(); }
    @Override default Optional<String> title() { return base().title(); }
    @Override default Optional<String> description() { return base().description(); }
    @Override default Optional<String> source() { return base().source(); }
    @Override default List<String> tags() { return base().tags(); }
    @Override default List<EchoCondition> unlockConditions() { return base().unlockConditions(); }
    @Override default List<EchoCondition> conditions() { return base().conditions(); }
    @Override default List<EchoAction> actions() { return base().actions(); }
    @Override default Map<String, Object> metadata() { return base().metadata(); }
    @Override default JsonObject rawJson() { return base().rawJson(); }
    @Override default Optional<Path> sourceFile() { return base().sourceFile(); }
}
