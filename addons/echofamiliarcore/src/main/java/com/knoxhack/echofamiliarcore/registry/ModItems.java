package com.knoxhack.echofamiliarcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import com.knoxhack.echofamiliarcore.item.FamiliarBondItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoFamiliarCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> AETHER_WISP_CHARM = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "aether_wisp_charm",
            props -> new FamiliarBondItem(props, FamiliarCoreApi.AETHER_WISP,
                    "tooltip.echofamiliarcore.aether_wisp_charm")));
    public static final EchoBackendRegistryEntry<Item> SPIRIT_DRONE_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "spirit_drone_core",
            props -> new FamiliarBondItem(props, FamiliarCoreApi.SPIRIT_DRONE,
                    "tooltip.echofamiliarcore.spirit_drone_core")));
    public static final EchoBackendRegistryEntry<Item> SPIRIT_TREAT =
            tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "spirit_treat", Item::new));

    private ModItems() {
    }

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
