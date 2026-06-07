package com.knoxhack.echoterminal.api;

import com.knoxhack.echoterminal.EchoTerminal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class TerminalActionRegistry {
    private static final int DEFAULT_THROTTLE_TICKS = 4;
    private static final Map<Key, Entry> HANDLERS = new ConcurrentHashMap<>();
    private static final Map<ThrottleKey, Long> LAST_ACTION_TICKS = new ConcurrentHashMap<>();

    private TerminalActionRegistry() {
    }

    public static void register(Identifier tabId, Identifier actionId, TerminalActionHandler handler) {
        register(tabId, actionId, handler, TerminalActionValidator.ALLOW);
    }

    public static void register(
            Identifier tabId,
            Identifier actionId,
            TerminalActionHandler handler,
            TerminalActionValidator validator) {
        if (handler == null) {
            throw new IllegalArgumentException("Terminal action handler is required.");
        }
        TerminalActionValidator safeValidator = validator == null ? TerminalActionValidator.ALLOW : validator;
        HANDLERS.put(new Key(
                TerminalApiIds.requireLowercase(tabId, "Terminal action tab"),
                TerminalApiIds.requireLowercase(actionId, "Terminal action")),
                new Entry(handler, safeValidator));
    }

    public static boolean handle(ServerPlayer player, Identifier tabId, Identifier actionId, String payload) {
        if (tabId == null || actionId == null) {
            return false;
        }
        Optional<Entry> entry = Optional.ofNullable(HANDLERS.get(new Key(tabId, actionId)));
        if (entry.isEmpty()) {
            return false;
        }
        TerminalActionContext context = new TerminalActionContext(player, tabId, actionId, payload);
        if (isThrottled(context)) {
            return true;
        }
        try {
            if (!entry.get().validator().validate(context)) {
                return true;
            }
            markHandled(context);
            entry.get().handler().handle(player, context.payload());
            return true;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal action {}:{} failed; ignoring action.",
                    tabId, actionId, exception);
            return false;
        }
    }

    public static void clearForTests() {
        HANDLERS.clear();
        LAST_ACTION_TICKS.clear();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<Key, Entry> snapshot = Map.copyOf(HANDLERS);
        Map<ThrottleKey, Long> throttleSnapshot = Map.copyOf(LAST_ACTION_TICKS);
        HANDLERS.clear();
        LAST_ACTION_TICKS.clear();
        try {
            runnable.run();
        } finally {
            HANDLERS.clear();
            HANDLERS.putAll(snapshot);
            LAST_ACTION_TICKS.clear();
            LAST_ACTION_TICKS.putAll(throttleSnapshot);
        }
    }

    private static boolean isThrottled(TerminalActionContext context) {
        ServerPlayer player = context.player();
        if (player == null) {
            return false;
        }
        long now = player.level().getGameTime();
        Long last = LAST_ACTION_TICKS.get(new ThrottleKey(player.getUUID(), context.tabId(), context.actionId()));
        return last != null && now - last < DEFAULT_THROTTLE_TICKS;
    }

    private static void markHandled(TerminalActionContext context) {
        ServerPlayer player = context.player();
        if (player == null) {
            return;
        }
        LAST_ACTION_TICKS.put(new ThrottleKey(player.getUUID(), context.tabId(), context.actionId()),
                player.level().getGameTime());
    }

    private record Key(Identifier tabId, Identifier actionId) {
    }

    private record ThrottleKey(UUID playerId, Identifier tabId, Identifier actionId) {
    }

    private record Entry(TerminalActionHandler handler, TerminalActionValidator validator) {
    }
}
