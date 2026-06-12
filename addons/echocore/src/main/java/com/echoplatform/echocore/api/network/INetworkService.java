package com.echoplatform.echocore.api.network;

public interface INetworkService {
    default void send(String channel, byte[] payload) {
    }

    default INetworkBridge bridge() {
        return INetworkBridge.NOOP;
    }

    default IPacketRegistrar packetRegistrar() {
        return IPacketRegistrar.NOOP;
    }

    default PacketDebugHooks debugHooks() {
        return PacketDebugHooks.NOOP;
    }

    default boolean available() {
        return false;
    }
}
