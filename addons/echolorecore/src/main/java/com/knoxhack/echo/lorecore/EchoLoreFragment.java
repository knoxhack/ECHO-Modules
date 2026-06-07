package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.contentcore.EchoContentOwner;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLoreFragment(
        EchoLoreFragmentId id,
        EchoLoreKind kind,
        EchoModuleId owningModule,
        EchoContentOwner contentOwner,
        String titleTranslationKey,
        String bodyTranslationKey,
        EchoLoreDelivery delivery,
        EchoLoreUnlockCondition unlockCondition,
        EchoContentReference sourceReference,
        EchoContentReference voiceReference,
        EchoContentReference audioReference,
        List<EchoContentReference> relatedContent,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoLoreFragment {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoLoreKind.UNKNOWN : kind;
        titleTranslationKey = LoreContractGuards.requireText(titleTranslationKey, "lore title translation key");
        bodyTranslationKey = LoreContractGuards.requireText(bodyTranslationKey, "lore body translation key");
        delivery = delivery == null ? EchoLoreDelivery.UNKNOWN : delivery;
        relatedContent = LoreContractGuards.immutableList(relatedContent);
        diagnostics = LoreContractGuards.immutableList(diagnostics);
        attributes = LoreContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return (unlockCondition != null && unlockCondition.blocksUnlock())
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
