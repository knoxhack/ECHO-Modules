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

public record EchoWeatherEventDefinition(
        EchoScriptDefinition base,
        int durationTicks,
        int warningSeconds,
        List<EchoAction> effects,
        String terminalWarning,
        Optional<Identifier> soundStinger) implements EchoScriptDefinitionView {
    public EchoWeatherEventDefinition {
        effects = List.copyOf(effects == null ? List.of() : effects);
        terminalWarning = terminalWarning == null ? "" : terminalWarning;
        soundStinger = soundStinger == null ? Optional.empty() : soundStinger;
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
