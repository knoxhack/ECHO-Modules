package com.knoxhack.echoweathercore.server;

import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereStateApplyBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherScheduleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherScheduleTickBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherStateApplyBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.echoplatform.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.config.WeatherCoreConfig;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class WeatherStateManager {
    private static final WeatherStateManager INSTANCE = new WeatherStateManager();
    private final Map<Identifier, List<ActiveWeatherEvent>> levelEvents = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWeatherScheduleResult> activeWeatherSchedules = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWeatherScheduleTickResult> lastWeatherScheduleTicks =
            new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWeatherStateApplyResult> lastWeatherStateApplications = new ConcurrentHashMap<>();
    private final Map<Identifier, EchoWorldContracts.EchoWeatherStateApplyResult> lastWeatherSurfaceStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoAtmosphereStateApplyResult> lastAtmosphereStateApplications = new ConcurrentHashMap<>();
    private final Map<Identifier, EchoWorldContracts.EchoAtmosphereStateApplyResult> lastAtmosphereSurfaceStates = new ConcurrentHashMap<>();
    private final Map<Identifier, WeatherProfile> knownWeatherProfiles = new ConcurrentHashMap<>();
    private MinecraftServer server;

    private WeatherStateManager() {}

    public static WeatherStateManager getInstance() {
        return INSTANCE;
    }

    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        for (ServerLevel level : server.getAllLevels()) {
            loadLevel(level);
        }
        EchoWeatherCore.LOGGER.info("WeatherStateManager loaded.");
    }

    public void onServerStopping() {
        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                saveLevel(level);
            }
        }
        this.server = null;
        levelEvents.clear();
        activeWeatherSchedules.clear();
        lastWeatherScheduleTicks.clear();
        lastWeatherStateApplications.clear();
        lastWeatherSurfaceStates.clear();
        lastAtmosphereStateApplications.clear();
        lastAtmosphereSurfaceStates.clear();
        knownWeatherProfiles.clear();
    }

    public void tickLevel(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        Identifier dimension = level.dimension().identifier();
        List<ActiveWeatherEvent> events = levelEvents.getOrDefault(dimension, new ArrayList<>());
        long tick = level.getGameTime();
        List<ActiveWeatherEvent> updated = new ArrayList<>();
        for (ActiveWeatherEvent event : events) {
            EchoWorldContracts.EchoWeatherScheduleResult schedule =
                    activeWeatherSchedules.getOrDefault(event.eventId(), scheduleFromEvent(event));
            EchoWorldContracts.EchoWeatherScheduleTickResult tickResult =
                    new EchoNativeWeatherScheduleTickBridge(EchoWeatherCore.MODID)
                            .tick(new EchoWorldContracts.EchoWeatherScheduleTickRequest(
                                    event.eventId().toString(),
                                    tick,
                                    schedule));
            lastWeatherScheduleTicks.put(event.eventId(), tickResult);
            WeatherPhase phase = WeatherPhase.valueOf(tickResult.phase());
            if (tickResult.ended()) {
                ActiveWeatherEvent ended = new ActiveWeatherEvent(event.eventId(), event.profileId(), event.type(), event.severity(),
                    event.scope(), phase, event.startTick(), event.endTick(), event.warningStartTick(),
                    event.centerPos(), event.radius(), event.regionId(), event.movementDirection(),
                    event.sourceReason(), event.generatedResources(), event.debugMetadata());
                resolveProfile(event.profileId()).ifPresent(profile -> applyWeatherState(sl, profile, ended));
                activeWeatherSchedules.remove(event.eventId());
                continue;
            }
            activeWeatherSchedules.put(event.eventId(), scheduleWithPhase(schedule, tickResult.phase()));
            if (tickResult.phaseChanged()) {
                ActiveWeatherEvent updatedEvent = new ActiveWeatherEvent(event.eventId(), event.profileId(), event.type(), event.severity(),
                    event.scope(), phase, event.startTick(), event.endTick(), event.warningStartTick(),
                    event.centerPos(), event.radius(), event.regionId(), event.movementDirection(),
                    event.sourceReason(), event.generatedResources(), event.debugMetadata());
                resolveProfile(updatedEvent.profileId()).ifPresent(profile -> applyWeatherState(sl, profile, updatedEvent));
                WeatherWarningManager.notifyPhaseChange(sl, updatedEvent);
                event = updatedEvent;
            }
            updated.add(event);
        }
        levelEvents.put(dimension, updated);
    }

    private WeatherPhase computePhase(ActiveWeatherEvent event, long tick) {
        if (tick >= event.endTick()) return WeatherPhase.ENDED;
        if (tick >= event.startTick() + (event.endTick() - event.startTick()) * 0.85) return WeatherPhase.CLEARING;
        if (tick >= event.startTick() + (event.endTick() - event.startTick()) * 0.6) return WeatherPhase.CRITICAL;
        if (tick >= event.startTick()) return WeatherPhase.ACTIVE;
        if (tick >= event.warningStartTick() + (event.startTick() - event.warningStartTick()) * 0.5) return WeatherPhase.INCOMING;
        return WeatherPhase.FORECAST;
    }

    public List<ActiveWeatherEvent> getActiveEvents(Level level) {
        if (level.isClientSide()) return List.of();
        Identifier dimension = level.dimension().identifier();
        long tick = level.getGameTime();
        List<ActiveWeatherEvent> result = new ArrayList<>();
        for (ActiveWeatherEvent event : levelEvents.getOrDefault(dimension, List.of())) {
            if (event.isActive(tick)) result.add(event);
        }
        return Collections.unmodifiableList(result);
    }

    public List<ActiveWeatherEvent> getEventsAt(Level level, BlockPos pos) {
        if (level.isClientSide()) return List.of();
        long tick = level.getGameTime();
        List<ActiveWeatherEvent> result = new ArrayList<>();
        for (ActiveWeatherEvent event : getActiveEvents(level)) {
            if (event.affectsPosition(pos)) result.add(event);
        }
        return result;
    }

    public ActiveWeatherEvent startEvent(ServerLevel level, WeatherProfile profile, WeatherSeverity severity, BlockPos center, int radius, String source) {
        if (!WeatherCoreConfig.ENABLE_WEATHER_CORE.get()) return null;
        long tick = level.getGameTime();
        EchoWorldContracts.EchoWeatherScheduleResult schedule = new EchoNativeWeatherScheduleBridge(EchoWeatherCore.MODID)
                .schedule(new EchoWorldContracts.EchoWeatherScheduleRequest(
                        tick,
                        WeatherCoreConfig.MINIMUM_WARNING_TICKS.get(),
                        center.getX(),
                        center.getY(),
                        center.getZ(),
                        radius,
                        source,
                        new EchoWorldContracts.EchoWeatherScheduleProfile(
                                profile.id().toString(),
                                profile.type().name(),
                                severity.name(),
                                profile.scope().name(),
                                profile.durationTicks(),
                                profile.warningTicks(),
                                profile.weight(),
                                profile.enabled()
                        )
                ));
        if (!schedule.scheduled()) {
            return null;
        }
        knownWeatherProfiles.put(profile.id(), profile);

        ActiveWeatherEvent event = new ActiveWeatherEvent(
            UUID.randomUUID(), profile.id(), profile.type(), severity, profile.scope(),
            WeatherPhase.valueOf(schedule.phase()), schedule.startTick(), schedule.endTick(),
            schedule.warningStartTick(), center, schedule.radius(), null, null, source, null, null
        );

        Identifier dimension = level.dimension().identifier();
        activeWeatherSchedules.put(event.eventId(), schedule);
        levelEvents.computeIfAbsent(dimension, k -> new ArrayList<>()).add(event);
        applyWeatherState(level, profile, event);
        WeatherWarningManager.broadcastForecast(level, event);
        saveLevel(level);
        EchoWeatherCore.LOGGER.info("Started weather event {} in dimension {}", profile.id(), dimension);
        return event;
    }

    public java.util.Optional<EchoWorldContracts.EchoWeatherScheduleResult> activeWeatherSchedule(UUID eventId) {
        return java.util.Optional.ofNullable(eventId == null ? null : activeWeatherSchedules.get(eventId));
    }

    public java.util.Optional<EchoWorldContracts.EchoWeatherScheduleResult> activeWeatherSchedule(Identifier profileId) {
        if (profileId == null) {
            return java.util.Optional.empty();
        }
        String id = profileId.toString();
        return activeWeatherSchedules.values().stream()
                .filter(schedule -> schedule.profileId().equals(id))
                .findFirst();
    }

    public Map<UUID, EchoWorldContracts.EchoWeatherScheduleResult> activeWeatherSchedules() {
        return Map.copyOf(activeWeatherSchedules);
    }

    public java.util.Optional<EchoWorldContracts.EchoWeatherScheduleTickResult> lastWeatherScheduleTick(UUID eventId) {
        return java.util.Optional.ofNullable(eventId == null ? null : lastWeatherScheduleTicks.get(eventId));
    }

    public java.util.Optional<EchoWorldContracts.EchoWeatherStateApplyResult> lastWeatherStateApplication(UUID eventId) {
        return java.util.Optional.ofNullable(eventId == null ? null : lastWeatherStateApplications.get(eventId));
    }

    public java.util.Optional<EchoWorldContracts.EchoWeatherStateApplyResult> lastWeatherSurfaceState(Level level) {
        if (level == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(lastWeatherSurfaceStates.get(level.dimension().identifier()));
    }

    public java.util.Optional<EchoWorldContracts.EchoAtmosphereStateApplyResult> lastAtmosphereStateApplication(UUID eventId) {
        return java.util.Optional.ofNullable(eventId == null ? null : lastAtmosphereStateApplications.get(eventId));
    }

    public java.util.Optional<EchoWorldContracts.EchoAtmosphereStateApplyResult> lastAtmosphereSurfaceState(Level level) {
        if (level == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(lastAtmosphereSurfaceStates.get(level.dimension().identifier()));
    }

    private java.util.Optional<WeatherProfile> resolveProfile(Identifier profileId) {
        WeatherProfile profile = knownWeatherProfiles.get(profileId);
        if (profile != null) {
            return java.util.Optional.of(profile);
        }
        return java.util.Optional.ofNullable(WeatherDataReloadListener.INSTANCE.getProfile(profileId));
    }

    private EchoWorldContracts.EchoWeatherStateApplyResult applyWeatherState(ServerLevel level,
            WeatherProfile profile,
            ActiveWeatherEvent event) {
        String path = profile.id().getPath();
        String warning = profile.terminalWarning() == null || profile.terminalWarning().isBlank()
                ? profile.displayName()
                : profile.terminalWarning();
        String audioCue = profile.soundCoreAmbienceId() == null || profile.soundCoreAmbienceId().isBlank()
                ? "echoweathercore:event." + path
                : profile.soundCoreAmbienceId();
        String renderProfile = profile.particleVisualProfileId() == null || profile.particleVisualProfileId().isBlank()
                ? "echorendercore:hazard/" + path
                : profile.particleVisualProfileId();
        String atmosphereId = "echoatmospherecore:" + path + "_field";
        double visibility = profile.effects().visibilityMultiplier();
        String particleProfile = profile.particleVisualProfileId() == null || profile.particleVisualProfileId().isBlank()
                ? "minecraft:ash"
                : profile.particleVisualProfileId();
        String skyFog = "weather_phase:" + event.phase().name();
        String hudLine = profile.displayName().toUpperCase() + ": " + warning;
        if (event.phase() == WeatherPhase.ENDED) {
            hudLine = profile.displayName().toUpperCase() + ": CLEAR";
            audioCue = "echoweathercore:event.clear";
            renderProfile = "echorendercore:weather/clear";
            atmosphereId = "echoatmospherecore:clear_field";
            visibility = 1.0D;
            particleProfile = "minecraft:empty";
            skyFog = "weather_phase:ENDED";
        }
        EchoWorldContracts.EchoAtmosphereState atmosphereState = new EchoWorldContracts.EchoAtmosphereState(
                atmosphereId,
                visibility,
                particleProfile,
                skyFog);
        EchoWorldContracts.EchoWeatherStateApplyResult result = new EchoNativeWeatherStateApplyBridge(EchoWeatherCore.MODID)
                .apply(new EchoWorldContracts.EchoWeatherStateApplyRequest(
                        event.eventId().toString(),
                        event.regionId() == null ? "" : event.regionId().toString(),
                        event.phase().name(),
                        level.getGameTime(),
                        event.sourceReason(),
                        new EchoWorldContracts.EchoWeatherState(
                                profile.id().toString(),
                                hudLine,
                                audioCue,
                                renderProfile),
                        atmosphereState
                ));
        EchoWorldContracts.EchoAtmosphereStateApplyResult atmosphereResult =
                new EchoNativeAtmosphereStateApplyBridge("echoatmospherecore")
                        .apply(new EchoWorldContracts.EchoAtmosphereStateApplyRequest(
                                event.eventId().toString(),
                                profile.id().toString(),
                                event.regionId() == null ? "" : event.regionId().toString(),
                                event.phase().name(),
                                level.getGameTime(),
                                event.sourceReason(),
                                atmosphereState));
        lastWeatherStateApplications.put(event.eventId(), result);
        lastWeatherSurfaceStates.put(level.dimension().identifier(), result);
        lastAtmosphereStateApplications.put(event.eventId(), atmosphereResult);
        lastAtmosphereSurfaceStates.put(level.dimension().identifier(), atmosphereResult);
        EchoWorldRuntimeBus.fireWeatherSurfaceApplied(new EchoWorldRuntimeBus.WeatherSurfaceApplied(
                level,
                result.eventId(),
                result.weatherId(),
                result.phase(),
                result.hudState(),
                result.audioState(),
                result.renderState(),
                result.applied()));
        return result;
    }

    public void clearEvents(ServerLevel level, WeatherType type) {
        Identifier dimension = level.dimension().identifier();
        List<ActiveWeatherEvent> events = levelEvents.getOrDefault(dimension, new ArrayList<>());
        events.removeIf(e -> {
            boolean remove = e.type() == type;
            if (remove) {
                activeWeatherSchedules.remove(e.eventId());
            }
            return remove;
        });
        saveLevel(level);
    }

    public void clearAllEvents(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        for (ActiveWeatherEvent event : levelEvents.getOrDefault(dimension, List.of())) {
            activeWeatherSchedules.remove(event.eventId());
        }
        levelEvents.put(dimension, new ArrayList<>());
        lastWeatherSurfaceStates.remove(dimension);
        saveLevel(level);
    }

    public boolean advanceEventsForSleep(ServerLevel level, long skippedTicks) {
        if (skippedTicks <= 0) {
            return false;
        }

        Identifier dimension = level.dimension().identifier();
        List<ActiveWeatherEvent> events = levelEvents.getOrDefault(dimension, new ArrayList<>());
        if (events.isEmpty()) {
            return false;
        }

        long tick = level.getGameTime();
        List<ActiveWeatherEvent> updated = new ArrayList<>();
        boolean changed = false;
        for (ActiveWeatherEvent event : events) {
            long shiftedEndTick = event.endTick() - skippedTicks;
            if (tick >= shiftedEndTick) {
                activeWeatherSchedules.remove(event.eventId());
                changed = true;
                continue;
            }

            ActiveWeatherEvent shifted = new ActiveWeatherEvent(
                event.eventId(), event.profileId(), event.type(), event.severity(), event.scope(), event.phase(),
                event.startTick() - skippedTicks, shiftedEndTick, event.warningStartTick() - skippedTicks,
                event.centerPos(), event.radius(), event.regionId(), event.movementDirection(),
                event.sourceReason(), event.generatedResources(), event.debugMetadata()
            );
            updated.add(shifted);
            shiftWeatherSchedule(shifted.eventId(), skippedTicks);
            changed = true;
        }

        if (changed) {
            levelEvents.put(dimension, updated);
            saveLevel(level);
        }
        return changed;
    }

    private void loadLevel(ServerLevel level) {
        WeatherSavedData data = WeatherSavedData.get(level);
        Identifier dimension = level.dimension().identifier();
        levelEvents.put(dimension, new ArrayList<>(data.getEvents()));
        for (ActiveWeatherEvent event : data.getEvents()) {
            activeWeatherSchedules.put(event.eventId(), scheduleFromEvent(event));
            resolveProfile(event.profileId()).ifPresent(profile -> applyWeatherState(level, profile, event));
        }
    }

    private void saveLevel(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        List<ActiveWeatherEvent> events = levelEvents.getOrDefault(dimension, new ArrayList<>());
        WeatherSavedData data = WeatherSavedData.get(level);
        data.setEvents(events);
    }

    private void shiftWeatherSchedule(UUID eventId, long skippedTicks) {
        activeWeatherSchedules.computeIfPresent(eventId, (id, schedule) -> new EchoWorldContracts.EchoWeatherScheduleResult(
                schedule.profileId(),
                schedule.type(),
                schedule.severity(),
                schedule.scope(),
                schedule.phase(),
                Math.max(0L, schedule.warningStartTick() - skippedTicks),
                Math.max(0L, schedule.startTick() - skippedTicks),
                Math.max(0L, schedule.endTick() - skippedTicks),
                schedule.centerX(),
                schedule.centerY(),
                schedule.centerZ(),
                schedule.radius(),
                schedule.sourceReason(),
                schedule.scheduled()));
    }

    private EchoWorldContracts.EchoWeatherScheduleResult scheduleWithPhase(
            EchoWorldContracts.EchoWeatherScheduleResult schedule,
            String phase) {
        return new EchoWorldContracts.EchoWeatherScheduleResult(
                schedule.profileId(),
                schedule.type(),
                schedule.severity(),
                schedule.scope(),
                phase,
                schedule.warningStartTick(),
                schedule.startTick(),
                schedule.endTick(),
                schedule.centerX(),
                schedule.centerY(),
                schedule.centerZ(),
                schedule.radius(),
                schedule.sourceReason(),
                schedule.scheduled());
    }

    private EchoWorldContracts.EchoWeatherScheduleResult scheduleFromEvent(ActiveWeatherEvent event) {
        return new EchoWorldContracts.EchoWeatherScheduleResult(
                event.profileId().toString(),
                event.type().name(),
                event.severity().name(),
                event.scope().name(),
                event.phase().name(),
                Math.max(0L, event.warningStartTick()),
                Math.max(0L, event.startTick()),
                Math.max(0L, event.endTick()),
                event.centerPos().getX(),
                event.centerPos().getY(),
                event.centerPos().getZ(),
                event.radius(),
                event.sourceReason(),
                true);
    }
}
