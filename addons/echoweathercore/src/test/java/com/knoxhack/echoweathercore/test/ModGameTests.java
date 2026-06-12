package com.knoxhack.echoweathercore.test;

import com.echoplatform.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherScope;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.block.ClimateSensorBlock;
import com.knoxhack.echoweathercore.block.EmergencySirenBlock;
import com.knoxhack.echoweathercore.block.RouteWarningPostBlock;
import com.knoxhack.echoweathercore.blockentity.WeatherStationBlockEntity;
import com.knoxhack.echoweathercore.config.WeatherCoreConfig;
import com.knoxhack.echoweathercore.item.WeatherRadioItem;
import com.knoxhack.echoweathercore.server.WeatherCountermeasureManager;
import com.knoxhack.echoweathercore.server.WeatherForecastManager;
import com.knoxhack.echoweathercore.server.WeatherSavedData;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import com.knoxhack.echoweathercore.server.WeatherWarningManager;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoWeatherCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_SLEEP_ADVANCE_CLEARS =
            TEST_FUNCTIONS.register("weather_sleep_advance_clears", () -> ModGameTests::weatherSleepAdvanceClears);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_SLEEP_ADVANCE_PARTIAL =
            TEST_FUNCTIONS.register("weather_sleep_advance_partial", () -> ModGameTests::weatherSleepAdvancePartial);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_ADAPTERCORE_SCHEDULE =
            TEST_FUNCTIONS.register("weather_adaptercore_schedule", () -> ModGameTests::weatherAdapterCoreSchedule);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_ADAPTERCORE_STATE =
            TEST_FUNCTIONS.register("weather_adaptercore_state", () -> ModGameTests::weatherAdapterCoreState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_ADAPTERCORE_ENDED_STATE =
            TEST_FUNCTIONS.register("weather_adaptercore_ended_state", () -> ModGameTests::weatherAdapterCoreEndedState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_ASHFALL_TOXIC_FRONT_SURFACE =
            TEST_FUNCTIONS.register("weather_ashfall_toxic_front_surface", () -> ModGameTests::weatherAshfallToxicFrontSurface);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_SAVED_EVENT_RELOAD_SURFACE =
            TEST_FUNCTIONS.register("weather_saved_event_reload_surface", () -> ModGameTests::weatherSavedEventReloadSurface);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_EXPOSURE_COUNTERMEASURE =
            TEST_FUNCTIONS.register("weather_exposure_countermeasure", () -> ModGameTests::weatherExposureCountermeasure);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WEATHER_ADAPTERCORE_FORECAST =
            TEST_FUNCTIONS.register("weather_adaptercore_forecast", () -> ModGameTests::weatherAdapterCoreForecast);

    private ModGameTests() {}

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("weathercore"));
        register(event, environment, "weather_sleep_advance_clears", WEATHER_SLEEP_ADVANCE_CLEARS.getId());
        register(event, environment, "weather_sleep_advance_partial", WEATHER_SLEEP_ADVANCE_PARTIAL.getId());
        register(event, environment, "weather_adaptercore_schedule", WEATHER_ADAPTERCORE_SCHEDULE.getId());
        register(event, environment, "weather_adaptercore_state", WEATHER_ADAPTERCORE_STATE.getId());
        register(event, environment, "weather_adaptercore_ended_state", WEATHER_ADAPTERCORE_ENDED_STATE.getId());
        register(event, environment, "weather_ashfall_toxic_front_surface", WEATHER_ASHFALL_TOXIC_FRONT_SURFACE.getId());
        register(event, environment, "weather_saved_event_reload_surface", WEATHER_SAVED_EVENT_RELOAD_SURFACE.getId());
        register(event, environment, "weather_exposure_countermeasure", WEATHER_EXPOSURE_COUNTERMEASURE.getId());
        register(event, environment, "weather_adaptercore_forecast", WEATHER_ADAPTERCORE_FORECAST.getId());
    }

    private static void weatherSleepAdvanceClears(GameTestHelper helper) {
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        try {
            var event = manager.startEvent(level, testProfile(1800), WeatherSeverity.SEVERE,
                    helper.absolutePos(new BlockPos(2, 2, 2)), 0, "gametest");
            long skippedTicks = event.endTick() - level.getGameTime() + 1L;

            manager.advanceEventsForSleep(level, skippedTicks);
            helper.assertTrue(WeatherSavedData.get(level).getEvents().isEmpty(),
                    "Sleep advance past WeatherCore event end should remove the event");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
        }
    }

    private static void weatherSleepAdvancePartial(GameTestHelper helper) {
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        try {
            var event = manager.startEvent(level, testProfile(3600), WeatherSeverity.SEVERE,
                    helper.absolutePos(new BlockPos(2, 2, 2)), 0, "gametest");
            long skippedTicks = WeatherCoreConfig.MINIMUM_WARNING_TICKS.get() + 600L;

            manager.advanceEventsForSleep(level, skippedTicks);
            var active = manager.getActiveEvents(level);
            helper.assertTrue(active.size() == 1, "Partial sleep advance should preserve active WeatherCore event");
            var shifted = active.get(0);
            helper.assertTrue(shifted.eventId().equals(event.eventId()),
                    "Partial sleep advance should preserve WeatherCore event identity");
            helper.assertTrue(shifted.startTick() == event.startTick() - skippedTicks,
                    "Partial sleep advance should shift WeatherCore start tick");
            helper.assertTrue(shifted.endTick() == event.endTick() - skippedTicks,
                    "Partial sleep advance should shift WeatherCore end tick");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
        }
    }

    private static void weatherAdapterCoreSchedule(GameTestHelper helper) {
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        try {
            long tick = level.getGameTime();
            var event = manager.startEvent(level, testProfile(12000), WeatherSeverity.MODERATE,
                    helper.absolutePos(new BlockPos(2, 2, 2)), 2400, "adaptercore-gametest");
            helper.assertTrue(event != null, "AdapterCore weather schedule should create an event");
            helper.assertTrue(event.warningStartTick() == tick,
                    "AdapterCore weather schedule should preserve warning start tick");
            helper.assertTrue(event.startTick() == tick + WeatherCoreConfig.MINIMUM_WARNING_TICKS.get(),
                    "AdapterCore weather schedule should use minimum warning ticks");
            helper.assertTrue(event.endTick() == event.startTick() + 12000L,
                    "AdapterCore weather schedule should derive end tick from profile duration");
            helper.assertTrue(event.phase().name().equals("FORECAST"),
                    "AdapterCore weather schedule should create a forecast event");
            var schedule = manager.activeWeatherSchedule(event.eventId()).orElseThrow();
            helper.assertTrue(schedule.profileId().equals(event.profileId().toString()),
                    "AdapterCore weather schedule should be retained as live native runtime state");
            helper.assertTrue(manager.activeWeatherSchedule(event.profileId()).orElseThrow().equals(schedule),
                    "AdapterCore weather schedule should be queryable by profile id");
            helper.assertTrue(manager.activeWeatherSchedules().containsKey(event.eventId()),
                    "AdapterCore weather schedule should be present in the active native schedule map");
            helper.assertTrue(WeatherSavedData.get(level).getEvents().stream()
                            .anyMatch(saved -> saved.eventId().equals(event.eventId())),
                    "AdapterCore weather schedule should persist the planned event");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
        }
    }

    private static void weatherAdapterCoreState(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        WeatherWarningManager.clearForTests();
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        AtomicInteger appliedSurfaces = new AtomicInteger();
        EchoWorldRuntimeBus.onWeatherSurfaceApplied(event -> appliedSurfaces.incrementAndGet());
        try {
            var player = helper.makeMockServerPlayerInLevel();
            var event = manager.startEvent(level, testProfile(12000), WeatherSeverity.MODERATE,
                    helper.absolutePos(new BlockPos(2, 2, 2)), 2400, "adaptercore-state-gametest");
            helper.assertTrue(event != null, "AdapterCore weather state should create an event");
            var forecastWarning = WeatherWarningManager.lastWarning(event.eventId()).orElseThrow();
            helper.assertTrue(forecastWarning.delivered()
                            && forecastWarning.phase().equals("FORECAST")
                            && forecastWarning.channel().equals("forecast_broadcast")
                            && forecastWarning.recipientPlayerIds().contains(player.getUUID().toString())
                            && forecastWarning.hudState().get("weatherWarning").toString().contains("Radiation storm incoming")
                            && forecastWarning.audioState().get("cue").equals(
                                    "echoweathercore:warning/sleep_test_radiation_storm"),
                    "AdapterCore weather warnings should deliver and retain forecast HUD/audio/render alert state");
            var schedule = manager.activeWeatherSchedule(event.eventId()).orElseThrow();
            helper.assertTrue(schedule.scheduled() && schedule.phase().equals("FORECAST"),
                    "AdapterCore weather state should keep the active forecast schedule before phase updates");
            var state = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
            var surfaceState = manager.lastWeatherSurfaceState(level).orElseThrow();
            var atmosphereState = manager.lastAtmosphereStateApplication(event.eventId()).orElseThrow();
            var atmosphereSurfaceState = manager.lastAtmosphereSurfaceState(level).orElseThrow();
            helper.assertTrue(state.applied(), "AdapterCore weather state should be applied");
            helper.assertTrue(appliedSurfaces.get() == 1,
                    "AdapterCore weather state should fire a live WeatherSurfaceApplied runtime event");
            helper.assertTrue(surfaceState.equals(state),
                    "AdapterCore weather state should persist the latest live HUD/audio/render surface state");
            helper.assertTrue(atmosphereState.applied() && atmosphereSurfaceState.equals(atmosphereState),
                    "AdapterCore atmosphere state should persist the latest live visibility/particle/sky-fog surface state");
            helper.assertTrue(atmosphereState.renderState().get("atmosphere").equals(
                            "echoatmospherecore:sleep_test_radiation_storm_field")
                            && atmosphereState.runtimeBindings().get("moduleId").equals("echoatmospherecore"),
                    "AdapterCore atmosphere state should expose the AtmosphereCore runtime binding");
            helper.assertTrue(state.hudState().get("weather").toString().contains("SLEEP TEST RADIATION STORM"),
                    "AdapterCore weather state should expose HUD weather text");
            helper.assertTrue(state.audioState().get("cue").equals("echoweathercore:event.sleep_test_radiation_storm"),
                    "AdapterCore weather state should expose an audio cue");
            helper.assertTrue(state.renderState().get("weatherProfile").equals("echorendercore:hazard/sleep_test_radiation_storm"),
                    "AdapterCore weather state should expose a render profile");
            long skippedTicks = WeatherCoreConfig.MINIMUM_WARNING_TICKS.get() + 600L;
            manager.advanceEventsForSleep(level, skippedTicks);
            manager.tickLevel(level);
            var activeScheduleTick = manager.lastWeatherScheduleTick(event.eventId()).orElseThrow();
            var activeState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
            var activeSurfaceState = manager.lastWeatherSurfaceState(level).orElseThrow();
            var activeAtmosphereState = manager.lastAtmosphereStateApplication(event.eventId()).orElseThrow();
            helper.assertTrue(activeScheduleTick.phase().equals("ACTIVE") && activeScheduleTick.phaseChanged(),
                    "AdapterCore weather schedule tick should materialize the ACTIVE phase transition");
            helper.assertTrue(manager.activeWeatherSchedule(event.eventId()).orElseThrow().phase().equals("ACTIVE"),
                    "AdapterCore weather schedule tick should retain the updated active schedule phase");
            helper.assertTrue(appliedSurfaces.get() == 2,
                    "AdapterCore weather phase change should fire a second live WeatherSurfaceApplied runtime event");
            helper.assertTrue(activeState.applied() && activeState.phase().equals("ACTIVE"),
                    "AdapterCore weather phase change should apply ACTIVE HUD/audio/render state");
            helper.assertTrue(activeSurfaceState.equals(activeState),
                    "AdapterCore weather phase change should refresh the latest live HUD/audio/render surface state");
            helper.assertTrue(activeAtmosphereState.applied() && activeAtmosphereState.phase().equals("ACTIVE")
                            && activeAtmosphereState.renderState().get("skyFog").equals("weather_phase:ACTIVE"),
                    "AdapterCore weather phase change should refresh AtmosphereCore visibility/particle/sky-fog state");
            var phaseWarning = WeatherWarningManager.lastWarning(player, event.eventId()).orElseThrow();
            helper.assertTrue(phaseWarning.delivered()
                            && phaseWarning.phase().equals("ACTIVE")
                            && phaseWarning.channel().equals("phase_change")
                            && Boolean.TRUE.equals(phaseWarning.renderState().get("severityPulse"))
                            && phaseWarning.recipientCount() == 1,
                    "AdapterCore weather warnings should retain phase-change alert state per player");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
            WeatherWarningManager.clearForTests();
            EchoWorldRuntimeBus.clearForTests();
        }
    }

    private static void weatherAdapterCoreEndedState(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        AtomicInteger appliedSurfaces = new AtomicInteger();
        EchoWorldRuntimeBus.onWeatherSurfaceApplied(event -> appliedSurfaces.incrementAndGet());

        var event = manager.startEvent(level, testProfile(1200), WeatherSeverity.MODERATE,
                helper.absolutePos(new BlockPos(2, 2, 2)), 2400, "adaptercore-ended-gametest");
        helper.assertTrue(event != null, "AdapterCore weather ended state should create an event");
        helper.assertTrue(manager.activeWeatherSchedule(event.eventId()).isPresent(),
                "AdapterCore weather ended state should retain the active schedule before event expiry");
        long skippedTicks = event.endTick() - level.getGameTime() - 1L;
        manager.advanceEventsForSleep(level, skippedTicks);

        helper.runAfterDelay(2L, () -> {
            try {
                manager.tickLevel(level);
                var endedScheduleTick = manager.lastWeatherScheduleTick(event.eventId()).orElseThrow();
                var endedState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
                var endedSurfaceState = manager.lastWeatherSurfaceState(level).orElseThrow();
                helper.assertTrue(endedScheduleTick.ended() && endedScheduleTick.phase().equals("ENDED"),
                        "AdapterCore weather schedule tick should materialize the ENDED phase transition");
                helper.assertTrue(manager.getActiveEvents(level).isEmpty(),
                        "AdapterCore weather ended state should remove the expired event");
                helper.assertTrue(manager.activeWeatherSchedule(event.eventId()).isEmpty()
                                && manager.activeWeatherSchedule(event.profileId()).isEmpty(),
                        "AdapterCore weather ended state should retire the active native schedule");
                helper.assertTrue(appliedSurfaces.get() >= 2,
                        "AdapterCore weather ended state should fire a live WeatherSurfaceApplied runtime event");
                helper.assertTrue(endedState.applied() && endedState.phase().equals("ENDED"),
                        "AdapterCore weather ended state should apply ENDED HUD/audio/render state");
                helper.assertTrue(endedSurfaceState.equals(endedState),
                        "AdapterCore weather ended state should become the latest live HUD/audio/render surface state");
                helper.assertTrue(endedState.hudState().get("weather").toString().contains("CLEAR"),
                        "AdapterCore weather ended state should clear the HUD weather line");
                helper.assertTrue(endedState.audioState().get("cue").equals("echoweathercore:event.clear"),
                        "AdapterCore weather ended state should clear the audio cue");
                helper.assertTrue(endedState.renderState().get("weatherProfile").equals("echorendercore:weather/clear"),
                        "AdapterCore weather ended state should clear the render profile");
                helper.succeed();
            } finally {
                manager.clearAllEvents(level);
                EchoWorldRuntimeBus.clearForTests();
            }
        });
    }

    private static void weatherAshfallToxicFrontSurface(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        AtomicInteger appliedSurfaces = new AtomicInteger();
        EchoWorldRuntimeBus.onWeatherSurfaceApplied(event -> appliedSurfaces.incrementAndGet());

        var event = manager.startEvent(level, ashfallToxicFrontProfile(), WeatherSeverity.MODERATE,
                helper.absolutePos(new BlockPos(4, 2, 4)), 2400, "agent7-toxic-front-gametest");
        helper.assertTrue(event != null, "Ashfall toxic-front profile should create a native weather event");
        var schedule = manager.activeWeatherSchedule(event.eventId()).orElseThrow();
        helper.assertTrue(schedule.profileId().equals("echoashfallprotocol:ashfall_toxic_front"),
                "Ashfall toxic front schedule should retain the reference weather id");
        helper.assertTrue(schedule.type().equals("TOXIC_RAIN") && schedule.severity().equals("MODERATE")
                        && schedule.scope().equals("ROUTE_BASED"),
                "Ashfall toxic front schedule should retain native WeatherCore enum contract values");
        var forecastState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
        helper.assertTrue(forecastState.hudState().get("weather").equals(
                        "ASHFALL TOXIC FRONT: Toxic front approaching. Return to the pod route or use a marked shelter."),
                "Ashfall toxic front should expose the reference HUD warning");
        helper.assertTrue(forecastState.audioState().get("cue").equals("echoashfallprotocol:event.ashfall_toxic_front"),
                "Ashfall toxic front should expose the reference audio cue");
        helper.assertTrue(forecastState.renderState().get("weatherProfile").equals("echorendercore:hazard/ashfall_toxic_front"),
                "Ashfall toxic front should expose the reference render profile");
        helper.assertTrue(forecastState.renderState().get("atmosphere").equals("echoatmospherecore:ashfall_toxic_front_field"),
                "Ashfall toxic front should expose the reference atmosphere field");
        helper.assertTrue(forecastState.renderState().get("visibility").equals(0.38D),
                "Ashfall toxic front should expose the reference visibility multiplier");
        helper.assertTrue(manager.lastWeatherSurfaceState(level).orElseThrow().equals(forecastState),
                "Ashfall toxic front forecast should be retained as the live HUD/audio/render surface");

        long skippedTicks = WeatherCoreConfig.MINIMUM_WARNING_TICKS.get() + 600L;
        manager.advanceEventsForSleep(level, skippedTicks);
        manager.tickLevel(level);
        var activeScheduleTick = manager.lastWeatherScheduleTick(event.eventId()).orElseThrow();
        var activeState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
        helper.assertTrue(activeScheduleTick.phase().equals("ACTIVE") && activeScheduleTick.phaseChanged(),
                "Ashfall toxic front schedule tick should materialize ACTIVE from the reference schedule");
        helper.assertTrue(activeState.phase().equals("ACTIVE") && activeState.hudState().get("phase").equals("ACTIVE"),
                "Ashfall toxic front should apply ACTIVE HUD/audio/render state after the schedule starts");
        helper.assertTrue(manager.lastWeatherSurfaceState(level).orElseThrow().equals(activeState),
                "Ashfall toxic front ACTIVE state should refresh the retained live surface");

        long remainingTicks = manager.getActiveEvents(level).get(0).endTick() - level.getGameTime() - 1L;
        manager.advanceEventsForSleep(level, remainingTicks);
        helper.runAfterDelay(2L, () -> {
            try {
                manager.tickLevel(level);
                var endedScheduleTick = manager.lastWeatherScheduleTick(event.eventId()).orElseThrow();
                var endedState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
                helper.assertTrue(endedScheduleTick.ended() && endedScheduleTick.phase().equals("ENDED"),
                        "Ashfall toxic front schedule tick should materialize ENDED from the reference schedule");
                helper.assertTrue(appliedSurfaces.get() >= 3,
                        "Ashfall toxic front should fire forecast, active, and ended live-surface events");
                helper.assertTrue(manager.activeWeatherSchedule(event.eventId()).isEmpty(),
                        "Ashfall toxic front should retire its active native schedule after ENDED");
                helper.assertTrue(endedState.phase().equals("ENDED")
                                && endedState.hudState().get("weather").equals("ASHFALL TOXIC FRONT: CLEAR"),
                        "Ashfall toxic front should clear HUD state when the event ends");
                helper.assertTrue(endedState.audioState().get("cue").equals("echoweathercore:event.clear"),
                        "Ashfall toxic front should clear audio state when the event ends");
                helper.assertTrue(endedState.renderState().get("weatherProfile").equals("echorendercore:weather/clear")
                                && endedState.renderState().get("visibility").equals(1.0D),
                        "Ashfall toxic front should clear render and atmosphere state when the event ends");
                helper.assertTrue(manager.lastWeatherSurfaceState(level).orElseThrow().equals(endedState),
                        "Ashfall toxic front ended state should remain the latest live surface");
                helper.succeed();
            } finally {
                manager.clearAllEvents(level);
                EchoWorldRuntimeBus.clearForTests();
            }
        });
    }

    private static void weatherSavedEventReloadSurface(GameTestHelper helper) {
        EchoWorldRuntimeBus.clearForTests();
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        WeatherCoreApi.registerWeatherProfile(ashfallToxicFrontProfile());
        AtomicInteger appliedSurfaces = new AtomicInteger();
        EchoWorldRuntimeBus.onWeatherSurfaceApplied(event -> appliedSurfaces.incrementAndGet());
        try {
            var event = manager.startEvent(level, ashfallToxicFrontProfile(), WeatherSeverity.MODERATE,
                    helper.absolutePos(new BlockPos(4, 2, 4)), 2400, "agent7-weather-reload-gametest");
            helper.assertTrue(event != null, "Saved weather reload surface should create an event");
            var forecastState = manager.lastWeatherStateApplication(event.eventId()).orElseThrow();
            helper.assertTrue(WeatherSavedData.get(level).getEvents().stream()
                            .anyMatch(saved -> saved.eventId().equals(event.eventId())),
                    "Saved weather reload surface should persist the active event before server reload");

            manager.onServerStopping();
            helper.assertTrue(manager.lastWeatherSurfaceState(level).isEmpty(),
                    "Saved weather reload surface should clear transient live surface state on stop");
            manager.onServerStarting(level.getServer());

            var restoredSchedule = manager.activeWeatherSchedule(event.eventId()).orElseThrow();
            var restoredSurface = manager.lastWeatherSurfaceState(level).orElseThrow();
            helper.assertTrue(restoredSchedule.profileId().equals("echoashfallprotocol:ashfall_toxic_front")
                            && restoredSchedule.phase().equals("FORECAST"),
                    "Saved weather reload surface should restore the active schedule from WeatherSavedData");
            helper.assertTrue(restoredSurface.applied() && restoredSurface.eventId().equals(event.eventId().toString()),
                    "Saved weather reload surface should re-apply the restored event through AdapterCore");
            helper.assertTrue(restoredSurface.hudState().equals(forecastState.hudState())
                            && restoredSurface.audioState().equals(forecastState.audioState())
                            && restoredSurface.renderState().equals(forecastState.renderState()),
                    "Saved weather reload surface should restore the same HUD/audio/render state");
            helper.assertTrue(appliedSurfaces.get() >= 2,
                    "Saved weather reload surface should fire an initial and restored WeatherSurfaceApplied event");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
            EchoWorldRuntimeBus.clearForTests();
        }
    }

    private static void weatherExposureCountermeasure(GameTestHelper helper) {
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        WeatherCountermeasureManager.resetForTests();
        ClimateSensorBlock.clearForTests();
        EmergencySirenBlock.clearForTests();
        RouteWarningPostBlock.clearForTests();
        try {
            WeatherCoreApi.registerWeatherCountermeasure(WeatherType.TOXIC_RAIN, new WeatherEffectModifiers(
                    1.0D, 1.0D, 1.0D, 1.0D, 0.5D, 1.0D, 0.25D, 1.0D, 1.0D, 1.0D,
                    1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 0.5D));
            helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
            var shelteredPos = helper.absolutePos(new BlockPos(2, 2, 2));
            var player = helper.makeMockServerPlayerInLevel();
            var event = manager.startEvent(level, ashfallToxicFrontProfile(), WeatherSeverity.MODERATE,
                    shelteredPos, 2400, "agent7-weather-exposure-gametest");
            helper.assertTrue(event != null, "Weather exposure mitigation should create a weather event");
            var modifiers = WeatherCoreApi.getWeatherModifiers(level, shelteredPos);
            helper.assertTrue(Math.abs(modifiers.toxicExposureMultiplier() - 0.375D) < 0.0001D,
                    "Sheltered toxic rain should apply AdapterCore countermeasure toxic exposure mitigation");
            helper.assertTrue(Math.abs(modifiers.filterDrainMultiplier() - 0.675D) < 0.0001D,
                    "Sheltered toxic rain should apply AdapterCore countermeasure filter drain mitigation");
            helper.assertTrue(Math.abs(modifiers.routeRiskModifier() - 0.725D) < 0.0001D,
                    "Sheltered toxic rain should apply AdapterCore route-risk mitigation");
            helper.assertTrue(WeatherCoreApi.getRouteWeatherRisk(level, shelteredPos, null) == WeatherRouteRisk.SAFE,
                    "Sheltered toxic rain should downgrade route risk through mitigated route-risk state");
            var routeWarning = RouteWarningPostBlock.resolveRouteWarning(level, shelteredPos, player,
                    "agent7-route-warning-post-gametest").orElseThrow();
            helper.assertTrue(routeWarning.delivered()
                            && routeWarning.risk().equals("SAFE")
                            && routeWarning.message().equals("Route Warning Post: Risk is SAFE"),
                    "Route warning post should materialize player-facing AdapterCore route-risk state");
            helper.assertTrue(RouteWarningPostBlock.lastRouteWarning(player).orElseThrow().equals(routeWarning)
                            && RouteWarningPostBlock.routeWarningPosts().size() == 1,
                    "Route warning post should retain native warning state by player and post position");
            helper.assertTrue(routeWarning.hudState().get("routeWarning").equals("Route Warning Post: Risk is SAFE")
                            && routeWarning.audioState().get("cue").equals("echoweathercore:route_warning/safe")
                            && routeWarning.renderState().get("warningPostOverlay")
                                    .equals("echoweathercore:route_warning_post/safe"),
                    "Route warning post should expose HUD/audio/render warning state");
            var siren = EmergencySirenBlock.resolveSirenUse(level, shelteredPos, player,
                    "agent7-emergency-siren-gametest").orElseThrow();
            helper.assertTrue(siren.delivered()
                            && siren.activeWeatherDetected()
                            && siren.message().equals("Emergency Siren: ACTIVE WEATHER DETECTED")
                            && siren.weatherIds().contains("echoashfallprotocol:ashfall_toxic_front"),
                    "Emergency siren should materialize active WeatherCore detection through AdapterCore");
            helper.assertTrue(EmergencySirenBlock.lastSirenUse(player).orElseThrow().equals(siren)
                            && EmergencySirenBlock.sirenPosts().size() == 1,
                    "Emergency siren should retain native warning state by player and siren position");
            helper.assertTrue(siren.hudState().get("emergencySiren").equals("Emergency Siren: ACTIVE WEATHER DETECTED")
                            && siren.audioState().get("cue").equals("echoweathercore:siren/active_weather")
                            && siren.renderState().get("overlay").equals("echoweathercore:emergency_siren/active"),
                    "Emergency siren should expose HUD/audio/render warning state");
            var climateReading = ClimateSensorBlock.resolveClimateReading(level, shelteredPos, player,
                    "agent7-climate-sensor-gametest").orElseThrow();
            helper.assertTrue(climateReading.delivered()
                            && climateReading.sheltered()
                            && climateReading.weatherIds().contains("echoashfallprotocol:ashfall_toxic_front")
                            && climateReading.visibilityPercent() == 38
                            && climateReading.scannerReliabilityPercent() == 100,
                    "Climate sensor should materialize sheltered weather modifier state through AdapterCore");
            helper.assertTrue(ClimateSensorBlock.lastClimateReading(player).orElseThrow().equals(climateReading)
                            && ClimateSensorBlock.sensorPositions().size() == 1,
                    "Climate sensor should retain native reading state by player and sensor position");
            helper.assertTrue(climateReading.hudState().get("climateSensor").equals("Visibility 38% / Scanner 100%")
                            && climateReading.audioState().get("cue").equals("echoweathercore:climate_sensor/sheltered")
                            && climateReading.renderState().get("readout").equals(
                                    "echoweathercore:climate_sensor/readout"),
                    "Climate sensor should expose HUD/audio/render readout state");
            WeatherCoreApi.reportShelterEntered(player, shelteredPos);
            helper.assertTrue(WeatherCountermeasureManager.shelterReports().size() == 1,
                    "Weather shelter reports should persist live shelter runtime state");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
            WeatherCountermeasureManager.resetForTests();
            ClimateSensorBlock.clearForTests();
            EmergencySirenBlock.clearForTests();
            RouteWarningPostBlock.clearForTests();
        }
    }

    private static void weatherAdapterCoreForecast(GameTestHelper helper) {
        var level = helper.getLevel();
        WeatherStateManager manager = WeatherStateManager.getInstance();
        manager.clearAllEvents(level);
        WeatherRadioItem.clearForTests();
        WeatherStationBlockEntity.clearForTests();
        try {
            var event = manager.startEvent(level, ashfallToxicFrontProfile(), WeatherSeverity.MODERATE,
                    helper.absolutePos(new BlockPos(4, 2, 4)), 2400, "agent7-weather-forecast-gametest");
            helper.assertTrue(event != null, "Weather forecast should create a weather event");
            var player = helper.makeMockServerPlayerInLevel();
            var forecasts = WeatherForecastManager.getForecastForPlayer(player);
            helper.assertTrue(forecasts.size() == 1, "Weather forecast should expose the active toxic-front event");
            var forecast = forecasts.get(0);
            var retainedForecast = WeatherForecastManager.lastForecastsForPlayer(player).get(0);
            helper.assertTrue(forecast.eventId().toString().equals("echoashfallprotocol:ashfall_toxic_front")
                            && retainedForecast.weatherId().equals("echoashfallprotocol:ashfall_toxic_front"),
                    "AdapterCore forecast should retain the toxic-front weather id");
            helper.assertTrue(forecast.phase().name().equals("FORECAST")
                            && retainedForecast.phase().equals("FORECAST")
                            && retainedForecast.forecasted(),
                    "AdapterCore forecast should retain the forecast phase as live runtime state");
            helper.assertTrue(forecast.routeRisk() == WeatherRouteRisk.WATCH
                            && retainedForecast.routeRisk().equals("WATCH")
                            && retainedForecast.routeRiskModifier() == 1.45D,
                    "AdapterCore forecast should materialize route risk from the weather definition");
            helper.assertTrue(forecast.recommendedGear().size() == 2
                            && retainedForecast.recommendedGear().contains("echoweathercore:ash_filter_wrap")
                            && retainedForecast.scannerReliability().equals("100%"),
                    "AdapterCore forecast should retain recommended gear and scanner reliability");
            var radioUse = WeatherRadioItem.resolveRadioUse(player, forecasts, 40,
                    "agent7-weather-radio-gametest").orElseThrow();
            helper.assertTrue(radioUse.delivered()
                            && radioUse.forecastsAvailable()
                            && radioUse.weatherIds().contains("echoashfallprotocol:ashfall_toxic_front")
                            && radioUse.routeRisk().equals("WATCH")
                            && radioUse.strongestSeverity().equals("MODERATE"),
                    "Weather Radio should materialize retained forecast state through AdapterCore");
            helper.assertTrue(radioUse.messageLines().get(0).equals("Weather Radio - Regional Forecast:")
                            && radioUse.messageLines().contains(
                                    " - Ashfall Toxic Front [FORECAST, MODERATE]"),
                    "Weather Radio should expose the same forecast text through AdapterCore result state");
            helper.assertTrue(WeatherRadioItem.lastRadioUse(player).orElseThrow().equals(radioUse)
                            && WeatherRadioItem.radioPlayers().size() == 1,
                    "Weather Radio should retain native forecast readout state by player");
            helper.assertTrue(radioUse.hudState().get("weatherRadio").equals(
                                    "Weather Radio - Regional Forecast:")
                            && radioUse.audioState().get("cue").equals(
                                    "echoweathercore:weather_radio/forecast")
                            && radioUse.renderState().get("readout").equals(
                                    "echoweathercore:weather_radio/forecast"),
                    "Weather Radio should expose HUD/audio/render forecast state");
            var stationPos = helper.absolutePos(new BlockPos(5, 2, 5));
            var stationUse = WeatherStationBlockEntity.resolveStationUse(level, stationPos, player, forecasts,
                    "agent7-weather-station-gametest").orElseThrow();
            helper.assertTrue(stationUse.delivered()
                            && stationUse.forecastsAvailable()
                            && stationUse.weatherIds().contains("echoashfallprotocol:ashfall_toxic_front")
                            && stationUse.routeRisk().equals("WATCH")
                            && stationUse.strongestSeverity().equals("MODERATE"),
                    "Weather Station should materialize retained forecast state through AdapterCore");
            helper.assertTrue(stationUse.messageLines().get(0).equals("=== Weather Station Forecast ===")
                            && stationUse.messageLines().contains(" - Ashfall Toxic Front [FORECAST]"),
                    "Weather Station should expose the same station forecast text through AdapterCore result state");
            helper.assertTrue(WeatherStationBlockEntity.lastStationUse(player).orElseThrow().equals(stationUse)
                            && WeatherStationBlockEntity.stationPositions().size() == 1,
                    "Weather Station should retain native forecast state by player and station position");
            helper.assertTrue(stationUse.hudState().get("weatherStation").equals(
                                    "=== Weather Station Forecast ===")
                            && stationUse.audioState().get("cue").equals(
                                    "echoweathercore:weather_station/forecast")
                            && stationUse.renderState().get("readout").equals(
                                    "echoweathercore:weather_station/forecast"),
                    "Weather Station should expose HUD/audio/render forecast state");
            helper.succeed();
        } finally {
            manager.clearAllEvents(level);
            WeatherRadioItem.clearForTests();
            WeatherStationBlockEntity.clearForTests();
        }
    }

    private static WeatherProfile testProfile(int durationTicks) {
        return new WeatherProfile(
                id("sleep_test_radiation_storm"),
                "Sleep Test Radiation Storm",
                WeatherType.RADIATION_STORM,
                WeatherSeverity.SEVERE,
                WeatherScope.GLOBAL,
                durationTicks,
                0,
                1,
                0,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                0,
                0,
                WeatherEffectModifiers.DEFAULT,
                List.of(),
                List.of(),
                "",
                List.of(),
                "",
                "",
                "",
                "",
                true);
    }

    private static WeatherProfile ashfallToxicFrontProfile() {
        return new WeatherProfile(
                Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_toxic_front"),
                "Ashfall Toxic Front",
                WeatherType.TOXIC_RAIN,
                WeatherSeverity.MODERATE,
                WeatherScope.ROUTE_BASED,
                12000,
                2400,
                40,
                24000,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                0,
                0,
                new WeatherEffectModifiers(
                        0.38D,
                        0.55D,
                        1.0D,
                        0.65D,
                        1.35D,
                        1.0D,
                        1.5D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.0D,
                        1.2D,
                        1.0D,
                        1.45D),
                List.of(Identifier.fromNamespaceAndPath(EchoWeatherCore.MODID, "ash_filter_wrap"),
                        Identifier.fromNamespaceAndPath(EchoWeatherCore.MODID, "toxic_rain_collector")),
                List.of(),
                "Toxic front approaching. Return to the pod route or use a marked shelter.",
                List.of(
                        "Toxic particulate rising across the swamp route.",
                        "Shelter marker recommended before the front peaks."),
                "",
                "Airborne particulate density rising; HoloMap marker recommended.",
                "echoashfallprotocol:event.ashfall_toxic_front",
                "echorendercore:hazard/ashfall_toxic_front",
                true);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWeatherCore.MODID, path);
    }
}
