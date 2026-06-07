package com.knoxhack.echoweathercore.server;

import com.knoxhack.echo.adaptercore.EchoNativeWeatherWarningBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WeatherWarningManager {
    private static final Map<UUID, WeatherPhase> lastReportedPhase = new ConcurrentHashMap<>();
    private static final Map<UUID, EchoWorldContracts.EchoWeatherWarningResult> lastWarnings =
            new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, EchoWorldContracts.EchoWeatherWarningResult>> playerWarnings =
            new ConcurrentHashMap<>();

    private WeatherWarningManager() {}

    public static void broadcastForecast(ServerLevel level, ActiveWeatherEvent event) {
        WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
        if (profile == null) return;

        String msg = profile.terminalWarning();
        if (msg == null || msg.isEmpty()) {
            msg = "ECHO WEATHER ALERT: " + profile.displayName() + " likely. Prepare accordingly.";
        }
        EchoWorldContracts.EchoWeatherWarningResult result = issueWarning(level, event, profile,
                WeatherPhase.FORECAST, "forecast_broadcast", msg, "WeatherWarningManager.broadcastForecast");
        for (ServerPlayer player : recipients(level, event)) {
            player.sendSystemMessage(Component.literal(result.message()));
        }
    }

    public static void notifyPhaseChange(ServerLevel level, ActiveWeatherEvent event) {
        WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
        if (profile == null) return;
        WeatherPhase last = lastReportedPhase.get(event.eventId());
        if (last == event.phase()) return;
        lastReportedPhase.put(event.eventId(), event.phase());

        String msg = switch (event.phase()) {
            case INCOMING -> "ECHO-7: " + profile.displayName() + " incoming. Shelter or reduce travel speed.";
            case ACTIVE -> "WEATHER EVENT ACTIVE: " + profile.displayName().toUpperCase() + ". Scanner range degraded.";
            case CRITICAL -> "ECHO-7: External conditions worsening. Expedition continuation not advised.";
            case CLEARING -> "Weather clearing. Scanner reliability restored.";
            default -> null;
        };

        if (msg == null) return;

        EchoWorldContracts.EchoWeatherWarningResult result = issueWarning(level, event, profile,
                event.phase(), "phase_change", msg, "WeatherWarningManager.notifyPhaseChange");
        for (ServerPlayer player : recipients(level, event)) {
            player.sendSystemMessage(Component.literal(result.message()));
        }

        if (event.phase() == WeatherPhase.ENDED) {
            lastReportedPhase.remove(event.eventId());
        }
    }

    public static void sendPersonalWarning(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    public static Optional<EchoWorldContracts.EchoWeatherWarningResult> lastWarning(UUID eventId) {
        return Optional.ofNullable(eventId == null ? null : lastWarnings.get(eventId));
    }

    public static Optional<EchoWorldContracts.EchoWeatherWarningResult> lastWarning(ServerPlayer player,
            UUID eventId) {
        if (player == null || eventId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerWarnings.getOrDefault(player.getUUID(), Map.of()).get(eventId.toString()));
    }

    public static Map<UUID, EchoWorldContracts.EchoWeatherWarningResult> lastWarnings() {
        return Map.copyOf(lastWarnings);
    }

    public static void clearForTests() {
        lastReportedPhase.clear();
        lastWarnings.clear();
        playerWarnings.clear();
    }

    private static EchoWorldContracts.EchoWeatherWarningResult issueWarning(ServerLevel level,
            ActiveWeatherEvent event,
            WeatherProfile profile,
            WeatherPhase phase,
            String channel,
            String message,
            String sourceReason) {
        List<String> recipientIds = recipients(level, event).stream()
                .map(player -> player.getUUID().toString())
                .toList();
        EchoWorldContracts.EchoWeatherWarningResult result =
                new EchoNativeWeatherWarningBridge(EchoWeatherCore.MODID)
                        .issue(new EchoWorldContracts.EchoWeatherWarningRequest(
                                event.eventId().toString(),
                                profile.id().toString(),
                                event.regionId() == null ? "" : event.regionId().toString(),
                                phase.name(),
                                channel,
                                message,
                                recipientIds,
                                level.getGameTime(),
                                sourceReason));
        lastWarnings.put(event.eventId(), result);
        for (ServerPlayer player : recipients(level, event)) {
            playerWarnings.compute(player.getUUID(), (id, warnings) -> {
                LinkedHashMap<String, EchoWorldContracts.EchoWeatherWarningResult> updated = new LinkedHashMap<>();
                if (warnings != null) {
                    updated.putAll(warnings);
                }
                updated.put(event.eventId().toString(), result);
                return Map.copyOf(updated);
            });
        }
        return result;
    }

    private static List<ServerPlayer> recipients(ServerLevel level, ActiveWeatherEvent event) {
        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (event.affectsPosition(player.blockPosition())) {
                recipients.add(player);
            }
        }
        return List.copyOf(recipients);
    }
}
