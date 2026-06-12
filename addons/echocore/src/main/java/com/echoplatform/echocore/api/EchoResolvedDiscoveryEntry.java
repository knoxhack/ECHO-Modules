package com.echoplatform.echocore.api;

public record EchoResolvedDiscoveryEntry(EchoDiscoveryEntry entry, EchoDiscoveryState state) {
    public EchoResolvedDiscoveryEntry {
        state = state == null ? EchoDiscoveryState.LOCKED : state;
    }
}
