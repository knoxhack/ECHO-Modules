package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.schemacore.EchoSchemaVersion;

import java.util.Objects;

public record EchoMetadataSchemaRef(
        EchoSchemaId id,
        EchoSchemaVersion version,
        EchoSchemaDocumentKind kind,
        EchoMetadataFileKind fileKind,
        String path
) {
    public EchoMetadataSchemaRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(fileKind, "fileKind");
        path = MetadataContractGuards.optionalText(path);
        if (path.isEmpty()) {
            path = fileKind.defaultPath();
        }
    }

    public static EchoMetadataSchemaRef moduleManifest(EchoSchemaVersion version) {
        return new EchoMetadataSchemaRef(
                EchoMetadataConstants.SCHEMA_ECHO_MOD_MANIFEST,
                version,
                EchoSchemaDocumentKind.ECHO_MOD_MANIFEST,
                EchoMetadataFileKind.MODULE_MANIFEST,
                ""
        );
    }

    public static EchoMetadataSchemaRef aiMetadata(EchoSchemaVersion version) {
        return new EchoMetadataSchemaRef(
                EchoMetadataConstants.SCHEMA_ECHO_AI_METADATA,
                version,
                EchoSchemaDocumentKind.ECHO_AI_METADATA,
                EchoMetadataFileKind.AI_METADATA,
                ""
        );
    }
}
