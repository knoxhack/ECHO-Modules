package com.knoxhack.echoweathercore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.item.StormScannerItem;
import com.knoxhack.echoweathercore.item.WeatherRadioItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class WeatherCoreItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoWeatherCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> STORM_SCANNER = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "storm_scanner", StormScannerItem::new, p -> p.stacksTo(1)));
    public static final EchoBackendRegistryEntry<Item> WEATHER_RADIO = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "weather_radio", WeatherRadioItem::new, p -> p.stacksTo(1)));
    public static final EchoBackendRegistryEntry<Item> PORTABLE_SHELTER_BEACON = simple("portable_shelter_beacon", p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> ASH_FILTER_WRAP = simple("ash_filter_wrap", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> FARADAY_COIL = simple("faraday_coil", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_ANCHOR = simple("signal_anchor", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> CRYO_HEAT_CELL = simple("cryo_heat_cell", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> TOXIC_RAIN_COLLECTOR = simple("toxic_rain_collector", p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> DEBRIS_TRACKER = simple("debris_tracker", p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> ROUTE_FLARE = simple("route_flare", p -> p.stacksTo(16));

    // Weather resources
    public static final EchoBackendRegistryEntry<Item> FINE_ASH = simple("fine_ash");
    public static final EchoBackendRegistryEntry<Item> ASH_GLASS_DUST = simple("ash_glass_dust");
    public static final EchoBackendRegistryEntry<Item> STORM_SIFTED_SCRAP = simple("storm_sifted_scrap");
    public static final EchoBackendRegistryEntry<Item> CONDENSED_TOXIN = simple("condensed_toxin");
    public static final EchoBackendRegistryEntry<Item> ACIDIC_SLUDGE = simple("acidic_sludge");
    public static final EchoBackendRegistryEntry<Item> TOXIC_RAINWATER = simple("toxic_rainwater");
    public static final EchoBackendRegistryEntry<Item> CHARGED_URANIUM_DUST = simple("charged_uranium_dust");
    public static final EchoBackendRegistryEntry<Item> IRRADIATED_CRYSTAL_DUST = simple("irradiated_crystal_dust");
    public static final EchoBackendRegistryEntry<Item> REACTOR_TRACE_PARTICLES = simple("reactor_trace_particles");
    public static final EchoBackendRegistryEntry<Item> CRYO_FROST = simple("cryo_frost");
    public static final EchoBackendRegistryEntry<Item> FROZEN_CONDUIT_SHARD = simple("frozen_conduit_shard");
    public static final EchoBackendRegistryEntry<Item> CONDENSED_ICE_FILM = simple("condensed_ice_film");
    public static final EchoBackendRegistryEntry<Item> THERMAL_RESIDUE = simple("thermal_residue");
    public static final EchoBackendRegistryEntry<Item> BAKED_ASH_GLASS = simple("baked_ash_glass");
    public static final EchoBackendRegistryEntry<Item> DRY_REACTOR_SALT = simple("dry_reactor_salt");
    public static final EchoBackendRegistryEntry<Item> STATIC_FILAMENT = simple("static_filament");
    public static final EchoBackendRegistryEntry<Item> MEMORY_RESIDUE = simple("memory_residue");
    public static final EchoBackendRegistryEntry<Item> NEXUS_TRACE = simple("nexus_trace");
    public static final EchoBackendRegistryEntry<Item> ECHO_CRYSTAL_CHARGE = simple("echo_crystal_charge");
    public static final EchoBackendRegistryEntry<Item> ORBITAL_ALLOY_SCRAP = simple("orbital_alloy_scrap");
    public static final EchoBackendRegistryEntry<Item> BURNED_CIRCUITRY = simple("burned_circuitry");
    public static final EchoBackendRegistryEntry<Item> SATELLITE_LENS = simple("satellite_lens");
    public static final EchoBackendRegistryEntry<Item> ECHO0_SIGNAL_SHARD = simple("echo0_signal_shard");
    public static final EchoBackendRegistryEntry<Item> MAGNETIZED_SCRAP = simple("magnetized_scrap");
    public static final EchoBackendRegistryEntry<Item> BURNED_RELAY_COIL = simple("burned_relay_coil");
    public static final EchoBackendRegistryEntry<Item> STATIC_GLASS = simple("static_glass");
    public static final EchoBackendRegistryEntry<Item> OVERLOADED_CAPACITOR = simple("overloaded_capacitor");

    static {
        WeatherCoreBlocks.blockItems().forEach(block -> tracked(EchoBackendRegistryBridge.registerSimpleBlockItem(ITEMS, block)));
    }

    private WeatherCoreItems() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static EchoBackendRegistryEntry<Item> simple(String name) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name, Item::new, p -> p));
    }

    private static EchoBackendRegistryEntry<Item> simple(String name, java.util.function.UnaryOperator<Item.Properties> props) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name, Item::new, props));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
