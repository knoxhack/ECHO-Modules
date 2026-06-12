package com.echoplatform.echocore.api.network;

@FunctionalInterface
public interface PacketDebugHook {
    void onPacket(EchoPacketDebugEvent event);
}
