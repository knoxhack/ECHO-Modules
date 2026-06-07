package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoContentValidationIssue(
        String issueId,
        EchoContentId contentId,
        EchoContentKind kind,
        EchoContentAvailability availability,
        EchoValidationCategory category,
        EchoDiagnostic diagnostic,
        EchoContentSource source,
        boolean repairable,
        String playerSummary,
        String developerDetails,
        List<String> relatedDocs,
        Map<String, String> attributes
) {
    public EchoContentValidationIssue {
        issueId = ContentContractGuards.requireText(issueId, "content validation issue id");
        Objects.requireNonNull(contentId, "contentId");
        kind = kind == null ? EchoContentKind.UNKNOWN : kind;
        availability = availability == null ? EchoContentAvailability.UNKNOWN : availability;
        category = category == null ? EchoValidationCategory.CONTENT_REFERENCE : category;
        playerSummary = ContentContractGuards.optionalText(playerSummary);
        developerDetails = ContentContractGuards.optionalText(developerDetails);
        relatedDocs = ContentContractGuards.immutableList(relatedDocs);
        attributes = ContentContractGuards.immutableMap(attributes);
        repairable = repairable || (diagnostic != null && diagnostic.repairable());
    }

    public boolean blocking() {
        return availability.blocking() || (diagnostic != null && diagnostic.blocking());
    }
}
