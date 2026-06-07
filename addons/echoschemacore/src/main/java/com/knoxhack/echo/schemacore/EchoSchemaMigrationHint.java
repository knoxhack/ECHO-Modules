package com.knoxhack.echo.schemacore;

import java.util.List;
import java.util.Objects;

public record EchoSchemaMigrationHint(
        EchoSchemaVersion fromVersion,
        EchoSchemaVersion toVersion,
        String summary,
        boolean automatic,
        boolean lossless,
        String docsPath,
        List<String> steps
) {
    public EchoSchemaMigrationHint {
        Objects.requireNonNull(fromVersion, "fromVersion");
        Objects.requireNonNull(toVersion, "toVersion");
        summary = SchemaContractGuards.optionalText(summary);
        docsPath = SchemaContractGuards.optionalText(docsPath);
        steps = SchemaContractGuards.immutableList(steps);
    }
}
