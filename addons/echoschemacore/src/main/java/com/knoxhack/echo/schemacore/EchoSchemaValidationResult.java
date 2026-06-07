package com.knoxhack.echo.schemacore;

import java.util.List;

public record EchoSchemaValidationResult(
        boolean valid,
        EchoSchemaId schemaId,
        EchoSchemaVersion schemaVersion,
        EchoSchemaDocumentKind documentKind,
        List<EchoSchemaIssue> issues,
        List<EchoSchemaMigrationHint> migrationHints
) {
    public EchoSchemaValidationResult {
        issues = SchemaContractGuards.immutableList(issues);
        migrationHints = SchemaContractGuards.immutableList(migrationHints);
        valid = valid && issues.stream().noneMatch(EchoSchemaValidationResult::isBlocking);
    }

    public static EchoSchemaValidationResult valid(EchoSchemaDescriptor descriptor) {
        return new EchoSchemaValidationResult(
                true,
                descriptor.id(),
                descriptor.version(),
                descriptor.kind(),
                List.of(),
                List.copyOf(descriptor.migrationHints())
        );
    }

    public static EchoSchemaValidationResult invalid(EchoSchemaDescriptor descriptor, List<EchoSchemaIssue> issues) {
        return new EchoSchemaValidationResult(
                false,
                descriptor.id(),
                descriptor.version(),
                descriptor.kind(),
                issues,
                List.copyOf(descriptor.migrationHints())
        );
    }

    public static EchoSchemaValidationResult issue(EchoSchemaIssue issue) {
        return new EchoSchemaValidationResult(
                false,
                issue.schemaId(),
                issue.schemaVersion(),
                issue.documentKind(),
                List.of(issue),
                List.of()
        );
    }

    private static boolean isBlocking(EchoSchemaIssue issue) {
        return issue.severity() == EchoSchemaIssueSeverity.ERROR || issue.severity() == EchoSchemaIssueSeverity.FATAL;
    }
}
