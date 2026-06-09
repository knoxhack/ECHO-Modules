package com.knoxhack.echocore.api.network;

public interface INetworkService {
    void send(String channel, byte[] payload);

    default boolean available() {
        return false;
    }
}
