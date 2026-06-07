package com.knoxhack.echopresencelink.client;

import com.google.gson.JsonObject;
import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.api.EchoPresenceContext;
import com.knoxhack.echopresencelink.api.EchoPresenceRegistry;
import com.knoxhack.echopresencelink.api.EchoPresenceSnapshot;
import com.knoxhack.echopresencelink.config.PresenceLinkConfig;
import com.knoxhack.echopresencelink.discord.DiscordIpcTransport;
import com.knoxhack.echopresencelink.discord.PresenceActivityPayload;
import com.knoxhack.echopresencelink.discord.PresenceIpcException;
import com.knoxhack.echopresencelink.discord.PresenceTransport;
import com.knoxhack.echopresencelink.presence.PresenceButtons;
import com.knoxhack.echopresencelink.presence.PresenceLinkDiagnostics;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum PresenceController {
    INSTANCE;

    private static final Identifier FALLBACK_ID = Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, "idle");

    private final long sessionStartEpochSeconds = Instant.now().getEpochSecond();
    private final AtomicBoolean blankAppIdLogged = new AtomicBoolean(false);
    private final AtomicBoolean configuredFailureLogged = new AtomicBoolean(false);
    private PresenceTransport transport = new DiscordIpcTransport();
    private long nextUpdateEpochMillis = 0L;
    private long lastTickEpochSeconds = 0L;
    private long tickCount = 0L;
    private String lastPayload = "";
    private String lastApplicationId = "";
    private boolean activitySet;

    public void tick() {
        long nowMillis = System.currentTimeMillis();
        tickCount++;
        lastTickEpochSeconds = Instant.now().getEpochSecond();
        if (nowMillis < nextUpdateEpochMillis) {
            return;
        }
        nextUpdateEpochMillis = nowMillis + Math.max(10, PresenceLinkConfig.updateIntervalSeconds()) * 1000L;

        if (!PresenceLinkConfig.enabled()) {
            clearIfNeeded();
            PresenceLinkDiagnostics.disabled();
            return;
        }

        String applicationId = PresenceLinkConfig.applicationId();
        if (applicationId.isBlank()) {
            clearIfNeeded();
            PresenceLinkDiagnostics.blankApplicationId();
            if (blankAppIdLogged.compareAndSet(false, true)) {
                EchoPresenceLink.LOGGER.debug("ECHO Presence Link Discord application id is blank; Rich Presence inactive.");
            }
            return;
        }
        blankAppIdLogged.set(false);

        EchoPresenceContext context = context();
        EchoPresenceSnapshot snapshot = selectedSnapshot(context);
        publishSnapshot(applicationId, context, snapshot, false);
    }

    public CommandResult sendMinimalTestActivity() {
        if (!PresenceLinkConfig.enabled()) {
            PresenceLinkDiagnostics.disabled();
            return CommandResult.failure("Presence Link is disabled by config.");
        }
        String applicationId = PresenceLinkConfig.applicationId();
        if (applicationId.isBlank()) {
            PresenceLinkDiagnostics.blankApplicationId();
            return CommandResult.failure("Discord application id is blank in echopresencelink-client.toml.");
        }
        EchoPresenceContext context = context();
        EchoPresenceSnapshot snapshot = EchoPresenceSnapshot.of(
                Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, "manual_test"),
                999,
                "ECHO Presence Link",
                "Manual no-asset RPC test",
                "echo_ashfall",
                context.sessionStartEpochSeconds());
        return publishSnapshot(applicationId, context, snapshot, true);
    }

    public CommandResult forceResend() {
        lastPayload = "";
        lastApplicationId = "";
        nextUpdateEpochMillis = 0L;
        tick();
        PresenceLinkDiagnostics.Snapshot snapshot = PresenceLinkDiagnostics.snapshot();
        return snapshot.lastFailure().isBlank()
                ? CommandResult.success("Presence resend requested. " + snapshot.statusLine())
                : CommandResult.failure("Presence resend failed. " + snapshot.statusLine());
    }

    public List<String> statusLines() {
        PresenceLinkDiagnostics.Snapshot snapshot = PresenceLinkDiagnostics.snapshot();
        List<String> lines = new ArrayList<>();
        lines.add("Loaded: yes");
        lines.add("Ticking: " + (lastTickEpochSeconds > 0L ? "yes" : "not yet")
                + " | ticks " + tickCount
                + " | last tick age " + age(lastTickEpochSeconds));
        lines.add("Enabled: " + PresenceLinkConfig.enabled());
        lines.add("Application ID: " + maskedApplicationId());
        lines.add("Config: " + configPath());
        lines.add("Provider: " + (snapshot.currentProviderId().isBlank() ? "none yet" : snapshot.currentProviderId()));
        lines.add("Diagnostics: " + snapshot.statusLine());
        lines.add("IPC: " + (transport.connected() ? "connected" : "not connected")
                + " | " + emptyFallback(transport.endpoint(), "no endpoint")
                + " | " + emptyFallback(transport.statusLine(), "no transport status"));
        lines.add("Last Discord response: " + emptyFallback(snapshot.lastResponse(), "none"));
        lines.add("Last failure: " + emptyFallback(snapshot.lastFailure(), "none"));
        return lines;
    }

    public void shutdown() {
        clearIfNeeded();
        transport.close();
    }

    public void setTransportForTests(PresenceTransport replacement) {
        transport.close();
        transport = replacement == null ? new DiscordIpcTransport() : replacement;
        lastPayload = "";
        lastApplicationId = "";
        activitySet = false;
    }

    private CommandResult publishSnapshot(String applicationId, EchoPresenceContext context,
            EchoPresenceSnapshot snapshot, boolean minimalOnly) {
        JsonObject activity = PresenceActivityPayload.activity(snapshot, PresenceButtons.buttons(context));
        JsonObject minimalActivity = PresenceActivityPayload.minimalActivity(snapshot);
        JsonObject requestedActivity = minimalOnly ? minimalActivity : activity;
        String payload = activity.toString();
        long nowSeconds = Instant.now().getEpochSecond();
        String providerId = snapshot.id().toString();
        if (!minimalOnly && applicationId.equals(lastApplicationId) && payload.equals(lastPayload)) {
            PresenceLinkDiagnostics.duplicateSuppressed(providerId, nowSeconds, transport.connected(),
                    transport.endpoint(), transport.lastResponse());
            return CommandResult.success("Duplicate Discord activity suppressed.");
        }

        try {
            transport.setActivity(applicationId, requestedActivity);
            lastPayload = minimalOnly ? minimalActivity.toString() : payload;
            lastApplicationId = applicationId;
            activitySet = true;
            configuredFailureLogged.set(false);
            PresenceLinkDiagnostics.success(providerId, nowSeconds, transport.connected(), transport.statusLine(),
                    transport.endpoint(), transport.lastResponse());
            return CommandResult.success(minimalOnly ? "Minimal no-asset presence test sent."
                    : "Discord activity updated.");
        } catch (PresenceIpcException exception) {
            if (!minimalOnly && exception.discordError()) {
                return retryMinimalAfterFullPayloadRejection(applicationId, minimalActivity, payload,
                        providerId, nowSeconds, exception);
            }
            return fail(providerId, nowSeconds, exception);
        } catch (IOException | RuntimeException exception) {
            return fail(providerId, nowSeconds, exception);
        }
    }

    private CommandResult retryMinimalAfterFullPayloadRejection(String applicationId, JsonObject minimalActivity,
            String fullPayload, String providerId, long nowSeconds, PresenceIpcException original) {
        try {
            transport.setActivity(applicationId, minimalActivity);
            lastPayload = fullPayload;
            lastApplicationId = applicationId;
            activitySet = true;
            configuredFailureLogged.set(false);
            PresenceLinkDiagnostics.payloadRejectedFull(providerId, failureReason(original), nowSeconds,
                    transport.endpoint(), transport.lastResponse());
            return CommandResult.success("Discord rejected the full payload; minimal no-asset activity was sent.");
        } catch (IOException | RuntimeException retryFailure) {
            return fail(providerId, nowSeconds, retryFailure);
        }
    }

    private CommandResult fail(String providerId, long nowSeconds, Exception exception) {
        activitySet = false;
        lastPayload = "";
        String reason = failureReason(exception);
        PresenceLinkDiagnostics.failure(providerId, reason, nowSeconds, transport.endpoint(),
                transport.lastResponse());
        if (configuredFailureLogged.compareAndSet(false, true)) {
            EchoPresenceLink.LOGGER.info("ECHO Presence Link could not publish Discord Rich Presence: {}",
                    reason);
        } else {
            EchoPresenceLink.LOGGER.debug("ECHO Presence Link could not publish Discord Rich Presence: {}",
                    reason);
        }
        return CommandResult.failure(reason);
    }

    private EchoPresenceContext context() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        String screenClass = screen == null ? "" : screen.getClass().getName();
        Component title = screen == null ? Component.empty() : screen.getTitle();
        String screenTitle = title == null ? "" : title.getString();
        String serverName = "";
        ServerData serverData = minecraft.getCurrentServer();
        if (serverData != null) {
            serverName = serverData.name;
        }
        BlockPos pos = minecraft.player == null ? BlockPos.ZERO : minecraft.player.blockPosition();
        return new EchoPresenceContext(
                minecraft.player,
                screenClass,
                screenTitle,
                "",
                serverName,
                pos,
                PresenceLinkConfig.privacyMode(),
                PresenceLinkConfig.includeWorldName(),
                PresenceLinkConfig.includeServerName(),
                PresenceLinkConfig.includeCoordinates(),
                PresenceLinkConfig.showButtons(),
                Instant.now().getEpochSecond(),
                sessionStartEpochSeconds);
    }

    private static EchoPresenceSnapshot selectedSnapshot(EchoPresenceContext context) {
        Optional<EchoPresenceSnapshot> selected = EchoPresenceRegistry.select(context);
        return selected.orElseGet(() -> EchoPresenceSnapshot.of(
                FALLBACK_ID,
                1,
                "ECHO Presence Link",
                "Standing by for field telemetry",
                "echo_ashfall",
                context.sessionStartEpochSeconds()));
    }

    private void clearIfNeeded() {
        if (!activitySet && lastApplicationId.isBlank()) {
            transport.close();
            return;
        }
        try {
            transport.clearActivity(lastApplicationId);
        } catch (IOException | RuntimeException ignored) {
            // Quiet by design; disabled or unconfigured clients should not nag.
        } finally {
            lastPayload = "";
            lastApplicationId = "";
            activitySet = false;
            PresenceLinkDiagnostics.cleared();
        }
    }

    private static String failureReason(Exception exception) {
        if (exception instanceof PresenceIpcException ipcException) {
            return ipcException.code() + ": " + ipcException.getMessage();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String configPath() {
        Path path = Path.of("config").resolve(EchoPresenceLink.MODID + "-client.toml")
                .toAbsolutePath().normalize();
        return path.toString();
    }

    private static String maskedApplicationId() {
        String id = PresenceLinkConfig.applicationId();
        if (id.isBlank()) {
            return "blank";
        }
        if (id.length() <= 8) {
            return "configured";
        }
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }

    private static String age(long epochSeconds) {
        if (epochSeconds <= 0L) {
            return "never";
        }
        return Math.max(0L, Instant.now().getEpochSecond() - epochSeconds) + "s";
    }

    private static String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record CommandResult(boolean success, String message) {
        public static CommandResult success(String message) {
            return new CommandResult(true, message);
        }

        public static CommandResult failure(String message) {
            return new CommandResult(false, message);
        }
    }
}
