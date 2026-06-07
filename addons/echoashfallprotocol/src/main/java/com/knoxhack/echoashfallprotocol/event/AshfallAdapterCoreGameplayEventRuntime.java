package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Routes live AdapterCore gameplay events through the Native Loader runtime host.
 */
public final class AshfallAdapterCoreGameplayEventRuntime {
    private static final String LAST_EVENT_PREFIX = "ashes_of_tomorrow.adaptercore.last_";
    private static final String LAST_EVENT_SUFFIX = "_event";
    private static final String LAST_EVENT_TICK_SUFFIX = "_event_tick";

    private AshfallAdapterCoreGameplayEventRuntime() {
    }

    public static NativeResult publish(
            ServerPlayer player,
            String lane,
            String eventId,
            Map<String, Object> payload) {
        return publish(player, lane, eventId, payload, null, true);
    }

    public static NativeResult publish(
            ServerPlayer player,
            String lane,
            String eventId,
            Map<String, Object> payload,
            @Nullable BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        Optional<NativeResult> guard = AshfallAdapterCoreRuntimeGuards.guardPublish(
                player,
                lane,
                eventId,
                payload,
                requiredLoadedPos,
                dedupeSameTick);
        if (guard.isPresent()) {
            return guard.get();
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return new NativeResult(false, "SKIPPED_WRONG_SIDE",
                    "AdapterCore gameplay event skipped outside the logical server.", Map.of(
                    "eventId", eventId,
                    "runtimeLane", lane,
                    "realNativeStateMutated", false));
        }

        NativeLoaderEchoRuntimeHost host = NativeLoaderRuntimeHostFactory.create(player, level);
        NativeMutationContext context = host.context(
                "event." + safeLane(lane) + "." + eventId,
                "EchoNativeRuntimeHost.Events",
                "publish");
        NativeResult result = host.events().publish(
                new NativeEvent(eventId, new NativePlayerRef(player.getUUID().toString()), safePayload(payload)),
                context);
        if (result.mutated()) {
            recordLastEvent(player, lane, eventId, context.gameTime());
        }
        return result;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (payload != null) {
            copy.putAll(payload);
        }
        return Map.copyOf(copy);
    }

    private static void recordLastEvent(ServerPlayer player, String lane, String eventId, long gameTime) {
        String suffix = laneSuffix(lane);
        CompoundTag playerData = player.getPersistentData();
        playerData.putString(LAST_EVENT_PREFIX + suffix + LAST_EVENT_SUFFIX, eventId);
        playerData.putLong(LAST_EVENT_PREFIX + suffix + LAST_EVENT_TICK_SUFFIX, gameTime);
    }

    private static String laneSuffix(String lane) {
        String safe = safeLane(lane);
        return "early_event".equals(safe) ? "early" : safe;
    }

    private static String safeLane(String lane) {
        return lane == null || lane.isBlank() ? "runtime" : lane.replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }
}
