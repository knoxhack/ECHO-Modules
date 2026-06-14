package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.item.GridDiagnosticToolItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoPowerGrid.MODID);
    private static final List<NativeRegistryHolder<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final NativeRegistryHolder<Item> COPPER_COIL = tracked("copper_coil", () -> new Item(itemProps("copper_coil")));
    public static final NativeRegistryHolder<Item> SCRAP_WIRE = tracked("scrap_wire", () -> new Item(itemProps("scrap_wire")));
    public static final NativeRegistryHolder<Item> INSULATED_WIRE = tracked("insulated_wire", () -> new Item(itemProps("insulated_wire")));
    public static final NativeRegistryHolder<Item> POWER_CELL = tracked("power_cell", () -> new Item(itemProps("power_cell")));
    public static final NativeRegistryHolder<Item> BATTERY_CORE = tracked("battery_core", () -> new Item(itemProps("battery_core")));
    public static final NativeRegistryHolder<Item> FUSE = tracked("fuse", () -> new Item(itemProps("fuse")));
    public static final NativeRegistryHolder<Item> BREAKER_SWITCH = tracked("breaker_switch", () -> new Item(itemProps("breaker_switch")));
    public static final NativeRegistryHolder<Item> PHOTOVOLTAIC_ARRAY = tracked("photovoltaic_array", () -> new Item(itemProps("photovoltaic_array")));
    public static final NativeRegistryHolder<Item> BIOFUEL_TURBINE = tracked("biofuel_turbine", () -> new Item(itemProps("biofuel_turbine")));
    public static final NativeRegistryHolder<Item> CERAMIC_INSULATOR = tracked("ceramic_insulator", () -> new Item(itemProps("ceramic_insulator")));
    public static final NativeRegistryHolder<Item> HIGH_VOLTAGE_BUS = tracked("high_voltage_bus", () -> new Item(itemProps("high_voltage_bus")));
    public static final NativeRegistryHolder<Item> GRID_DIAGNOSTIC_TOOL = tracked("grid_diagnostic_tool",
            () -> new GridDiagnosticToolItem(itemProps("grid_diagnostic_tool")));

    static {
        ModBlocks.blockItems().forEach(block -> tracked(block.id(), () -> new BlockItem(block.get(),
                itemProps(block.id()).useBlockDescriptionPrefix())));
    }

    private ModItems() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<NativeRegistryHolder<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, EchoPowerGrid.id(name)));
    }

    private static <T extends Item> NativeRegistryHolder<T> tracked(String name, java.util.function.Supplier<? extends T> item) {
        EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.register(ITEMS, name, item);
        NativeRegistryHolder<T> holder = NativeRegistryHolder.deferred(name, entry);
        CREATIVE_ITEMS.add(holder);
        return holder;
    }
}
