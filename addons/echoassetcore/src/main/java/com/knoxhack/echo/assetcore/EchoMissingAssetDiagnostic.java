package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoMissingAssetDiagnostic(
        EchoAssetReference reference,
        EchoValidationCategory category,
        EchoDiagnosticSeverity severity,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> relatedDiagnostics,
        Map<String, String> attributes
) {
    public EchoMissingAssetDiagnostic {
        Objects.requireNonNull(reference, "reference");
        category = category == null ? defaultCategory(reference.kind()) : category;
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        playerSummary = AssetContractGuards.optionalText(playerSummary);
        developerDetails = AssetContractGuards.optionalText(developerDetails);
        relatedDiagnostics = AssetContractGuards.immutableList(relatedDiagnostics);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return reference.required()
                || severity == EchoDiagnosticSeverity.ERROR
                || severity == EchoDiagnosticSeverity.FATAL
                || relatedDiagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }

    private static EchoValidationCategory defaultCategory(EchoAssetKind kind) {
        if (kind == EchoAssetKind.MODEL) {
            return EchoValidationCategory.MISSING_MODEL;
        }
        if (kind == EchoAssetKind.SOUND || kind == EchoAssetKind.MUSIC || kind == EchoAssetKind.SOUND_PROFILE) {
            return EchoValidationCategory.MISSING_SOUND;
        }
        if (kind == EchoAssetKind.ICON || kind == EchoAssetKind.UI_TEXTURE) {
            return EchoValidationCategory.MISSING_ICON;
        }
        return EchoValidationCategory.MISSING_TEXTURE;
    }
}
