package com.knoxhack.echocommunitybridge.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StatusHttpServer {
    public static final StatusHttpServer INSTANCE = new StatusHttpServer();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private HttpServer server;
    private ExecutorService executor;

    private StatusHttpServer() {
    }

    public synchronized void start() {
        if (!CommunityBridgeConfig.enabled() || !CommunityBridgeConfig.PUBLIC_STATUS_ENABLED.get()) {
            stop();
            return;
        }
        if (server != null) {
            return;
        }
        String host = CommunityBridgeConfig.string(CommunityBridgeConfig.PUBLIC_STATUS_HOST);
        int port = CommunityBridgeConfig.PUBLIC_STATUS_PORT.get();
        String path = CommunityBridgeConfig.statusPath();
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext(path, this::handleStatus);
            server.createContext("/health", this::handleHealth);
            server.createContext("/v1/community/bootstrap", this::handleChatBootstrap);
            server.createContext("/v1/channels", this::handleChatMessages);
            server.createContext("/v1/chat/socket", this::handleChatSocket);
            executor = Executors.newCachedThreadPool(task -> {
                Thread thread = new Thread(task, "ECHO Community Bridge HTTP");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();
            EchoCommunityBridge.LOGGER.info("ECHO Community Bridge public status serving on {}:{}{}", host, port, path);
        } catch (IOException | IllegalArgumentException ex) {
            EchoCommunityBridge.LOGGER.warn("Could not start ECHO Community Bridge public status HTTP server on {}:{}.",
                    host, port, ex);
            stop();
        }
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized boolean running() {
        return server != null;
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        applyCors(exchange.getResponseHeaders());
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "");
            return;
        }
        if (!"GET".equals(method)) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = GSON.toJson(ServerStatusService.INSTANCE.snapshotJson());
        sendJson(exchange, 200, body);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        applyCors(exchange.getResponseHeaders());
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "");
            return;
        }
        if (!"GET".equals(method)) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleChatBootstrap(HttpExchange exchange) throws IOException {
        applyCors(exchange.getResponseHeaders());
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "");
            return;
        }
        if (!CommunityBridgeConfig.launcherChatReady()) {
            sendError(exchange, 503, "Official chat is disabled.");
            return;
        }
        if (!"GET".equals(method)) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        try {
            OfficialChatService.normalizePublicSource(header(exchange, "X-ECHO-Chat-Source", "launcher"));
            JsonObject bootstrap = OfficialChatService.INSTANCE.bootstrap(
                    header(exchange, "X-ECHO-Chat-Client", "anonymous-preview"),
                    header(exchange, "X-ECHO-Chat-Nickname", ""));
            sendJson(exchange, 200, GSON.toJson(bootstrap));
        } catch (OfficialChatService.ChatRequestException ex) {
            sendError(exchange, ex.statusCode(), ex.getMessage());
        }
    }

    private void handleChatMessages(HttpExchange exchange) throws IOException {
        applyCors(exchange.getResponseHeaders());
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "");
            return;
        }
        if (!CommunityBridgeConfig.launcherChatReady()) {
            sendError(exchange, 503, "Official chat is disabled.");
            return;
        }

        String channelId = channelIdFromPath(exchange.getRequestURI().getPath());
        if (!OfficialChatService.channelId().equals(channelId)) {
            sendError(exchange, 404, "Unknown channel.");
            return;
        }

        try {
            if ("GET".equals(method)) {
                int limit = parseInt(query(exchange).get("limit"), 50);
                sendJson(exchange, 200, GSON.toJson(OfficialChatService.INSTANCE.listMessages(limit)));
                return;
            }
            if ("POST".equals(method)) {
                JsonObject body = parseJsonObject(exchange.getRequestBody());
                OfficialChatMessage message = OfficialChatService.INSTANCE.acceptPublicMessage(
                        header(exchange, "X-ECHO-Chat-Source", "launcher"),
                        header(exchange, "X-ECHO-Chat-Client", ""),
                        header(exchange, "X-ECHO-Chat-Nickname", ""),
                        readString(body, "body"),
                        readString(body, "nonce"));
                sendJson(exchange, 200, GSON.toJson(message.toJson()));
                return;
            }
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
        } catch (OfficialChatService.ChatRequestException ex) {
            sendError(exchange, ex.statusCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            sendError(exchange, 400, "Invalid chat request.");
        }
    }

    private void handleChatSocket(HttpExchange exchange) throws IOException {
        applyCors(exchange.getResponseHeaders());
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "");
            return;
        }
        if (!CommunityBridgeConfig.launcherChatReady()) {
            sendError(exchange, 503, "Official chat is disabled.");
            return;
        }
        if (!"GET".equals(method)) {
            send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        try {
            OfficialChatService.normalizePublicSource(query(exchange).getOrDefault("source", "launcher"));
        } catch (OfficialChatService.ChatRequestException ex) {
            sendError(exchange, ex.statusCode(), ex.getMessage());
            return;
        }
        String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
        if (key == null || key.isBlank() || upgrade == null || !"websocket".equalsIgnoreCase(upgrade)) {
            sendError(exchange, 400, "Expected WebSocket upgrade.");
            return;
        }
        Headers headers = exchange.getResponseHeaders();
        headers.set("Upgrade", "websocket");
        headers.set("Connection", "Upgrade");
        headers.set("Sec-WebSocket-Accept", webSocketAccept(key));
        exchange.sendResponseHeaders(101, -1);
        ChatWebSocketSession session = new ChatWebSocketSession(exchange.getResponseBody());
        OfficialChatService.INSTANCE.addSocket(session);
        session.readUntilClosed(exchange.getRequestBody(), () -> OfficialChatService.INSTANCE.removeSocket(session));
        exchange.close();
    }

    private static void applyCors(Headers headers) {
        String origin = CommunityBridgeConfig.string(CommunityBridgeConfig.PUBLIC_CORS_ORIGIN);
        headers.set("Access-Control-Allow-Origin", origin.isBlank() ? "*" : origin);
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, X-ECHO-Chat-Client, X-ECHO-Chat-Nickname, X-ECHO-Chat-Source");
        headers.set("Cache-Control", "no-store");
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, body);
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("error", status >= 500 ? "server_error" : "bad_request");
        root.addProperty("message", message);
        sendJson(exchange, status, GSON.toJson(root));
    }

    private static String header(HttpExchange exchange, String name, String fallback) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static JsonObject parseJsonObject(InputStream input) throws IOException {
        String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        if (text.isBlank()) {
            return new JsonObject();
        }
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String channelIdFromPath(String path) {
        String prefix = "/v1/channels/";
        String suffix = "/messages";
        if (path == null || !path.startsWith(prefix) || !path.endsWith(suffix)) {
            return "";
        }
        String raw = path.substring(prefix.length(), path.length() - suffix.length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            result.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String webSocketAccept(String key) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((key.strip() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 unavailable.", ex);
        }
    }
}
