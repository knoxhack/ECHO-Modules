package com.knoxhack.echoweathercore.integration.prime;

import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class WeatherCorePrimeIntegration implements EchoPrimeIntegration {
    private static final WeatherCorePrimeIntegration INSTANCE = new WeatherCorePrimeIntegration();

    private WeatherCorePrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/weather");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoWeatherCore.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/weather");
        Identifier unlock = prime("holomap_online");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Weather",
                "Prime weather warnings, environmental event markers, and machine safety hooks.",
                unlock,
                List.of(EchoWeatherCore.MODID),
                35,
                0xFF79C7D9));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/weather"),
                "Prime Weather",
                "Observe a weather event and keep Prime route machinery safe.",
                route,
                List.of(id("mission/first_weather_event"), id("mission/weather_safe_base")),
                35));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/weather_device"),
                prime("scan/prime_weather_device"),
                id("weather_device").toString(),
                "Reports forecast device status, event risk, and warning coverage.",
                "environmental",
                "Weather components and field instruments.",
                "Weather warnings keep Prime survival normal, but better informed.",
                EchoWeatherCore.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/weather_events"),
                "Weather Events",
                "Forecast hazards, active events, and safety markers.",
                0xFF79C7D9,
                false,
                35));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/weather_event"),
                id("prime/layer/weather_events"),
                "Weather Event",
                "weather_event",
                35));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/weather_warnings"),
                route,
                "Weather Warnings",
                "Shows active weather events, device coverage, and environmental route warnings.",
                unlock,
                EchoWeatherCore.MODID,
                35));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWeatherCore.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
