package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoPlatformConstants;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;
import com.knoxhack.echo.schemacore.EchoSchemaCompatibility;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;
import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.schemacore.EchoSchemaOwner;
import com.knoxhack.echo.schemacore.EchoSchemaVersion;

import java.util.List;
import java.util.Set;

public final class EchoMetadataConstants {
    public static final String MOD_ID = "echometadatacore";
    public static final String MOD_NAME = "ECHO: MetadataCore";

    public static final EchoSchemaVersion SCHEMA_VERSION_1 = EchoSchemaVersion.of("1");
    public static final EchoSchemaId SCHEMA_ECHO_MOD_MANIFEST = EchoSchemaId.of("echo.mod");
    public static final EchoSchemaId SCHEMA_ECHO_AI_METADATA = EchoSchemaId.of("echo.ai");

    public static final EchoFeatureId FEATURE_METADATA_MANIFEST = EchoFeatureId.of("metadata.manifest");
    public static final EchoFeatureId FEATURE_METADATA_AI = EchoFeatureId.of("metadata.ai");
    public static final EchoFeatureId FEATURE_METADATA_MIGRATION = EchoFeatureId.of("metadata.migration");

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.METADATA_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(FEATURE_METADATA_MANIFEST, FEATURE_METADATA_AI, FEATURE_METADATA_MIGRATION),
            Set.of(),
            EchoPermissionSet.of(
                    EchoPlatformConstants.PERMISSION_PACK_READ,
                    EchoPlatformConstants.PERMISSION_DIAGNOSTICS_WRITE
            )
    );

    public static final EchoSchemaOwner SCHEMA_OWNER = new EchoSchemaOwner(
            EchoModuleId.of(MOD_ID),
            "ECHO Platform",
            "KnoxHack",
            Set.of(
                    EchoRuntimeSide.COMMON,
                    EchoRuntimeSide.DEV,
                    EchoRuntimeSide.LAUNCHER,
                    EchoRuntimeSide.COMMAND_CENTER,
                    EchoRuntimeSide.AI_AGENT
            )
    );

    public static final List<EchoSchemaDescriptor> SCHEMA_DESCRIPTORS = List.of(
            new EchoSchemaDescriptor(
                    SCHEMA_ECHO_MOD_MANIFEST,
                    SCHEMA_VERSION_1,
                    EchoSchemaDocumentKind.ECHO_MOD_MANIFEST,
                    SCHEMA_OWNER,
                    "ECHO Module Manifest",
                    "Optional module manifest contract for ECHO units.",
                    EchoApiStability.BETA,
                    EchoSchemaCompatibility.CURRENT,
                    "META-INF/echo.mod.json",
                    "docs/echo/metadata/ECHO_MOD_MANIFEST.md",
                    Set.of(FEATURE_METADATA_MANIFEST),
                    SCHEMA_OWNER.supportedSides(),
                    Set.of()
            ),
            new EchoSchemaDescriptor(
                    SCHEMA_ECHO_AI_METADATA,
                    SCHEMA_VERSION_1,
                    EchoSchemaDocumentKind.ECHO_AI_METADATA,
                    SCHEMA_OWNER,
                    "ECHO AI Metadata",
                    "Optional AI-readable ownership, task, and safe edit metadata contract.",
                    EchoApiStability.BETA,
                    EchoSchemaCompatibility.CURRENT,
                    "META-INF/echo.ai.json",
                    "docs/echo/metadata/ECHO_AI_METADATA.md",
                    Set.of(FEATURE_METADATA_AI),
                    SCHEMA_OWNER.supportedSides(),
                    Set.of()
            )
    );

    private EchoMetadataConstants() {
    }
}
