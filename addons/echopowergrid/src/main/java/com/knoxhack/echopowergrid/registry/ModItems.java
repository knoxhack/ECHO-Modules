package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.item.GridDiagnosticToolItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final List<NativeRegistryHolder<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final NativeRegistryHolder<Item> COPPER_COIL = tracked("copper_coil", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> SCRAP_WIRE = tracked("scrap_wire", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> INSULATED_WIRE = tracked("insulated_wire", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> POWER_CELL = tracked("power_cell", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> BATTERY_CORE = tracked("battery_core", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> FUSE = tracked("fuse", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> BREAKER_SWITCH = tracked("breaker_switch", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> PHOTOVOLTAIC_ARRAY = tracked("photovoltaic_array", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> BIOFUEL_TURBINE = tracked("biofuel_turbine", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> CERAMIC_INSULATOR = tracked("ceramic_insulator", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> HIGH_VOLTAGE_BUS = tracked("high_voltage_bus", new Item(new Item.Properties()));
    public static final NativeRegistryHolder<Item> GRID_DIAGNOSTIC_TOOL = tracked("grid_diagnostic_tool", new GridDiagnosticToolItem(new Item.Properties()));

    static {
        ModBlocks.blockItems().forEach(block -> tracked(block.id(), new BlockItem(block.get(), new Item.Properties())));
    }

    private ModItems() {}

    public static void register() {
    }

    public static List<NativeRegistryHolder<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static <T extends Item> NativeRegistryHolder<T> tracked(String name, T item) {
        NativeRegistryHolder<T> holder = NativeRegistryHolder.of(name, item);
        CREATIVE_ITEMS.add(holder);
        return holder;
    }
}
