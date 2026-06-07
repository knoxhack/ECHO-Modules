package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoProgressionGate(
        String gateId,
        EchoUnlockKind kind,
        EchoUnlockNodeId nodeId,
        EchoContentId contentId,
        EchoRecipeId recipeId,
        EchoFeatureId featureId,
        EchoContentGate contentGate,
        EchoUnlockCondition condition,
        EchoObjectiveScope scope,
        boolean hiddenUntilAvailable,
        List<EchoDiagnostic> diagnostics,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoProgressionGate {
        gateId = ProgressionContractGuards.requireText(gateId, "progression gate id");
        kind = kind == null ? EchoUnlockKind.UNKNOWN : kind;
        nodeId = nodeId == null ? EchoUnlockNodeId.of(gateId) : nodeId;
        contentGate = Objects.requireNonNullElseGet(contentGate, EchoContentGate::open);
        condition = condition == null ? EchoUnlockCondition.open(gateId + "/condition", kind) : condition;
        scope = scope == null ? EchoObjectiveScope.PLAYER : scope;
        diagnostics = ProgressionContractGuards.immutableList(diagnostics);
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public static EchoProgressionGate routeGate(String gateId, EchoContentId routeId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.ROUTE_GATE, routeId, null, null, condition);
    }

    public static EchoProgressionGate chapterGate(String gateId, EchoContentId chapterId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.CHAPTER_GATE, chapterId, null, null, condition);
    }

    public static EchoProgressionGate featureUnlock(String gateId, EchoFeatureId featureId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.FEATURE_UNLOCK, null, null, featureId, condition);
    }

    public static EchoProgressionGate recipeUnlock(String gateId, EchoRecipeId recipeId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.RECIPE_UNLOCK, null, recipeId, null, condition);
    }

    public static EchoProgressionGate terminalTabUnlock(String gateId, EchoContentId tabId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.TERMINAL_TAB_UNLOCK, tabId, null, null, condition);
    }

    public static EchoProgressionGate lensScanUnlock(String gateId, EchoContentId scanId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.LENS_SCAN_UNLOCK, scanId, null, null, condition);
    }

    public static EchoProgressionGate holomapLayerUnlock(String gateId, EchoContentId layerId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.HOLOMAP_LAYER_UNLOCK, layerId, null, null, condition);
    }

    public static EchoProgressionGate worldEventUnlock(String gateId, EchoContentId eventId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.WORLD_EVENT_UNLOCK, eventId, null, null, condition);
    }

    public static EchoProgressionGate factionUnlock(String gateId, EchoContentId factionId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.FACTION_UNLOCK, factionId, null, null, condition);
    }

    public static EchoProgressionGate arcanaUnlock(String gateId, EchoContentId arcanaId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.ARCANA_UNLOCK, arcanaId, null, null, condition);
    }

    public static EchoProgressionGate techTreeGate(String gateId, EchoContentId techId, EchoUnlockCondition condition) {
        return simple(gateId, EchoUnlockKind.TECH_TREE_GATE, techId, null, null, condition);
    }

    private static EchoProgressionGate simple(
            String gateId,
            EchoUnlockKind kind,
            EchoContentId contentId,
            EchoRecipeId recipeId,
            EchoFeatureId featureId,
            EchoUnlockCondition condition
    ) {
        return new EchoProgressionGate(
                gateId,
                kind,
                EchoUnlockNodeId.of(gateId),
                contentId,
                recipeId,
                featureId,
                EchoContentGate.open(),
                condition,
                EchoObjectiveScope.PLAYER,
                false,
                List.of(),
                "",
                "",
                Map.of()
        );
    }

    public boolean blocking() {
        return contentGate.blocksWhenMissing()
                || condition.blocksProgression()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
