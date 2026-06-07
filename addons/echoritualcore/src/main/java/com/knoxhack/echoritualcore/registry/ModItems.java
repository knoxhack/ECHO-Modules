package com.knoxhack.echoritualcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoritualcore.EchoRitualCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoRitualCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> AETHER_CHALK = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "aether_chalk", Item::new));
    public static final EchoBackendRegistryEntry<Item> PURITY_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "purity_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> STABILITY_SEAL = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "stability_seal", Item::new));
    public static final EchoBackendRegistryEntry<Item> RITUAL_FOCUS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "ritual_focus", p -> new Item(p.stacksTo(16))));
    public static final EchoBackendRegistryEntry<Item> CURSE_ASH = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "curse_ash", Item::new));
    public static final EchoBackendRegistryEntry<Item> REFINED_AETHER_SAMPLE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "refined_aether_sample", Item::new));
    public static final EchoBackendRegistryEntry<Item> AWAKENED_SPELL_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "awakened_spell_core", p -> new Item(p.stacksTo(16))));

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
