package com.knoxhack.echorelictech.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.item.*;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, EchoRelicTech.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    // MVP Relics
    public static final EchoBackendRegistryEntry<Item> PHASE_ANCHOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "phase_anchor", p -> new PhaseAnchorItem(p.stacksTo(1).durability(256))));
    public static final EchoBackendRegistryEntry<Item> NULL_BATTERY = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "null_battery", p -> new NullBatteryItem(p.stacksTo(1))));
    public static final EchoBackendRegistryEntry<Item> GUARDIAN_LENS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "guardian_lens", p -> new GuardianLensItem(p.stacksTo(1).durability(128))));
    public static final EchoBackendRegistryEntry<Item> ECHO_MIRROR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "echo_mirror", p -> new EchoMirrorItem(p.stacksTo(1).durability(128))));
    public static final EchoBackendRegistryEntry<Item> MATTER_STITCHER = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "matter_stitcher", p -> new MatterStitcherItem(p.stacksTo(1).durability(256))));
    public static final EchoBackendRegistryEntry<Item> GRAVITY_CLAMP = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "gravity_clamp", p -> new RelicDeviceItem(RelicDeviceItem.Device.GRAVITY_CLAMP, p)));
    public static final EchoBackendRegistryEntry<Item> RIFT_LANTERN = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "rift_lantern", p -> new RelicDeviceItem(RelicDeviceItem.Device.RIFT_LANTERN, p)));
    public static final EchoBackendRegistryEntry<Item> BLOOD_CIRCUIT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "blood_circuit", p -> new RelicDeviceItem(RelicDeviceItem.Device.BLOOD_CIRCUIT, p)));
    public static final EchoBackendRegistryEntry<Item> BROKEN_CLIMATE_KEY = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "broken_climate_key", p -> new RelicDeviceItem(RelicDeviceItem.Device.BROKEN_CLIMATE_KEY, p)));
    public static final EchoBackendRegistryEntry<Item> SOUL_CAPACITOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "soul_capacitor", p -> new RelicDeviceItem(RelicDeviceItem.Device.SOUL_CAPACITOR, p)));
    public static final EchoBackendRegistryEntry<Item> VOID_COMPASS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "void_compass", p -> new RelicDeviceItem(RelicDeviceItem.Device.VOID_COMPASS, p)));

    // Materials
    public static final EchoBackendRegistryEntry<Item> UNIDENTIFIED_RELIC = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "unidentified_relic", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> RELIC_SHARD = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "relic_shard", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> DAMAGED_AI_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "damaged_ai_core", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> PRE_GRIDFALL_CIRCUIT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "pre_gridfall_circuit", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> QUANTUM_LATTICE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "quantum_lattice", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> NULL_CELL = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "null_cell", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> STABILIZED_RIFTSTONE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "stabilized_riftstone", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> GUARDIAN_ALLOY = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "guardian_alloy", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> MEMORY_FILAMENT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "memory_filament", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> CONTAINMENT_GLASS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "containment_glass", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> OLD_WORLD_ACTUATOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "old_world_actuator", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> BROKEN_CLIMATE_KEY_FRAGMENT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "broken_climate_key_fragment", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> NEXUS_STAINED_CAPACITOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "nexus_stained_capacitor", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> RELIC_DIAGNOSTIC_REPORT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "relic_diagnostic_report", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> FORBIDDEN_PROTOTYPE_FILE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "forbidden_prototype_file", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> OLD_WORLD_PATENT_FRAGMENT = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "old_world_patent_fragment", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> ECHO_RECOVERY_LOG = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "echo_recovery_log", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> NEXUS_WARNING_PACKET = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "nexus_warning_packet", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> BLACKBOX_RELIC_RECORD = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "blackbox_relic_record", p -> new Item(p)));
    public static final EchoBackendRegistryEntry<Item> LEGENDARY_RELIC_FRAME = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "legendary_relic_frame", p -> new Item(p)));

    static {
        ModBlocks.blockItems().forEach(block -> tracked(EchoBackendRegistryBridge.registerSimpleBlockItem(ITEMS, block)));
    }

    private ModItems() {}

    public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
