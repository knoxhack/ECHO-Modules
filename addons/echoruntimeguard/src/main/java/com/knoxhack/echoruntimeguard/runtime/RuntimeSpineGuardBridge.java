package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echocore.api.EchoRuntimeSpineBus;
import com.knoxhack.echocore.api.EchoRuntimeSpineEvent;
import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import net.minecraft.resources.Identifier;

/**
 * Lets RuntimeGuard budget shared runtime spine fanout without Core depending on RuntimeGuard.
 */
public final class RuntimeSpineGuardBridge {
    private static boolean registered;
    private static int acceptedEvents;
    private static int droppedEvents;

    private RuntimeSpineGuardBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoRuntimeSpineBus.registerGate(RuntimeSpineGuardBridge::allowRuntimeSpineEvent);
    }

    public static synchronized void registerForTests() {
        registered = true;
        reset();
        EchoRuntimeSpineBus.registerGate(RuntimeSpineGuardBridge::allowRuntimeSpineEvent);
    }

    public static boolean allowRuntimeSpineEvent(EchoRuntimeSpineEvent event) {
        Identifier workId = event == null || event.eventId() == null
                ? EchoRuntimeGuard.id("runtime_spine/event")
                : event.eventId();
        boolean allowed = IntegrationThrottleService.INSTANCE.tryAcquireWork(
                workId,
                RuntimeWorkType.NETWORK_SYNC,
                event == null ? 1 : event.amount());
        if (allowed) {
            acceptedEvents++;
            IntegrationThrottleService.INSTANCE.recordWork(workId, RuntimeWorkType.NETWORK_SYNC, 0L);
        } else {
            droppedEvents++;
        }
        return allowed;
    }

    public static int acceptedEvents() {
        return acceptedEvents;
    }

    public static int droppedEvents() {
        return droppedEvents;
    }

    public static void reset() {
        acceptedEvents = 0;
        droppedEvents = 0;
    }
}
