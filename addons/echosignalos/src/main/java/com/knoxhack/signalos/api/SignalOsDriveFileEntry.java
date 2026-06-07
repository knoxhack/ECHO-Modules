package com.knoxhack.signalos.api;

import net.minecraft.resources.Identifier;

public record SignalOsDriveFileEntry(
        Identifier id,
        String path,
        String name,
        String type,
        boolean folder,
        boolean readOnly,
        String mime,
        String tags,
        int order) {
    public SignalOsDriveFileEntry {
        path = path == null || path.isBlank() ? "/" : path.strip();
        name = name == null || name.isBlank() ? SignalOsDriveFileSystem.nameOf(path) : name.strip();
        type = type == null || type.isBlank() ? "file" : type.strip().toLowerCase(java.util.Locale.ROOT);
        mime = mime == null ? "" : mime.strip();
        tags = tags == null ? "" : tags.strip();
    }

    public static SignalOsDriveFileEntry fromRecord(SignalOsDataRecord record) {
        if (record == null) {
            return null;
        }
        String path = record.metadataValue(SignalOsDriveFileSystem.META_PATH, "");
        boolean folder = SignalOsDriveFileSystem.TYPE_FOLDER.equals(record.type());
        return new SignalOsDriveFileEntry(record.id(), path, SignalOsDriveFileSystem.nameOf(path), record.type(),
                folder,
                Boolean.parseBoolean(record.metadataValue(SignalOsDriveFileSystem.META_READONLY, "false")),
                record.metadataValue(SignalOsDriveFileSystem.META_MIME, ""),
                record.metadataValue(SignalOsDriveFileSystem.META_TAGS, ""),
                record.order());
    }
}
