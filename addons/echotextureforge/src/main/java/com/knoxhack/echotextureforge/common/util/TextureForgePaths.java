package com.knoxhack.echotextureforge.common.util;

import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public record TextureForgePaths(Path workspaceRoot, Path outputRoot) {
    public static TextureForgePaths discover() {
        Path workspace = findWorkspaceRoot(Path.of("").toAbsolutePath().normalize());
        Path output = Path.of(TextureForgeConfig.outputDirectory());
        if (!output.isAbsolute()) {
            output = workspace.resolve(output);
        }
        return new TextureForgePaths(workspace, output.normalize());
    }

    public Path reportsDir() {
        return outputRoot.resolve("reports");
    }

    public Path reportAddonDir() {
        return reportsDir().resolve("by_addon");
    }

    public Path reportIssuesDir() {
        return reportsDir().resolve("issues");
    }

    public Path promptsDir() {
        return outputRoot.resolve("prompts");
    }

    public Path promptAddonDir() {
        return promptsDir().resolve("by_addon");
    }

    public Path promptTypeDir() {
        return promptsDir().resolve("by_type");
    }

    public Path promptSheetsDir() {
        return promptsDir().resolve("sheets");
    }

    public Path specsGeneratedDir() {
        return outputRoot.resolve("specs").resolve("generated");
    }

    public Path specsMergedDir() {
        return outputRoot.resolve("specs").resolve("merged");
    }

    public Path reviewDir() {
        return outputRoot.resolve("review");
    }

    public Path importDir() {
        return outputRoot.resolve("import");
    }

    public Path importIncomingDir() {
        return importDir().resolve("incoming");
    }

    public Path importPreviewDir() {
        return importDir().resolve("preview");
    }

    public Path importStagedDir() {
        return importDir().resolve("staged");
    }

    public Path generatedSuggestionsDir() {
        return outputRoot.resolve("generated_suggestions");
    }

    public static Path findWorkspaceRoot(Path start) {
        Path current = start == null ? Path.of("").toAbsolutePath().normalize() : start.normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))
                    && Files.isDirectory(candidate.resolve("addons"))
                    && Files.isDirectory(candidate.resolve("core"))) {
                return candidate;
            }
        }
        if (current.getFileName() != null && "run".equals(current.getFileName().toString().toLowerCase(Locale.ROOT))
                && current.getParent() != null) {
            return current.getParent();
        }
        return current;
    }

    public Path assetPath(String namespace, String relativeAssetPath) {
        return sourceAssetPath(namespace, relativeAssetPath);
    }

    public Path sourceAssetPath(String namespace, String relativeAssetPath) {
        String cleanNamespace = namespace == null ? "" : namespace.strip().toLowerCase(Locale.ROOT);
        String cleanRelative = relativeAssetPath == null ? "" : relativeAssetPath.replace('\\', '/');
        return sourceAssetsRoot(cleanNamespace).resolve(cleanRelative).normalize();
    }

    public Path sourceAssetsRoot(String namespace) {
        String cleanNamespace = namespace == null ? "" : namespace.strip().toLowerCase(Locale.ROOT);
        for (Path root : possibleAssetsRoots(cleanNamespace)) {
            if (Files.isDirectory(root)) {
                return root;
            }
        }
        Path addonRoot = workspaceRoot.resolve("addons").resolve(cleanNamespace)
                .resolve("src/main/resources/assets").resolve(cleanNamespace);
        if (Files.isDirectory(workspaceRoot.resolve("core").resolve(cleanNamespace))) {
            return workspaceRoot.resolve("core").resolve(cleanNamespace)
                    .resolve("src/main/resources/assets").resolve(cleanNamespace);
        }
        if (Files.isDirectory(workspaceRoot.resolve("src/main/resources/assets").resolve(cleanNamespace))) {
            return workspaceRoot.resolve("src/main/resources/assets").resolve(cleanNamespace);
        }
        return addonRoot;
    }

    private java.util.List<Path> possibleAssetsRoots(String namespace) {
        return java.util.List.of(
                workspaceRoot.resolve("addons").resolve(namespace).resolve("src/main/resources/assets").resolve(namespace),
                workspaceRoot.resolve("core").resolve(namespace).resolve("src/main/resources/assets").resolve(namespace),
                workspaceRoot.resolve("src/main/resources/assets").resolve(namespace));
    }
}
