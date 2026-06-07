package com.knoxhack.echoweathercore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.block.ClimateSensorBlock;
import com.knoxhack.echoweathercore.block.EmergencySirenBlock;
import com.knoxhack.echoweathercore.block.RouteWarningPostBlock;
import com.knoxhack.echoweathercore.block.WeatherStationBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public final class WeatherCoreBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoWeatherCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> WEATHER_STATION = machine("weather_station", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> STORM_BEACON = machine("storm_beacon", MapColor.COLOR_ORANGE);
    public static final EchoBackendRegistryEntry<Block> FARADAY_SHELTER_CORE = machine("faraday_shelter_core", MapColor.COLOR_GRAY);
    public static final EchoBackendRegistryEntry<Block> ATMOSPHERIC_SHIELD_EMITTER = machine("atmospheric_shield_emitter", MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> ROUTE_WARNING_POST = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS,
            "route_warning_post", RouteWarningPostBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final EchoBackendRegistryEntry<Block> DEBRIS_RADAR_DISH = machine("debris_radar_dish", MapColor.METAL);
    public static final EchoBackendRegistryEntry<Block> SIGNAL_STABILIZER = machine("signal_stabilizer", MapColor.COLOR_PURPLE);
    public static final EchoBackendRegistryEntry<Block> EMERGENCY_SIREN = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS,
            "emergency_siren", EmergencySirenBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED).strength(2.0F, 4.0F).sound(SoundType.METAL)));
    public static final EchoBackendRegistryEntry<Block> CLIMATE_SENSOR = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS,
            "climate_sensor", ClimateSensorBlock::new,
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 3.0F).sound(SoundType.METAL)));

    private WeatherCoreBlocks() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static EchoBackendRegistryEntry<Block> machine(String name, MapColor color) {
        return tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, name, WeatherStationBlock::new,
                p -> p.mapColor(color).strength(3.0F, 6.0F).sound(SoundType.METAL)));
    }

    static <T extends Block> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> block) {
        BLOCK_ITEMS.add(block);
        return block;
    }
}
