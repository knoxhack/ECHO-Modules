package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.data.SaveMigrationHandler;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Shared guard rails for live AdapterCore runtime publishers.
 */
public final class AshfallAdapterCoreRuntimeGuards {
    private static final String DEDUPE_KEY_PREFIX = "ashes_of_tomorrow.adaptercore.dedupe.";
    private static long lastMissionReplayTick = Long.MIN_VALUE;

    private AshfallAdapterCoreRuntimeGuards() {
    }

    public static Optional<NativeResult> guardPublish(
            @Nullable ServerPlayer player,
            String lane,
            String eventId,
            Map<String, Object> payload,
            @Nullable BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        Optional<NativeResult> invalidPlayer = guardServerPlayer(player, lane, eventId);
        if (invalidPlayer.isPresent()) {
            return invalidPlayer;
        }
        ServerPlayer serverPlayer = player;
        SaveMigrationHandler.ensureCurrent(serverPlayer, "adaptercore_" + safeLane(lane));

        if (requiredLoadedPos != null && !serverPlayer.level().isLoaded(requiredLoadedPos)) {
            return Optional.of(skipped(
                    lane,
                    eventId,
                    "SKIPPED_UNLOADED_CHUNK",
                    "AdapterCore runtime event skipped because its target chunk is not loaded.",
                    Map.of("pos", positionSnapshot(requiredLoadedPos))));
        }

        if (dedupeSameTick && !claimSameTickEvent(serverPlayer, lane, eventId, payload)) {
            return Optional.of(skipped(
                    lane,
                    eventId,
                    "SKIPPED_DUPLICATE_EVENT",
                    "AdapterCore runtime event already executed for this player on the current tick.",
                    Map.of("dedupeKey", dedupeKey(eventId, payload))));
        }
        return Optional.empty();
    }

    public static Optional<NativeResult> guardServerPlayer(
            @Nullable ServerPlayer player,
            String lane,
            String eventId) {
        if (player == null) {
            return Optional.of(skipped(lane, eventId, "SKIPPED_INVALID_PLAYER",
                    "AdapterCore runtime event skipped for missing player.", Map.of()));
        }
        if (!(player.level() instanceof ServerLevel) || player.level().isClientSide()) {
            return Optional.of(skipped(lane, eventId, "SKIPPED_WRONG_SIDE",
                    "AdapterCore runtime event skipped outside the logical server.", Map.of()));
        }
        if (player.isRemoved()) {
            return Optional.of(skipped(lane, eventId, "SKIPPED_INVALID_PLAYER",
                    "AdapterCore runtime event skipped for a detached player.", Map.of(
                            "playerId", player.getUUID().toString())));
        }
        return Optional.empty();
    }

    public static void ensureMissionContentReady(ServerPlayer player, String reason) {
        if (player == null || EchoCoreServices.missionCoreAvailable()) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime == lastMissionReplayTick) {
            return;
        }
        lastMissionReplayTick = gameTime;
        EchoCoreServices.replayDeferredContent("ashfall_adaptercore_" + safeLane(reason));
    }

    private static boolean claimSameTickEvent(
            ServerPlayer player,
            String lane,
            String eventId,
            Map<String, Object> payload) {
        long gameTime = player.level().getGameTime();
        String prefix = DEDUPE_KEY_PREFIX + safeLane(lane) + ".";
        String key = dedupeKey(eventId, payload);
        CompoundTag playerData = player.getPersistentData();
        if (key.equals(playerData.getStringOr(prefix + "key", ""))
                && playerData.getLongOr(prefix + "tick", Long.MIN_VALUE) == gameTime) {
            return false;
        }
        playerData.putString(prefix + "key", key);
        playerData.putLong(prefix + "tick", gameTime);
        return true;
    }

    private static String dedupeKey(String eventId, Map<String, Object> payload) {
        return eventId + "|"
                + stringValue(payload, "target") + "|"
                + stringValue(payload, "marker") + "|"
                + stringValue(payload, "source") + "|"
                + stringValue(payload, "pos");
    }

    private static String safeLane(String lane) {
        return lane == null || lane.isBlank() ? "runtime" : lane.replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static NativeResult skipped(
            String lane,
            String eventId,
            String status,
            String message,
            Map<String, Object> snapshot) {
        return new NativeResult(false, status, message, Map.of(
                "runtimeLane", lane == null ? "runtime" : lane,
                "eventId", eventId == null ? "" : eventId,
                "details", snapshot == null ? Map.of() : snapshot,
                "realNativeStateMutated", false));
    }
}
