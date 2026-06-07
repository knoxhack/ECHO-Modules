package com.knoxhack.signalos.api;

import com.knoxhack.signalos.SignalOS;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class SignalOsDriveFileSystem {
    public static final String META_PATH = "signalos.path";
    public static final String META_MIME = "signalos.mime";
    public static final String META_CREATED = "signalos.created";
    public static final String META_MODIFIED = "signalos.modified";
    public static final String META_READONLY = "signalos.readonly";
    public static final String META_TAGS = "signalos.tags";

    public static final String TYPE_FILE = "file";
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_NOTE = "note";

    private static final int MAX_PATH = 160;

    private final SignalOsDriveData drive;

    private SignalOsDriveFileSystem(SignalOsDriveData drive) {
        this.drive = drive == null ? SignalOsDriveData.EMPTY : drive;
    }

    public static SignalOsDriveFileSystem of(SignalOsDriveData drive) {
        return new SignalOsDriveFileSystem(drive);
    }

    public SignalOsDriveData drive() {
        return drive;
    }

    public boolean writable() {
        return drive.isV2Supported();
    }

    public String status() {
        if (drive.schemaVersion() == 0) {
            return "NO DRIVE";
        }
        return drive.isV2Supported() ? "READY" : "UNSUPPORTED V" + drive.schemaVersion();
    }

    public List<SignalOsDriveFileEntry> list() {
        return drive.records().stream()
                .filter(record -> !pathOf(record).isBlank())
                .map(SignalOsDriveFileEntry::fromRecord)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SignalOsDriveFileEntry::path)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
    }

    public List<SignalOsDriveFileEntry> list(String directory) {
        String normalized = normalizePath(directory);
        if (normalized == null) {
            return List.of();
        }
        String prefix = "/".equals(normalized) ? "/" : normalized + "/";
        return list().stream()
                .filter(entry -> entry.path().equals(normalized) || entry.path().startsWith(prefix))
                .toList();
    }

    public SignalOsDataRecord findRecord(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        return drive.records().stream()
                .filter(record -> normalized.equals(pathOf(record)))
                .findFirst()
                .orElse(null);
    }

    public SignalOsDriveWriteResult createFile(String path, String title, String body, String mime) {
        String normalized = normalizePath(path);
        if (normalized == null || "/".equals(normalized)) {
            return failure(SignalOsDriveResultCode.INVALID_PATH, "[SignalOS] File path is invalid.");
        }
        if (findRecord(normalized) != null) {
            return failure(SignalOsDriveResultCode.ALREADY_EXISTS, "[SignalOS] File already exists.");
        }
        SignalOsDriveWriteResult capacity = requireWritableCapacity(false);
        if (!capacity.success()) {
            return capacity;
        }
        SignalOsDataRecord record = baseRecord(idForPath(TYPE_FILE, normalized),
                blank(title, nameOf(normalized)), TYPE_FILE, body, drive.records().size())
                .withMetadata(META_PATH, normalized)
                .withMetadata(META_MIME, blank(mime, "text/plain"))
                .withMetadata(META_CREATED, stamp())
                .withMetadata(META_MODIFIED, stamp());
        return success(withRecords(appendOrReplace(record)), "[SignalOS] File created.");
    }

    public SignalOsDriveWriteResult createFolder(String path) {
        String normalized = normalizePath(path);
        if (normalized == null || "/".equals(normalized)) {
            return failure(SignalOsDriveResultCode.INVALID_PATH, "[SignalOS] Folder path is invalid.");
        }
        if (findRecord(normalized) != null) {
            return failure(SignalOsDriveResultCode.ALREADY_EXISTS, "[SignalOS] Folder already exists.");
        }
        SignalOsDriveWriteResult capacity = requireWritableCapacity(false);
        if (!capacity.success()) {
            return capacity;
        }
        SignalOsDataRecord record = baseRecord(idForPath(TYPE_FOLDER, normalized),
                nameOf(normalized), TYPE_FOLDER, "", drive.records().size())
                .withMetadata(META_PATH, normalized)
                .withMetadata(META_CREATED, stamp())
                .withMetadata(META_MODIFIED, stamp());
        return success(withRecords(appendOrReplace(record)), "[SignalOS] Folder created.");
    }

    public SignalOsDriveWriteResult renamePath(String fromPath, String toPath) {
        String from = normalizePath(fromPath);
        String to = normalizePath(toPath);
        if (from == null || to == null || "/".equals(from) || "/".equals(to)) {
            return failure(SignalOsDriveResultCode.INVALID_PATH, "[SignalOS] Rename path is invalid.");
        }
        if (findRecord(from) == null) {
            return failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] File path not found.");
        }
        if (findRecord(to) != null) {
            return failure(SignalOsDriveResultCode.ALREADY_EXISTS, "[SignalOS] Target path already exists.");
        }
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        if (!writable.success()) {
            return writable;
        }
        String fromPrefix = from + "/";
        List<SignalOsDataRecord> next = new ArrayList<>();
        boolean changed = false;
        for (SignalOsDataRecord record : drive.records()) {
            String path = pathOf(record);
            if (path.equals(from) || path.startsWith(fromPrefix)) {
                if (readOnly(record)) {
                    return failure(SignalOsDriveResultCode.READ_ONLY, "[SignalOS] File is read-only.");
                }
                String renamed = path.equals(from) ? to : to + path.substring(from.length());
                next.add(record.withMetadata(META_PATH, renamed).withMetadata(META_MODIFIED, stamp()));
                changed = true;
            } else {
                next.add(record);
            }
        }
        return changed
                ? success(withRecords(next), "[SignalOS] Path renamed.")
                : failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] File path not found.");
    }

    public SignalOsDriveWriteResult deletePath(String path) {
        String normalized = normalizePath(path);
        if (normalized == null || "/".equals(normalized)) {
            return failure(SignalOsDriveResultCode.INVALID_PATH, "[SignalOS] Delete path is invalid.");
        }
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        if (!writable.success()) {
            return writable;
        }
        String prefix = normalized + "/";
        List<SignalOsDataRecord> next = new ArrayList<>();
        boolean removed = false;
        for (SignalOsDataRecord record : drive.records()) {
            String candidate = pathOf(record);
            if (candidate.equals(normalized) || candidate.startsWith(prefix)) {
                if (readOnly(record)) {
                    return failure(SignalOsDriveResultCode.READ_ONLY, "[SignalOS] File is read-only.");
                }
                removed = true;
            } else {
                next.add(record);
            }
        }
        return removed
                ? success(withRecords(next), "[SignalOS] Path deleted.")
                : failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] File path not found.");
    }

    public SignalOsDriveWriteResult copyRecord(SignalOsDataRecord source, String path) {
        if (source == null) {
            return failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] Network record unavailable.");
        }
        String normalized = normalizePath(path);
        if (normalized == null || "/".equals(normalized)) {
            normalized = uniquePath("/records/" + fileSlug(source.title()) + ".txt");
        }
        if (findRecord(normalized) != null) {
            return failure(SignalOsDriveResultCode.ALREADY_EXISTS, "[SignalOS] File already exists.");
        }
        SignalOsDriveWriteResult capacity = requireWritableCapacity(false);
        if (!capacity.success()) {
            return capacity;
        }
        SignalOsDataRecord record = new SignalOsDataRecord(
                idForPath("record", normalized),
                source.title(),
                source.type(),
                source.source(),
                source.body(),
                source.order(),
                source.archived(),
                source.metadata())
                .withMetadata(META_PATH, normalized)
                .withMetadata(META_MIME, "text/plain")
                .withMetadata("signalos.source_id", source.id().toString())
                .withMetadata(META_CREATED, stamp())
                .withMetadata(META_MODIFIED, stamp());
        return success(withRecords(appendOrReplace(record)), "[SignalOS] Network record copied to drive.");
    }

    public SignalOsDriveWriteResult saveNote(Identifier noteId, String title, String body) {
        SignalOsDriveWriteResult writable = requireWritableCapacity(noteId != null);
        if (!writable.success()) {
            return writable;
        }
        SignalOsDataRecord existing = noteId == null ? null : record(noteId);
        if (existing != null && readOnly(existing)) {
            return failure(SignalOsDriveResultCode.READ_ONLY, "[SignalOS] Note is read-only.");
        }
        String safeTitle = blank(title, "Operator Note");
        String path = existing == null
                ? uniquePath("/notes/" + fileSlug(safeTitle) + ".md")
                : pathOf(existing);
        if (path.isBlank()) {
            path = uniquePath("/notes/" + fileSlug(safeTitle) + ".md");
        }
        Identifier id = existing == null ? idForPath(TYPE_NOTE, path) : existing.id();
        SignalOsDataRecord record = new SignalOsDataRecord(id, safeTitle, TYPE_NOTE, "Operator Notes",
                body == null ? "" : body, existing == null ? 1000 + drive.records().size() : existing.order(),
                false, existing == null ? Map.of() : existing.metadata())
                .withMetadata(META_PATH, path)
                .withMetadata(META_MIME, "text/markdown")
                .withMetadata(META_MODIFIED, stamp());
        if (existing == null) {
            record = record.withMetadata(META_CREATED, stamp());
        }
        return success(withRecords(appendOrReplace(record)), "[SignalOS] Note saved to active drive.");
    }

    public SignalOsDriveWriteResult deleteNote(Identifier noteId) {
        if (noteId == null) {
            return failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] Note unavailable.");
        }
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        if (!writable.success()) {
            return writable;
        }
        SignalOsDataRecord note = record(noteId);
        if (note == null || !TYPE_NOTE.equals(note.type())) {
            return failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] Note unavailable.");
        }
        if (readOnly(note)) {
            return failure(SignalOsDriveResultCode.READ_ONLY, "[SignalOS] Note is read-only.");
        }
        List<SignalOsDataRecord> next = drive.records().stream()
                .filter(record -> !record.id().equals(noteId))
                .toList();
        return success(withRecords(next), "[SignalOS] Note deleted from active drive.");
    }

    public SignalOsDriveWriteResult clearNotes() {
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        if (!writable.success()) {
            return writable;
        }
        for (SignalOsDataRecord record : drive.records()) {
            if (TYPE_NOTE.equals(record.type()) && readOnly(record)) {
                return failure(SignalOsDriveResultCode.READ_ONLY, "[SignalOS] One or more notes are read-only.");
            }
        }
        List<SignalOsDataRecord> next = drive.records().stream()
                .filter(record -> !TYPE_NOTE.equals(record.type()))
                .toList();
        return success(withRecords(next), "[SignalOS] Active drive notes cleared.");
    }

    public SignalOsDriveWriteResult setSetting(String key, String value) {
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        return writable.success()
                ? success(drive.withSetting(key, value), "[SignalOS] Active drive setting updated.")
                : writable;
    }

    public SignalOsDriveWriteResult setSession(String key, String value) {
        SignalOsDriveWriteResult writable = requireWritableCapacity(true);
        return writable.success()
                ? success(drive.withSessionValue(key, value), "")
                : writable;
    }

    private SignalOsDriveWriteResult requireWritableCapacity(boolean replacing) {
        if (drive.schemaVersion() == 0) {
            return failure(SignalOsDriveResultCode.NO_ACTIVE_DRIVE,
                    "[SignalOS] Insert a V2 SignalOS Data Drive first.");
        }
        if (!drive.isV2Supported()) {
            return failure(SignalOsDriveResultCode.UNSUPPORTED_DRIVE,
                    "[SignalOS] This drive is legacy V" + drive.schemaVersion() + " and is read-only.");
        }
        if (!replacing && drive.records().size() >= SignalOsDriveData.MAX_PLAYER_RECORDS) {
            return failure(SignalOsDriveResultCode.CAPACITY_FULL, "[SignalOS] Active drive is full.");
        }
        return SignalOsDriveWriteResult.success(drive, "");
    }

    private SignalOsDataRecord record(Identifier id) {
        return id == null ? null : drive.records().stream()
                .filter(record -> record.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private List<SignalOsDataRecord> appendOrReplace(SignalOsDataRecord record) {
        List<SignalOsDataRecord> next = new ArrayList<>();
        boolean replaced = false;
        for (SignalOsDataRecord existing : drive.records()) {
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
        return next;
    }

    private SignalOsDriveData withRecords(List<SignalOsDataRecord> records) {
        return new SignalOsDriveData(drive.schemaVersion(), drive.label(), records, drive.settings(), drive.session());
    }

    private SignalOsDriveWriteResult success(SignalOsDriveData next, String message) {
        return SignalOsDriveWriteResult.success(next, message);
    }

    private SignalOsDriveWriteResult failure(SignalOsDriveResultCode code, String message) {
        return SignalOsDriveWriteResult.failure(code, drive, message);
    }

    private static SignalOsDataRecord baseRecord(Identifier id, String title, String type, String body, int order) {
        return new SignalOsDataRecord(id, title, type, "SignalOS Files", body, order, false);
    }

    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String safe = path.strip().replace('\\', '/');
        if (safe.isBlank()) {
            return null;
        }
        if (!safe.startsWith("/")) {
            safe = "/" + safe;
        }
        while (safe.contains("//")) {
            safe = safe.replace("//", "/");
        }
        if (safe.length() > 1 && safe.endsWith("/")) {
            safe = safe.substring(0, safe.length() - 1);
        }
        if (safe.length() > MAX_PATH || safe.contains("/../") || safe.endsWith("/..")
                || safe.contains("/./") || safe.endsWith("/.")) {
            return null;
        }
        for (int i = 0; i < safe.length(); i++) {
            if (Character.isISOControl(safe.charAt(i))) {
                return null;
            }
        }
        return safe;
    }

    public static String pathOf(SignalOsDataRecord record) {
        return record == null ? "" : normalizeOrBlank(record.metadataValue(META_PATH, ""));
    }

    public static String nameOf(String path) {
        String normalized = normalizePath(path);
        if (normalized == null || "/".equals(normalized)) {
            return "/";
        }
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? normalized : normalized.substring(slash + 1);
    }

    public static boolean readOnly(SignalOsDataRecord record) {
        return record != null && Boolean.parseBoolean(record.metadataValue(META_READONLY, "false"));
    }

    private String uniquePath(String basePath) {
        String normalized = normalizePath(basePath);
        if (normalized == null || "/".equals(normalized)) {
            normalized = "/files/new_file.txt";
        }
        if (findRecord(normalized) == null) {
            return normalized;
        }
        int dot = normalized.lastIndexOf('.');
        int slash = normalized.lastIndexOf('/');
        String stem = dot > slash ? normalized.substring(0, dot) : normalized;
        String ext = dot > slash ? normalized.substring(dot) : "";
        for (int i = 2; i < 1000; i++) {
            String candidate = stem + "_" + i + ext;
            if (findRecord(candidate) == null) {
                return candidate;
            }
        }
        return normalized;
    }

    private static Identifier idForPath(String type, String path) {
        String safeType = fileSlug(type).replace('/', '_');
        String safePath = path == null ? "root" : path.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_")
                .replaceAll("^/+", "")
                .replace('.', '_');
        if (safePath.isBlank()) {
            safePath = "root";
        }
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, safeType + "/" + safePath);
    }

    private static String fileSlug(String value) {
        String safe = value == null ? "file" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]+", "_")
                .replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "file" : safe;
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String stamp() {
        return Long.toString(System.currentTimeMillis());
    }

    private static String normalizeOrBlank(String path) {
        String normalized = normalizePath(path);
        return normalized == null ? "" : normalized;
    }

    public Map<String, SignalOsDataRecord> pathIndex() {
        LinkedHashMap<String, SignalOsDataRecord> index = new LinkedHashMap<>();
        for (SignalOsDataRecord record : drive.records()) {
            String path = pathOf(record);
            if (!path.isBlank()) {
                index.put(path, record);
            }
        }
        return Map.copyOf(index);
    }
}
