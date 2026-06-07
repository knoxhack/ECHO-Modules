package com.knoxhack.echo.socialcore;

import java.util.List;
import java.util.Map;

public record EchoDialogueChoice(
        String choiceId,
        String label,
        EchoDialogueNodeId targetNodeId,
        String translationKey,
        List<EchoDialogueCondition> conditions,
        List<EchoDialogueConsequence> consequences,
        boolean endsDialogue,
        Map<String, String> attributes
) {
    public EchoDialogueChoice {
        choiceId = SocialContractGuards.requireText(choiceId, "dialogue choice id");
        label = SocialContractGuards.requireText(label, "dialogue choice label");
        translationKey = SocialContractGuards.optionalText(translationKey);
        conditions = SocialContractGuards.immutableList(conditions);
        consequences = SocialContractGuards.immutableList(consequences);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean gated() {
        return !conditions.isEmpty();
    }
}
