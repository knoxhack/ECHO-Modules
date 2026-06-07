package com.knoxhack.signalos.service;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SignalOsPlayerData {
    private static final String ROOT = "signalos";
    public static final int MAX_NOTES = 64;
    public static final int MAX_NOTE_TITLE = 80;
    public static final int MAX_NOTE_BODY = 2000;
    public static final int MAX_PREFERENCE_KEY = 64;
    public static final int MAX_PREFERENCE_VALUE = 160;

    private SignalOsPlayerData() {
    }

    public static boolean isMissionClaimed(Player player, Identifier missionId) {
        return missionId != null && readSet(player, "claimed_mission").contains(missionId.toString());
    }

    public static void markMissionClaimed(Player player, Identifier missionId) {
        if (player == null || missionId == null) {
            return;
        }
        writeSet(player, "claimed_mission", with(readSet(player, "claimed_mission"), missionId.toString()));
        SignalOsComputerNetworkService.invalidateCache();
    }

    public static boolean isArchiveRead(Player player, Identifier archiveId) {
        return archiveId != null && readSet(player, "read_archive").contains(archiveId.toString());
    }

    public static void markArchiveRead(Player player, Identifier archiveId) {
        if (player == null || archiveId == null) {
            return;
        }
        writeSet(player, "read_archive", with(readSet(player, "read_archive"), archiveId.toString()));
        SignalOsComputerNetworkService.invalidateCache();
    }

    public static List<SignalOsDataRecord> notes(Player player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        int count = Math.min(MAX_NOTES, root.getIntOr("note_count", 0));
        List<SignalOsDataRecord> notes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String idValue = root.getStringOr("note_" + i + "_id", "");
            Identifier id = Identifier.tryParse(idValue);
            if (id == null) {
                continue;
            }
            notes.add(new SignalOsDataRecord(
                    id,
                    root.getStringOr("note_" + i + "_title", id.getPath()),
                    "note",
                    "Operator Notes",
                    root.getStringOr("note_" + i + "_body", ""),
                    1000 + i,
                    root.getBooleanOr("note_" + i + "_archived", false)));
        }
        return List.copyOf(notes);
    }

    public static SignalOsDataRecord addNote(ServerPlayer player, String title, String body) {
        return saveNote(player, null, title, body);
    }

    public static SignalOsDataRecord saveNote(ServerPlayer player, Identifier noteId, String title, String body) {
        if (player == null) {
            return null;
        }
        List<SignalOsDataRecord> notes = new ArrayList<>(notes(player));
        String safeTitle = clamp(title == null || title.isBlank() ? "Operator Note" : title.strip(), MAX_NOTE_TITLE);
        String safeBody = clamp(body == null ? "" : body.strip(), MAX_NOTE_BODY);
        for (int i = 0; i < notes.size(); i++) {
            SignalOsDataRecord existing = notes.get(i);
            if (noteId != null && existing.id().equals(noteId)) {
                SignalOsDataRecord updated = new SignalOsDataRecord(existing.id(),
                        safeTitle,
                        "note",
                        existing.source(),
                        safeBody,
                        existing.order(),
                        existing.archived());
                notes.set(i, updated);
                writeNotes(player, notes);
                return updated;
            }
        }
        int next = Math.min(MAX_NOTES - 1, notes.size());
        Identifier id = nextNoteId(player);
        SignalOsDataRecord note = new SignalOsDataRecord(id,
                safeTitle,
                "note",
                "Operator Notes",
                safeBody,
                1000 + next,
                false);
        if (notes.size() >= MAX_NOTES) {
            notes.removeFirst();
        }
        notes.add(note);
        writeNotes(player, notes);
        return note;
    }

    public static boolean deleteNote(Player player, Identifier noteId) {
        if (player == null || noteId == null) {
            return false;
        }
        List<SignalOsDataRecord> notes = new ArrayList<>(notes(player));
        boolean removed = notes.removeIf(note -> note.id().equals(noteId));
        if (removed) {
            writeNotes(player, notes);
        }
        return removed;
    }

    public static void clearNotes(Player player) {
        writeNotes(player, List.of());
    }

    public static String preference(Player player, String key, String fallback) {
        if (player == null || key == null || key.isBlank()) {
            return fallback;
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        return root.getStringOr("pref_" + key, fallback == null ? "" : fallback);
    }

    public static void setPreference(Player player, String key, String value) {
        if (player == null || key == null || key.isBlank()) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        root.putString("pref_" + clamp(key.strip(), MAX_PREFERENCE_KEY),
                clamp(value == null ? "" : value.strip(), MAX_PREFERENCE_VALUE));
        player.getPersistentData().put(ROOT, root);
        SignalOsComputerNetworkService.invalidateCache();
    }

    private static void writeNotes(Player player, List<SignalOsDataRecord> notes) {
        if (player == null) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        int previousCount = root.getIntOr("note_count", 0);
        int index = 0;
        for (SignalOsDataRecord note : notes == null ? List.<SignalOsDataRecord>of() : notes) {
            if (note == null || index >= MAX_NOTES) {
                continue;
            }
            String prefix = "note_" + index + "_";
            root.putString(prefix + "id", note.id().toString());
            root.putString(prefix + "title", clamp(note.title(), MAX_NOTE_TITLE));
            root.putString(prefix + "body", clamp(note.body(), MAX_NOTE_BODY));
            root.putBoolean(prefix + "archived", note.archived());
            index++;
        }
        for (int stale = index; stale < previousCount; stale++) {
            String prefix = "note_" + stale + "_";
            root.remove(prefix + "id");
            root.remove(prefix + "title");
            root.remove(prefix + "body");
            root.remove(prefix + "archived");
        }
        root.putInt("note_count", index);
        player.getPersistentData().put(ROOT, root);
        SignalOsComputerNetworkService.invalidateCache();
    }

    private static Set<String> readSet(Player player, String prefix) {
        if (player == null) {
            return Set.of();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        int count = root.getIntOr(prefix + "_count", 0);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            String value = root.getStringOr(prefix + "_" + i, "");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static void writeSet(Player player, String prefix, Set<String> values) {
        if (player == null) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        int previousCount = root.getIntOr(prefix + "_count", 0);
        int index = 0;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                root.putString(prefix + "_" + index++, value);
            }
        }
        for (int stale = index; stale < previousCount; stale++) {
            root.remove(prefix + "_" + stale);
        }
        root.putInt(prefix + "_count", index);
        player.getPersistentData().put(ROOT, root);
    }

    private static Set<String> with(Set<String> values, String value) {
        LinkedHashSet<String> next = new LinkedHashSet<>(values);
        if (value != null && !value.isBlank()) {
            next.add(value);
        }
        return next;
    }

    private static Identifier nextNoteId(ServerPlayer player) {
        long tick = player.level() == null ? 0L : player.level().getGameTime();
        return Identifier.fromNamespaceAndPath(SignalOS.MODID,
                "note/" + player.getUUID().toString().substring(0, 8).toLowerCase(java.util.Locale.ROOT) + "/" + tick);
    }

    private static String clamp(String value, int maxLength) {
        String safe = value == null ? "" : value;
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength);
    }
}
