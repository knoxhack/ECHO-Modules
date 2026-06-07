package com.knoxhack.echoscreencore.api;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;

public final class EchoScreens {
    private static volatile ScreenOpener screenOpener = (pageId, context) -> false;
    private static final CopyOnWriteArrayList<InvalidationHandler> INVALIDATION_HANDLERS = new CopyOnWriteArrayList<>();

    private EchoScreens() {
    }

    public static boolean open(Identifier pageId, EchoDataContext context) {
        Objects.requireNonNull(pageId, "pageId");
        return screenOpener.open(pageId, context == null ? EchoDataContext.empty() : context);
    }

    public static boolean open(String pageId, EchoDataContext context) {
        return open(Identifier.parse(pageId), context);
    }

    public static void registerClientOpener(ScreenOpener opener) {
        screenOpener = Objects.requireNonNull(opener, "opener");
    }

    public static void registerInvalidationHandler(InvalidationHandler handler) {
        if (handler != null && !INVALIDATION_HANDLERS.contains(handler)) {
            INVALIDATION_HANDLERS.add(handler);
        }
    }

    public static void invalidateData() {
        for (InvalidationHandler handler : INVALIDATION_HANDLERS) {
            handler.invalidateData(null);
        }
    }

    public static void invalidatePage(Identifier pageId) {
        for (InvalidationHandler handler : INVALIDATION_HANDLERS) {
            handler.invalidateData(pageId);
        }
    }

    @FunctionalInterface
    public interface ScreenOpener {
        boolean open(Identifier pageId, EchoDataContext context);
    }

    @FunctionalInterface
    public interface InvalidationHandler {
        void invalidateData(Identifier pageId);
    }
}
