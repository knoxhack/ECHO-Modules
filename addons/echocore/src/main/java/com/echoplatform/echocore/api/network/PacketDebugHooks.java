package com.echoplatform.echocore.api.network;

public interface PacketDebugHooks {
    PacketDebugHooks NOOP = new PacketDebugHooks() {
    };

    default void add(PacketDebugHook hook) {
    }

    default void remove(PacketDebugHook hook) {
    }

    default void emit(EchoPacketDebugEvent event) {
    }
}
