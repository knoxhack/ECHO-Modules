package com.knoxhack.signalos.integration;

import com.knoxhack.echocore.api.EchoRuntimeSpineBus;
import com.knoxhack.echocore.api.EchoRuntimeSpineEvent;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsActionResult;
import com.knoxhack.signalos.network.SignalOsActionPacket;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Publishes successful server-side SignalOS app actions into the shared runtime spine.
 */
public final class SignalOsRuntimeSpineBridge {
    public static final Identifier SIGNALOS_ACTION_SUCCEEDED =
            Identifier.fromNamespaceAndPath(SignalOS.MODID, "runtime/action_succeeded");

    private static final int MAX_CONTEXT_VALUE = 160;

    private SignalOsRuntimeSpineBridge() {
    }

    public static boolean publishAction(
            ServerPlayer player,
            SignalOsActionPacket packet,
            SignalOsActionResult result) {
        if (player == null || packet == null || result == null || !result.success()) {
            return false;
        }
        Map<String, String> context = new LinkedHashMap<>();
        context.put("ui_surface", "signalos");
        context.put("action_page", packet.pageId().toString());
        context.put("action_id", packet.actionId().toString());
        context.put("result_code", result.code().name().toLowerCase(Locale.ROOT));
        context.put("objective_type", "custom");
        if (!result.message().isBlank()) {
            context.put("action_message", shorten(result.message()));
        }
        return EchoRuntimeSpineBus.publish(EchoRuntimeSpineEvent.of(
                SignalOS.MODID,
                SIGNALOS_ACTION_SUCCEEDED,
                player,
                packet.actionId(),
                1,
                context));
    }

    private static String shorten(String value) {
        String safe = value == null ? "" : value.strip();
        if (safe.length() <= MAX_CONTEXT_VALUE) {
            return safe;
        }
        return safe.substring(0, MAX_CONTEXT_VALUE);
    }
}
