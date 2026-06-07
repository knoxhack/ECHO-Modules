package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import net.minecraft.resources.Identifier;

public final class AutomationEffectHandlers {
    private static final List<AutomationEffectHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private AutomationEffectHandlers() {
    }

    public static boolean register(AutomationEffectHandler handler) {
        if (handler == null || handler.providerId() == null) {
            return false;
        }
        if (HANDLERS.stream().anyMatch(existing -> handler.providerId().equals(existing.providerId()))) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore automation effect provider {} ignored because that id is already registered.",
                    handler.providerId());
            return false;
        }
        HANDLERS.add(handler);
        return true;
    }

    public static List<AutomationEffectHandler> handlersFor(Identifier effectId) {
        if (effectId == null || HANDLERS.isEmpty()) {
            return List.of();
        }
        List<AutomationEffectHandler> matches = new ArrayList<>();
        for (AutomationEffectHandler handler : HANDLERS) {
            try {
                if (handler.handles(effectId)) {
                    matches.add(handler);
                }
            } catch (RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore automation effect provider {} failed handles({}); ignoring provider for this effect.",
                        handler.providerId(), effectId, exception);
            }
        }
        return List.copyOf(matches);
    }

    public static AutomationEffectResult beforeStart(AutomationEffectInvocation invocation) {
        return invoke(invocation, AutomationEffectHandler::beforeStart);
    }

    public static AutomationEffectResult onStart(AutomationEffectInvocation invocation) {
        return invoke(invocation, AutomationEffectHandler::onStart);
    }

    public static AutomationEffectResult onTick(AutomationEffectInvocation invocation) {
        return invoke(invocation, AutomationEffectHandler::onTick);
    }

    public static AutomationEffectResult onComplete(AutomationEffectInvocation invocation) {
        return invoke(invocation, AutomationEffectHandler::onComplete);
    }

    public static AutomationEffectResult onFail(AutomationEffectInvocation invocation) {
        return invoke(invocation, AutomationEffectHandler::onFail);
    }

    public static int providerCount() {
        return HANDLERS.size();
    }

    public static void withClearedForTests(Runnable runnable) {
        List<AutomationEffectHandler> previous = new ArrayList<>(HANDLERS);
        HANDLERS.clear();
        try {
            runnable.run();
        } finally {
            HANDLERS.clear();
            HANDLERS.addAll(previous);
        }
    }

    private static AutomationEffectResult invoke(AutomationEffectInvocation invocation,
            BiFunction<AutomationEffectHandler, AutomationEffectInvocation, AutomationEffectResult> phase) {
        if (invocation == null || invocation.effectId() == null) {
            return AutomationEffectResult.allow();
        }
        List<AutomationEffectHandler> handlers = handlersFor(invocation.effectId());
        if (handlers.isEmpty()) {
            return AutomationEffectResult.allow("No automation effect handler registered for " + invocation.effectId() + ".");
        }
        AutomationEffectResult latest = AutomationEffectResult.allow();
        for (AutomationEffectHandler handler : handlers) {
            try {
                AutomationEffectResult result = phase.apply(handler, invocation);
                result = result == null ? AutomationEffectResult.allow() : result;
                if (!result.reason().isBlank()) {
                    latest = result;
                }
                if (!result.allowed()) {
                    return result;
                }
            } catch (RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore automation effect provider {} failed during {} for {}.",
                        handler.providerId(), invocation.phase(), invocation.effectId(), exception);
                return AutomationEffectResult.fail("Automation effect " + invocation.effectId() + " failed during "
                        + invocation.phase() + ": " + exception.getMessage());
            }
        }
        return latest;
    }
}
