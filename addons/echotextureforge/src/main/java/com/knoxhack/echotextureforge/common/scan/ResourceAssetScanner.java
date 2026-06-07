package com.knoxhack.echotextureforge.common.scan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echotextureforge.EchoTextureForgeMod;
import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ResourceAssetScanner {
    private ResourceAssetScanner() {
    }

    public static ResourceScanResult scan(TextureForgePaths paths) {
        ResourceScanResult result = new ResourceScanResult(paths.workspaceRoot());
        for (Path moduleRoot : moduleRoots(paths.workspaceRoot())) {
            scanModule(moduleRoot, result);
        }
        return result;
    }

    private static List<Path> moduleRoots(Path workspaceRoot) {
        List<Path> roots = new ArrayList<>();
        if (Files.isDirectory(workspaceRoot.resolve("src/main/resources"))) {
            roots.add(workspaceRoot);
        }
        Path core = workspaceRoot.resolve("core");
        if (Files.isDirectory(core)) {
            try (Stream<Path> stream = Files.list(core)) {
                stream.filter(Files::isDirectory).forEach(roots::add);
            } catch (IOException exception) {
                EchoTextureForgeMod.LOGGER.warn("TextureForge could not list core modules under {}.", core, exception);
            }
        }
        Path addons = workspaceRoot.resolve("addons");
        if (Files.isDirectory(addons)) {
            try (Stream<Path> stream = Files.list(addons)) {
                stream.filter(Files::isDirectory).forEach(roots::add);
            } catch (IOException exception) {
                EchoTextureForgeMod.LOGGER.warn("TextureForge could not list addon modules under {}.", addons, exception);
            }
        }
        roots.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return List.copyOf(roots);
    }

    private static void scanModule(Path moduleRoot, ResourceScanResult result) {
        List<Path> resourceRoots = new ArrayList<>();
        Path main = moduleRoot.resolve("src/main/resources");
        if (Files.isDirectory(main)) {
            resourceRoots.add(main);
        }
        Path generated = moduleRoot.resolve("src/generated/resources");
        if (TextureForgeConfig.includeGeneratedResources() && Files.isDirectory(generated)) {
            resourceRoots.add(generated);
        }
        if (resourceRoots.isEmpty()) {
            return;
        }
        result.addScannedModuleRoot(moduleRoot);
        for (Path resourceRoot : resourceRoots) {
            scanResourceRoot(resourceRoot, result);
        }
    }

    private static void scanResourceRoot(Path resourceRoot, ResourceScanResult result) {
        Path assetsRoot = resourceRoot.resolve("assets");
        if (!Files.isDirectory(assetsRoot)) {
            return;
        }
        try (Stream<Path> namespaces = Files.list(assetsRoot)) {
            namespaces.filter(Files::isDirectory)
                    .filter(path -> includeNamespace(path.getFileName().toString()))
                    .forEach(namespacePath -> scanNamespace(namespacePath.getFileName().toString(), namespacePath, result));
        } catch (IOException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge could not scan assets root {}.", assetsRoot, exception);
        }
    }

    private static void scanNamespace(String namespace, Path namespacePath, ResourceScanResult result) {
        ResourceScanResult.NamespaceAssets assets = result.namespace(namespace);
        try (Stream<Path> stream = Files.walk(namespacePath)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                String relative = namespacePath.relativize(file).toString().replace('\\', '/');
                assets.addFile(relative, file);
                if ("lang/en_us.json".equals(relative)) {
                    loadLangKeys(file, assets);
                }
            });
        } catch (IOException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge could not scan namespace {} at {}.", namespace, namespacePath, exception);
        }
    }

    private static void loadLangKeys(Path file, ResourceScanResult.NamespaceAssets assets) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return;
            }
            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                assets.addLangKey(entry.getKey());
            }
        } catch (RuntimeException | IOException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge could not parse lang keys from {}.", file, exception);
        }
    }

    public static boolean includeNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        if (TextureForgeConfig.includeExternalNamespaces()) {
            return true;
        }
        String id = namespace.toLowerCase(Locale.ROOT);
        return id.startsWith("echo") || id.equals("signalosexample");
    }
}
