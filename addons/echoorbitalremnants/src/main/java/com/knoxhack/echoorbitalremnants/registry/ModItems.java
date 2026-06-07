package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.item.EchoTerminalItem;
import com.knoxhack.echoorbitalremnants.item.EmergencyRocketItem;
import com.knoxhack.echoorbitalremnants.item.EmergencyOxygenCellItem;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import com.knoxhack.echoorbitalremnants.item.NexusDriveVesselItem;
import com.knoxhack.echoorbitalremnants.item.OrbitalShuttleItem;
import com.knoxhack.echoorbitalremnants.item.OrbitalWeaponItem;
import com.knoxhack.echoorbitalremnants.item.PlanetaryRouteItem;
import com.knoxhack.echoorbitalremnants.item.SuitModuleItem;
import com.knoxhack.echoorbitalremnants.item.SuitSealantPatchItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoOrbitalRemnants.MODID);

    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> ECHO_TERMINAL = item("echo_terminal", EchoTerminalItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> ORBITAL_TRANSPONDER = simple("orbital_transponder", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> SEALED_SUIT_FRAGMENT = simple("sealed_suit_fragment", p -> p.rarity(Rarity.UNCOMMON));

    public static final EchoBackendRegistryEntry<Item> EMERGENCY_ROCKET = item("emergency_rocket", EmergencyRocketItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> ORBITAL_SHUTTLE = item("orbital_shuttle", OrbitalShuttleItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> MARS_TRANSFER_WINDOW = item("mars_transfer_window",
            properties -> new PlanetaryRouteItem(PlanetaryRouteItem.Target.MARS, properties),
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> EUROPA_TRANSFER_WINDOW = item("europa_transfer_window",
            properties -> new PlanetaryRouteItem(PlanetaryRouteItem.Target.EUROPA, properties),
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> SATURN_TRANSFER_WINDOW = item("saturn_transfer_window",
            properties -> new PlanetaryRouteItem(PlanetaryRouteItem.Target.SATURN, properties),
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> TITAN_TRANSFER_WINDOW = item("titan_transfer_window",
            properties -> new PlanetaryRouteItem(PlanetaryRouteItem.Target.TITAN, properties),
            p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> NEXUS_DRIVE_VESSEL = item("nexus_drive_vessel", NexusDriveVesselItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final EchoBackendRegistryEntry<Item> PRESSURIZED_HELMET = armor("pressurized_helmet", ArmorType.HELMET);
    public static final EchoBackendRegistryEntry<Item> PRESSURIZED_CHESTPLATE = armor("pressurized_chestplate", ArmorType.CHESTPLATE);
    public static final EchoBackendRegistryEntry<Item> PRESSURIZED_LEGGINGS = armor("pressurized_leggings", ArmorType.LEGGINGS);
    public static final EchoBackendRegistryEntry<Item> MAGNETIC_BOOTS = armor("magnetic_boots", ArmorType.BOOTS);
    public static final EchoBackendRegistryEntry<Item> OXYGEN_TANK = simple("oxygen_tank", p -> p.stacksTo(1).durability(240));
    public static final EchoBackendRegistryEntry<Item> OXYGEN_BOOSTER = item("oxygen_booster",
            properties -> new SuitModuleItem(SuitModuleItem.Module.OXYGEN_BOOSTER, properties),
            p -> p.stacksTo(1).durability(160));
    public static final EchoBackendRegistryEntry<Item> EMERGENCY_OXYGEN_CELL = item("emergency_oxygen_cell", EmergencyOxygenCellItem::new,
            p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> SUIT_SEALANT_PATCH = item("suit_sealant_patch", SuitSealantPatchItem::new,
            p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> RADIATION_VISOR = item("radiation_visor",
            properties -> new SuitModuleItem(SuitModuleItem.Module.RADIATION_VISOR, properties),
            p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> THERMAL_SPACE_LINER = item("thermal_space_liner",
            properties -> new SuitModuleItem(SuitModuleItem.Module.THERMAL_REGULATOR, properties),
            p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> JET_BURST_MODULE = item("jet_burst_module",
            properties -> new SuitModuleItem(SuitModuleItem.Module.JET_BURST, properties),
            p -> p.stacksTo(1).durability(180));
    public static final EchoBackendRegistryEntry<Item> SCANNER_VISOR = item("scanner_visor",
            properties -> new SuitModuleItem(SuitModuleItem.Module.SCANNER, properties),
            p -> p.stacksTo(1));

    public static final EchoBackendRegistryEntry<Item> ROCKET_NOSE_CONE = simple("rocket_nose_cone");
    public static final EchoBackendRegistryEntry<Item> SALVAGED_ENGINE = simple("salvaged_engine");
    public static final EchoBackendRegistryEntry<Item> FUEL_TANK = simple("fuel_tank");
    public static final EchoBackendRegistryEntry<Item> HEAT_SHIELD_PLATE = simple("heat_shield_plate");
    public static final EchoBackendRegistryEntry<Item> LANDING_GEAR = simple("landing_gear");
    public static final EchoBackendRegistryEntry<Item> CARGO_BAY_MODULE = simple("cargo_bay_module");
    public static final EchoBackendRegistryEntry<Item> LIFE_SUPPORT_MODULE = simple("life_support_module");
    public static final EchoBackendRegistryEntry<Item> ECHO_FLIGHT_CORE = simple("echo_flight_core", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> NAVIGATION_COMPUTER = simple("navigation_computer", p -> p.rarity(Rarity.UNCOMMON));

    public static final EchoBackendRegistryEntry<Item> ORBITAL_ALLOY = simple("orbital_alloy");
    public static final EchoBackendRegistryEntry<Item> VACUUM_CIRCUIT = simple("vacuum_circuit");
    public static final EchoBackendRegistryEntry<Item> FROZEN_WIRING = simple("frozen_wiring");
    public static final EchoBackendRegistryEntry<Item> NAVIGATION_CHIP = simple("navigation_chip");
    public static final EchoBackendRegistryEntry<Item> OXYGEN_CANISTER = simple("oxygen_canister");
    public static final EchoBackendRegistryEntry<Item> CRYO_BATTERY = simple("cryo_battery");
    public static final EchoBackendRegistryEntry<Item> LUNAR_TITANIUM = simple("lunar_titanium");
    public static final EchoBackendRegistryEntry<Item> HELIUM_3_CELL = simple("helium_3_cell");
    public static final EchoBackendRegistryEntry<Item> MARTIAN_SILICA = simple("martian_silica");
    public static final EchoBackendRegistryEntry<Item> CRYO_CRYSTAL = simple("cryo_crystal");
    public static final EchoBackendRegistryEntry<Item> NEXUS_DUST = simple("nexus_dust", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> LUNAR_CORE_FRAGMENT = simple("lunar_core_fragment", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> NEXUS_DRIVE_CORE = simple("nexus_drive_core", p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final EchoBackendRegistryEntry<Item> ORBITAL_BLACK_BOX = simple("orbital_black_box", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final EchoBackendRegistryEntry<Item> ORBIT_SURVEY_DATA = simple("orbit_survey_data", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> LUNAR_CORE_SAMPLE = simple("lunar_core_sample", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> MARTIAN_PRESSURE_VALVE = simple("martian_pressure_valve", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> EUROPA_THERMAL_PROBE = simple("europa_thermal_probe", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> SATURN_RING_FRAGMENT = simple("saturn_ring_fragment", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> SATURN_RELAY_LENS = simple("saturn_relay_lens", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> TITAN_METHANE_CELL = simple("titan_methane_cell", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> TITAN_SURVEY_CORE = simple("titan_survey_core", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> NEXUS_STABILIZER_SHARD = simple("nexus_stabilizer_shard", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final EchoBackendRegistryEntry<Item> STABILIZED_ECHO_CORE = simple("stabilized_echo_core", p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final EchoBackendRegistryEntry<Item> STATION_RELAY_FUSE = simple("station_relay_fuse", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> STATION_POWER_MATRIX = simple("station_power_matrix", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> HELIUM_EXTRACTOR_CORE = simple("helium_extractor_core", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> LUNAR_PRESSURE_MAP = simple("lunar_pressure_map", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> MARTIAN_HABITAT_KEY = simple("martian_habitat_key", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> PRESSURE_REGULATOR = simple("pressure_regulator", p -> p.rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> EUROPA_PROBE_ARRAY = simple("europa_probe_array", p -> p.rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> THERMAL_STABILIZER = simple("thermal_stabilizer", p -> p.rarity(Rarity.RARE));

    public static final EchoBackendRegistryEntry<Item> ORBITAL_REMNANT_BADGE = item("orbital_remnant_badge",
            properties -> new FactionPledgeItem(FactionPledgeItem.Faction.ORBITAL_REMNANT, properties),
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> VOID_SALVAGER_MARKER = item("void_salvager_marker",
            properties -> new FactionPledgeItem(FactionPledgeItem.Faction.VOID_SALVAGERS, properties),
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> NEXUS_CHOIR_SIGIL = item("nexus_choir_sigil",
            properties -> new FactionPledgeItem(FactionPledgeItem.Faction.NEXUS_CHOIR, properties),
            p -> p.stacksTo(1).rarity(Rarity.RARE).fireResistant());

    public static final EchoBackendRegistryEntry<Item> PLASMA_CUTTER = weapon("plasma_cutter", OrbitalWeaponItem.WeaponProfile.PLASMA_CUTTER, p -> p.stacksTo(1).durability(512));
    public static final EchoBackendRegistryEntry<Item> RAIL_SPIKE_LAUNCHER = weapon("rail_spike_launcher", OrbitalWeaponItem.WeaponProfile.RAIL_SPIKE_LAUNCHER, p -> p.stacksTo(1).durability(384));
    public static final EchoBackendRegistryEntry<Item> GRAVITY_HAMMER = weapon("gravity_hammer", OrbitalWeaponItem.WeaponProfile.GRAVITY_HAMMER, p -> p.stacksTo(1).durability(640).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> SOLAR_LANCE = weapon("solar_lance", OrbitalWeaponItem.WeaponProfile.SOLAR_LANCE, p -> p.stacksTo(1).durability(768).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> NEXUS_PULSE_BLADE = weapon("nexus_pulse_blade", OrbitalWeaponItem.WeaponProfile.NEXUS_PULSE_BLADE, p -> p.stacksTo(1).durability(1024).rarity(Rarity.EPIC).fireResistant());

    static {
        ModBlocks.ALL_BLOCKS.forEach(block -> tracked(blockItem(block.id().getPath(), block)));
    }

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static EchoBackendRegistryEntry<Item> armor(String name, ArmorType type) {
        return simple(name, p -> p.humanoidArmor(ArmorMaterials.IRON, type));
    }

    private static EchoBackendRegistryEntry<Item> simple(String name) {
        return simple(name, p -> p);
    }

    private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Item.Properties> properties) {
        return item(name, Item::new, properties);
    }

    private static EchoBackendRegistryEntry<Item> weapon(String name, OrbitalWeaponItem.WeaponProfile profile, UnaryOperator<Item.Properties> properties) {
        return item(name, itemProperties -> new OrbitalWeaponItem(profile, itemProperties), properties);
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> item(
            String name,
            Function<Item.Properties, T> factory,
            UnaryOperator<Item.Properties> customizer) {
        return tracked(EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> factory.apply(
                customizer.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))))));
    }

    private static EchoBackendRegistryEntry<BlockItem> blockItem(String name, EchoBackendRegistryEntry<? extends Block> block) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> new BlockItem(
                block.get(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}

