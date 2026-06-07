package com.knoxhack.echoprimecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.item.CrudeScannerItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoPrimeCore.MODID);
    private static final List<EchoBackendRegistryEntry<Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> PRIME_FIELD_MANUAL = simple("prime_field_manual", p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_SHARD = simple("signal_shard", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> RELAY_FRAGMENT = simple("relay_fragment", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> BROKEN_CIRCUIT = simple("broken_circuit", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> CIRCUIT_PLATE = simple("circuit_plate", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> INTACT_CIRCUIT = simple("intact_circuit", p -> p.stacksTo(32));
    public static final EchoBackendRegistryEntry<Item> WIRE_BUNDLE = simple("wire_bundle", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> MACHINE_FRAME = simple("machine_frame", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> BASIC_LENS = simple("basic_lens", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> SCANNER_HANDLE = simple("scanner_handle", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> DATA_WAFER_BLANK = simple("data_wafer_blank", p -> p.stacksTo(32));
    public static final EchoBackendRegistryEntry<Item> DATA_WAFER_ENCODED = simple("data_wafer_encoded", p -> p.stacksTo(32).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> DATA_CORE_FRAGMENT = simple("data_core_fragment", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> ECHO_CAPACITOR = simple("echo_capacitor", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> STORAGE_CHIP = simple("storage_chip", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> RELAY_COIL = simple("relay_coil", p -> p.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> BASIC_POWER_CELL = simple("basic_power_cell", p -> p.stacksTo(8));
    public static final EchoBackendRegistryEntry<Item> STABILIZED_ALLOY_INGOT = simple("stabilized_alloy_ingot", p -> p.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> PRIME_CIRCUIT = simple("prime_circuit", p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> PRIME_KEY_FRAGMENT = simple("prime_key_fragment", p -> p.stacksTo(8).rarity(Rarity.RARE));

    public static final EchoBackendRegistryEntry<CrudeScannerItem> CRUDE_SCANNER = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "crude_scanner", CrudeScannerItem::new, p -> p.stacksTo(1).durability(80)));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_WRENCH = simple("signal_wrench", p -> p.stacksTo(1).durability(160));
    public static final EchoBackendRegistryEntry<Item> SCRAP_HAMMER = simple("scrap_hammer", p -> p.stacksTo(1).durability(120));
    public static final EchoBackendRegistryEntry<Item> FIELD_MULTITOOL = simple("field_multitool", p -> p.stacksTo(1).durability(220));
    public static final EchoBackendRegistryEntry<Item> PORTABLE_WORKLIGHT = simple("portable_worklight", p -> p.stacksTo(1).durability(120));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_COMPASS = simple("signal_compass", p -> p.stacksTo(1));
    public static final EchoBackendRegistryEntry<Item> DATA_EXTRACTOR = simple("data_extractor", p -> p.stacksTo(1).durability(180));

    static {
        ModBlocks.blockItems().forEach(block -> EchoBackendRegistryBridge.registerSimpleBlockItem(ITEMS, block));
    }

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Item.Properties> properties) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name, Item::new, properties));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        @SuppressWarnings("unchecked")
        EchoBackendRegistryEntry<Item> cast = (EchoBackendRegistryEntry<Item>) item;
        CREATIVE_ITEMS.add(cast);
        return item;
    }
}
