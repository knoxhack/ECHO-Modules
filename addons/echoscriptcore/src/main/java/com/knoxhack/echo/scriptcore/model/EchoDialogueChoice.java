package com.knoxhack.echo.scriptcore.model;

import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoDialogueChoice(
        String id,
        String label,
        List<EchoCondition> conditions,
        List<EchoAction> actions,
        Optional<Identifier> nextDialogue,
        Map<String, Object> metadata) {
    public EchoDialogueChoice {
        id = id == null || id.isBlank() ? "choice" : id;
        label = label == null ? id : label;
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
        actions = List.copyOf(actions == null ? List.of() : actions);
        nextDialogue = nextDialogue == null ? Optional.empty() : nextDialogue;
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
