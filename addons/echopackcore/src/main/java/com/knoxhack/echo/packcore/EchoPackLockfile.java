package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoPackLockfile(
        EchoSchemaDescriptor schema,
        EchoPackId packId,
        EchoPackLockfileVersion packVersion,
        EchoPackVariantId variantId,
        EchoPackChannelId channelId,
        String minecraftVersion,
        String loaderKind,
        String loaderVersion,
        String echoPlatformVersion,
        Instant generatedAt,
        String generator,
        EchoModuleId rootModule,
        List<EchoLockedModule> lockedModules,
        List<EchoLockedFeature> lockedFeatures,
        List<EchoLockedConfig> lockedConfigs,
        List<EchoLockedResource> lockedResources,
        List<EchoLockedSchema> schemaVersions,
        String saveCompatibilityVersion,
        Map<String, EchoLockfileChecksum> checksums,
        EchoLockfileMetadata metadata,
        EchoLockfileStatus status,
        List<EchoLockfileIssue> issues,
        List<EchoPackSnapshot> knownGoodSnapshots
) {
    public EchoPackLockfile {
        Objects.requireNonNull(packId, "packId");
        packVersion = packVersion == null ? EchoPackLockfileVersion.of("0.0.0") : packVersion;
        Objects.requireNonNull(variantId, "variantId");
        Objects.requireNonNull(channelId, "channelId");
        minecraftVersion = PackContractGuards.requireText(minecraftVersion, "minecraft version");
        loaderKind = PackContractGuards.requireText(loaderKind, "loader kind");
        loaderVersion = PackContractGuards.requireText(loaderVersion, "loader version");
        echoPlatformVersion = PackContractGuards.optionalText(echoPlatformVersion);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        generator = PackContractGuards.optionalText(generator);
        lockedModules = PackContractGuards.immutableList(lockedModules);
        lockedFeatures = PackContractGuards.immutableList(lockedFeatures);
        lockedConfigs = PackContractGuards.immutableList(lockedConfigs);
        lockedResources = PackContractGuards.immutableList(lockedResources);
        schemaVersions = PackContractGuards.immutableList(schemaVersions);
        saveCompatibilityVersion = PackContractGuards.optionalText(saveCompatibilityVersion);
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
        status = status == null ? EchoLockfileStatus.UNKNOWN : status;
        issues = PackContractGuards.immutableList(issues);
        knownGoodSnapshots = PackContractGuards.immutableList(knownGoodSnapshots);
    }
}
