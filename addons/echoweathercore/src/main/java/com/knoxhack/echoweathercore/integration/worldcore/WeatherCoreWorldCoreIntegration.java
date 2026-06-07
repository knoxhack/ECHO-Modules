package com.knoxhack.echoweathercore.integration.worldcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WeatherCoreWorldCoreIntegration {
    private static final Map<UUID, WorldHazardSnapshot> LAST_SNAPSHOTS = new HashMap<>();
    private static boolean registered;

    private WeatherCoreWorldCoreIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoBackendLifecycleBridge.registerGameEventHandler(WeatherCoreWorldCoreIntegration::onLevelTick);
        EchoWeatherCore.LOGGER.info("WeatherCore publishing storm hazard context through WorldCore snapshots.");
    }

    private static void onLevelTick(Object event) {
        ServerLevel level = EchoBackendWorldEventBridge.postTickServerLevel(event);
        if (level == null || level.getGameTime() % 40L != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            WorldHazardSnapshot current = snapshotFor(player);
            WorldHazardSnapshot previous = LAST_SNAPSHOTS.put(player.getUUID(), current);
            if (previous != null && !previous.equals(current)) {
                EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player, previous, current));
            } else if (previous == null && !current.safeZone()) {
                EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player,
                        WorldHazardSnapshot.nominal(), current));
            }
        }
    }

    private static WorldHazardSnapshot snapshotFor(ServerPlayer player) {
        List<ActiveWeatherEvent> events = WeatherStateManager.getInstance()
                .getEventsAt(player.level(), player.blockPosition())
                .stream()
                .sorted(Comparator.comparing((ActiveWeatherEvent event) -> event.profileId().toString())
                        .thenComparing(event -> event.eventId().toString()))
                .toList();
        if (events.isEmpty()) {
            return WorldHazardSnapshot.nominal();
        }

        List<Identifier> regionIds = events.stream()
                .map(ActiveWeatherEvent::regionId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        List<Identifier> hazardIds = events.stream()
                .map(WeatherCoreWorldCoreIntegration::hazardId)
                .distinct()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        int severity = events.stream()
                .mapToInt(event -> severity(event.severity()))
                .max()
                .orElse(0);
        String summary = events.size() == 1
                ? "Weather hazard active: " + events.get(0).profileId() + "."
                : "Weather hazards active: " + events.size() + " events.";
        return new WorldHazardSnapshot(regionIds, hazardIds, severity, false, summary);
    }

    private static Identifier hazardId(ActiveWeatherEvent event) {
        return hazardIdForWeatherType(event.type());
    }

    static Identifier hazardIdForWeatherType(WeatherType type) {
        return Identifier.fromNamespaceAndPath("echoworldcore", switch (type == null ? WeatherType.NONE : type) {
            case ASH_STORM, TOXIC_RAIN, SPORE_BLOOM, HEAT_SURGE -> "hazard/toxic_air";
            case RADIATION_STORM -> "hazard/radiation";
            case CRYO_FRONT -> "hazard/cryo_cold";
            case NEXUS_SIGNAL_STORM, ELECTROMAGNETIC_BLACKOUT, STATIC_FOG, MEMORY_RAIN -> "hazard/nexus_anomaly";
            case ORBITAL_DEBRIS_SHOWER -> "hazard/orbital_exposure";
            case NONE -> "hazard/secure_zone";
        });
    }

    private static int severity(WeatherSeverity severity) {
        return switch (severity == null ? WeatherSeverity.LOW : severity) {
            case LOW -> 25;
            case MODERATE -> 50;
            case SEVERE -> 75;
            case EXTREME -> 100;
        };
    }
}
