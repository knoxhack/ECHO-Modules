package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.EchoIndex;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class IndexFavoriteStore {
    private static final Properties VALUES = new Properties();
    private static boolean loaded;
    private static long revision;

    private IndexFavoriteStore() {
    }

    public static long revision() {
        load();
        return revision;
    }

    public static boolean contains(String kind, String id) {
        return set(kind).contains(cleanId(id));
    }

    public static Set<String> set(String kind) {
        load();
        return readSet(key(kind));
    }

    public static boolean toggle(String kind, String id) {
        String clean = cleanId(id);
        if (clean.isBlank()) {
            return false;
        }
        load();
        String key = key(kind);
        LinkedHashSet<String> values = new LinkedHashSet<>(readSet(key));
        boolean enabled;
        if (!values.add(clean)) {
            values.remove(clean);
            enabled = false;
        } else {
            enabled = true;
        }
        writeSet(key, values);
        save();
        revision++;
        return enabled;
    }

    public static void add(String kind, String id) {
        String clean = cleanId(id);
        if (clean.isBlank()) {
            return;
        }
        load();
        String key = key(kind);
        LinkedHashSet<String> values = new LinkedHashSet<>(readSet(key));
        if (values.add(clean)) {
            writeSet(key, values);
            save();
            revision++;
        }
    }

    public static void remove(String kind, String id) {
        String clean = cleanId(id);
        if (clean.isBlank()) {
            return;
        }
        load();
        String key = key(kind);
        LinkedHashSet<String> values = new LinkedHashSet<>(readSet(key));
        if (values.remove(clean)) {
            writeSet(key, values);
            save();
            revision++;
        }
    }

    public static String setting(String name) {
        load();
        return VALUES.getProperty(profileKey("setting." + name), "");
    }

    public static void setSetting(String name, String value) {
        load();
        VALUES.setProperty(profileKey("setting." + name), value == null ? "" : value);
        save();
        revision++;
    }

    private static Set<String> readSet(String key) {
        String raw = VALUES.getProperty(profileKey(key), "");
        if (raw.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        Arrays.stream(raw.split("\\|"))
                .map(IndexFavoriteStore::cleanId)
                .filter(value -> !value.isBlank())
                .forEach(values::add);
        return Set.copyOf(values);
    }

    private static void writeSet(String key, Set<String> values) {
        VALUES.setProperty(profileKey(key), String.join("|", values));
    }

    private static String key(String kind) {
        String clean = kind == null ? "item" : kind.strip().toLowerCase(Locale.ROOT);
        return switch (clean) {
            case "recipe", "recipes" -> "favorite.recipes";
            case "machine", "machines" -> "favorite.machines";
            case "bookmark", "bookmarks" -> "bookmarks";
            default -> "favorite.items";
        };
    }

    private static String profileKey(String key) {
        Minecraft minecraft = minecraft();
        String profile = minecraft == null || minecraft.player == null ? "local" : minecraft.player.getUUID().toString();
        return profile + "." + key;
    }

    private static String cleanId(String id) {
        return id == null ? "" : id.strip();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = statePath();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            VALUES.load(reader);
        } catch (IOException exception) {
            EchoIndex.LOGGER.warn("ECHO: Index could not load local ScreenCore favorites.", exception);
        }
    }

    private static void save() {
        Path path = statePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                VALUES.store(writer, "ECHO Index ScreenCore client state");
            }
        } catch (IOException exception) {
            EchoIndex.LOGGER.warn("ECHO: Index could not save local ScreenCore favorites.", exception);
        }
    }

    private static Path statePath() {
        Minecraft minecraft = minecraft();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return Path.of("build", "echoindex", "screencore-state", "echoindex_screencore.properties");
        }
        return minecraft.gameDirectory.toPath()
                .resolve("config")
                .resolve("echoindex_screencore.properties");
    }

    private static Minecraft minecraft() {
        try {
            return Minecraft.getInstance();
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }
}
