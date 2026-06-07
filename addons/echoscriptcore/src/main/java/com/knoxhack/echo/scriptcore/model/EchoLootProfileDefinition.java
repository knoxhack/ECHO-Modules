package com.knoxhack.echo.scriptcore.model;

import com.google.gson.JsonArray;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoLootProfileDefinition(
        EchoScriptDefinition base,
        Optional<Identifier> table,
        JsonArray entries,
        List<EchoCondition> lootUnlockConditions) implements DelegatingScriptDefinition {
    public EchoLootProfileDefinition {
        table = table == null ? Optional.empty() : table;
        entries = entries == null ? new JsonArray() : entries.deepCopy().getAsJsonArray();
        lootUnlockConditions = List.copyOf(lootUnlockConditions == null ? List.of() : lootUnlockConditions);
    }
}
