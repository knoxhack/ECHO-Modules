package com.knoxhack.echoterminal.player;

import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.registry.ModAttachments;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TerminalPlayerData implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalPlayerData> STREAM_CODEC = StreamCodec.of(
            TerminalPlayerData::writeSync,
            TerminalPlayerData::readSync);

    private static final Map<UUID, TerminalPlayerData> NATIVE_SESSION_DATA = new ConcurrentHashMap<>();

    private String trackedTabId = "";
    private String trackedChapterId = "";
    private String trackedMissionId = "";
    private String trackedTitle = "";
    private String trackedNextStep = "";
    private int trackedColor;
    private int trackedTick;
    private final Set<String> readArchiveIds = new HashSet<>();

    public static TerminalPlayerData get(Player player) {
        if (nativeLoaderClientActive()) {
            return nativeSessionData(player);
        }
        if (player == null) {
            return new TerminalPlayerData();
        }
        try {
            return AttachmentAccess.get(player);
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Terminal player attachments unavailable; using native session data.", exception);
            return nativeSessionData(player);
        }
    }

    public static void saveAndSync(ServerPlayer player, TerminalPlayerData data) {
        if (player == null || data == null) {
            return;
        }
        if (nativeLoaderClientActive()) {
            NATIVE_SESSION_DATA.put(player.getUUID(), data);
            return;
        }
        try {
            AttachmentAccess.saveAndSync(player, data);
        } catch (RuntimeException | LinkageError exception) {
            EchoTerminal.LOGGER.debug("Terminal player data saved without client sync.", exception);
            NATIVE_SESSION_DATA.put(player.getUUID(), data);
        }
    }

    public TrackedMission trackedMission() {
        if (trackedMissionId.isBlank() || trackedTabId.isBlank()) {
            return null;
        }
        Identifier tabId = Identifier.tryParse(trackedTabId);
        Identifier chapterId = Identifier.tryParse(trackedChapterId);
        Identifier missionId = Identifier.tryParse(trackedMissionId);
        if (tabId == null || chapterId == null || missionId == null) {
            return null;
        }
        return new TrackedMission(tabId, chapterId, missionId, trackedTitle, trackedNextStep, trackedColor, trackedTick);
    }

    public boolean isTracking(Identifier tabId, Identifier missionId) {
        return tabId != null && missionId != null
                && tabId.toString().equals(trackedTabId)
                && missionId.toString().equals(trackedMissionId);
    }

    public void trackMission(
            Identifier tabId,
            Identifier chapterId,
            Identifier missionId,
            String title,
            String nextStep,
            int color,
            int tick) {
        trackedTabId = tabId == null ? "" : tabId.toString();
        trackedChapterId = chapterId == null ? "" : chapterId.toString();
        trackedMissionId = missionId == null ? "" : missionId.toString();
        trackedTitle = title == null ? "" : title;
        trackedNextStep = nextStep == null ? "" : nextStep;
        trackedColor = color;
        trackedTick = Math.max(0, tick);
    }

    public void clearTrackedMission() {
        trackedTabId = "";
        trackedChapterId = "";
        trackedMissionId = "";
        trackedTitle = "";
        trackedNextStep = "";
        trackedColor = 0;
        trackedTick = 0;
    }

    public boolean isArchiveRead(Identifier archiveId) {
        return archiveId != null && readArchiveIds.contains(archiveId.toString());
    }

    public void markArchiveRead(Identifier archiveId) {
        if (archiveId != null) {
            readArchiveIds.add(archiveId.toString());
        }
    }

    public Set<String> readArchiveIds() {
        return Set.copyOf(readArchiveIds);
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, TerminalPlayerData data) {
        buf.writeUtf(data.trackedTabId);
        buf.writeUtf(data.trackedChapterId);
        buf.writeUtf(data.trackedMissionId);
        buf.writeUtf(data.trackedTitle);
        buf.writeUtf(data.trackedNextStep);
        buf.writeVarInt(data.trackedColor);
        buf.writeVarInt(data.trackedTick);
        writeStringSet(buf, data.readArchiveIds);
    }

    private static TerminalPlayerData readSync(RegistryFriendlyByteBuf buf) {
        TerminalPlayerData data = new TerminalPlayerData();
        data.trackedTabId = buf.readUtf();
        data.trackedChapterId = buf.readUtf();
        data.trackedMissionId = buf.readUtf();
        data.trackedTitle = buf.readUtf();
        data.trackedNextStep = buf.readUtf();
        data.trackedColor = buf.readVarInt();
        data.trackedTick = buf.readVarInt();
        readStringSet(buf, data.readArchiveIds);
        return data;
    }

    private static void writeStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }

    private static boolean nativeLoaderClientActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static TerminalPlayerData nativeSessionData(Player player) {
        if (player == null) {
            return new TerminalPlayerData();
        }
        return NATIVE_SESSION_DATA.computeIfAbsent(player.getUUID(), ignored -> new TerminalPlayerData());
    }

    private static final class AttachmentAccess {
        private AttachmentAccess() {
        }

        private static TerminalPlayerData get(Player player) {
            return player.getData(ModAttachments.TERMINAL_PLAYER_DATA.get());
        }

        private static void saveAndSync(ServerPlayer player, TerminalPlayerData data) {
            player.setData(ModAttachments.TERMINAL_PLAYER_DATA.get(), data);
            player.syncData(ModAttachments.TERMINAL_PLAYER_DATA.get());
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString("trackedTabId", trackedTabId);
        output.putString("trackedChapterId", trackedChapterId);
        output.putString("trackedMissionId", trackedMissionId);
        output.putString("trackedTitle", trackedTitle);
        output.putString("trackedNextStep", trackedNextStep);
        output.putInt("trackedColor", trackedColor);
        output.putInt("trackedTick", trackedTick);
        output.putInt("archiveReadCount", readArchiveIds.size());
        int index = 0;
        for (String id : readArchiveIds) {
            output.putString("archiveRead_" + index++, id);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        trackedTabId = input.getStringOr("trackedTabId", "");
        trackedChapterId = input.getStringOr("trackedChapterId", "");
        trackedMissionId = input.getStringOr("trackedMissionId", "");
        trackedTitle = input.getStringOr("trackedTitle", "");
        trackedNextStep = input.getStringOr("trackedNextStep", "");
        trackedColor = input.getIntOr("trackedColor", 0);
        trackedTick = input.getIntOr("trackedTick", 0);
        readArchiveIds.clear();
        int count = input.getIntOr("archiveReadCount", 0);
        for (int i = 0; i < count; i++) {
            String id = input.getStringOr("archiveRead_" + i, "");
            if (!id.isBlank()) {
                readArchiveIds.add(id);
            }
        }
    }

    public record TrackedMission(
            Identifier tabId,
            Identifier chapterId,
            Identifier missionId,
            String title,
            String nextStep,
            int color,
            int tick) {
    }
}
