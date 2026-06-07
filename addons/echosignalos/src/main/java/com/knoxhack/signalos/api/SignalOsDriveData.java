package com.knoxhack.signalos.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Persistent data component carried by SignalOS data drive items.
 */
public record SignalOsDriveData(
        int schemaVersion,
        String label,
        List<SignalOsDataRecord> records,
        Map<String, String> settings,
        Map<String, String> session) {
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final SignalOsDriveData EMPTY = new SignalOsDriveData(0, "No Drive", List.of(), Map.of(), Map.of());
    public static final int MAX_PLAYER_RECORDS = 64;
    public static final int MAX_OS_VALUES = 64;
    private static final int MAX_LABEL = 80;
    private static final int MAX_KEY = 64;
    private static final int MAX_VALUE = 512;
    private static final int MAX_MAP_ENTRIES = 64;

    public static final Codec<SignalOsDriveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schemaVersion", LEGACY_SCHEMA_VERSION).forGetter(SignalOsDriveData::schemaVersion),
            Codec.STRING.optionalFieldOf("label", "Blank Drive").forGetter(SignalOsDriveData::label),
            SignalOsDataRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(SignalOsDriveData::records),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("settings", Map.of())
                    .forGetter(SignalOsDriveData::settings),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("session", Map.of())
                    .forGetter(SignalOsDriveData::session)
    ).apply(instance, SignalOsDriveData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsDriveData> STREAM_CODEC =
            StreamCodec.of(SignalOsDriveData::write, SignalOsDriveData::read);

    public SignalOsDriveData {
        schemaVersion = Math.max(0, schemaVersion);
        label = clamp(label == null || label.isBlank() ? "Blank Drive" : label.strip(), MAX_LABEL);
        records = List.copyOf(records == null ? List.of() : records);
        settings = cleanMap(settings);
        session = cleanMap(session);
    }

    public SignalOsDriveData(String label, List<SignalOsDataRecord> records) {
        this(LEGACY_SCHEMA_VERSION, label, records, Map.of(), Map.of());
    }

    public SignalOsDriveData(String label, List<SignalOsDataRecord> records, Map<String, String> settings,
            Map<String, String> session) {
        this(LEGACY_SCHEMA_VERSION, label, records, settings, session);
    }

    public static SignalOsDriveData blankV2() {
        return new SignalOsDriveData(CURRENT_SCHEMA_VERSION, "Blank Drive", List.of(), Map.of(), Map.of());
    }

    public boolean isV2Supported() {
        return schemaVersion == CURRENT_SCHEMA_VERSION;
    }

    public SignalOsDriveData asV2() {
        return new SignalOsDriveData(CURRENT_SCHEMA_VERSION, label, records, settings, session);
    }

    public SignalOsDriveData withSchemaVersion(int nextSchemaVersion) {
        return new SignalOsDriveData(nextSchemaVersion, label, records, settings, session);
    }

    public SignalOsDriveData withLabel(String nextLabel) {
        return new SignalOsDriveData(schemaVersion, nextLabel, records, settings, session);
    }

    public SignalOsDriveData withRecord(SignalOsDataRecord record) {
        return withRecord(record, Integer.MAX_VALUE);
    }

    public SignalOsDriveData withRecord(SignalOsDataRecord record, int maxRecords) {
        if (record == null) {
            return this;
        }
        java.util.ArrayList<SignalOsDataRecord> next = new java.util.ArrayList<>();
        boolean replaced = false;
        for (SignalOsDataRecord existing : records) {
            if (existing.id().equals(record.id())) {
                next.add(record);
                replaced = true;
            } else {
                next.add(existing);
            }
        }
        if (!replaced) {
            next.add(record);
        }
        trim(next, maxRecords);
        return new SignalOsDriveData(schemaVersion, label, next, settings, session);
    }

    public SignalOsDriveData withoutRecord(net.minecraft.resources.Identifier recordId) {
        if (recordId == null || records.isEmpty()) {
            return this;
        }
        java.util.ArrayList<SignalOsDataRecord> next = new java.util.ArrayList<>();
        for (SignalOsDataRecord record : records) {
            if (!record.id().equals(recordId)) {
                next.add(record);
            }
        }
        return new SignalOsDriveData(schemaVersion, label, next, settings, session);
    }

    public SignalOsDriveData clearRecords() {
        return new SignalOsDriveData(schemaVersion, label, List.of(), settings, session);
    }

    public SignalOsDriveData merge(SignalOsDriveData template, int maxRecords) {
        if (template == null || template.records().isEmpty()) {
            return this;
        }
        SignalOsDriveData next = this;
        for (SignalOsDataRecord record : template.records()) {
            next = next.withRecord(record, maxRecords);
        }
        return next;
    }

    public String setting(String key, String fallback) {
        return settings.getOrDefault(cleanKey(key), fallback == null ? "" : fallback);
    }

    public SignalOsDriveData withSetting(String key, String value) {
        return new SignalOsDriveData(schemaVersion, label, records, withMapValue(settings, key, value), session);
    }

    public String sessionValue(String key, String fallback) {
        return session.getOrDefault(cleanKey(key), fallback == null ? "" : fallback);
    }

    public SignalOsDriveData withSessionValue(String key, String value) {
        return new SignalOsDriveData(schemaVersion, label, records, settings, withMapValue(session, key, value));
    }

    private static void trim(java.util.ArrayList<SignalOsDataRecord> records, int maxRecords) {
        int safeMax = Math.max(0, maxRecords);
        while (records.size() > safeMax) {
            records.removeFirst();
        }
    }

    private static Map<String, String> cleanMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (cleaned.size() >= MAX_MAP_ENTRIES) {
                break;
            }
            String key = cleanKey(entry.getKey());
            if (!key.isBlank()) {
                cleaned.put(key, clamp(entry.getValue() == null ? "" : entry.getValue().strip(), MAX_VALUE));
            }
        }
        return Map.copyOf(cleaned);
    }

    private static Map<String, String> withMapValue(Map<String, String> values, String key, String value) {
        String safeKey = cleanKey(key);
        if (safeKey.isBlank()) {
            return values == null ? Map.of() : values;
        }
        LinkedHashMap<String, String> next = new LinkedHashMap<>(values == null ? Map.of() : values);
        String safeValue = value == null ? "" : value.strip();
        if (safeValue.isBlank()) {
            next.remove(safeKey);
        } else {
            next.put(safeKey, clamp(safeValue, MAX_VALUE));
        }
        while (next.size() > MAX_OS_VALUES) {
            next.remove(next.keySet().iterator().next());
        }
        return Map.copyOf(next);
    }

    private static String cleanKey(String key) {
        if (key == null) {
            return "";
        }
        return clamp(key.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"), MAX_KEY);
    }

    private static String clamp(String value, int maxLength) {
        String safe = value == null ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static void write(RegistryFriendlyByteBuf buffer, SignalOsDriveData data) {
        SignalOsDriveData safe = data == null ? EMPTY : data;
        buffer.writeVarInt(safe.schemaVersion());
        buffer.writeUtf(safe.label(), MAX_LABEL);
        buffer.writeVarInt(safe.records().size());
        for (SignalOsDataRecord record : safe.records()) {
            SignalOsDataRecord.STREAM_CODEC.encode(buffer, record);
        }
        writeMap(buffer, safe.settings());
        writeMap(buffer, safe.session());
    }

    private static SignalOsDriveData read(RegistryFriendlyByteBuf buffer) {
        int schemaVersion = buffer.readVarInt();
        String label = buffer.readUtf(MAX_LABEL);
        int recordCount = buffer.readVarInt();
        if (recordCount < 0 || recordCount > MAX_PLAYER_RECORDS * 4) {
            throw new IllegalArgumentException("Invalid SignalOS drive record count: " + recordCount);
        }
        java.util.ArrayList<SignalOsDataRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            records.add(SignalOsDataRecord.STREAM_CODEC.decode(buffer));
        }
        return new SignalOsDriveData(schemaVersion, label, records, readMap(buffer), readMap(buffer));
    }

    private static void writeMap(RegistryFriendlyByteBuf buffer, Map<String, String> values) {
        Map<String, String> safe = cleanMap(values);
        buffer.writeVarInt(safe.size());
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            buffer.writeUtf(entry.getKey(), MAX_KEY);
            buffer.writeUtf(entry.getValue(), MAX_VALUE);
        }
    }

    private static Map<String, String> readMap(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_MAP_ENTRIES) {
            throw new IllegalArgumentException("Invalid SignalOS drive map count: " + count);
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            values.put(buffer.readUtf(MAX_KEY), buffer.readUtf(MAX_VALUE));
        }
        return values;
    }
}
