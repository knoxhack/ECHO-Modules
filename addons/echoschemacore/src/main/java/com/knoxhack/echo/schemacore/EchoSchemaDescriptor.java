package com.knoxhack.echo.schemacore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Objects;
import java.util.Set;

public record EchoSchemaDescriptor(
        EchoSchemaId id,
        EchoSchemaVersion version,
        EchoSchemaDocumentKind kind,
        EchoSchemaOwner owner,
        String name,
        String summary,
        EchoApiStability apiStability,
        EchoSchemaCompatibility compatibility,
        String schemaResource,
        String docsPath,
        Set<EchoFeatureId> relatedFeatures,
        Set<EchoRuntimeSide> supportedSides,
        Set<EchoSchemaMigrationHint> migrationHints
) {
    public EchoSchemaDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(owner, "owner");
        name = SchemaContractGuards.requireText(name, "schema name");
        summary = SchemaContractGuards.optionalText(summary);
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        compatibility = compatibility == null ? EchoSchemaCompatibility.UNKNOWN : compatibility;
        schemaResource = SchemaContractGuards.optionalText(schemaResource);
        docsPath = SchemaContractGuards.optionalText(docsPath);
        relatedFeatures = SchemaContractGuards.immutableSet(relatedFeatures);
        supportedSides = SchemaContractGuards.immutableSet(supportedSides);
        migrationHints = SchemaContractGuards.immutableSet(migrationHints);
    }
}
