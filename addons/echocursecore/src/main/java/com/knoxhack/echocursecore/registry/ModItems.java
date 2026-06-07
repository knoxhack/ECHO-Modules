package com.knoxhack.echocursecore.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.item.CurseDiagnosticSlipItem;
import com.knoxhack.echocursecore.item.EchoRotSampleItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, EchoCurseCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> ECHO_ROT_SAMPLE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "echo_rot_sample", EchoRotSampleItem::new));
    public static final EchoBackendRegistryEntry<Item> CURSE_DIAGNOSTIC_SLIP = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "curse_diagnostic_slip", CurseDiagnosticSlipItem::new));
    public static final EchoBackendRegistryEntry<Item> PURIFIED_CURSE_ASH = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "purified_curse_ash", Item::new));

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
