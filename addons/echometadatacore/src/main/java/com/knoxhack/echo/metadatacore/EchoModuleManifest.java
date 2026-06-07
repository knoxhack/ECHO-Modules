package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.packcore.EchoPackChannelId;
import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoPermission;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoModuleManifest(
        EchoMetadataSchemaRef schema,
        EchoModuleId id,
        String name,
        EchoModuleVersion version,
        String type,
        EchoModuleKind kind,
        EchoModuleRole role,
        String entrypoint,
        String publisher,
        EchoPackChannelId channel,
        boolean official,
        EchoTrustLevel trustLevel,
        boolean standalone,
        boolean clientOnly,
        boolean serverOnly,
        List<EchoMetadataDependency> requires,
        List<EchoMetadataDependency> optional,
        Set<EchoFeatureId> provides,
        Set<EchoFeatureRequirement> consumes,
        Set<EchoGameModeId> gameModes,
        Set<EchoPermission> permissions,
        List<EchoMetadataAsset> assets,
        List<EchoMetadataTransform> transforms,
        EchoMetadataAccessPolicy access,
        EchoApiStability apiStability,
        EchoMetadataAiLink ai,
        List<EchoMetadataDeprecation> deprecatedFeatures,
        List<EchoMetadataReplacement> replacements,
        List<EchoMetadataConflict> conflicts
) {
    public EchoModuleManifest {
        schema = schema == null ? EchoMetadataSchemaRef.moduleManifest(EchoMetadataConstants.SCHEMA_VERSION_1) : schema;
        Objects.requireNonNull(id, "id");
        name = MetadataContractGuards.requireText(name, "module manifest name");
        Objects.requireNonNull(version, "version");
        type = MetadataContractGuards.optionalText(type);
        kind = kind == null ? EchoModuleKind.ADDON : kind;
        role = role == null ? EchoModuleRole.CONTENT_EXPANSION : role;
        entrypoint = MetadataContractGuards.optionalText(entrypoint);
        publisher = MetadataContractGuards.optionalText(publisher);
        channel = channel == null ? EchoPackChannelId.of("beta") : channel;
        trustLevel = trustLevel == null ? EchoTrustLevel.COMMUNITY : trustLevel;
        requires = MetadataContractGuards.immutableList(requires);
        optional = MetadataContractGuards.immutableList(optional);
        provides = MetadataContractGuards.immutableSet(provides);
        consumes = MetadataContractGuards.immutableSet(consumes);
        gameModes = MetadataContractGuards.immutableSet(gameModes);
        permissions = MetadataContractGuards.immutableSet(permissions);
        assets = MetadataContractGuards.immutableList(assets);
        transforms = MetadataContractGuards.immutableList(transforms);
        access = access == null ? EchoMetadataAccessPolicy.readOnly(Set.of()) : access;
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        ai = ai == null ? new EchoMetadataAiLink(false, "", Set.of(), false, "") : ai;
        deprecatedFeatures = MetadataContractGuards.immutableList(deprecatedFeatures);
        replacements = MetadataContractGuards.immutableList(replacements);
        conflicts = MetadataContractGuards.immutableList(conflicts);
    }

    public boolean commonSide() {
        return !clientOnly && !serverOnly;
    }

    public static EchoModuleManifest minimal(
            EchoModuleId id,
            String name,
            EchoModuleVersion version,
            EchoModuleKind kind,
            EchoModuleRole role
    ) {
        return new EchoModuleManifest(
                EchoMetadataSchemaRef.moduleManifest(EchoMetadataConstants.SCHEMA_VERSION_1),
                id,
                name,
                version,
                kind.serializedName(),
                kind,
                role,
                "",
                "",
                EchoPackChannelId.of("beta"),
                false,
                EchoTrustLevel.COMMUNITY,
                true,
                false,
                false,
                List.of(),
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(),
                List.of(),
                EchoMetadataAccessPolicy.readOnly(Set.of()),
                EchoApiStability.EXPERIMENTAL,
                new EchoMetadataAiLink(false, "", Set.of(), false, ""),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
