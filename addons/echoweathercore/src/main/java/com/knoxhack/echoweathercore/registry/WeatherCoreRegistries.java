package com.knoxhack.echoweathercore.registry;

public final class WeatherCoreRegistries {
    private WeatherCoreRegistries() {}

    public static void register(Object eventBus) {
        WeatherCoreItems.register(eventBus);
        WeatherCoreBlocks.register(eventBus);
        WeatherCoreBlockEntities.register(eventBus);
        WeatherCoreMenus.register(eventBus);
    }
}
