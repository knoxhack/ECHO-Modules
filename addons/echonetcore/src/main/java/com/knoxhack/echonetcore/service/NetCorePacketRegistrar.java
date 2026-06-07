package com.knoxhack.echonetcore.service;

import com.knoxhack.echocore.api.network.IPacketRegistrar;
import com.knoxhack.echonetcore.api.EchoNetPayloads;

public final class NetCorePacketRegistrar implements IPacketRegistrar {
    public static final NetCorePacketRegistrar INSTANCE = new NetCorePacketRegistrar();

    private NetCorePacketRegistrar() {
    }

    @Override
    public String protocolVersion() {
        return EchoNetPayloads.VERSION;
    }

    @Override
    public boolean optionalPackets() {
        return true;
    }
}
