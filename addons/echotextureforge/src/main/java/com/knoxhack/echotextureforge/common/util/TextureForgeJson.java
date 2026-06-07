package com.knoxhack.echotextureforge.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class TextureForgeJson {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (value, type, context) ->
                    new JsonPrimitive(value.toString()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (value, type, context) ->
                    Instant.parse(value.getAsString()))
            .registerTypeHierarchyAdapter(Path.class, (JsonSerializer<Path>) (value, type, context) ->
                    new JsonPrimitive(value.toString()))
            .create();

    private TextureForgeJson() {
    }

    public static <T> T read(Path path, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    public static <T> T read(Path path, Type type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    public static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        }
    }
}
