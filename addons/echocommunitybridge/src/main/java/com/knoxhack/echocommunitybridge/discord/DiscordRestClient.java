package com.knoxhack.echocommunitybridge.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class DiscordRestClient {
    private static final Gson GSON = new Gson();
    private static final String API_BASE = "https://discord.com/api/v10";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SendResult sendMessage(String channelId, String content) {
        if (!CommunityBridgeConfig.discordReady() || channelId == null || channelId.isBlank()) {
            return SendResult.skippedResult();
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("content", content == null ? "" : content);
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", allowedMentions);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/channels/" + encodePath(channelId.strip()) + "/messages"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bot " + CommunityBridgeConfig.discordBotToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return SendResult.success(status);
            }
            if (status == 429) {
                return SendResult.retry(status, retryAfterMillis(response.body()));
            }
            return SendResult.failure(status);
        } catch (IOException ex) {
            return SendResult.retry(0, 2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SendResult.failure(0);
        }
    }

    private static long retryAfterMillis(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("retry_after")) {
                double seconds = json.get("retry_after").getAsDouble();
                return Math.max(250L, (long) (seconds * 1000.0D));
            }
        } catch (RuntimeException ignored) {
        }
        return 2500L;
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record SendResult(boolean success, boolean skipped, boolean retry, int statusCode, long retryAfterMillis) {
        static SendResult success(int statusCode) {
            return new SendResult(true, false, false, statusCode, 0L);
        }

        static SendResult skippedResult() {
            return new SendResult(false, true, false, 0, 0L);
        }

        static SendResult retry(int statusCode, long retryAfterMillis) {
            return new SendResult(false, false, true, statusCode, retryAfterMillis);
        }

        static SendResult failure(int statusCode) {
            return new SendResult(false, false, false, statusCode, 0L);
        }
    }
}
