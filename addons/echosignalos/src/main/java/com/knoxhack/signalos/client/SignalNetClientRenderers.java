package com.knoxhack.signalos.client;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.client.api.SignalOsAppRenderers;
import com.knoxhack.signalos.platform.SignalOsModuleAccess;

public final class SignalNetClientRenderers {
    private SignalNetClientRenderers() {
    }

    public static void register() {
        if (SignalOsModuleAccess.isLoaded("echoscreencore")) {
            try {
                SignalOsAppRenderers.register("signalnet",
                        (com.knoxhack.signalos.client.api.SignalOsAppRenderer) Class.forName(
                                "com.knoxhack.signalos.client.SignalNetScreenCoreRenderer")
                                .getConstructor()
                                .newInstance());
                return;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                SignalOS.LOGGER.warn("SignalOS SignalNet ScreenCore renderer unavailable; using native renderer.",
                        exception);
            }
        }
        SignalOsAppRenderers.register("signalnet", new SignalNetNativeRenderer());
    }
}
