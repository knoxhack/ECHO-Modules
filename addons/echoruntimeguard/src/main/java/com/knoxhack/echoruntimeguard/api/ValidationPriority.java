package com.knoxhack.echoruntimeguard.api;

public enum ValidationPriority {
    PLAYER_REQUEST(0),
    BLOCK_CHANGED(10),
    CHUNK_LOADED(20),
    STRUCTURE_DAMAGED(30),
    DEBUG(40),
    SCHEDULED_IDLE(100);

    private final int rank;

    ValidationPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
