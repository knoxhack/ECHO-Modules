package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.metadatacore.EchoModuleManifest;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoModuleGraphNode(
        EchoModuleId id,
        String name,
        String version,
        String path,
        String kind,
        EchoModuleRole role,
        Set<EchoRuntimeSide> side,
        String trustLevel,
        boolean official,
        String apiStability,
        boolean standalone,
        String metadataStatus,
        String aiMetadataStatus,
        Set<EchoFeatureId> provides,
        Set<EchoFeatureRequirement> consumes,
        List<EchoMetadataDependency> requires,
        List<EchoMetadataDependency> optional,
        String status
) {
    public EchoModuleGraphNode {
        Objects.requireNonNull(id, "id");
        name = name == null || name.isBlank() ? id.value() : name.trim();
        version = ModuleGraphContractGuards.optionalText(version);
        path = ModuleGraphContractGuards.optionalText(path);
        kind = ModuleGraphContractGuards.optionalText(kind);
        role = role == null ? EchoModuleRole.CONTENT_EXPANSION : role;
        side = ModuleGraphContractGuards.immutableSet(side);
        trustLevel = trustLevel == null || trustLevel.isBlank() ? "unknown" : trustLevel.trim();
        apiStability = apiStability == null || apiStability.isBlank() ? "unknown" : apiStability.trim();
        metadataStatus = metadataStatus == null || metadataStatus.isBlank() ? "unknown" : metadataStatus.trim();
        aiMetadataStatus = aiMetadataStatus == null || aiMetadataStatus.isBlank() ? "unknown" : aiMetadataStatus.trim();
        provides = ModuleGraphContractGuards.immutableSet(provides);
        consumes = ModuleGraphContractGuards.immutableSet(consumes);
        requires = ModuleGraphContractGuards.immutableList(requires);
        optional = ModuleGraphContractGuards.immutableList(optional);
        status = status == null || status.isBlank() ? "unknown" : status.trim();
    }

    public static EchoModuleGraphNode fromScannedModule(EchoScannedModule module) {
        Objects.requireNonNull(module, "module");
        EchoModuleManifest manifest = module.manifest();
        EchoModuleIdentity identity = module.identity();
        String version = "unknown";
        String kind = "unknown";
        String trustLevel = "unknown";
        String apiStability = "unknown";
        boolean official = false;
        boolean standalone = false;
        EchoModuleRole role = EchoModuleRole.CONTENT_EXPANSION;
        if (manifest != null) {
            version = manifest.version().value();
            kind = manifest.kind().serializedName();
            trustLevel = manifest.trustLevel().serializedName();
            apiStability = manifest.apiStability().serializedName();
            official = manifest.official();
            standalone = manifest.standalone();
            role = manifest.role();
        } else if (identity != null) {
            version = identity.version().value();
            kind = identity.kind().serializedName();
            trustLevel = identity.trustLevel().serializedName();
            apiStability = identity.apiStability().serializedName();
            official = identity.official();
            standalone = identity.standalone();
            role = identity.role();
        }
        String metadataStatus = !module.manifestPresent() ? "missing" : module.manifestValid() ? "present" : "invalid";
        String status = module.issues().stream().anyMatch(issue -> issue.toDiagnostic().blocking())
                ? "blocked"
                : module.issues().isEmpty() ? "available" : "degraded";
        return new EchoModuleGraphNode(
                module.moduleId(),
                module.displayName(),
                version,
                module.sourcePath(),
                kind,
                role,
                module.supportedSides(),
                trustLevel,
                official,
                apiStability,
                standalone,
                metadataStatus,
                module.attributes().getOrDefault("aiMetadataStatus", "unknown"),
                module.providedFeatures(),
                module.consumedFeatures(),
                module.requiredDependencies(),
                module.optionalDependencies(),
                status
        );
    }
}
