package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoNpcProfile(
        EchoNpcProfileId id,
        String displayName,
        EchoNpcRole role,
        EchoModuleId owningModule,
        EchoFactionId factionId,
        EchoNpcAiProfileId aiProfileId,
        EchoDialogueTreeId dialogueTreeId,
        EchoContentReference skinTexture,
        EchoContentReference modelReference,
        EchoContentReference portraitIcon,
        List<EchoNpcBinding> bindings,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoNpcProfile {
        Objects.requireNonNull(id, "id");
        displayName = SocialContractGuards.requireText(displayName, "npc display name");
        role = role == null ? EchoNpcRole.UNKNOWN : role;
        bindings = SocialContractGuards.immutableList(bindings);
        diagnostics = SocialContractGuards.immutableList(diagnostics);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
