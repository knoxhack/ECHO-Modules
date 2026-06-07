package com.knoxhack.echoweathercore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class WeatherCoreMenus {
    public static final Object MENUS = EchoBackendRegistryBridge.create(BuiltInRegistries.MENU, EchoWeatherCore.MODID);

    private WeatherCoreMenus() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
