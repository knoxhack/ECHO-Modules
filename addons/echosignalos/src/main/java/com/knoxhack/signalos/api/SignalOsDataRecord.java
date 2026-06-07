package com.knoxhack.signalos.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Small text-oriented record that can be loaded from datapacks, provided by an
 * addon, stored on a drive, or persisted as an operator note.
 */
public record SignalOsDataRecord(
        Identifier id,
        String title,
        String type,
        String source,
        String body,
        int order,
        boolean archived,
        Map<String, String> metadata) {
    public static final int MAX_METADATA_ENTRIES = 64;
    public static final int MAX_METADATA_KEY = 64;
    public static final int MAX_METADATA_VALUE = 512;

    public static final Codec<SignalOsDataRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(record -> record.id().toString()),
            Codec.STRING.optionalFieldOf("title", "").forGetter(SignalOsDataRecord::title),
            Codec.STRING.optionalFieldOf("type", "record").forGetter(SignalOsDataRecord::type),
            Codec.STRING.optionalFieldOf("source", "SignalOS").forGetter(SignalOsDataRecord::source),
            Codec.STRING.optionalFieldOf("body", "").forGetter(SignalOsDataRecord::body),
            Codec.INT.optionalFieldOf("order", 0).forGetter(SignalOsDataRecord::order),
            Codec.BOOL.optionalFieldOf("archived", false).forGetter(SignalOsDataRecord::archived),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("metadata", Map.of())
                    .forGetter(SignalOsDataRecord::metadata)
    ).apply(instance, SignalOsDataRecord::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsDataRecord> STREAM_CODEC =
            StreamCodec.of(SignalOsDataRecord::write, SignalOsDataRecord::read);

    public SignalOsDataRecord {
        id = TerminalIds.requireLowercase(id, "SignalOS data record");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        type = type == null || type.isBlank() ? "record" : type.strip().toLowerCase(java.util.Locale.ROOT);
        source = source == null || source.isBlank() ? id.getNamespace() : source.strip();
        body = body == null ? "" : body.strip();
        metadata = cleanMetadata(metadata);
    }

    public SignalOsDataRecord(Identifier id, String title, String type, String source, String body, int order,
            boolean archived) {
        this(id, title, type, source, body, order, archived, Map.of());
    }

    public static SignalOsDataRecord of(String id, String title, String type, String source, String body, int order) {
        return new SignalOsDataRecord(TerminalIds.parse(id, "SignalOS data record"), title, type, source, body, order, false);
    }

    public String metadataValue(String key, String fallback) {
        return metadata.getOrDefault(cleanKey(key), fallback == null ? "" : fallback);
    }

    public SignalOsDataRecord withMetadata(String key, String value) {
        LinkedHashMap<String, String> next = new LinkedHashMap<>(metadata);
        String safeKey = cleanKey(key);
        if (safeKey.isBlank()) {
            return this;
        }
        String safeValue = value == null ? "" : value.strip();
        if (safeValue.isBlank()) {
            next.remove(safeKey);
        } else {
            next.put(safeKey, clamp(safeValue, MAX_METADATA_VALUE));
        }
        while (next.size() > MAX_METADATA_ENTRIES) {
            next.remove(next.keySet().iterator().next());
        }
        return new SignalOsDataRecord(id, title, type, source, body, order, archived, next);
    }

    private static SignalOsDataRecord fromCodec(String id, String title, String type, String source, String body,
            int order, boolean archived, Map<String, String> metadata) {
        return new SignalOsDataRecord(TerminalIds.parse(id, "SignalOS data record"), title, type, source, body,
                order, archived, metadata);
    }

    private static void write(RegistryFriendlyByteBuf buffer, SignalOsDataRecord record) {
        SignalOsDataRecord safe = record == null
                ? SignalOsDataRecord.of("signalos:empty", "Empty", "record", "SignalOS", "", 0)
                : record;
        buffer.writeUtf(safe.id().toString(), 160);
        buffer.writeUtf(safe.title(), 4096);
        buffer.writeUtf(safe.type(), 160);
        buffer.writeUtf(safe.source(), 4096);
        buffer.writeUtf(safe.body(), 4096);
        buffer.writeVarInt(safe.order());
        buffer.writeBoolean(safe.archived());
        writeMetadata(buffer, safe.metadata());
    }

    private static SignalOsDataRecord read(RegistryFriendlyByteBuf buffer) {
        return new SignalOsDataRecord(
                Identifier.parse(buffer.readUtf(160)),
                buffer.readUtf(4096),
                buffer.readUtf(160),
                buffer.readUtf(4096),
                buffer.readUtf(4096),
                buffer.readVarInt(),
                buffer.readBoolean(),
                readMetadata(buffer));
    }

    public static Map<String, String> cleanMetadata(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (cleaned.size() >= MAX_METADATA_ENTRIES) {
                break;
            }
            String key = cleanKey(entry.getKey());
            if (!key.isBlank()) {
                cleaned.put(key, clamp(entry.getValue() == null ? "" : entry.getValue().strip(),
                        MAX_METADATA_VALUE));
            }
        }
        return Map.copyOf(cleaned);
    }

    private static void writeMetadata(RegistryFriendlyByteBuf buffer, Map<String, String> values) {
        Map<String, String> safe = cleanMetadata(values);
        buffer.writeVarInt(safe.size());
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            buffer.writeUtf(entry.getKey(), MAX_METADATA_KEY);
            buffer.writeUtf(entry.getValue(), MAX_METADATA_VALUE);
        }
    }

    private static Map<String, String> readMetadata(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("Invalid SignalOS record metadata count: " + count);
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            values.put(buffer.readUtf(MAX_METADATA_KEY), buffer.readUtf(MAX_METADATA_VALUE));
        }
        return values;
    }

    private static String cleanKey(String key) {
        if (key == null) {
            return "";
        }
        return clamp(key.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"),
                MAX_METADATA_KEY);
    }

    private static String clamp(String value, int maxLength) {
        String safe = value == null ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
