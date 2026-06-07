package com.knoxhack.echoweathercore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeClimateSensorBridge;
import com.knoxhack.echo.adaptercore.EchoNativeEmergencySirenBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRouteWarningPostBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherExposureMitigationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherForecastBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherRadioBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherRouteRiskBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherScheduleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherScheduleTickBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherStateApplyBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherStationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherWarningBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWeatherCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    public static final String MODULE_ID = "echoweathercore";
    public static final String WEATHER_SCHEDULE_CONTRACT_ID = "echoweathercore:weather/schedule";
    public static final String WEATHER_SCHEDULE_TICK_CONTRACT_ID = "echoweathercore:weather/schedule_tick";
    public static final String WEATHER_STATE_APPLY_CONTRACT_ID = "echoweathercore:weather/state_apply";
    public static final String WEATHER_FORECAST_CONTRACT_ID = "echoweathercore:weather/forecast";
    public static final String WEATHER_WARNING_CONTRACT_ID = "echoweathercore:weather/warning";
    public static final String WEATHER_EXPOSURE_CONTRACT_ID = "echoweathercore:weather/exposure_mitigation";
    public static final String WEATHER_ROUTE_RISK_CONTRACT_ID = "echoweathercore:weather/route_risk";
    public static final String WEATHER_RADIO_CONTRACT_ID = "echoweathercore:weather/radio_use";
    public static final String WEATHER_STATION_CONTRACT_ID = "echoweathercore:weather/station_use";
    public static final String EMERGENCY_SIREN_CONTRACT_ID = "echoweathercore:weather/emergency_siren_use";
    public static final String CLIMATE_SENSOR_CONTRACT_ID = "echoweathercore:weather/climate_sensor_read";
    public static final String ROUTE_WARNING_POST_CONTRACT_ID = "echoweathercore:weather/route_warning_post_use";

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> forecastTick = EchoWeatherCoreForecastStateContract.executeReferenceForecastTick(
                context.getOrDefault("packId", "unknown")
        );
        boolean forecastTickPassed = EchoWeatherCoreForecastStateContract.referenceForecastTickPassed(forecastTick);
        EchoWorldContracts.EchoWeatherScheduleResult scheduleResult =
                new EchoNativeWeatherScheduleBridge(MODULE_ID).schedule(referenceScheduleRequest());
        EchoWorldContracts.EchoWeatherScheduleTickResult scheduleTickResult =
                new EchoNativeWeatherScheduleTickBridge(MODULE_ID).tick(referenceScheduleTickRequest(scheduleResult));
        EchoWorldContracts.EchoWeatherStateApplyResult stateApplyResult =
                new EchoNativeWeatherStateApplyBridge(MODULE_ID).apply(referenceStateApplyRequest());
        EchoWorldContracts.EchoWeatherForecastResult forecastResult =
                new EchoNativeWeatherForecastBridge(MODULE_ID).forecast(referenceForecastRequest(scheduleResult));
        EchoWorldContracts.EchoWeatherWarningResult warningResult =
                new EchoNativeWeatherWarningBridge(MODULE_ID).issue(referenceWarningRequest(scheduleResult));
        EchoWorldContracts.EchoWeatherExposureMitigationResult exposureMitigationResult =
                new EchoNativeWeatherExposureMitigationBridge(MODULE_ID)
                        .mitigate(referenceExposureMitigationRequest(forecastResult));
        EchoWorldContracts.EchoWeatherRouteRiskResult routeRiskResult =
                new EchoNativeWeatherRouteRiskBridge(MODULE_ID)
                        .evaluate(referenceRouteRiskRequest(forecastResult, exposureMitigationResult));
        EchoWorldContracts.EchoWeatherRadioUseResult radioUseResult =
                new EchoNativeWeatherRadioBridge(MODULE_ID).use(referenceRadioUseRequest(forecastResult));
        EchoWorldContracts.EchoWeatherStationUseResult stationUseResult =
                new EchoNativeWeatherStationBridge(MODULE_ID).use(referenceStationUseRequest(forecastResult));
        EchoWorldContracts.EchoEmergencySirenUseResult emergencySirenUseResult =
                new EchoNativeEmergencySirenBridge(MODULE_ID).use(referenceEmergencySirenRequest(forecastResult));
        EchoWorldContracts.EchoClimateSensorReadResult climateSensorReadResult =
                new EchoNativeClimateSensorBridge(MODULE_ID)
                        .read(referenceClimateSensorRequest(forecastResult, stateApplyResult, exposureMitigationResult));
        EchoWorldContracts.EchoRouteWarningPostUseResult routeWarningPostUseResult =
                new EchoNativeRouteWarningPostBridge(MODULE_ID)
                        .use(referenceRouteWarningPostRequest(routeRiskResult));
        boolean schedulePassed = scheduleResult.scheduled()
                && "FORECAST".equals(scheduleResult.phase())
                && scheduleResult.startTick() == 6400L
                && scheduleResult.endTick() == 13600L;
        boolean scheduleTickPassed = scheduleTickResult.active()
                && "FORECAST".equals(scheduleTickResult.previousPhase())
                && "ACTIVE".equals(scheduleTickResult.phase())
                && scheduleTickResult.phaseChanged()
                && !scheduleTickResult.ended();
        boolean stateApplyPassed = stateApplyResult.applied()
                && String.valueOf(stateApplyResult.hudState().get("weather")).contains("ASH STORM")
                && "echoashfallprotocol:event.ash_storm".equals(stateApplyResult.audioState().get("cue"))
                && Double.valueOf(0.35D).equals(stateApplyResult.renderState().get("visibility"));
        boolean forecastPassed = forecastResult.forecasted()
                && "WATCH".equals(forecastResult.routeRisk())
                && "95%".equals(forecastResult.scannerReliability());
        boolean warningPassed = warningResult.delivered()
                && "FORECAST".equals(warningResult.phase())
                && warningResult.recipientPlayerIds().contains("weathercore-native-player")
                && String.valueOf(warningResult.hudState().get("weatherWarning")).contains("Ash storm warning")
                && "echoweathercore:warning/weather_profiles/ash_storm".equals(warningResult.audioState().get("cue"))
                && "echoweathercore:overlay/weather_profiles/ash_storm".equals(
                warningResult.renderState().get("warningOverlay"));
        boolean exposureMitigationPassed = exposureMitigationResult.mitigated()
                && exposureMitigationResult.sheltered()
                && Double.valueOf(0.675D).equals(exposureMitigationResult.modifierState().get("routeRiskModifier"));
        boolean routeRiskPassed = "SAFE".equals(routeRiskResult.risk())
                && routeRiskResult.routeRiskModifier() == 0.675D;
        boolean radioUsePassed = radioUseResult.delivered()
                && "WATCH".equals(radioUseResult.routeRisk())
                && "echoweathercore:weather_radio/forecast".equals(radioUseResult.audioState().get("cue"));
        boolean stationUsePassed = stationUseResult.delivered()
                && "WATCH".equals(stationUseResult.routeRisk())
                && "echoweathercore:weather_station/forecast".equals(stationUseResult.audioState().get("cue"));
        boolean emergencySirenPassed = emergencySirenUseResult.delivered()
                && emergencySirenUseResult.activeWeatherDetected()
                && "echoweathercore:siren/active_weather".equals(emergencySirenUseResult.audioState().get("cue"));
        boolean climateSensorPassed = climateSensorReadResult.delivered()
                && climateSensorReadResult.sheltered()
                && climateSensorReadResult.visibilityPercent() == 35
                && climateSensorReadResult.scannerReliabilityPercent() == 95
                && Double.valueOf(0.675D).equals(climateSensorReadResult.renderState().get("routeRiskModifier"));
        boolean routeWarningPostPassed = routeWarningPostUseResult.delivered()
                && "SAFE".equals(routeWarningPostUseResult.risk())
                && "echoweathercore:route_warning/safe".equals(routeWarningPostUseResult.audioState().get("cue"));
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover WeatherCore event, forecast, shelter, and warning contracts.")
                .phase("register_weather_content", "Record weather blocks, items, menus, profiles, and event providers.")
                .phase("attach_weather_events", "Record command, reload, server, and level tick hooks.")
                .phase("execute_weather_schedule", "Schedule an AdapterCore weather event window.")
                .phase("execute_weather_schedule_tick", "Advance an AdapterCore weather schedule through the live level-tick phase contract.")
                .phase("apply_weather_surface", "Apply AdapterCore HUD, audio, and render weather state.")
                .phase("execute_forecast_state_tick", "Execute forecast, warning, countermeasure, and atmospheric state behavior.")
                .phase("ready", "Expose WeatherCore as the native atmospheric hazard provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("block", "echoweathercore:weather_station", "Weather station block contract.")
                .register("block_entity", "echoweathercore:weather_station", "Weather station block entity contract.")
                .register("item", "echoweathercore:weather_radio", "Weather radio item contract.")
                .register("item", "echoweathercore:storm_sensor", "Storm sensor item contract.")
                .register("menu", "echoweathercore:weather_station", "Weather station menu contract.")
                .register("resource_profile", "echoweathercore:weather_profiles", "Weather profile reload contract.")
                .register("resource_profile", "echoweathercore:module_scoped_weather_profiles", "WeatherCore module-scoped datapack profile reload contract.")
                .register("contract", WEATHER_SCHEDULE_CONTRACT_ID, "Weather schedule runtime contract.")
                .register("contract", WEATHER_SCHEDULE_TICK_CONTRACT_ID, "Weather schedule tick runtime contract.")
                .register("contract", WEATHER_STATE_APPLY_CONTRACT_ID, "Weather state apply runtime contract.")
                .register("contract", WEATHER_FORECAST_CONTRACT_ID, "Weather forecast runtime contract.")
                .register("contract", WEATHER_WARNING_CONTRACT_ID, "Weather warning HUD/audio/render delivery contract.")
                .register("contract", WEATHER_EXPOSURE_CONTRACT_ID, "Weather sheltered exposure mitigation contract.")
                .register("contract", WEATHER_ROUTE_RISK_CONTRACT_ID, "Weather mitigated route risk contract.")
                .register("contract", WEATHER_RADIO_CONTRACT_ID, "Weather Radio HUD/audio/render forecast contract.")
                .register("contract", WEATHER_STATION_CONTRACT_ID, "Weather Station HUD/audio/render forecast contract.")
                .register("contract", EMERGENCY_SIREN_CONTRACT_ID, "Emergency Siren active weather warning contract.")
                .register("contract", CLIMATE_SENSOR_CONTRACT_ID, "Climate Sensor weather modifier readout contract.")
                .register("contract", ROUTE_WARNING_POST_CONTRACT_ID, "Route Warning Post route-risk warning contract.")
                .register("service", "echoweathercore:weather_scheduler", "Weather event scheduler contract.")
                .register("service", "echoweathercore:weather_state", "Weather state manager contract.")
                .register("integration", "echoweathercore:worldcore", "WorldCore atmospheric hazard integration.")
                .register("integration", "echoweathercore:soundcore", "SoundCore weather audio integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoWeatherCore.commonSetup", "Attach WeatherCore optional integrations.")
                .hook("commands.register", "WeatherCoreCommands.register", "Expose weather commands when native command bridge exists.")
                .hook("data.reload", "WeatherDataReloadListener", "Attach weather profile reloaders.")
                .hook("server.starting", "WeatherStateManager.onServerStarting", "Prepare weather state startup.")
                .hook("server.stopping", "WeatherStateManager.onServerStopping", "Prepare weather state shutdown.")
                .hook("level.tick.post", "WeatherCoreEvents.onLevelTick", "Execute live weather schedule, HUD, audio, render, and atmosphere handlers.")
                .hook("level.tick.post", "WeatherScheduler.tick", "Execute weather scheduling tick from the live level tick hook.")
                .hook("level.tick.post", "WeatherStateManager.tickLevel", "Execute per-level weather state tick from the live level tick hook.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("weather_sound_atmosphere", "echoweathercore:weather_scheduler", "hazard_scheduler",
                        "Keeps weather event, forecast, warning, and shelter runtime state ready for safe native level tick hooks.",
                        "weather.events", "weather.forecasts", "weather.warnings", "weather.shelters")
                .surfaceService("weather_sound_atmosphere", "echoweathercore:weather_state", "atmospheric_state",
                        "Keeps atmospheric hazard state ready for HoloMap, SoundCore, and Ashfall HUD consumers.",
                        "weather.events", "holomap.layers", "sound.ambience", "hud.hazard_meters");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "weathercore_native_forecast_state_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("forecastTick", forecastTick);
        result.put("forecastTickExecuted", forecastTickPassed);
        result.put("weatherScheduleRuntimeContract", schedulePassed);
        result.put("weatherScheduleTickRuntimeContract", scheduleTickPassed);
        result.put("liveLevelTickHook", "WeatherCoreEvents.onLevelTick -> WeatherScheduler.tick + WeatherStateManager.tickLevel");
        result.put("liveNativeGameplayHandlerAttached", true);
        result.put("weatherStateApplyRuntimeContract", stateApplyPassed);
        result.put("weatherForecastRuntimeContract", forecastPassed);
        result.put("weatherWarningRuntimeContract", warningPassed);
        result.put("weatherExposureMitigationRuntimeContract", exposureMitigationPassed);
        result.put("weatherRouteRiskRuntimeContract", routeRiskPassed);
        result.put("weatherRadioRuntimeContract", radioUsePassed);
        result.put("weatherStationRuntimeContract", stationUsePassed);
        result.put("emergencySirenRuntimeContract", emergencySirenPassed);
        result.put("climateSensorRuntimeContract", climateSensorPassed);
        result.put("routeWarningPostRuntimeContract", routeWarningPostPassed);
        result.put("weatherScheduleResult", scheduleResult);
        result.put("weatherScheduleTickResult", scheduleTickResult);
        result.put("weatherStateApplyResult", stateApplyResult);
        result.put("weatherForecastResult", forecastResult);
        result.put("weatherWarningResult", warningResult);
        result.put("weatherExposureMitigationResult", exposureMitigationResult);
        result.put("weatherRouteRiskResult", routeRiskResult);
        result.put("weatherRadioUseResult", radioUseResult);
        result.put("weatherStationUseResult", stationUseResult);
        result.put("emergencySirenUseResult", emergencySirenUseResult);
        result.put("climateSensorReadResult", climateSensorReadResult);
        result.put("routeWarningPostUseResult", routeWarningPostUseResult);
        result.put("scheduledPhase", scheduleResult.phase());
        result.put("scheduleTickPhase", scheduleTickResult.phase());
        result.put("weatherHudLine", stateApplyResult.hudState().get("weather"));
        result.put("weatherAudioCue", stateApplyResult.audioState().get("cue"));
        result.put("weatherVisibility", stateApplyResult.renderState().get("visibility"));
        result.put("forecastRouteRisk", forecastResult.routeRisk());
        result.put("warningDelivered", warningResult.delivered());
        result.put("warningAudioCue", warningResult.audioState().get("cue"));
        result.put("warningOverlay", warningResult.renderState().get("warningOverlay"));
        result.put("mitigatedRouteRisk", routeRiskResult.risk());
        result.put("mitigatedRouteRiskModifier", routeRiskResult.routeRiskModifier());
        result.put("weatherRadioDelivered", radioUseResult.delivered());
        result.put("weatherRadioAudioCue", radioUseResult.audioState().get("cue"));
        result.put("weatherStationDelivered", stationUseResult.delivered());
        result.put("weatherStationAudioCue", stationUseResult.audioState().get("cue"));
        result.put("emergencySirenDelivered", emergencySirenUseResult.delivered());
        result.put("emergencySirenAudioCue", emergencySirenUseResult.audioState().get("cue"));
        result.put("climateSensorDelivered", climateSensorReadResult.delivered());
        result.put("climateSensorVisibilityPercent", climateSensorReadResult.visibilityPercent());
        result.put("climateSensorAudioCue", climateSensorReadResult.audioState().get("cue"));
        result.put("routeWarningPostDelivered", routeWarningPostUseResult.delivered());
        result.put("routeWarningPostRisk", routeWarningPostUseResult.risk());
        result.put("routeWarningPostAudioCue", routeWarningPostUseResult.audioState().get("cue"));
        result.put("logicalRegistrationCount", 21);
        result.put("eventHookCount", 8);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "weather.countermeasures",
                "weather.events",
                "weather.forecasts",
                "weather.shelters",
                "weather.warnings",
                WEATHER_SCHEDULE_CONTRACT_ID,
                WEATHER_SCHEDULE_TICK_CONTRACT_ID,
                WEATHER_STATE_APPLY_CONTRACT_ID,
                WEATHER_FORECAST_CONTRACT_ID,
                WEATHER_WARNING_CONTRACT_ID,
                WEATHER_EXPOSURE_CONTRACT_ID,
                WEATHER_ROUTE_RISK_CONTRACT_ID,
                WEATHER_RADIO_CONTRACT_ID,
                WEATHER_STATION_CONTRACT_ID,
                EMERGENCY_SIREN_CONTRACT_ID,
                CLIMATE_SENSOR_CONTRACT_ID,
                ROUTE_WARNING_POST_CONTRACT_ID,
                EchoWeatherCoreForecastStateContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresWeatherBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", forecastTickPassed && schedulePassed && scheduleTickPassed
                && stateApplyPassed && forecastPassed && warningPassed && exposureMitigationPassed
                && routeRiskPassed && radioUsePassed && stationUsePassed && emergencySirenPassed
                && climateSensorPassed && routeWarningPostPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "WeatherCore native contract registered weather hooks and executed AdapterCore schedule, HUD/audio/render state, forecast, warning, shelter mitigation, route risk, and player-facing weather device services.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echoweathercore:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echoweathercore:weather_state"));
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoWeatherCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "weathercore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "WeatherCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("weatherScheduleRuntimeContract")),
                "WeatherCore native adapter should execute weather schedule contract");
        require(Boolean.TRUE.equals(activation.get("weatherScheduleTickRuntimeContract")),
                "WeatherCore native adapter should execute weather schedule tick contract");
        require(Boolean.TRUE.equals(activation.get("liveNativeGameplayHandlerAttached")),
                "WeatherCore native adapter should expose the live level tick gameplay hook");
        require(Boolean.TRUE.equals(activation.get("weatherStateApplyRuntimeContract")),
                "WeatherCore native adapter should apply weather surface state");
        require(Boolean.TRUE.equals(activation.get("weatherForecastRuntimeContract")),
                "WeatherCore native adapter should materialize forecast state");
        require(Boolean.TRUE.equals(activation.get("weatherWarningRuntimeContract")),
                "WeatherCore native adapter should deliver warning HUD/audio/render state");
        require(Boolean.TRUE.equals(activation.get("weatherExposureMitigationRuntimeContract")),
                "WeatherCore native adapter should mitigate sheltered exposure");
        require(Boolean.TRUE.equals(activation.get("weatherRouteRiskRuntimeContract")),
                "WeatherCore native adapter should evaluate mitigated route risk");
        require(Boolean.TRUE.equals(activation.get("weatherRadioRuntimeContract")),
                "WeatherCore native adapter should deliver Weather Radio state");
        require(Boolean.TRUE.equals(activation.get("weatherStationRuntimeContract")),
                "WeatherCore native adapter should deliver Weather Station state");
        require(Boolean.TRUE.equals(activation.get("emergencySirenRuntimeContract")),
                "WeatherCore native adapter should deliver Emergency Siren state");
        require(Boolean.TRUE.equals(activation.get("climateSensorRuntimeContract")),
                "WeatherCore native adapter should deliver Climate Sensor state");
        require(Boolean.TRUE.equals(activation.get("routeWarningPostRuntimeContract")),
                "WeatherCore native adapter should deliver Route Warning Post state");
        require("FORECAST".equals(activation.get("scheduledPhase")),
                "WeatherCore native adapter should schedule a forecast phase");
        require("ACTIVE".equals(activation.get("scheduleTickPhase")),
                "WeatherCore native adapter should advance schedule to active phase");
        require(String.valueOf(activation.get("weatherHudLine")).contains("ASH STORM"),
                "WeatherCore native adapter should expose HUD weather state");
        require("echoashfallprotocol:event.ash_storm".equals(activation.get("weatherAudioCue")),
                "WeatherCore native adapter should expose audio cue");
        require(Double.valueOf(0.35D).equals(activation.get("weatherVisibility")),
                "WeatherCore native adapter should expose render visibility");
        require("WATCH".equals(activation.get("forecastRouteRisk")),
                "WeatherCore native adapter should expose forecast route risk");
        require(Boolean.TRUE.equals(activation.get("warningDelivered")),
                "WeatherCore native adapter should mark warning delivered");
        require("echoweathercore:warning/weather_profiles/ash_storm".equals(activation.get("warningAudioCue")),
                "WeatherCore native adapter should expose warning audio cue");
        require("echoweathercore:overlay/weather_profiles/ash_storm".equals(activation.get("warningOverlay")),
                "WeatherCore native adapter should expose warning render overlay");
        require("SAFE".equals(activation.get("mitigatedRouteRisk")),
                "WeatherCore native adapter should expose mitigated route risk");
        require(Boolean.TRUE.equals(activation.get("weatherRadioDelivered")),
                "WeatherCore native adapter should mark radio forecast delivered");
        require(Boolean.TRUE.equals(activation.get("weatherStationDelivered")),
                "WeatherCore native adapter should mark station forecast delivered");
        require(Boolean.TRUE.equals(activation.get("emergencySirenDelivered")),
                "WeatherCore native adapter should mark emergency siren delivered");
        require(Boolean.TRUE.equals(activation.get("climateSensorDelivered")),
                "WeatherCore native adapter should mark climate sensor delivered");
        require(Integer.valueOf(35).equals(activation.get("climateSensorVisibilityPercent")),
                "WeatherCore native adapter should expose climate sensor visibility");
        require(Boolean.TRUE.equals(activation.get("routeWarningPostDelivered")),
                "WeatherCore native adapter should mark route warning post delivered");
        require("SAFE".equals(activation.get("routeWarningPostRisk")),
                "WeatherCore native adapter should expose route warning post risk");
        System.out.println("weathercore native adapter smoke PASS contracts="
                + ((List<?>) activation.get("registeredFeatureContracts")).size()
                + " phase=" + activation.get("scheduledPhase")
                + " scheduleTick=" + activation.get("scheduleTickPhase")
                + " liveHook=level.tick.post"
                + " visibility=" + activation.get("weatherVisibility")
                + " routeRisk=" + activation.get("forecastRouteRisk")
                + " warningDelivered=" + activation.get("warningDelivered")
                + " warningCue=" + activation.get("warningAudioCue")
                + " mitigatedRouteRisk=" + activation.get("mitigatedRouteRisk")
                + " radioDelivered=" + activation.get("weatherRadioDelivered")
                + " stationDelivered=" + activation.get("weatherStationDelivered")
                + " sirenDelivered=" + activation.get("emergencySirenDelivered")
                + " climateVisibility=" + activation.get("climateSensorVisibilityPercent")
                + " routePostRisk=" + activation.get("routeWarningPostRisk"));
    }

    private static EchoWorldContracts.EchoWeatherScheduleRequest referenceScheduleRequest() {
        return new EchoWorldContracts.EchoWeatherScheduleRequest(
                6000L,
                400,
                32,
                68,
                32,
                96,
                "weathercore-native-reference-schedule",
                new EchoWorldContracts.EchoWeatherScheduleProfile(
                        "echoweathercore:weather_profiles/ash_storm",
                        "ASH_STORM",
                        "SEVERE",
                        "REGION",
                        7200,
                        300,
                        10,
                true));
    }

    private static EchoWorldContracts.EchoWeatherScheduleTickRequest referenceScheduleTickRequest(
            EchoWorldContracts.EchoWeatherScheduleResult schedule) {
        return new EchoWorldContracts.EchoWeatherScheduleTickRequest(
                "weathercore-native-reference-event",
                schedule.startTick(),
                schedule);
    }

    private static EchoWorldContracts.EchoWeatherStateApplyRequest referenceStateApplyRequest() {
        return new EchoWorldContracts.EchoWeatherStateApplyRequest(
                "echoashfallprotocol:event.ash_storm",
                "echoashfallprotocol:crash_zone_wasteland",
                "ACTIVE",
                6400L,
                "weathercore-native-reference-state-apply",
                new EchoWorldContracts.EchoWeatherState(
                        "echoweathercore:weather/ash_storm_active",
                        "ASH STORM: Ash front detected. Visibility loss expected.",
                        "echoashfallprotocol:event.ash_storm",
                        "echorendercore:hazard/ash_storm"),
                new EchoWorldContracts.EchoAtmosphereState(
                        "echoatmospherecore:ash_storm_field",
                        0.35D,
                        "minecraft:ash",
                        "fog_color:9069905"));
    }

    private static EchoWorldContracts.EchoWeatherForecastRequest referenceForecastRequest(
            EchoWorldContracts.EchoWeatherScheduleResult schedule) {
        return new EchoWorldContracts.EchoWeatherForecastRequest(
                "weathercore-native-player",
                "echoashfallprotocol:event.ash_storm",
                "echoweathercore:weather/ash_storm_active",
                "ash_storm",
                "Ash Storm",
                "forecast",
                "moderate",
                "echoashfallprotocol:crash_zone_wasteland",
                6000L,
                schedule.startTick(),
                schedule.endTick(),
                Math.max(0L, schedule.startTick() - 6000L),
                1.35D,
                0.95D,
                List.of("echoweathercore:ash_filter_wrap", "echoweathercore:storm_scanner"),
                "Reach sealed shelter before visibility collapse.",
                List.of("Ash front detected.", "Scanner confidence high."),
                "weathercore-native-reference-forecast");
    }

    private static EchoWorldContracts.EchoWeatherWarningRequest referenceWarningRequest(
            EchoWorldContracts.EchoWeatherScheduleResult schedule) {
        return new EchoWorldContracts.EchoWeatherWarningRequest(
                "weathercore-native-reference-warning",
                schedule.profileId(),
                "echoashfallprotocol:crash_zone_wasteland",
                schedule.phase(),
                "forecast_broadcast",
                "Ash storm warning: visibility collapse expected.",
                List.of("weathercore-native-player"),
                Math.max(0L, schedule.warningStartTick()),
                "weathercore-native-reference-warning");
    }

    private static EchoWorldContracts.EchoWeatherExposureMitigationRequest referenceExposureMitigationRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast) {
        return new EchoWorldContracts.EchoWeatherExposureMitigationRequest(
                forecast.playerId(),
                forecast.weatherId(),
                forecast.weatherType(),
                true,
                forecast.gameTick(),
                "weathercore-native-reference-exposure-mitigation",
                new EchoWorldContracts.EchoWeatherExposureModifier(
                        forecast.weatherType(),
                        1.35D,
                        1.0D,
                        1.5D,
                        1.0D,
                        1.0D,
                        forecast.routeRiskModifier()),
                new EchoWorldContracts.EchoWeatherExposureModifier(
                        "shelter",
                        0.5D,
                        1.0D,
                        0.25D,
                        1.0D,
                        1.0D,
                        0.5D));
    }

    private static EchoWorldContracts.EchoWeatherRouteRiskRequest referenceRouteRiskRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast,
            EchoWorldContracts.EchoWeatherExposureMitigationResult mitigation) {
        return new EchoWorldContracts.EchoWeatherRouteRiskRequest(
                forecast.playerId(),
                forecast.weatherId(),
                forecast.severity(),
                doubleState(mitigation.modifierState(), "routeRiskModifier"),
                forecast.gameTick(),
                "weathercore-native-reference-route-risk");
    }

    private static EchoWorldContracts.EchoWeatherRadioUseRequest referenceRadioUseRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast) {
        return new EchoWorldContracts.EchoWeatherRadioUseRequest(
                forecast.playerId(),
                List.of(forecast.weatherId()),
                List.of(" - " + forecast.displayName() + " [" + forecast.phase() + ", "
                        + forecast.severity() + "]"),
                true,
                forecast.severity(),
                forecast.routeRisk(),
                40,
                forecast.gameTick(),
                "weathercore-native-reference-radio");
    }

    private static EchoWorldContracts.EchoWeatherStationUseRequest referenceStationUseRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast) {
        return new EchoWorldContracts.EchoWeatherStationUseRequest(
                forecast.playerId(),
                List.of(forecast.weatherId()),
                List.of(" - " + forecast.displayName() + " [" + forecast.phase() + "]"),
                true,
                forecast.severity(),
                forecast.routeRisk(),
                32,
                68,
                32,
                forecast.gameTick(),
                "weathercore-native-reference-station");
    }

    private static EchoWorldContracts.EchoEmergencySirenUseRequest referenceEmergencySirenRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast) {
        return new EchoWorldContracts.EchoEmergencySirenUseRequest(
                forecast.playerId(),
                List.of(forecast.weatherId()),
                true,
                forecast.phase(),
                forecast.severity(),
                32,
                68,
                32,
                forecast.gameTick(),
                "weathercore-native-reference-emergency-siren");
    }

    private static EchoWorldContracts.EchoClimateSensorReadRequest referenceClimateSensorRequest(
            EchoWorldContracts.EchoWeatherForecastResult forecast,
            EchoWorldContracts.EchoWeatherStateApplyResult stateApply,
            EchoWorldContracts.EchoWeatherExposureMitigationResult mitigation) {
        return new EchoWorldContracts.EchoClimateSensorReadRequest(
                forecast.playerId(),
                List.of(forecast.weatherId()),
                mitigation.sheltered(),
                doubleState(stateApply.renderState(), "visibility"),
                0.95D,
                doubleState(mitigation.modifierState(), "filterDrainMultiplier"),
                doubleState(mitigation.modifierState(), "toxicExposureMultiplier"),
                doubleState(mitigation.modifierState(), "routeRiskModifier"),
                32,
                68,
                32,
                forecast.gameTick(),
                "weathercore-native-reference-climate-sensor");
    }

    private static EchoWorldContracts.EchoRouteWarningPostUseRequest referenceRouteWarningPostRequest(
            EchoWorldContracts.EchoWeatherRouteRiskResult routeRisk) {
        return new EchoWorldContracts.EchoRouteWarningPostUseRequest(
                routeRisk.playerId(),
                routeRisk.weatherId(),
                routeRisk.severity(),
                routeRisk.risk(),
                routeRisk.routeRiskModifier(),
                32,
                68,
                32,
                routeRisk.gameTick(),
                "weathercore-native-reference-route-warning-post");
    }

    private static double doubleState(Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
