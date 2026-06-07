package com.knoxhack.echoweathercore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.blockentity.WeatherStationBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class WeatherCoreBlockEntities {
    public static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoWeatherCore.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<WeatherStationBlockEntity>> WEATHER_STATION = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "weather_station",
        () -> new BlockEntityType<>(WeatherStationBlockEntity::new, Set.of((Block) WeatherCoreBlocks.WEATHER_STATION.get())));

    private WeatherCoreBlockEntities() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
