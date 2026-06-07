package com.knoxhack.echocommunitybridge.discord;

import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.server.ServerStatusService;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordMessageQueue {
    public static final DiscordMessageQueue INSTANCE = new DiscordMessageQueue();
    private static final int MAX_ATTEMPTS = 5;

    private final DiscordRestClient client = new DiscordRestClient();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private LinkedBlockingQueue<OutboundMessage> queue = new LinkedBlockingQueue<>(CommunityBridgeConfig.DISCORD_MAX_QUEUE_SIZE.get());
    private Thread worker;

    private DiscordMessageQueue() {
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        queue = new LinkedBlockingQueue<>(CommunityBridgeConfig.DISCORD_MAX_QUEUE_SIZE.get());
        worker = new Thread(this::drainLoop, "ECHO Community Bridge Discord");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void shutdown() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        queue.clear();
    }

    public boolean enqueueChat(String player, String message) {
        if (!CommunityBridgeConfig.RELAY_MINECRAFT_CHAT.get()) {
            return false;
        }
        String channel = CommunityBridgeConfig.string(CommunityBridgeConfig.DISCORD_CHAT_CHANNEL_ID);
        String safePlayer = ServerStatusService.sanitizePlayerName(player);
        String safeMessage = ServerStatusService.sanitizeDiscordText(message, 1800);
        return enqueue(channel, "**" + safePlayer + "**: " + safeMessage, "minecraft_chat", safePlayer);
    }

    public boolean enqueueBridgeChat(String sourceLabel, String author, String message) {
        if (!CommunityBridgeConfig.RELAY_MINECRAFT_CHAT.get()) {
            return false;
        }
        String channel = CommunityBridgeConfig.string(CommunityBridgeConfig.DISCORD_CHAT_CHANNEL_ID);
        String safeSource = ServerStatusService.sanitizeDiscordText(sourceLabel, 24);
        String safeAuthor = ServerStatusService.sanitizeDiscordText(author, 32);
        String safeMessage = ServerStatusService.sanitizeDiscordText(message, 1800);
        if (safeSource.isBlank()) {
            safeSource = "ECHO";
        }
        if (safeAuthor.isBlank()) {
            safeAuthor = safeSource;
        }
        return enqueue(channel, "**[" + safeSource + "] " + safeAuthor + "**: " + safeMessage, "echo_chat", safeSource + ":" + safeAuthor);
    }

    public boolean enqueueStatus(String message) {
        String channel = CommunityBridgeConfig.string(CommunityBridgeConfig.DISCORD_STATUS_CHANNEL_ID);
        return enqueue(channel, ServerStatusService.sanitizeDiscordText(message, 1800), "minecraft_status", "");
    }

    public int pendingCount() {
        return queue.size();
    }

    private boolean enqueue(String channelId, String content, String source, String sourceMessageId) {
        if (!CommunityBridgeConfig.discordReady() || channelId.isBlank() || content == null || content.isBlank()) {
            return false;
        }
        start();
        OutboundMessage message = new OutboundMessage(
                channelId,
                content,
                source,
                sourceMessageId == null || sourceMessageId.isBlank() ? UUID.randomUUID().toString() : sourceMessageId,
                0);
        boolean accepted = queue.offer(message);
        if (!accepted) {
            EchoCommunityBridge.LOGGER.warn("ECHO Community Bridge Discord queue full; dropping {} event.", source);
        }
        return accepted;
    }

    private void drainLoop() {
        while (running.get()) {
            try {
                OutboundMessage message = queue.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                int cooldownMs = CommunityBridgeConfig.DISCORD_POST_COOLDOWN_MS.get();
                if (cooldownMs > 0) {
                    Thread.sleep(cooldownMs);
                }
                DiscordRestClient.SendResult result = client.sendMessage(message.channelId(), message.content());
                if (result.skipped() || result.success()) {
                    continue;
                }
                if (result.retry() && message.attempts() + 1 < MAX_ATTEMPTS) {
                    Thread.sleep(Math.max(250L, result.retryAfterMillis()));
                    queue.offer(message.nextAttempt());
                } else {
                    EchoCommunityBridge.LOGGER.warn("ECHO Community Bridge Discord post failed for {} event with status {}.",
                            message.source().toLowerCase(Locale.ROOT), result.statusCode());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                EchoCommunityBridge.LOGGER.warn("ECHO Community Bridge Discord queue worker failed; continuing.", ex);
            }
        }
    }

    public record OutboundMessage(String channelId, String content, String source, String sourceMessageId, int attempts) {
        OutboundMessage nextAttempt() {
            return new OutboundMessage(channelId, content, source, sourceMessageId, attempts + 1);
        }
    }
}
