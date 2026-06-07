package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoDialogueNode(
        EchoDialogueNodeId id,
        EchoDialogueNodeKind kind,
        EchoNpcProfileId speakerNpc,
        EchoFactionId speakerFaction,
        String text,
        String translationKey,
        EchoContentId voiceLineContent,
        EchoContentId cinematicCameraContent,
        List<EchoDialogueCondition> conditions,
        List<EchoDialogueChoice> choices,
        List<EchoDialogueConsequence> consequences,
        Map<String, String> attributes
) {
    public EchoDialogueNode {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoDialogueNodeKind.UNKNOWN : kind;
        text = SocialContractGuards.optionalText(text);
        translationKey = SocialContractGuards.optionalText(translationKey);
        conditions = SocialContractGuards.immutableList(conditions);
        choices = SocialContractGuards.immutableList(choices);
        consequences = SocialContractGuards.immutableList(consequences);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean terminal() {
        return kind == EchoDialogueNodeKind.EXIT || choices.isEmpty();
    }
}
