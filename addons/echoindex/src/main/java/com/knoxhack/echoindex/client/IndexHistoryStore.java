package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.EchoIndex;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.minecraft.client.Minecraft;

public final class IndexHistoryStore {
    private static final int LIMIT = 48;
    private static final Properties VALUES = new Properties();
    private static boolean loaded;
    private static long revision;

    private IndexHistoryStore() {
    }

    public static long revision() {
        load();
        return revision;
    }

    public static void add(String kind, String id, String label, String icon) {
        if (id == null || id.isBlank()) {
            return;
        }
        load();
        ArrayList<Entry> entries = new ArrayList<>(entries());
        entries.removeIf(entry -> entry.id().equals(id) && entry.kind().equals(kind));
        entries.addFirst(new Entry(clean(kind, "item"), id.strip(), clean(label, id), clean(icon, id), System.currentTimeMillis()));
        while (entries.size() > LIMIT) {
            entries.removeLast();
        }
        write(entries);
        save();
        revision++;
    }

    public static void clear() {
        load();
        VALUES.remove(profileKey("history.count"));
        for (int i = 0; i < LIMIT; i++) {
            VALUES.remove(profileKey("history." + i + ".kind"));
            VALUES.remove(profileKey("history." + i + ".id"));
            VALUES.remove(profileKey("history." + i + ".label"));
            VALUES.remove(profileKey("history." + i + ".icon"));
            VALUES.remove(profileKey("history." + i + ".time"));
        }
        save();
        revision++;
    }

    public static List<String> recent() {
        return entries().stream().map(Entry::id).toList();
    }

    public static List<Map<String, Object>> rows() {
        return entries().stream().map(Entry::toMap).toList();
    }

    private static List<Entry> entries() {
        load();
        int count = parseInt(VALUES.getProperty(profileKey("history.count"), "0"));
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(count, LIMIT); i++) {
            String base = "history." + i + ".";
            String id = VALUES.getProperty(profileKey(base + "id"), "");
            if (id.isBlank()) {
                continue;
            }
            entries.add(new Entry(
                    VALUES.getProperty(profileKey(base + "kind"), "item"),
                    id,
                    VALUES.getProperty(profileKey(base + "label"), id),
                    VALUES.getProperty(profileKey(base + "icon"), id),
                    parseLong(VALUES.getProperty(profileKey(base + "time"), "0"))));
        }
        return List.copyOf(entries);
    }

    private static void write(List<Entry> entries) {
        VALUES.setProperty(profileKey("history.count"), String.valueOf(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            String base = "history." + i + ".";
            VALUES.setProperty(profileKey(base + "kind"), entry.kind());
            VALUES.setProperty(profileKey(base + "id"), entry.id());
            VALUES.setProperty(profileKey(base + "label"), entry.label());
            VALUES.setProperty(profileKey(base + "icon"), entry.icon());
            VALUES.setProperty(profileKey(base + "time"), String.valueOf(entry.time()));
        }
    }

    private static String profileKey(String key) {
        Minecraft minecraft = Minecraft.getInstance();
        String profile = minecraft.player == null ? "local" : minecraft.player.getUUID().toString();
        return profile + "." + key;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
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
            EchoIndex.LOGGER.warn("ECHO: Index could not load local ScreenCore history.", exception);
        }
    }

    private static void save() {
        Path path = statePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                VALUES.store(writer, "ECHO Index ScreenCore history");
            }
        } catch (IOException exception) {
            EchoIndex.LOGGER.warn("ECHO: Index could not save local ScreenCore history.", exception);
        }
    }

    private static Path statePath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("echoindex_screencore_history.properties");
    }

    private record Entry(String kind, String id, String label, String icon, long time) {
        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("kind", kind);
            row.put("id", id);
            row.put("label", label);
            row.put("icon", icon);
            row.put("time", time);
            return row;
        }
    }
}
