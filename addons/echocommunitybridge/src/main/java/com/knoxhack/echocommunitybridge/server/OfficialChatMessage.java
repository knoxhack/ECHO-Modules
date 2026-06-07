package com.knoxhack.echocommunitybridge.server;

import com.google.gson.JsonObject;

public record OfficialChatMessage(
        String id,
        String channelId,
        String source,
        String authorId,
        String authorName,
        String body,
        String createdAt,
        String nonce) {
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("channelId", channelId);
        root.addProperty("authorUserId", authorId);
        root.add("author", authorJson());
        root.addProperty("body", body);
        root.addProperty("createdAt", createdAt);
        root.addProperty("hidden", false);
        root.addProperty("pinned", false);
        if (nonce != null && !nonce.isBlank()) {
            root.addProperty("nonce", nonce);
        }
        root.addProperty("source", source);
        return root;
    }

    private JsonObject authorJson() {
        JsonObject author = new JsonObject();
        author.addProperty("id", authorId);
        author.addProperty("displayName", authorName);
        author.addProperty("role", "member");
        author.addProperty("source", source);
        return author;
    }
}
