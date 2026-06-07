package com.knoxhack.echo.schemacore;

public record EchoSchemaIssue(
        EchoSchemaIssueSeverity severity,
        String code,
        String title,
        String summary,
        EchoSchemaId schemaId,
        EchoSchemaVersion schemaVersion,
        EchoSchemaDocumentKind documentKind,
        String pointer,
        String recommendation
) {
    public EchoSchemaIssue {
        severity = severity == null ? EchoSchemaIssueSeverity.ERROR : severity;
        code = SchemaContractGuards.requireText(code, "issue code");
        title = SchemaContractGuards.requireText(title, "issue title");
        summary = SchemaContractGuards.optionalText(summary);
        pointer = SchemaContractGuards.optionalText(pointer);
        recommendation = SchemaContractGuards.optionalText(recommendation);
    }
}
