package com.knoxhack.echonetcore.service;

import com.knoxhack.echocore.api.network.INetworkBridge;
import com.knoxhack.echocore.api.network.INetworkService;
import com.knoxhack.echocore.api.network.IPacketRegistrar;
import com.knoxhack.echocore.api.network.PacketDebugHooks;
import com.knoxhack.echonetcore.network.EchoNetDebug;

public final class NetCoreNetworkService implements INetworkService {
    public static final NetCoreNetworkService INSTANCE = new NetCoreNetworkService();

    private NetCoreNetworkService() {
    }

    @Override
    public INetworkBridge bridge() {
        return NetCoreNetworkBridge.INSTANCE;
    }

    @Override
    public IPacketRegistrar packetRegistrar() {
        return NetCorePacketRegistrar.INSTANCE;
    }

    @Override
    public PacketDebugHooks debugHooks() {
        return EchoNetDebug.HOOKS;
    }
}
