package com.knoxhack.echoweathercore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class WeatherCoreCreativeTabs {
    private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoWeatherCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> WEATHERCORE_TAB = EchoBackendRegistryBridge.register(TABS,
        "weathercore",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echoweathercore.weathercore"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.FUNCTIONAL_BLOCKS})
            .icon(() -> ((Item) WeatherCoreItems.STORM_SCANNER.get()).getDefaultInstance())
            .displayItems((parameters, output) -> WeatherCoreItems.creativeItems().forEach(output::accept))
            .build()
    );

    private WeatherCoreCreativeTabs() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}
