package com.knoxhack.echomissioncore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echomissioncore.EchoMissionCore;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.packs.resources.ResourceManager;

public final class MissionCoreNativeJsonBootstrap {
    private static final int MAX_BACKGROUND_ATTEMPTS = 180;
    private static boolean loaded;
    private static boolean loading;
    private static boolean backgroundStarted;

    private MissionCoreNativeJsonBootstrap() {
    }

    public static synchronized boolean ensureLoaded(String reason) {
        if (loaded || loading || !nativeLoaderActive()) {
            return loaded;
        }
        loading = true;
        try {
            int missions = MissionCoreJsonReloadListener.reloadNativeClasspathIfPresent(
                    nativeModuleClasspath(),
                    "native_loader_classpath_" + safeReason(reason));
            if (missions <= 0) {
                ResourceManager manager = resourceManager();
                if (manager == null) {
                    return false;
                }
                missions = MissionCoreJsonReloadListener.reloadIfPresent(manager,
                        "native_loader_lazy_" + safeReason(reason));
            }
            loaded = missions > 0;
            if (loaded) {
                EchoMissionCore.LOGGER.info("MissionCore Native JSON content loaded with {} missions.", missions);
            }
            return loaded;
        } catch (RuntimeException | LinkageError exception) {
            EchoMissionCore.LOGGER.warn("MissionCore Native JSON content is not ready yet; it will be retried.",
                    exception);
            return false;
        } finally {
            loading = false;
        }
    }

    public static synchronized void startBackgroundLoad(String reason) {
        if (loaded || backgroundStarted || !nativeLoaderActive()) {
            return;
        }
        backgroundStarted = true;
        Thread thread = new Thread(() -> retryBackgroundLoad(reason),
                "echo-missioncore-native-json-bootstrap");
        thread.setDaemon(true);
        thread.start();
    }

    private static void retryBackgroundLoad(String reason) {
        for (int attempt = 1; attempt <= MAX_BACKGROUND_ATTEMPTS; attempt++) {
            if (ensureLoaded(reason + "_attempt_" + attempt)) {
                return;
            }
            try {
                Thread.sleep(attempt < 20 ? 500L : 1_000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        EchoMissionCore.LOGGER.warn("MissionCore Native JSON content did not become available after {} attempts.",
                MAX_BACKGROUND_ATTEMPTS);
    }

    private static boolean nativeLoaderActive() {
        if (Boolean.getBoolean("echo.native.loader")) {
            return true;
        }
        String runtimeMode = System.getProperty("echo.native.runtime.mode", "");
        return runtimeMode.contains("native");
    }

    private static List<Path> nativeModuleClasspath() {
        Set<Path> paths = new LinkedHashSet<>();
        addClasspath(paths, System.getProperty("echo.native.moduleClasspath", ""));
        addClasspathFile(paths, System.getProperty("echo.native.moduleClasspathFile", ""));
        if (paths.isEmpty()) {
            Path gameDir = gameDir();
            if (gameDir != null) {
                addClasspathFile(paths, gameDir.resolve(".echo")
                        .resolve("native-loader")
                        .resolve("module-activation-handoff.json")
                        .toString());
                addClasspathFile(paths, gameDir.resolve(".echo")
                        .resolve("native-loader")
                        .resolve("materialized-addons.json")
                        .toString());
            }
        }
        return List.copyOf(paths);
    }

    private static void addClasspath(Set<Path> paths, String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return;
        }
        for (String token : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            addPath(paths, token);
        }
    }

    private static void addClasspathFile(Set<Path> paths, String classpathFile) {
        if (classpathFile == null || classpathFile.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(classpathFile).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                return;
            }
            JsonElement root = JsonParser.parseString(Files.readString(path));
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject object = root.getAsJsonObject();
            JsonElement inlineClasspath = object.get("moduleClasspath");
            if (inlineClasspath != null && inlineClasspath.isJsonPrimitive()) {
                addClasspath(paths, inlineClasspath.getAsString());
            }
            JsonElement modules = object.get("materialized");
            if (modules != null && modules.isJsonArray()) {
                modules.getAsJsonArray().forEach(module -> addRuntimeJarPaths(paths, module));
            }
        } catch (IOException | RuntimeException exception) {
            EchoMissionCore.LOGGER.debug("MissionCore could not read Native module classpath file {}.",
                    classpathFile, exception);
        }
    }

    private static void addRuntimeJarPaths(Set<Path> paths, JsonElement module) {
        if (module == null || !module.isJsonObject()) {
            return;
        }
        JsonElement runtimeJars = module.getAsJsonObject().get("runtimeJars");
        if (runtimeJars == null || !runtimeJars.isJsonArray()) {
            return;
        }
        runtimeJars.getAsJsonArray().forEach(entry -> {
            if (!entry.isJsonObject()) {
                return;
            }
            JsonElement path = entry.getAsJsonObject().get("path");
            if (path != null && path.isJsonPrimitive()) {
                addPath(paths, path.getAsString());
            }
        });
    }

    private static void addPath(Set<Path> paths, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(value).toAbsolutePath().normalize();
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                paths.add(path);
            }
        } catch (RuntimeException exception) {
            EchoMissionCore.LOGGER.debug("MissionCore skipped invalid Native module classpath entry {}.", value,
                    exception);
        }
    }

    private static Path gameDir() {
        String raw = System.getProperty("echo.native.gameDir", "").trim();
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ResourceManager resourceManager() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return null;
            }
            ResourceManager serverManager = integratedServerResourceManager(minecraft);
            if (serverManager != null) {
                return serverManager;
            }
            Object manager = minecraftClass.getMethod("getResourceManager").invoke(minecraft);
            return manager instanceof ResourceManager resourceManager ? resourceManager : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static ResourceManager integratedServerResourceManager(Object minecraft) {
        try {
            Object server = minecraft.getClass().getMethod("getSingleplayerServer").invoke(minecraft);
            if (server == null) {
                return null;
            }
            Object manager = server.getClass().getMethod("getResourceManager").invoke(server);
            return manager instanceof ResourceManager resourceManager ? resourceManager : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "query";
        }
        return reason.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
