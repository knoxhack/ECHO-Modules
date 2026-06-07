package com.knoxhack.echo.adaptercore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * AdapterCore backend bridge for mod lifecycle and event-bus wiring.
 */
public final class EchoBackendLifecycleBridge {
    private EchoBackendLifecycleBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerModListener(Object eventBus, Consumer<?> listener) {
        if (eventBus instanceof IEventBus bus) {
            bus.addListener((Consumer) listener);
        }
    }

    public static void registerGameEventListener(Object listener) {
        if (listener != null) {
            NeoForge.EVENT_BUS.register(listener);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerGameEventHandler(Consumer<?> listener) {
        if (listener != null) {
            NeoForge.EVENT_BUS.addListener((Consumer) listener);
        }
    }

    public static void postGameEvent(Object event) {
        if (event instanceof Event liveEvent) {
            NeoForge.EVENT_BUS.post(liveEvent);
        }
    }

    public static void runCommonSetupWork(Object event, Runnable work) {
        if (work == null) {
            return;
        }
        if (event instanceof FMLCommonSetupEvent setupEvent) {
            setupEvent.enqueueWork(work);
        } else {
            work.run();
        }
    }
}
