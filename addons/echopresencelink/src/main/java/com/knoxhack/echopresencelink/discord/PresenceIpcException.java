package com.knoxhack.echopresencelink.discord;

import java.io.IOException;

public final class PresenceIpcException extends IOException {
    private final String code;
    private final String discordResponse;
    private final boolean discordError;

    public PresenceIpcException(String code, String message, String discordResponse, boolean discordError) {
        super(message);
        this.code = code == null || code.isBlank() ? "DISCORD_IPC_ERROR" : code;
        this.discordResponse = discordResponse == null ? "" : discordResponse;
        this.discordError = discordError;
    }

    public String code() {
        return code;
    }

    public String discordResponse() {
        return discordResponse;
    }

    public boolean discordError() {
        return discordError;
    }
}
