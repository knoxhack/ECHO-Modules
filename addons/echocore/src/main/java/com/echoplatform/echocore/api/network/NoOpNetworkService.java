package com.echoplatform.echocore.api.network;

public final class NoOpNetworkService implements INetworkService {
    public static final NoOpNetworkService INSTANCE = new NoOpNetworkService();

    @Override
    public void send(String channel, byte[] payload) {
    }
}
