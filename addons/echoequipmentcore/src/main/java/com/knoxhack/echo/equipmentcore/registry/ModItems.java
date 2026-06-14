package com.knoxhack.echo.equipmentcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.equipmentcore.EchoEquipmentCore;
import com.knoxhack.echo.equipmentcore.item.DiveToolItem;
import com.knoxhack.echo.equipmentcore.item.DivingSuitItem;
import com.knoxhack.echo.equipmentcore.item.LightSensorItem;
import com.knoxhack.echo.equipmentcore.item.RebreatherItem;
import com.knoxhack.echo.equipmentcore.item.UpgradeModuleItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoEquipmentCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> SHOAL_SUIT = suit("shoal_suit", 0.2F, 120);
    public static final EchoBackendRegistryEntry<Item> DIVERS_RIG = suit("divers_rig", 0.5F, 240);
    public static final EchoBackendRegistryEntry<Item> ABYSSAL_EXOSUIT = suit("abyssal_exosuit", 0.75F, 360);
    public static final EchoBackendRegistryEntry<Item> LATTICE_VOID_SUIT = suit("lattice_void_suit", 0.9F, 480, Rarity.UNCOMMON);
    public static final EchoBackendRegistryEntry<Item> HADAL_HARDSUIT = suit("hadal_hardsuit", 1.0F, 600, Rarity.RARE);

    public static final EchoBackendRegistryEntry<Item> REBREATHER = rebreather("rebreather", 0.4F, 160);
    public static final EchoBackendRegistryEntry<Item> LIGHT_SENSOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "light_sensor", LightSensorItem::new, p -> p.stacksTo(1).durability(80)));
    public static final EchoBackendRegistryEntry<Item> DIVE_TOOL = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "dive_tool", DiveToolItem::new, p -> p.stacksTo(1).durability(200)));

    public static final EchoBackendRegistryEntry<Item> REINFORCED_JOINTS = upgrade("reinforced_joints");
    public static final EchoBackendRegistryEntry<Item> OXYGEN_SCRUBBER = upgrade("oxygen_scrubber");
    public static final EchoBackendRegistryEntry<Item> THERMAL_REGULATOR = upgrade("thermal_regulator");
    public static final EchoBackendRegistryEntry<Item> EMERGENCY_BUOYANCY = upgrade("emergency_buoyancy");
    public static final EchoBackendRegistryEntry<Item> LASER_CUTTER = upgrade("laser_cutter");

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static EchoBackendRegistryEntry<Item> suit(String name, float pressure, int maxDurability) {
        return suit(name, pressure, maxDurability, Rarity.COMMON);
    }

    private static EchoBackendRegistryEntry<Item> suit(String name, float pressure, int maxDurability, Rarity rarity) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name,
                props -> new DivingSuitItem(EchoEquipmentCore.MODID + ":" + name, pressure, maxDurability, props),
                p -> p.stacksTo(1).durability(maxDurability).rarity(rarity)));
    }

    private static EchoBackendRegistryEntry<Item> rebreather(String name, float oxygenBonus, int maxDurability) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name,
                props -> new RebreatherItem(EchoEquipmentCore.MODID + ":" + name, oxygenBonus, props),
                p -> p.stacksTo(1).durability(maxDurability)));
    }

    private static EchoBackendRegistryEntry<Item> upgrade(String name) {
        return tracked(EchoBackendRegistryBridge.registerItem(ITEMS, name,
                props -> new UpgradeModuleItem(EchoEquipmentCore.MODID + ":" + name, props),
                p -> p.stacksTo(16).rarity(Rarity.UNCOMMON)));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
