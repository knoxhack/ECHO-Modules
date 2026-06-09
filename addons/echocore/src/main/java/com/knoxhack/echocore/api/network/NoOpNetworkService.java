package com.knoxhack.echocore.api.network;

public final class NoOpNetworkService implements INetworkService {
    @Override
    public void send(String channel, byte[] payload) {
    }
}
