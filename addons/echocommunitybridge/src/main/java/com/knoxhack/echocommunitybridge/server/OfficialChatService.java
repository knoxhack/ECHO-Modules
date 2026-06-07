package com.knoxhack.echocommunitybridge.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.discord.DiscordMessageQueue;
import com.knoxhack.echocommunitybridge.launcher.LauncherChatBridgeClient;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OfficialChatService {
    public static final OfficialChatService INSTANCE = new OfficialChatService();
    private static final Gson GSON = new Gson();
    private static final int MAX_BODY = 1800;
    private static final int MAX_NICKNAME = 32;
    private static final int MAX_CLIENT_ID = 96;

    private final Deque<OfficialChatMessage> messages = new ArrayDeque<>();
    private final Map<String, OfficialChatMessage> messagesByNonce = new LinkedHashMap<>();
    private final Set<ChatWebSocketSession> sockets = ConcurrentHashMap.newKeySet();

    private OfficialChatService() {
    }

    public synchronized JsonObject bootstrap(String clientId, String nickname) {
        JsonObject root = new JsonObject();
        root.add("groups", groupsJson());
        root.add("channels", channelsJson());
        root.add("members", membersJson());

        JsonObject self = new JsonObject();
        self.addProperty("clientId", cleanIdentity(clientId, "anonymous-preview"));
        self.addProperty("nickname", cleanNickname(nickname, ""));
        self.addProperty("role", "member");
        root.add("self", self);

        JsonObject allMessages = new JsonObject();
        allMessages.add(channelId(), messagesJson(limit()));
        root.add("messages", allMessages);

        JsonObject hasMore = new JsonObject();
        hasMore.addProperty(channelId(), false);
        root.add("hasMore", hasMore);

        JsonArray bridge = new JsonArray();
        JsonObject bridgeItem = new JsonObject();
        bridgeItem.addProperty("serverId", CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_ID));
        bridgeItem.addProperty("channelId", channelId());
        bridgeItem.addProperty("label", CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_NAME));
        bridgeItem.addProperty("connected", CommunityBridgeConfig.launcherChatReady());
        bridge.add(bridgeItem);
        root.add("bridge", bridge);

        JsonObject moderation = new JsonObject();
        moderation.addProperty("slowModeSeconds", 0);
        JsonArray rules = new JsonArray();
        rules.add("Keep it helpful.");
        rules.add("No harassment or hate speech.");
        rules.add("Do not paste tokens, secrets, or private logs.");
        moderation.add("rules", rules);
        root.add("moderation", moderation);
        return root;
    }

    public synchronized JsonObject listMessages(int requestedLimit) {
        JsonObject root = new JsonObject();
        root.add("messages", messagesJson(Math.max(1, Math.min(limit(), requestedLimit))));
        root.addProperty("hasMore", false);
        return root;
    }

    public OfficialChatMessage acceptPublicMessage(String source, String clientId, String nickname, String body, String nonce) {
        String normalizedSource = normalizePublicSource(source);
        String cleanBody = ServerStatusService.sanitizePublicText(body, MAX_BODY);
        if (cleanBody.isBlank()) {
            throw new ChatRequestException(400, "Message is empty.");
        }
        if ("launcher".equals(normalizedSource) && !CommunityBridgeConfig.LAUNCHER_CHAT_ALLOW_LAUNCHER.get()) {
            throw new ChatRequestException(403, "Launcher chat is disabled.");
        }
        if ("android".equals(normalizedSource) && !CommunityBridgeConfig.LAUNCHER_CHAT_ALLOW_ANDROID.get()) {
            throw new ChatRequestException(403, "Android chat is disabled.");
        }
        String cleanClientId = cleanIdentity(clientId, normalizedSource + ":" + UUID.randomUUID());
        String cleanNickname = cleanNickname(nickname, sourceLabel(normalizedSource));
        String cleanNonce = ServerStatusService.sanitizePublicText(nonce, 160);
        OfficialChatMessage duplicate = findByNonce(cleanNonce);
        if (duplicate != null) {
            return duplicate;
        }
        OfficialChatMessage message = storeMessage(new OfficialChatMessage(
                "message_" + UUID.randomUUID(),
                channelId(),
                normalizedSource,
                cleanClientId,
                cleanNickname,
                cleanBody,
                Instant.now().toString(),
                cleanNonce));
        broadcastCreated(message);
        if (!cleanBody.startsWith("/")) {
            LauncherChatBridgeClient.launcherChatLine(normalizedSource, cleanNickname, cleanBody)
                    .ifPresent(ServerStatusService.INSTANCE::broadcastLauncherChatLine);
            DiscordMessageQueue.INSTANCE.enqueueBridgeChat(sourceLabel(normalizedSource), cleanNickname, cleanBody);
        }
        return message;
    }

    public OfficialChatMessage recordMinecraftChat(String player, String body) {
        OfficialChatMessage message = storeMessage(new OfficialChatMessage(
                "message_" + UUID.randomUUID(),
                channelId(),
                "minecraft",
                "minecraft:" + cleanIdentity(player, "player").toLowerCase(Locale.ROOT),
                ServerStatusService.sanitizePlayerName(player),
                ServerStatusService.sanitizePublicText(body, MAX_BODY),
                Instant.now().toString(),
                "minecraft:" + UUID.randomUUID()));
        broadcastCreated(message);
        return message;
    }

    public OfficialChatMessage recordDiscordChat(String authorId, String authorName, String body) {
        return recordDiscordChat(authorId, authorId, authorName, body);
    }

    public OfficialChatMessage recordDiscordChat(String sourceId, String authorId, String authorName, String body) {
        String cleanBody = ServerStatusService.sanitizePublicText(body, MAX_BODY);
        if (cleanBody.isBlank()) {
            throw new ChatRequestException(400, "Message is empty.");
        }
        String cleanSourceId = cleanIdentity(sourceId, UUID.randomUUID().toString());
        OfficialChatMessage message = storeMessage(new OfficialChatMessage(
                "message_" + UUID.randomUUID(),
                channelId(),
                "discord",
                "discord:" + cleanIdentity(authorId, authorName),
                cleanNickname(authorName, "Discord User"),
                cleanBody,
                Instant.now().toString(),
                "discord:" + cleanSourceId));
        broadcastCreated(message);
        return message;
    }

    public OfficialChatMessage recordSystem(String type, String body) {
        OfficialChatMessage message = storeMessage(new OfficialChatMessage(
                "message_" + UUID.randomUUID(),
                channelId(),
                "system",
                "system:" + CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_ID),
                "ECHO Server",
                ServerStatusService.sanitizePublicText(body, MAX_BODY),
                Instant.now().toString(),
                "system:" + ServerStatusService.sanitizePublicText(type, 32) + ":" + UUID.randomUUID()));
        broadcastCreated(message);
        return message;
    }

    public void addSocket(ChatWebSocketSession session) {
        sockets.add(session);
    }

    public void removeSocket(ChatWebSocketSession session) {
        sockets.remove(session);
    }

    public int socketCount() {
        sockets.removeIf((socket) -> !socket.isOpen());
        return sockets.size();
    }

    public synchronized int historyCount() {
        return messages.size();
    }

    public synchronized void clearForTests() {
        messages.clear();
        messagesByNonce.clear();
    }

    public static String channelId() {
        String channelId = CommunityBridgeConfig.string(CommunityBridgeConfig.LAUNCHER_CHAT_CHANNEL_ID);
        return channelId.isBlank() ? "server-ashfall" : channelId;
    }

    public static String normalizePublicSource(String source) {
        String safe = source == null ? "" : source.strip().toLowerCase(Locale.ROOT);
        if ("launcher".equals(safe) || "android".equals(safe)) {
            return safe;
        }
        throw new ChatRequestException(400, "Unsupported chat source.");
    }

    public static String cleanNickname(String nickname, String fallback) {
        String safe = ServerStatusService.sanitizePublicText(nickname, MAX_NICKNAME);
        return safe.isBlank() ? fallback : safe;
    }

    private synchronized OfficialChatMessage storeMessage(OfficialChatMessage message) {
        if (message.body().isBlank()) {
            return message;
        }
        if (message.nonce() != null && !message.nonce().isBlank()) {
            OfficialChatMessage duplicate = messagesByNonce.get(nonceKey(message.nonce()));
            if (duplicate != null) {
                return duplicate;
            }
            messagesByNonce.put(nonceKey(message.nonce()), message);
        }
        messages.addLast(message);
        trimHistory();
        return message;
    }

    private synchronized OfficialChatMessage findByNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return null;
        }
        return messagesByNonce.get(nonceKey(nonce));
    }

    private void broadcastCreated(OfficialChatMessage message) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", "message.created");
        envelope.add("payload", message.toJson());
        envelope.addProperty("createdAt", Instant.now().toString());
        String payload = GSON.toJson(envelope);
        List<ChatWebSocketSession> dead = new ArrayList<>();
        for (ChatWebSocketSession socket : sockets) {
            if (!socket.sendText(payload)) {
                dead.add(socket);
            }
        }
        sockets.removeAll(dead);
    }

    private JsonArray groupsJson() {
        JsonArray groups = new JsonArray();
        JsonObject servers = new JsonObject();
        servers.addProperty("id", "servers");
        servers.addProperty("label", "Official Servers");
        JsonArray channelIds = new JsonArray();
        channelIds.add(channelId());
        servers.add("channelIds", channelIds);
        groups.add(servers);
        return groups;
    }

    private JsonArray channelsJson() {
        JsonArray channels = new JsonArray();
        JsonObject channel = new JsonObject();
        channel.addProperty("id", channelId());
        channel.addProperty("groupId", "servers");
        channel.addProperty("groupLabel", "Official Servers");
        channel.addProperty("name", "ashfall-official");
        channel.addProperty("description", "Official all-way chat for launcher, Android, Minecraft, and Discord.");
        channel.addProperty("kind", "minecraft_server");
        channel.addProperty("readOnly", !CommunityBridgeConfig.launcherChatReady());
        channel.addProperty("slowModeSeconds", 0);
        channel.addProperty("unreadCount", 0);
        channel.addProperty("onlineCount", ServerStatusService.INSTANCE.playerCount());
        channel.addProperty("serverId", CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_ID));
        channel.addProperty("position", 70);
        channels.add(channel);
        return channels;
    }

    private JsonArray membersJson() {
        JsonArray members = new JsonArray();
        for (String player : ServerStatusService.INSTANCE.players()) {
            JsonObject member = new JsonObject();
            member.addProperty("id", "minecraft:" + player.toLowerCase(Locale.ROOT));
            member.addProperty("displayName", player);
            member.addProperty("role", "member");
            member.addProperty("status", "online");
            member.addProperty("source", "minecraft");
            member.addProperty("channelId", channelId());
            members.add(member);
        }
        return members;
    }

    private synchronized JsonArray messagesJson(int requestedLimit) {
        JsonArray array = new JsonArray();
        int skip = Math.max(0, messages.size() - requestedLimit);
        int index = 0;
        for (OfficialChatMessage message : messages) {
            if (index++ < skip) {
                continue;
            }
            array.add(message.toJson());
        }
        return array;
    }

    private void trimHistory() {
        int limit = limit();
        while (messages.size() > limit) {
            OfficialChatMessage removed = messages.removeFirst();
            if (removed.nonce() != null && !removed.nonce().isBlank()) {
                messagesByNonce.remove(nonceKey(removed.nonce()));
            }
        }
    }

    private static int limit() {
        return Math.max(1, CommunityBridgeConfig.LAUNCHER_CHAT_HISTORY_LIMIT.get());
    }

    private static String cleanIdentity(String value, String fallback) {
        String safe = ServerStatusService.sanitizePublicText(value, MAX_CLIENT_ID)
                .replaceAll("[^A-Za-z0-9_.:-]", "");
        return safe.isBlank() ? fallback : safe;
    }

    private static String nonceKey(String nonce) {
        return channelId() + ":" + nonce;
    }

    private static String sourceLabel(String source) {
        return "android".equals(source) ? "Android" : "Launcher";
    }

    public static final class ChatRequestException extends RuntimeException {
        private final int statusCode;

        public ChatRequestException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
