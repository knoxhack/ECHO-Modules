package com.knoxhack.signalos.api;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class TerminalActionRegistry {
    private static final Map<Key, TerminalActionResultHandler> HANDLERS = new ConcurrentHashMap<>();

    private TerminalActionRegistry() {
    }

    public static void register(Identifier pageId, Identifier actionId, TerminalActionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("SignalOS terminal action handler is required.");
        }
        registerResult(pageId, actionId, (player, payload) -> {
            handler.handle(player, payload);
            return SignalOsActionResult.success("");
        });
    }

    public static void registerResult(Identifier pageId, Identifier actionId, TerminalActionResultHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("SignalOS terminal action handler is required.");
        }
        HANDLERS.put(new Key(
                TerminalIds.requireLowercase(pageId, "SignalOS action page"),
                TerminalIds.requireLowercase(actionId, "SignalOS action")),
                handler);
    }

    public static void registerAppAction(Identifier appId, Identifier actionId, SignalOsAppActionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("SignalOS app action handler is required.");
        }
        registerAppActionResult(appId, actionId, (context, payload) -> {
            handler.handle(context, payload);
            return SignalOsActionResult.success("");
        });
    }

    public static void registerAppActionResult(Identifier appId, Identifier actionId, SignalOsAppActionResultHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("SignalOS app action handler is required.");
        }
        Identifier safeAppId = TerminalIds.requireLowercase(appId, "SignalOS app action page");
        registerResult(safeAppId, actionId, (player, payload) -> {
            SignalOsComputerNetworkService.NetworkSnapshot snapshot =
                    SignalOsComputerNetworkService.snapshot(player);
            return handler.handle(new SignalOsAppContext(player, safeAppId, snapshot.networkId(), snapshot.accessTier(),
                            snapshot.activeDrivePresent(), snapshot.activeDriveLabel(), snapshot.activeDriveVersion(),
                            snapshot.activeDriveWritable(), snapshot.activeDriveStatus()),
                    payload == null ? "" : payload);
        });
    }

    public static boolean handle(ServerPlayer player, Identifier pageId, Identifier actionId, String payload) {
        return handleResult(player, pageId, actionId, payload).handled();
    }

    public static SignalOsActionResult handleResult(ServerPlayer player, Identifier pageId, Identifier actionId,
            String payload) {
        TerminalActionResultHandler handler = HANDLERS.get(new Key(pageId, actionId));
        if (handler == null) {
            return SignalOsActionResult.unknown();
        }
        try {
            SignalOsActionResult result = handler.handle(player, payload == null ? "" : payload);
            return result == null ? SignalOsActionResult.success("") : result;
        } catch (RuntimeException exception) {
            SignalOS.LOGGER.warn("SignalOS action {}:{} failed.", pageId, actionId, exception);
            return SignalOsActionResult.failure(SignalOsDriveResultCode.ERROR,
                    "[SignalOS] Terminal action failed.");
        }
    }

    public static void clearForTests() {
        HANDLERS.clear();
    }

    public static void withClearedForTests(Runnable body) {
        Map<Key, TerminalActionResultHandler> snapshot = Map.copyOf(HANDLERS);
        HANDLERS.clear();
        try {
            body.run();
        } finally {
            HANDLERS.clear();
            HANDLERS.putAll(snapshot);
        }
    }

    private record Key(Identifier pageId, Identifier actionId) {
    }
}
