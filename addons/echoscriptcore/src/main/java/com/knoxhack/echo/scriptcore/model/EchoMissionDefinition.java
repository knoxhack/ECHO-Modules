package com.knoxhack.echo.scriptcore.model;

import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import com.knoxhack.echo.scriptcore.api.EchoMissionDefinitionView;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoMissionDefinition(
        EchoScriptDefinition base,
        String route,
        String phase,
        String role,
        String briefing,
        List<EchoObjective> objectives,
        List<EchoReward> rewards,
        List<EchoCondition> prerequisites,
        List<EchoAction> onStart,
        List<EchoAction> onComplete,
        List<EchoAction> onFail,
        JsonObject terminal,
        JsonObject lens,
        JsonObject holomap) implements EchoMissionDefinitionView {
    public EchoMissionDefinition {
        route = route == null ? "" : route;
        phase = phase == null ? "" : phase;
        role = role == null || role.isBlank() ? "main" : role.trim().toLowerCase(java.util.Locale.ROOT);
        briefing = briefing == null ? "" : briefing;
        objectives = List.copyOf(objectives == null ? List.of() : objectives);
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
        prerequisites = List.copyOf(prerequisites == null ? List.of() : prerequisites);
        onStart = List.copyOf(onStart == null ? List.of() : onStart);
        onComplete = List.copyOf(onComplete == null ? List.of() : onComplete);
        onFail = List.copyOf(onFail == null ? List.of() : onFail);
        terminal = terminal == null ? new JsonObject() : terminal.deepCopy();
        lens = lens == null ? new JsonObject() : lens.deepCopy();
        holomap = holomap == null ? new JsonObject() : holomap.deepCopy();
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
