package com.knoxhack.echoaetherworks.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, EchoAetherWorks.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> AETHER_COIL = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "aether_coil", Item::new));
    public static final EchoBackendRegistryEntry<Item> AETHER_CAPACITOR = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "aether_capacitor", Item::new));
    public static final EchoBackendRegistryEntry<Item> PURITY_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "purity_catalyst", Item::new));

    static {
        ModBlocks.blockItems().forEach(block -> tracked(EchoBackendRegistryBridge.registerSimpleBlockItem(ITEMS, block)));
    }

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
