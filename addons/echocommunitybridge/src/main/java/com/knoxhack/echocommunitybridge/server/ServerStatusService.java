package com.knoxhack.echocommunitybridge.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echocommunitybridge.CommunityBridgeAdapterCoreContracts;
import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerStatusService {
    public static final ServerStatusService INSTANCE = new ServerStatusService();
    private static final int MAX_PUBLIC_TEXT = 240;
    private static final int MAX_PLAYER_NAME = 32;

    private final Deque<BridgeEvent> recentEvents = new ArrayDeque<>();
    private MinecraftServer server;
    private boolean online;
    private int lastPlayerCount;
    private int lastMaxPlayers;
    private List<String> lastPlayers = List.of();
    private Instant lastUpdated = Instant.EPOCH;

    private ServerStatusService() {
    }

    public synchronized void setServer(MinecraftServer server, boolean online) {
        this.server = server;
        this.online = online;
        refreshFromServer();
        touch();
    }

    public synchronized void markOffline() {
        this.online = false;
        this.lastPlayerCount = 0;
        this.lastPlayers = List.of();
        touch();
    }

    public synchronized void refresh() {
        refreshFromServer();
        touch();
    }

    public synchronized void playerLoggedIn(ServerPlayer player) {
        refreshFromServer();
        rememberPlayer(player);
        touch();
    }

    public synchronized void playerLoggedOut(ServerPlayer player) {
        refreshFromServer();
        forgetPlayer(player);
        touch();
    }

    public synchronized void addEvent(String type, String player, String message) {
        int limit = CommunityBridgeConfig.RECENT_EVENT_LIMIT.get();
        if (limit <= 0) {
            recentEvents.clear();
            touch();
            return;
        }
        recentEvents.addLast(new BridgeEvent(
                cleanType(type),
                sanitizePlayerName(player),
                sanitizePublicText(message, MAX_PUBLIC_TEXT),
                Instant.now()));
        trimEvents(limit);
        touch();
    }

    public synchronized JsonObject snapshotJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("serverId", CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_ID));
        root.addProperty("serverName", CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_NAME));
        root.addProperty("motd", sanitizePublicText(CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_MOTD), MAX_PUBLIC_TEXT));
        root.addProperty("online", online);
        root.addProperty("playerCount", lastPlayerCount);
        root.addProperty("maxPlayers", lastMaxPlayers);

        JsonArray players = new JsonArray();
        if (CommunityBridgeConfig.SHOW_PLAYER_NAMES.get()) {
            for (String player : lastPlayers) {
                players.add(player);
            }
        }
        root.add("players", players);

        JsonObject discord = new JsonObject();
        String inviteUrl = CommunityBridgeConfig.string(CommunityBridgeConfig.DISCORD_INVITE_URL);
        discord.addProperty("linked", !inviteUrl.isBlank());
        if (!inviteUrl.isBlank()) {
            discord.addProperty("inviteUrl", inviteUrl);
        }
        root.add("discord", discord);

        JsonObject version = new JsonObject();
        version.addProperty("minecraft", modVersion("minecraft"));
        version.addProperty("echo", modVersion(EchoCommunityBridge.MODID));
        root.add("version", version);

        JsonArray events = new JsonArray();
        for (BridgeEvent event : recentEvents) {
            events.add(event.toJson());
        }
        root.add("recentEvents", events);
        root.addProperty("lastUpdated", lastUpdated.toString());
        return root;
    }

    public synchronized List<BridgeEvent> recentEvents() {
        return List.copyOf(recentEvents);
    }

    public synchronized boolean online() {
        return online;
    }

    public synchronized int playerCount() {
        return lastPlayerCount;
    }

    public synchronized List<String> players() {
        return List.copyOf(lastPlayers);
    }

    public synchronized int maxPlayers() {
        return lastMaxPlayers;
    }

    public synchronized Instant lastUpdated() {
        return lastUpdated;
    }

    public synchronized boolean broadcastLauncherChatLine(String line) {
        MinecraftServer target = server;
        if (target == null || !online) {
            return false;
        }
        String safeLine = sanitizePublicText(line, 600);
        if (safeLine.isBlank()) {
            return false;
        }
        target.execute(() -> target.getPlayerList().broadcastSystemMessage(Component.literal(safeLine), false));
        return true;
    }

    public static String sanitizePublicText(String value, int maxLength) {
        return CommunityBridgeAdapterCoreContracts.sanitizePublicText(value, maxLength);
    }

    public static String sanitizeDiscordText(String value, int maxLength) {
        return CommunityBridgeAdapterCoreContracts.sanitizeDiscordText(value, maxLength);
    }

    public static String sanitizePlayerName(String value) {
        return CommunityBridgeAdapterCoreContracts.sanitizePlayerName(value);
    }

    public String summaryLine() {
        JsonObject snapshot = snapshotJson();
        return snapshot.get("serverName").getAsString() + " is "
                + (snapshot.get("online").getAsBoolean() ? "online" : "offline")
                + " with " + snapshot.get("playerCount").getAsInt()
                + " / " + snapshot.get("maxPlayers").getAsInt() + " players.";
    }

    private void refreshFromServer() {
        if (server == null) {
            return;
        }
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : players) {
            String playerName = sanitizePlayerName(player.getScoreboardName());
            if (!playerName.isBlank()) {
                names.add(playerName);
            }
        }
        lastPlayers = List.copyOf(names);
        lastPlayerCount = online ? lastPlayers.size() : 0;
        lastMaxPlayers = server.getPlayerList().getMaxPlayers();
    }

    private void rememberPlayer(ServerPlayer player) {
        String playerName = sanitizePlayerName(player.getScoreboardName());
        if (playerName.isBlank() || lastPlayers.contains(playerName)) {
            return;
        }
        List<String> names = new ArrayList<>(lastPlayers);
        names.add(playerName);
        lastPlayers = List.copyOf(names);
        lastPlayerCount = online ? lastPlayers.size() : 0;
    }

    private void forgetPlayer(ServerPlayer player) {
        String playerName = sanitizePlayerName(player.getScoreboardName());
        if (playerName.isBlank() || !lastPlayers.contains(playerName)) {
            return;
        }
        List<String> names = new ArrayList<>(lastPlayers);
        names.removeIf(playerName::equals);
        lastPlayers = List.copyOf(names);
        lastPlayerCount = online ? lastPlayers.size() : 0;
    }

    private void touch() {
        lastUpdated = Instant.now();
    }

    private void trimEvents(int limit) {
        while (recentEvents.size() > limit) {
            recentEvents.removeFirst();
        }
    }

    private static String cleanType(String type) {
        String safe = type == null ? "event" : type.strip().toLowerCase(Locale.ROOT);
        safe = safe.replaceAll("[^a-z0-9_.-]", "_");
        return safe.isBlank() ? "event" : clamp(safe, 32);
    }

    private static String modVersion(String modId) {
        return EchoRuntimeModules.metadata(modId, modId).version();
    }

    private static String clamp(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        String safe = value == null ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
