package com.knoxhack.signalos.api;

@FunctionalInterface
public interface SignalOsAppActionHandler {
    void handle(SignalOsAppContext context, String payload);
}
