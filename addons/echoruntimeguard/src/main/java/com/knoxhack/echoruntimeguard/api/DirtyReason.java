package com.knoxhack.echoruntimeguard.api;

public enum DirtyReason {
    BLOCK_PLACED,
    BLOCK_BROKEN,
    NEIGHBOR_CHANGED,
    CONTROLLER_OPENED,
    CHUNK_LOADED,
    DATA_RELOADED,
    DEBUG
}
