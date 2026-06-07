package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.metadatacore.EchoModuleManifest;
import com.knoxhack.echo.packcore.EchoPackProfile;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleIdentity;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoScannedModule(
        EchoModuleId moduleId,
        String displayName,
        EchoModuleIdentity identity,
        EchoModuleManifest manifest,
        EchoPackProfile packProfile,
        String sourcePath,
        boolean echoUnit,
        boolean manifestPresent,
        boolean manifestValid,
        Set<EchoRuntimeSide> supportedSides,
        Set<EchoFeatureId> providedFeatures,
        Set<EchoFeatureRequirement> consumedFeatures,
        List<EchoMetadataDependency> requiredDependencies,
        List<EchoMetadataDependency> optionalDependencies,
        List<EchoModuleGraphIssue> issues,
        Map<String, String> attributes
) {
    public EchoScannedModule {
        if (moduleId == null) {
            moduleId = identity != null ? identity.id() : manifest != null ? manifest.id() : null;
        }
        Objects.requireNonNull(moduleId, "moduleId");
        displayName = displayName == null || displayName.isBlank()
                ? identity != null ? identity.name().value() : manifest != null ? manifest.name() : moduleId.value()
                : displayName.trim();
        sourcePath = ModuleGraphContractGuards.optionalText(sourcePath);
        supportedSides = ModuleGraphContractGuards.immutableSet(supportedSides);
        if (supportedSides.isEmpty()) {
            supportedSides = inferSides(identity, manifest);
        }
        providedFeatures = ModuleGraphContractGuards.immutableSet(providedFeatures);
        if (providedFeatures.isEmpty()) {
            providedFeatures = identity != null ? identity.providedFeatures() : manifest != null ? manifest.provides() : Set.of();
        }
        consumedFeatures = ModuleGraphContractGuards.immutableSet(consumedFeatures);
        if (consumedFeatures.isEmpty()) {
            consumedFeatures = identity != null ? identity.consumedFeatures() : manifest != null ? manifest.consumes() : Set.of();
        }
        requiredDependencies = ModuleGraphContractGuards.immutableList(requiredDependencies);
        if (requiredDependencies.isEmpty() && manifest != null) {
            requiredDependencies = manifest.requires();
        }
        optionalDependencies = ModuleGraphContractGuards.immutableList(optionalDependencies);
        if (optionalDependencies.isEmpty() && manifest != null) {
            optionalDependencies = manifest.optional();
        }
        issues = ModuleGraphContractGuards.immutableList(issues);
        attributes = ModuleGraphContractGuards.immutableMap(attributes);
    }

    public boolean clientOnly() {
        return supportedSides.size() == 1 && supportedSides.contains(EchoRuntimeSide.CLIENT);
    }

    public boolean serverOnly() {
        return supportedSides.size() == 1 && supportedSides.contains(EchoRuntimeSide.SERVER);
    }

    public static EchoScannedModule fromManifest(EchoModuleManifest manifest, String sourcePath) {
        Objects.requireNonNull(manifest, "manifest");
        return new EchoScannedModule(
                manifest.id(),
                manifest.name(),
                null,
                manifest,
                null,
                sourcePath,
                true,
                true,
                true,
                inferSides(null, manifest),
                manifest.provides(),
                manifest.consumes(),
                manifest.requires(),
                manifest.optional(),
                List.of(),
                Map.of()
        );
    }

    private static Set<EchoRuntimeSide> inferSides(EchoModuleIdentity identity, EchoModuleManifest manifest) {
        if (manifest != null) {
            if (manifest.clientOnly()) {
                return Set.of(EchoRuntimeSide.CLIENT);
            }
            if (manifest.serverOnly()) {
                return Set.of(EchoRuntimeSide.SERVER);
            }
        }
        if (identity != null) {
            return Set.of(identity.side());
        }
        return Set.of(EchoRuntimeSide.COMMON);
    }
}
