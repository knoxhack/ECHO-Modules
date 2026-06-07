package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<Block> ORBITAL_ALLOY_BLOCK = metal("orbital_alloy_block", MapColor.METAL);
    public static final EchoBackendRegistryEntry<Block> SATELLITE_PLATING = metal("satellite_plating", MapColor.COLOR_GRAY);
    public static final EchoBackendRegistryEntry<Block> SOLAR_GLASS = glass("solar_glass", MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> VACUUM_CIRCUIT_BLOCK = metal("vacuum_circuit_block", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> LUNAR_TITANIUM_BLOCK = metal("lunar_titanium_block", MapColor.COLOR_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> MOON_GLASS = glass("moon_glass", MapColor.TERRACOTTA_WHITE);
    public static final EchoBackendRegistryEntry<Block> MARTIAN_SILICA_BLOCK = stone("martian_silica_block", MapColor.COLOR_RED);
    public static final EchoBackendRegistryEntry<Block> CRYO_CRYSTAL_BLOCK = block("cryo_crystal_block",
            p -> p.mapColor(MapColor.ICE)
                    .strength(0.8F, 1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .lightLevel(state -> 11));
    public static final EchoBackendRegistryEntry<Block> SATURN_ICE_RUBBLE = stone("saturn_ice_rubble", MapColor.TERRACOTTA_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> TITAN_THOLIN_DUST = dust("titan_tholin_dust", MapColor.TERRACOTTA_ORANGE);
    public static final EchoBackendRegistryEntry<Block> METHANE_ICE = glass("methane_ice", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> NEXUS_DUST_BLOCK = stone("nexus_dust_block", MapColor.COLOR_PURPLE);

    public static final EchoBackendRegistryEntry<Block> LAUNCH_PLATFORM = metal("launch_platform", MapColor.COLOR_BLACK);
    public static final EchoBackendRegistryEntry<Block> ROCKET_ASSEMBLY_FRAME = machine("rocket_assembly_frame", MachineKind.ROCKET_ASSEMBLY_FRAME, MapColor.METAL);
    public static final EchoBackendRegistryEntry<Block> FUEL_REFINERY = machine("fuel_refinery", MachineKind.FUEL_REFINERY, MapColor.COLOR_ORANGE);
    public static final EchoBackendRegistryEntry<Block> OXYGEN_COMPRESSOR = machine("oxygen_compressor", MachineKind.OXYGEN_COMPRESSOR, MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> HEAT_SHIELD_FABRICATOR = machine("heat_shield_fabricator", MachineKind.HEAT_SHIELD_FABRICATOR, MapColor.COLOR_RED);
    public static final EchoBackendRegistryEntry<Block> ORBITAL_FABRICATOR = machine("orbital_fabricator", MachineKind.ORBITAL_FABRICATOR, MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> VACUUM_SMELTER = machine("vacuum_smelter", MachineKind.VACUUM_SMELTER, MapColor.COLOR_GRAY);
    public static final EchoBackendRegistryEntry<Block> SOLAR_RECLAIMER = machine("solar_reclaimer", MachineKind.SOLAR_RECLAIMER, MapColor.GOLD);
    public static final EchoBackendRegistryEntry<Block> SUIT_CHARGING_STATION = machine("suit_charging_station", MachineKind.SUIT_CHARGING_STATION, MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> SIGNAL_ANALYZER = machine("signal_analyzer", MachineKind.SIGNAL_ANALYZER, MapColor.COLOR_PURPLE);
    public static final EchoBackendRegistryEntry<Block> NAVIGATION_CONSOLE = machine("navigation_console", MachineKind.NAVIGATION_CONSOLE, MapColor.COLOR_BLUE);
    public static final EchoBackendRegistryEntry<Block> DOCKING_BEACON = metal("docking_beacon", MapColor.COLOR_GREEN);
    public static final EchoBackendRegistryEntry<Block> STATION_LIFE_SUPPORT_CORE = machine("station_life_support_core", MachineKind.STATION_LIFE_SUPPORT_CORE, MapColor.PLANT);
    public static final EchoBackendRegistryEntry<Block> SURVEY_MARKER = metal("survey_marker", MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> SIGNAL_RELAY = metal("signal_relay", MapColor.COLOR_BLUE);
    public static final EchoBackendRegistryEntry<Block> THERMAL_VENT = stone("thermal_vent", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> NEXUS_ANCHOR = stone("nexus_anchor", MapColor.COLOR_PURPLE);
    public static final EchoBackendRegistryEntry<Block> STATION_RELAY_NODE = metal("station_relay_node", MapColor.COLOR_BLUE);
    public static final EchoBackendRegistryEntry<Block> HELIUM_EXTRACTOR_NODE = metal("helium_extractor_node", MapColor.COLOR_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> MARS_PRESSURE_CONSOLE = metal("mars_pressure_console", MapColor.COLOR_ORANGE);
    public static final EchoBackendRegistryEntry<Block> EUROPA_THERMAL_ARRAY = metal("europa_thermal_array", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> SATURN_RING_RELAY = metal("saturn_ring_relay", MapColor.TERRACOTTA_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> TITAN_METHANE_PUMP = metal("titan_methane_pump", MapColor.TERRACOTTA_ORANGE);
    public static final EchoBackendRegistryEntry<Block> FACTION_RELAY_HUB = metal("faction_relay_hub", MapColor.COLOR_GREEN);
    public static final EchoBackendRegistryEntry<Block> FACTION_VENDOR_KIOSK = metal("faction_vendor_kiosk", MapColor.COLOR_YELLOW);

    public static final EchoBackendRegistryEntry<Block> MOON_DUST = dust("moon_dust", MapColor.COLOR_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> LUNAR_ROCK = stone("lunar_rock", MapColor.STONE);
    public static final EchoBackendRegistryEntry<Block> ORBITAL_PLATING = metal("orbital_plating", MapColor.COLOR_GRAY);
    public static final EchoBackendRegistryEntry<Block> STATION_WALL_PANEL = metal("station_wall_panel", MapColor.METAL);
    public static final EchoBackendRegistryEntry<Block> BROKEN_SOLAR_PANEL = glass("broken_solar_panel", MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> OXYGEN_PIPE = metal("oxygen_pipe", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> FROZEN_CABLE = metal("frozen_cable", MapColor.ICE);
    public static final EchoBackendRegistryEntry<Block> MARTIAN_DUST = dust("martian_dust", MapColor.COLOR_RED);
    public static final EchoBackendRegistryEntry<Block> CRYO_ICE = glass("cryo_ice", MapColor.ICE);
    public static final EchoBackendRegistryEntry<Block> NEXUS_TOUCHED_STONE = stone("nexus_touched_stone", MapColor.COLOR_PURPLE);
    public static final EchoBackendRegistryEntry<Block> LUNAR_REGOLITH = dust("lunar_regolith", MapColor.COLOR_LIGHT_GRAY);
    public static final EchoBackendRegistryEntry<Block> MARTIAN_BASALT = stone("martian_basalt", MapColor.TERRACOTTA_RED);
    public static final EchoBackendRegistryEntry<Block> PACKED_CRYO_ICE = glass("packed_cryo_ice", MapColor.ICE);
    public static final EchoBackendRegistryEntry<Block> NEXUS_GROWTH = glass("nexus_growth", MapColor.COLOR_PURPLE);

    public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
            ORBITAL_ALLOY_BLOCK, SATELLITE_PLATING, SOLAR_GLASS, VACUUM_CIRCUIT_BLOCK,
            LUNAR_TITANIUM_BLOCK, MOON_GLASS, MARTIAN_SILICA_BLOCK, CRYO_CRYSTAL_BLOCK,
            SATURN_ICE_RUBBLE, TITAN_THOLIN_DUST, METHANE_ICE,
            NEXUS_DUST_BLOCK, LAUNCH_PLATFORM, ROCKET_ASSEMBLY_FRAME, FUEL_REFINERY,
            OXYGEN_COMPRESSOR, HEAT_SHIELD_FABRICATOR, ORBITAL_FABRICATOR, VACUUM_SMELTER,
            SOLAR_RECLAIMER, SUIT_CHARGING_STATION, NAVIGATION_CONSOLE, DOCKING_BEACON,
            STATION_LIFE_SUPPORT_CORE, SIGNAL_ANALYZER, SURVEY_MARKER, SIGNAL_RELAY,
            THERMAL_VENT, NEXUS_ANCHOR, STATION_RELAY_NODE, HELIUM_EXTRACTOR_NODE,
            MARS_PRESSURE_CONSOLE, EUROPA_THERMAL_ARRAY, SATURN_RING_RELAY, TITAN_METHANE_PUMP,
            FACTION_RELAY_HUB, FACTION_VENDOR_KIOSK, MOON_DUST, LUNAR_ROCK, ORBITAL_PLATING,
            STATION_WALL_PANEL, BROKEN_SOLAR_PANEL, OXYGEN_PIPE, FROZEN_CABLE,
            MARTIAN_DUST, CRYO_ICE, NEXUS_TOUCHED_STONE, LUNAR_REGOLITH,
            MARTIAN_BASALT, PACKED_CRYO_ICE, NEXUS_GROWTH);

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color) {
        return block(name, p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL));
    }

    private static EchoBackendRegistryEntry<Block> machine(String name, MachineKind kind, MapColor color) {
        return block(name, p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL),
                properties -> new OrbitalMachineBlock(kind, properties));
    }

    private static EchoBackendRegistryEntry<Block> stone(String name, MapColor color) {
        return block(name, p -> p.mapColor(color).strength(2.5F, 6.0F).sound(SoundType.STONE));
    }

    private static EchoBackendRegistryEntry<Block> dust(String name, MapColor color) {
        return block(name, p -> p.mapColor(color).strength(0.6F).sound(SoundType.SAND));
    }

    private static EchoBackendRegistryEntry<Block> glass(String name, MapColor color) {
        return block(name, p -> p.mapColor(color)
                .strength(0.8F, 1.5F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, entityType) -> false));
    }

    private static EchoBackendRegistryEntry<Block> block(String name, UnaryOperator<BlockBehaviour.Properties> customizer) {
        return block(name, customizer, Block::new);
    }

    private static EchoBackendRegistryEntry<Block> block(
            String name,
            UnaryOperator<BlockBehaviour.Properties> customizer,
            Function<BlockBehaviour.Properties, Block> factory) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> factory.apply(
                customizer.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)))));
    }
}
