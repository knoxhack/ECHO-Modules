package com.knoxhack.signalos.api;

@FunctionalInterface
public interface SignalOsAppActionResultHandler {
    SignalOsActionResult handle(SignalOsAppContext context, String payload);
}
