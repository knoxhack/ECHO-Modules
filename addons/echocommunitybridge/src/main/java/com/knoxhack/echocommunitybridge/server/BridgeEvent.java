package com.knoxhack.echocommunitybridge.server;

import com.google.gson.JsonObject;
import java.time.Instant;

public record BridgeEvent(String type, String player, String message, Instant createdAt) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        if (!player.isBlank()) {
            json.addProperty("player", player);
        }
        if (!message.isBlank()) {
            json.addProperty("message", message);
        }
        json.addProperty("createdAt", createdAt.toString());
        return json;
    }
}
