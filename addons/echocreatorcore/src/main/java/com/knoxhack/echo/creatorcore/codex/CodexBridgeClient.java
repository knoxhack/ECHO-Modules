package com.knoxhack.echo.creatorcore.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CodexBridgeClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(800);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private final HttpClient http;
    private final String baseUrl;
    private final String authToken;

    public CodexBridgeClient(String baseUrl) {
        this(baseUrl, "");
    }

    public CodexBridgeClient(String baseUrl, String authToken) {
        this.baseUrl = normalize(baseUrl);
        this.authToken = authToken == null ? "" : authToken.trim();
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    public CodexBridgeStatus status() throws IOException, InterruptedException {
        JsonObject json = request("GET", "/status", null);
        return statusFrom(json);
    }

    public CodexJobSnapshot startJob(CodexJobRequest request) throws IOException, InterruptedException {
        JsonObject json = request("POST", "/jobs", request.toJson().toString());
        return jobFrom(wrappedJob(json));
    }

    public CodexJobSnapshot getJob(String id) throws IOException, InterruptedException {
        JsonObject json = request("GET", "/jobs/" + encodeSegment(id), null);
        return jobFrom(wrappedJob(json));
    }

    public CodexJobSnapshot cancelJob(String id) throws IOException, InterruptedException {
        JsonObject json = request("POST", "/jobs/" + encodeSegment(id) + "/cancel", "{}");
        return jobFrom(wrappedJob(json));
    }

    public CodexJobSnapshot validateJob(String id) throws IOException, InterruptedException {
        JsonObject json = request("POST", "/jobs/" + encodeSegment(id) + "/validate", "{}");
        return jobFrom(wrappedJob(json));
    }

    public CodexPilotSnapshot pilotStatus() throws IOException, InterruptedException {
        JsonObject json = request("GET", "/pilot/status", null);
        return pilotSnapshotFrom(json);
    }

    public List<CodexPilotAction> claimPilotActions() throws IOException, InterruptedException {
        JsonObject json = request("GET", "/pilot/status?claim=1", null);
        List<CodexPilotAction> actions = new ArrayList<>();
        if (json.has("pendingActions") && json.get("pendingActions").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("pendingActions")) {
                if (element.isJsonObject()) {
                    actions.add(pilotActionFrom(element.getAsJsonObject()));
                }
            }
        }
        return List.copyOf(actions);
    }

    public JsonObject sendPilotCommand(String command, JsonObject args) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("command", command == null ? "" : command);
        body.add("args", args == null ? new JsonObject() : args);
        body.addProperty("source", "creatorcore");
        return request("POST", "/pilot/command", body.toString());
    }

    public JsonObject sendPilotTask(String prompt, int maxSteps, boolean useLatestCapture) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("prompt", prompt == null ? "" : prompt);
        body.addProperty("maxSteps", maxSteps);
        body.addProperty("useLatestCapture", useLatestCapture);
        body.addProperty("source", "creatorcore");
        return request("POST", "/pilot/task", body.toString());
    }

    public void reportPilotStatus(JsonObject status) throws IOException, InterruptedException {
        request("POST", "/pilot/status", status == null ? "{}" : status.toString());
    }

    public void reportPilotEvent(String type, String message, JsonObject data) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("command", "event");
        body.addProperty("event", type == null ? "event" : type);
        body.addProperty("message", message == null ? "" : message);
        body.add("data", data == null ? new JsonObject() : data);
        body.addProperty("source", "creatorcore");
        request("POST", "/pilot/command", body.toString());
    }

    public void reportPilotActionResult(CodexPilotAction action, String status, String message, JsonObject data)
            throws IOException, InterruptedException {
        if (action == null || action.id().isBlank()) {
            reportPilotEvent(status, message, data);
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("command", action.command());
        body.addProperty("status", status == null || status.isBlank() ? "done" : status);
        body.addProperty("message", message == null ? "" : message);
        body.add("data", data == null ? new JsonObject() : data);
        body.addProperty("source", "creatorcore");
        request("POST", "/pilot/actions/" + encodeSegment(action.id()) + "/result", body.toString());
    }

    private JsonObject request(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        if (!authToken.isBlank()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
        } else {
            builder.GET();
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseObject(response.body());
        if (response.statusCode() >= 400) {
            String message = string(json, "message");
            if (message.isBlank()) {
                message = string(json, "error");
            }
            throw new IOException(message.isBlank() ? "Bridge HTTP " + response.statusCode() : message);
        }
        return json;
    }

    private static CodexBridgeStatus statusFrom(JsonObject json) {
        return new CodexBridgeStatus(
                bool(json, "ok"),
                string(json, "message"),
                string(json, "bridge"),
                string(json, "workspace"),
                string(json, "codexPath"),
                bool(json, "codexAvailable"),
                string(json, "codexError"),
                bool(json, "dryRun"),
                string(json, "defaultModel"),
                bool(json, "authRequired"),
                bool(json, "repoEditsAllowed"),
                integer(json, "maxJobs"),
                bool(json, "commandTemplateConfigured"),
                string(json, "defaultValidationProfile"),
                stringList(json, "diagnostics"),
                integer(json, "jobCount"),
                integer(json, "runningJobCount"),
                stringList(json, "profiles"),
                stringList(json, "validationProfiles"),
                json.has("latestJob") && json.get("latestJob").isJsonObject()
                        ? jobFrom(json.getAsJsonObject("latestJob"))
                        : CodexJobSnapshot.empty(""));
    }

    private static CodexJobSnapshot jobFrom(JsonObject json) {
        return new CodexJobSnapshot(
                string(json, "id"),
                string(json, "profile"),
                string(json, "prompt"),
                string(json, "module"),
                string(json, "model"),
                string(json, "state"),
                string(json, "stdoutSummary"),
                string(json, "error"),
                stringList(json, "changedFiles"),
                stringList(json, "repoStatusBefore"),
                stringList(json, "repoStatusAfter"),
                string(json, "validationProfile"),
                string(json, "validationStatus"),
                stringList(json, "validationLines"),
                string(json, "commandLine"));
    }

    private static CodexPilotSnapshot pilotSnapshotFrom(JsonObject json) {
        JsonObject pilot = json.has("pilot") && json.get("pilot").isJsonObject()
                ? json.getAsJsonObject("pilot")
                : new JsonObject();
        List<String> events = new ArrayList<>();
        if (json.has("recentEvents") && json.get("recentEvents").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("recentEvents")) {
                if (element.isJsonObject()) {
                    events.add(string(element.getAsJsonObject(), "message"));
                }
            }
        }
        return new CodexPilotSnapshot(
                bool(pilot, "enabled"),
                bool(pilot, "spawned"),
                bool(pilot, "paused"),
                bool(pilot, "autopilotAllowed"),
                bool(pilot, "worldActionsAllowed"),
                string(pilot, "profile"),
                string(pilot, "label"),
                string(pilot, "dimension"),
                pilot.has("position") ? pilot.get("position").toString() : "{}",
                string(pilot, "lastMessage"),
                integer(json, "pendingCount"),
                events);
    }

    private static CodexPilotAction pilotActionFrom(JsonObject json) {
        return new CodexPilotAction(
                string(json, "id"),
                string(json, "command"),
                json.has("args") && json.get("args").isJsonObject() ? json.getAsJsonObject("args") : new JsonObject(),
                string(json, "prompt"),
                string(json, "source"));
    }

    private static JsonObject wrappedJob(JsonObject json) throws IOException {
        if (json.has("job") && json.get("job").isJsonObject()) {
            return json.getAsJsonObject("job");
        }
        if (json.has("error")) {
            throw new IOException(string(json, "error"));
        }
        throw new IOException("Bridge response did not contain a job object.");
    }

    private static JsonObject parseObject(String body) throws IOException {
        try {
            JsonElement element = JsonParser.parseString(body == null ? "" : body);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (RuntimeException ignored) {
            // handled below
        }
        throw new IOException("Bridge returned malformed JSON.");
    }

    private static String normalize(String url) {
        String value = url == null || url.isBlank() ? "http://127.0.0.1:47321" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String encodeSegment(String id) {
        return id == null ? "" : URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static boolean bool(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsBoolean();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static int integer(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : 0;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static String string(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static List<String> stringList(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return List.of();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            try {
                if (!element.isJsonNull()) {
                    values.add(element.getAsString());
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed list members from bridge-compatible clients.
            }
        }
        return List.copyOf(values);
    }
}
