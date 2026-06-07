package com.knoxhack.echo.contentcore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EchoContentRegistry(
        String registryId,
        long createdAtEpochMillis,
        List<EchoContentOwner> owners,
        List<EchoContentReference> references,
        List<EchoContentValidationIssue> issues,
        Map<String, String> attributes
) {
    public EchoContentRegistry {
        registryId = ContentContractGuards.requireText(registryId, "content registry id");
        createdAtEpochMillis = ContentContractGuards.nonNegativeLong(createdAtEpochMillis, "content registry created time");
        owners = ContentContractGuards.immutableList(owners);
        references = ContentContractGuards.immutableList(references);
        issues = ContentContractGuards.immutableList(issues);
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public Optional<EchoContentOwner> ownerOf(EchoContentId contentId) {
        return owners.stream()
                .filter(owner -> owner.contentId().equals(contentId))
                .min(Comparator.comparing(owner -> owner.moduleId().value()));
    }

    public List<EchoContentReference> referencesFrom(EchoContentId contentId) {
        return references.stream()
                .filter(reference -> reference.fromContent().equals(contentId))
                .toList();
    }

    public List<EchoContentReference> referencesTo(EchoContentId contentId) {
        return references.stream()
                .filter(reference -> reference.targetContent().equals(contentId))
                .toList();
    }

    public List<EchoContentValidationIssue> issuesFor(EchoContentId contentId) {
        return issues.stream()
                .filter(issue -> issue.contentId().equals(contentId))
                .toList();
    }

    public boolean hasBlockingIssues() {
        return references.stream().anyMatch(EchoContentReference::blocking)
                || issues.stream().anyMatch(EchoContentValidationIssue::blocking);
    }
}
