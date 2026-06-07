package com.knoxhack.echocommunitybridge.discord;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.server.OfficialChatService;
import com.knoxhack.echocommunitybridge.server.ServerStatusService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordGatewayClient {
    public static final DiscordGatewayClient INSTANCE = new DiscordGatewayClient();
    private static final Gson GSON = new Gson();
    private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    private static final int GUILD_MESSAGES_INTENT = 512;
    private static final int MESSAGE_CONTENT_INTENT = 32768;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final Object heartbeatLock = new Object();
    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile Integer lastSequence;
    private volatile long heartbeatIntervalMs = 45_000L;
    private Thread heartbeatThread;

    private DiscordGatewayClient() {
    }

    public synchronized void start() {
        if (!CommunityBridgeConfig.discordGatewayReady() || running.get()) {
            return;
        }
        running.set(true);
        connect();
    }

    public synchronized void restart() {
        shutdown();
        start();
    }

    public synchronized void shutdown() {
        running.set(false);
        reconnecting.set(false);
        connected = false;
        stopHeartbeat();
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            socket.abort();
        }
    }

    public boolean connected() {
        return connected;
    }

    public String statusLabel() {
        if (!CommunityBridgeConfig.discordGatewayReady()) {
            return "disabled";
        }
        if (connected) {
            return "connected";
        }
        return running.get() ? "connecting" : "stopped";
    }

    public static Optional<String> discordChatLine(String authorName, String body) {
        String safeBody = ServerStatusService.sanitizePublicText(body, 500);
        if (safeBody.isBlank()) {
            return Optional.empty();
        }
        String safeAuthor = ServerStatusService.sanitizePublicText(authorName, 32);
        if (safeAuthor.isBlank()) {
            safeAuthor = "Discord";
        }
        return Optional.of("[Discord] <" + safeAuthor + ">: " + safeBody);
    }

    public static Optional<InboundDiscordMessage> parseMessageCreate(JsonObject payload, String chatChannelId) {
        if (payload == null || chatChannelId == null || chatChannelId.isBlank()) {
            return Optional.empty();
        }
        if (!chatChannelId.equals(readString(payload, "channel_id"))) {
            return Optional.empty();
        }
        if (payload.has("webhook_id") && !payload.get("webhook_id").isJsonNull()) {
            return Optional.empty();
        }
        JsonObject author = payload.has("author") && payload.get("author").isJsonObject()
                ? payload.getAsJsonObject("author")
                : null;
        if (author == null || readBoolean(author, "bot")) {
            return Optional.empty();
        }
        String body = ServerStatusService.sanitizePublicText(readString(payload, "content"), 1800);
        if (body.isBlank()) {
            return Optional.empty();
        }
        String authorName = discordAuthorName(payload, author);
        String authorId = safeIdentity(readString(author, "id"));
        String messageId = safeIdentity(readString(payload, "id"));
        return Optional.of(new InboundDiscordMessage(
                messageId.isBlank() ? authorId + ":" + System.nanoTime() : messageId,
                authorId.isBlank() ? authorName : authorId,
                authorName.isBlank() ? "Discord User" : authorName,
                body));
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(GATEWAY_URL), new GatewayListener())
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        EchoCommunityBridge.LOGGER.warn("ECHO Discord gateway failed to connect.");
                        scheduleReconnect();
                        return;
                    }
                    webSocket = socket;
                    socket.request(1);
                });
    }

    private void handleGatewayMessage(String text) {
        try {
            JsonObject envelope = JsonParser.parseString(text).getAsJsonObject();
            JsonElement sequence = envelope.get("s");
            if (sequence != null && !sequence.isJsonNull()) {
                lastSequence = sequence.getAsInt();
            }
            int op = envelope.has("op") ? envelope.get("op").getAsInt() : -1;
            if (op == 10) {
                JsonObject data = envelope.getAsJsonObject("d");
                heartbeatIntervalMs = data != null && data.has("heartbeat_interval")
                        ? Math.max(1_000L, data.get("heartbeat_interval").getAsLong())
                        : heartbeatIntervalMs;
                startHeartbeat();
                identify();
                return;
            }
            if (op == 0 && "MESSAGE_CREATE".equals(readString(envelope, "t"))) {
                JsonObject data = envelope.getAsJsonObject("d");
                handleMessageCreate(data);
                return;
            }
            if (op == 7 || op == 9) {
                scheduleReconnect();
            }
        } catch (RuntimeException ex) {
            EchoCommunityBridge.LOGGER.warn("ECHO Discord gateway ignored malformed payload.");
        }
    }

    private void handleMessageCreate(JsonObject payload) {
        parseMessageCreate(payload, CommunityBridgeConfig.string(CommunityBridgeConfig.DISCORD_CHAT_CHANNEL_ID))
                .ifPresent((message) -> {
                    discordChatLine(message.authorName(), message.body())
                            .ifPresent(ServerStatusService.INSTANCE::broadcastLauncherChatLine);
                    ServerStatusService.INSTANCE.addEvent("discord_chat", message.authorName(), message.body());
                    OfficialChatService.INSTANCE.recordDiscordChat(
                            message.sourceId(),
                            message.authorId(),
                            message.authorName(),
                            message.body());
                });
    }

    private void identify() {
        JsonObject properties = new JsonObject();
        properties.addProperty("os", System.getProperty("os.name", "unknown"));
        properties.addProperty("browser", "echocommunitybridge");
        properties.addProperty("device", "echocommunitybridge");

        JsonObject data = new JsonObject();
        data.addProperty("token", CommunityBridgeConfig.discordBotToken());
        data.addProperty("intents", GUILD_MESSAGES_INTENT | MESSAGE_CONTENT_INTENT);
        data.add("properties", properties);

        JsonObject payload = new JsonObject();
        payload.addProperty("op", 2);
        payload.add("d", data);
        send(payload);
        connected = true;
    }

    private void startHeartbeat() {
        synchronized (heartbeatLock) {
            stopHeartbeat();
            heartbeatThread = new Thread(() -> {
                while (running.get()) {
                    try {
                        Thread.sleep(heartbeatIntervalMs);
                        heartbeat();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "ECHO Discord Gateway Heartbeat");
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
        }
    }

    private void stopHeartbeat() {
        synchronized (heartbeatLock) {
            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
                heartbeatThread = null;
            }
        }
    }

    private void heartbeat() {
        JsonObject payload = new JsonObject();
        payload.addProperty("op", 1);
        Integer sequence = lastSequence;
        if (sequence == null) {
            payload.add("d", JsonNull.INSTANCE);
        } else {
            payload.addProperty("d", sequence);
        }
        send(payload);
    }

    private void send(JsonObject payload) {
        WebSocket socket = webSocket;
        if (socket != null && !socket.isOutputClosed()) {
            socket.sendText(GSON.toJson(payload), true);
        }
    }

    private void scheduleReconnect() {
        connected = false;
        stopHeartbeat();
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            socket.abort();
        }
        if (!running.get() || !reconnecting.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                reconnecting.set(false);
            }
            connect();
        }, "ECHO Discord Gateway Reconnect");
        thread.setDaemon(true);
        thread.start();
    }

    private static String discordAuthorName(JsonObject payload, JsonObject author) {
        JsonObject member = payload.has("member") && payload.get("member").isJsonObject()
                ? payload.getAsJsonObject("member")
                : null;
        String nickname = member == null ? "" : readString(member, "nick");
        if (!nickname.isBlank()) {
            return ServerStatusService.sanitizePublicText(nickname, 32);
        }
        String globalName = readString(author, "global_name");
        if (!globalName.isBlank()) {
            return ServerStatusService.sanitizePublicText(globalName, 32);
        }
        return ServerStatusService.sanitizePublicText(readString(author, "username"), 32);
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static boolean readBoolean(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    private static String safeIdentity(String value) {
        return ServerStatusService.sanitizePublicText(value, 96).replaceAll("[^A-Za-z0-9_.:-]", "");
    }

    public record InboundDiscordMessage(String sourceId, String authorId, String authorName, String body) {
    }

    private final class GatewayListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleGatewayMessage(buffer.toString());
                buffer.setLength(0);
            }
            socket.request(1);
            return WebSocket.Listener.super.onText(socket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            connected = false;
            webSocket = null;
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(socket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            connected = false;
            webSocket = null;
            EchoCommunityBridge.LOGGER.warn("ECHO Discord gateway socket error.");
            scheduleReconnect();
            WebSocket.Listener.super.onError(socket, error);
        }
    }
}
