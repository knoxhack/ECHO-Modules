package com.knoxhack.echo.schemacore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleName;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermissionSet;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Set;

public final class EchoSchemaConstants {
    public static final String MOD_ID = "echoschemacore";
    public static final String MOD_NAME = "ECHO: SchemaCore";
    public static final EchoSchemaVersion INITIAL_VERSION = EchoSchemaVersion.of("1.0.0");
    public static final EchoFeatureId FEATURE_SCHEMA_REGISTRY = EchoFeatureId.of("validation.schema");
    public static final EchoFeatureId FEATURE_SCHEMA_CONTRACTS = EchoFeatureId.of("schema.contracts");
    public static final EchoSchemaRegistry GLOBAL_REGISTRY = new EchoSchemaRegistry();

    public static final EchoModuleIdentity MODULE_IDENTITY = new EchoModuleIdentity(
            EchoModuleId.of(MOD_ID),
            EchoModuleName.of(MOD_NAME),
            EchoModuleVersion.of("1.0.0"),
            EchoModuleKind.LIBRARY,
            EchoModuleRole.SCHEMA_CORE,
            EchoRuntimeSide.COMMON,
            EchoApiStability.BETA,
            EchoTrustLevel.OFFICIAL,
            true,
            true,
            Set.of(FEATURE_SCHEMA_REGISTRY, FEATURE_SCHEMA_CONTRACTS),
            Set.of(),
            EchoPermissionSet.empty()
    );

    public static final EchoSchemaOwner OWNER = new EchoSchemaOwner(
            MODULE_IDENTITY.id(),
            "ECHO Platform",
            "KnoxHack",
            Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.DEV, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT)
    );

    public static final List<EchoSchemaDescriptor> BUILTIN_DESCRIPTORS = List.of(
            descriptor("echo.mod_manifest", EchoSchemaDocumentKind.ECHO_MOD_MANIFEST, "ECHO Module Manifest", "Optional module metadata contract for ECHO units."),
            descriptor("echo.ai_metadata", EchoSchemaDocumentKind.ECHO_AI_METADATA, "ECHO AI Metadata", "Optional AI-readable module metadata contract."),
            descriptor("echo.pack_profile", EchoSchemaDocumentKind.ECHO_PACK_PROFILE, "ECHO Pack Profile", "Pack profile contract for Launcher, Command Center, PackOS, and AI tools."),
            descriptor("echo.lockfile", EchoSchemaDocumentKind.ECHO_LOCKFILE, "ECHO Lockfile", "Lockfile contract for known-good module and config snapshots."),
            descriptor("echo.repair_plan", EchoSchemaDocumentKind.ECHO_REPAIR_PLAN, "ECHO Repair Plan", "Repair plan contract for future PackOS and recovery tooling."),
            descriptor("echo.prompt_bundle", EchoSchemaDocumentKind.ECHO_PROMPT_BUNDLE, "ECHO Prompt Bundle", "Prompt bundle contract for Codex and CyberDex automation.")
    );

    private EchoSchemaConstants() {
    }

    public static void registerBuiltIns(EchoSchemaRegistry registry) {
        BUILTIN_DESCRIPTORS.forEach(registry::register);
    }

    private static EchoSchemaDescriptor descriptor(String id, EchoSchemaDocumentKind kind, String name, String summary) {
        return new EchoSchemaDescriptor(
                EchoSchemaId.of(id),
                INITIAL_VERSION,
                kind,
                OWNER,
                name,
                summary,
                EchoApiStability.BETA,
                EchoSchemaCompatibility.CURRENT,
                "",
                "docs/echo/schema/ECHO_SCHEMA_REGISTRY.md",
                Set.of(FEATURE_SCHEMA_REGISTRY, FEATURE_SCHEMA_CONTRACTS),
                Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.DEV, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT),
                Set.of()
        );
    }
}
