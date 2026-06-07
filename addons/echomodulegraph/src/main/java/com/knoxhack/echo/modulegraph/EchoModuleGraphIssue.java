package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoAffectedFeature;
import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoDiagnosticCode;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;
import java.util.Objects;

public record EchoModuleGraphIssue(
        EchoModuleGraphIssueKind kind,
        EchoDiagnosticSeverity severity,
        EchoModuleId moduleId,
        EchoModuleId relatedModuleId,
        EchoPackId packId,
        EchoFeatureId featureId,
        String title,
        String summary,
        String path,
        String repairHint,
        boolean repairable,
        List<String> relatedDocs
) {
    public EchoModuleGraphIssue {
        Objects.requireNonNull(kind, "kind");
        severity = severity == null ? kind.defaultSeverity() : severity;
        title = title == null || title.isBlank() ? defaultTitle(kind) : title.trim();
        summary = ModuleGraphContractGuards.optionalText(summary);
        path = ModuleGraphContractGuards.optionalText(path);
        repairHint = ModuleGraphContractGuards.optionalText(repairHint);
        relatedDocs = ModuleGraphContractGuards.immutableList(relatedDocs);
    }

    public EchoDiagnostic toDiagnostic() {
        EchoValidationCategory category = kind.validationCategory();
        EchoDiagnostic.Builder builder = EchoDiagnostic.builder(
                        EchoDiagnosticCode.of("ECHO-GRAPH-" + kind.serializedName().toUpperCase().replace('-', '_')),
                        severity,
                        title,
                        summary
                )
                .moduleId(moduleId)
                .packId(packId)
                .category(category)
                .developerDetails("relatedModule=" + (relatedModuleId == null ? "" : relatedModuleId.value()))
                .repairable(repairable);
        if (featureId != null) {
            builder.affectedFeature(new EchoAffectedFeature(featureId, featureId.value(), category));
        }
        if (!path.isEmpty()) {
            builder.likelyFile(path);
        }
        if (!repairHint.isEmpty()) {
            builder.playerFix(repairHint);
        }
        relatedDocs.forEach(builder::relatedDoc);
        return builder.build();
    }

    public static EchoModuleGraphIssue of(EchoModuleGraphIssueKind kind, EchoModuleId moduleId, String summary) {
        return new EchoModuleGraphIssue(kind, null, moduleId, null, null, null, "", summary, "", "", false, List.of());
    }

    private static String defaultTitle(EchoModuleGraphIssueKind kind) {
        String[] parts = kind.serializedName().split("_");
        StringBuilder builder = new StringBuilder("Graph issue");
        if (parts.length > 0) {
            builder.setLength(0);
            for (String part : parts) {
                if (!part.isEmpty()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            }
        }
        return builder.toString();
    }
}
