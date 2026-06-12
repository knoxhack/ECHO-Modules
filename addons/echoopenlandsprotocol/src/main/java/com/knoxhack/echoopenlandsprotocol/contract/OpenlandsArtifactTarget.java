package com.knoxhack.echoopenlandsprotocol.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OpenlandsArtifactTarget(
        String id,
        String file,
        String editionRepo,
        String artifactFamily,
        boolean requiredForPublicAlpha
) {
    public OpenlandsArtifactTarget {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(editionRepo, "editionRepo");
        Objects.requireNonNull(artifactFamily, "artifactFamily");
    }

    public Map<String, Object> asAdapterRecord() {
        return Map.of(
                "id", id,
                "file", file,
                "editionRepo", editionRepo,
                "artifactFamily", artifactFamily,
                "requiredForPublicAlpha", requiredForPublicAlpha
        );
    }

    public static List<OpenlandsArtifactTarget> mvpTargets(String version) {
        String prefix = "echoopenlandsprotocol-" + version;
        return List.of(
                new OpenlandsArtifactTarget(
                        "native",
                        prefix + ".echo-addon",
                        "ECHO-Openlands-Native-Edition",
                        "echo-addon",
                        true
                ),
                new OpenlandsArtifactTarget(
                        "standalone",
                        prefix + "-standalone.jar",
                        "ECHO-Openlands-Standalone-Edition",
                        "standalone",
                        true
                ),
                new OpenlandsArtifactTarget(
                        "neoforge",
                        prefix + "-neoforge.jar",
                        "ECHO-Openlands-NeoForge-Edition",
                        "neoforge",
                        true
                ),
                new OpenlandsArtifactTarget(
                        "sources",
                        prefix + "-sources.jar",
                        "ECHO-Modules",
                        "sources",
                        true
                )
        );
    }
}
