package com.knoxhack.echoarcanacore.api;

public interface AetherStorageTarget {
    AetherStorage aetherStorage();

    default boolean setAetherStorage(AetherStorage storage) {
        return false;
    }
}
