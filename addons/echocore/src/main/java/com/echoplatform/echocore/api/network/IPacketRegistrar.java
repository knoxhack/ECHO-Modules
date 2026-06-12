package com.echoplatform.echocore.api.network;

public interface IPacketRegistrar {
    IPacketRegistrar NOOP = new IPacketRegistrar() {
        @Override
        public String protocolVersion() {
            return "noop";
        }

        @Override
        public boolean optionalPackets() {
            return false;
        }
    };

    String protocolVersion();

    boolean optionalPackets();
}
