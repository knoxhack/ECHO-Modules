package com.knoxhack.signalos.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsActionResult;
import com.knoxhack.signalos.api.SignalOsAppContext;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveFileSystem;
import com.knoxhack.signalos.api.SignalOsDriveResultCode;
import com.knoxhack.signalos.api.TerminalActionRegistry;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalMission;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SignalOsBuiltinActions {
    public static final Identifier PAGE_REWARDS = id("rewards");
    public static final Identifier PAGE_MISSIONS = id("missions");
    public static final Identifier PAGE_ARCHIVES = id("archives");
    public static final Identifier PAGE_NOTES = id("notes");
    public static final Identifier PAGE_SETTINGS = id("settings");
    public static final Identifier PAGE_FILES = id("files");
    public static final Identifier PAGE_SIGNALNET = id("signalnet");
    public static final Identifier CLAIM_REWARDS = id("claim_rewards");
    public static final Identifier CLAIM_MISSION = id("claim_mission");
    public static final Identifier MARK_ARCHIVE_READ = id("mark_archive_read");
    public static final Identifier SAVE_NOTE = id("save_note");
    public static final Identifier DELETE_NOTE = id("delete_note");
    public static final Identifier CLEAR_NOTES = id("clear_notes");
    public static final Identifier SET_PREFERENCE = id("set_preference");
    public static final Identifier SET_SESSION = id("set_session");
    public static final Identifier CREATE_FILE = id("create_file");
    public static final Identifier CREATE_FOLDER = id("create_folder");
    public static final Identifier RENAME_PATH = id("rename_path");
    public static final Identifier DELETE_PATH = id("delete_path");
    public static final Identifier COPY_RECORD_TO_DRIVE = id("copy_record_to_drive");
    public static final Identifier BOOKMARK_NET_PAGE = id("bookmark_net_page");
    public static final Identifier SAVE_NET_PAGE = id("save_net_page");
    public static final Identifier RECORD_NET_RECENT = id("record_net_recent");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private SignalOsBuiltinActions() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalActionRegistry.registerResult(PAGE_REWARDS, CLAIM_REWARDS,
                (player, payload) -> SignalOsTerminalServices.claimRewards(player)
                        ? SignalOsActionResult.success("")
                        : SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR, ""));
        TerminalActionRegistry.registerResult(PAGE_MISSIONS, CLAIM_MISSION, SignalOsBuiltinActions::claimMission);
        TerminalActionRegistry.registerResult(PAGE_ARCHIVES, MARK_ARCHIVE_READ, SignalOsBuiltinActions::markArchiveRead);
        TerminalActionRegistry.registerResult(PAGE_NOTES, SAVE_NOTE, SignalOsBuiltinActions::saveNote);
        TerminalActionRegistry.registerResult(PAGE_NOTES, DELETE_NOTE, SignalOsBuiltinActions::deleteNote);
        TerminalActionRegistry.registerResult(PAGE_NOTES, CLEAR_NOTES, SignalOsBuiltinActions::clearNotes);
        TerminalActionRegistry.registerResult(PAGE_SETTINGS, SET_PREFERENCE, SignalOsBuiltinActions::setPreference);
        TerminalActionRegistry.registerResult(PAGE_SETTINGS, SET_SESSION, SignalOsBuiltinActions::setSession);
        TerminalActionRegistry.registerResult(PAGE_FILES, CREATE_FILE, SignalOsBuiltinActions::createFile);
        TerminalActionRegistry.registerResult(PAGE_FILES, CREATE_FOLDER, SignalOsBuiltinActions::createFolder);
        TerminalActionRegistry.registerResult(PAGE_FILES, RENAME_PATH, SignalOsBuiltinActions::renamePath);
        TerminalActionRegistry.registerResult(PAGE_FILES, DELETE_PATH, SignalOsBuiltinActions::deletePath);
        TerminalActionRegistry.registerResult(PAGE_FILES, COPY_RECORD_TO_DRIVE, SignalOsBuiltinActions::copyRecordToDrive);
        TerminalActionRegistry.registerAppActionResult(PAGE_SIGNALNET, BOOKMARK_NET_PAGE,
                SignalOsBuiltinActions::bookmarkNetPage);
        TerminalActionRegistry.registerAppActionResult(PAGE_SIGNALNET, SAVE_NET_PAGE,
                SignalOsBuiltinActions::saveNetPage);
        TerminalActionRegistry.registerAppActionResult(PAGE_SIGNALNET, RECORD_NET_RECENT,
                SignalOsBuiltinActions::recordNetRecent);
    }

    private static SignalOsActionResult claimMission(ServerPlayer player, String payload) {
        Identifier missionId = Identifier.tryParse(payload == null ? "" : payload);
        TerminalMission mission = SignalOsContentRegistry.mission(missionId);
        if (mission == null) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.NOT_FOUND,
                    missionId == null ? "" : "[SignalOS] Mission cache unavailable.");
        }
        if (!mission.rewardClaim() || mission.rewards().isEmpty()) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD,
                    "[SignalOS] Mission has no claimable cache.");
        }
        if (SignalOsPlayerData.isMissionClaimed(player, mission.id())) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.ALREADY_EXISTS,
                    "[SignalOS] Mission cache already claimed.");
        }
        List<ItemStack> rewards = mission.rewardStacks();
        if (rewards.isEmpty()) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD,
                    "[SignalOS] Mission cache has no valid rewards.");
        }
        if (!completed(player, mission)) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR,
                    "[SignalOS] Mission completion signal is not ready.");
        }
        if (!SignalOsTerminalServices.storeRewards(player, mission.id().toString(), rewards)) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR, "");
        }
        SignalOsPlayerData.markMissionClaimed(player, mission.id());
        return SignalOsActionResult.success("[SignalOS] Mission cache claimed.");
    }

    private static SignalOsActionResult markArchiveRead(ServerPlayer player, String payload) {
        Identifier archiveId = Identifier.tryParse(payload == null ? "" : payload);
        return markArchiveRead(player, archiveId)
                ? SignalOsActionResult.success("[SignalOS] Archive marked read.")
                : SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR, "");
    }

    public static boolean markArchiveRead(Player player, Identifier archiveId) {
        TerminalArchiveRecord archive = SignalOsContentRegistry.archive(archiveId);
        if (archive == null) {
            if (archiveId != null) {
                status(player, "[SignalOS] Archive record unavailable.");
            }
            return false;
        }
        if (archive.locked()) {
            status(player, "[SignalOS] Archive record locked.");
            return false;
        }
        SignalOsPlayerData.markArchiveRead(player, archive.id());
        return true;
    }

    public static boolean completed(ServerPlayer player, TerminalMission mission) {
        if (mission == null) {
            return false;
        }
        if (mission.completionAdvancement() == null) {
            return true;
        }
        if (player == null || player.level().getServer() == null) {
            return false;
        }
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(mission.completionAdvancement());
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static SignalOsActionResult saveNote(ServerPlayer player, String payload) {
        String safe = payload == null ? "" : payload;
        String title = "Operator Note";
        String body = "Created from the SignalOS Notes app.";
        Identifier noteId = null;
        if (safe.stripLeading().startsWith("{")) {
            try {
                JsonObject json = JsonParser.parseString(safe).getAsJsonObject();
                title = string(json, "title", title);
                body = string(json, "body", body);
                String idValue = string(json, "id", "");
                noteId = idValue.isBlank() ? null : Identifier.tryParse(idValue);
            } catch (RuntimeException exception) {
                return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD,
                        "[SignalOS] Invalid note payload.");
            }
        } else {
            int split = safe.indexOf('\n');
            if (split >= 0) {
                title = safe.substring(0, split).strip();
                body = safe.substring(split + 1).strip();
            } else if (!safe.isBlank()) {
                body = safe.strip();
            }
        }
        Identifier parsedNoteId = noteId;
        String safeTitle = title;
        String safeBody = body;
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.saveNote(parsedNoteId, safeTitle, safeBody)));
    }

    private static SignalOsActionResult deleteNote(ServerPlayer player, String payload) {
        Identifier noteId = Identifier.tryParse(payload == null ? "" : payload.strip());
        if (noteId == null) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.NOT_FOUND, "[SignalOS] Note unavailable.");
        }
        Identifier safeNoteId = noteId;
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.deleteNote(safeNoteId)));
    }

    private static SignalOsActionResult clearNotes(ServerPlayer player, String payload) {
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                SignalOsDriveFileSystem::clearNotes));
    }

    private static SignalOsActionResult setPreference(ServerPlayer player, String payload) {
        String safe = payload == null ? "" : payload;
        int split = safe.indexOf('=');
        if (split <= 0) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD,
                    "[SignalOS] Invalid setting payload.");
        }
        String key = safe.substring(0, split).strip();
        String value = safe.substring(split + 1).strip();
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.setSetting(key, value)));
    }

    private static SignalOsActionResult setSession(ServerPlayer player, String payload) {
        String safe = payload == null ? "" : payload;
        int split = safe.indexOf('=');
        if (split <= 0) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD, "");
        }
        String key = safe.substring(0, split).strip();
        String value = safe.substring(split + 1).strip();
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.setSession(key, value)));
    }

    private static SignalOsActionResult createFile(ServerPlayer player, String payload) {
        JsonObject json = jsonPayload(payload);
        String path = string(json, "path", "/files/new_file.txt");
        String title = string(json, "title", "New File");
        String body = string(json, "body", "");
        String mime = string(json, "mime", "text/plain");
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.createFile(path, title, body, mime)));
    }

    private static SignalOsActionResult createFolder(ServerPlayer player, String payload) {
        JsonObject json = jsonPayload(payload);
        String path = string(json, "path", "/files/new_folder");
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.createFolder(path)));
    }

    private static SignalOsActionResult renamePath(ServerPlayer player, String payload) {
        JsonObject json = jsonPayload(payload);
        String from = string(json, "from", "");
        String to = string(json, "to", "");
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.renamePath(from, to)));
    }

    private static SignalOsActionResult deletePath(ServerPlayer player, String payload) {
        JsonObject json = jsonPayload(payload);
        String path = json.size() == 0 ? payload == null ? "" : payload.strip() : string(json, "path", "");
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.deletePath(path)));
    }

    private static SignalOsActionResult copyRecordToDrive(ServerPlayer player, String payload) {
        JsonObject json = jsonPayload(payload);
        String recordValue = json.size() == 0 ? payload == null ? "" : payload.strip() : string(json, "record", "");
        String path = string(json, "path", "");
        Identifier recordId = Identifier.tryParse(recordValue);
        if (recordId == null) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.INVALID_PAYLOAD,
                    "[SignalOS] Network record id is invalid.");
        }
        Optional<SignalOsDataRecord> record = SignalOsComputerNetworkService.networkRecords(player).stream()
                .filter(candidate -> candidate.id().equals(recordId))
                .findFirst();
        if (record.isEmpty()) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.NOT_FOUND,
                    "[SignalOS] Network record unavailable.");
        }
        return SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(player,
                fileSystem -> fileSystem.copyRecord(record.get(), path)));
    }

    private static SignalOsActionResult bookmarkNetPage(SignalOsAppContext context, String payload) {
        return writeNetPage(context, payload, NetWriteMode.BOOKMARK);
    }

    private static SignalOsActionResult saveNetPage(SignalOsAppContext context, String payload) {
        return writeNetPage(context, payload, NetWriteMode.SAVE);
    }

    private static SignalOsActionResult recordNetRecent(SignalOsAppContext context, String payload) {
        return writeNetPage(context, payload, NetWriteMode.RECENT);
    }

    private static SignalOsActionResult writeNetPage(SignalOsAppContext context, String payload, NetWriteMode mode) {
        if (context == null || context.player() == null) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR, "[SignalOS] SignalNet action failed.");
        }
        String address = addressPayload(payload);
        Optional<SignalOsDataRecord> record = SignalOsNetService.recordForAddress(context.player(), context.accessTier(), address);
        if (record.isEmpty()) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.NOT_FOUND,
                    "[SignalOS] SignalNet page unavailable.");
        }
        SignalOsDataRecord page = record.get();
        String safeSlug = SignalOsNetService.slug(page.metadataValue(SignalOsNetService.META_ADDRESS, page.title()));
        return switch (mode) {
            case BOOKMARK -> SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(
                    context.player(),
                    fileSystem -> fileSystem.createFile(
                            "/signalnet/bookmarks/" + safeSlug + ".url",
                            "Bookmark - " + page.title(),
                            page.metadataValue(SignalOsNetService.META_ADDRESS, address),
                            "text/signalnet-bookmark")));
            case SAVE -> SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(
                    context.player(),
                    fileSystem -> fileSystem.copyRecord(page, "/signalnet/saved/" + safeSlug + ".txt")));
            case RECENT -> SignalOsActionResult.fromDriveResult(SignalOsTerminalServices.updateActiveDriveFileSystem(
                    context.player(),
                    fileSystem -> fileSystem.createFile(
                            "/signalnet/recent/" + System.currentTimeMillis() + "_" + safeSlug + ".url",
                            "Recent - " + page.title(),
                            page.metadataValue(SignalOsNetService.META_ADDRESS, address),
                            "text/signalnet-recent")));
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json != null && json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static JsonObject jsonPayload(String payload) {
        String safe = payload == null ? "" : payload.strip();
        if (safe.startsWith("{")) {
            try {
                return JsonParser.parseString(safe).getAsJsonObject();
            } catch (RuntimeException ignored) {
                return new JsonObject();
            }
        }
        return new JsonObject();
    }

    private static void status(Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(message), true);
        } else if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static String addressPayload(String payload) {
        JsonObject json = jsonPayload(payload);
        if (json.size() > 0) {
            return string(json, "address", "");
        }
        return payload == null ? "" : payload.strip();
    }

    private enum NetWriteMode {
        BOOKMARK,
        SAVE,
        RECENT
    }
}
