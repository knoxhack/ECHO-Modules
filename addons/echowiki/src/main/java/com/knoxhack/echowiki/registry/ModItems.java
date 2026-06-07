package com.knoxhack.echowiki.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.item.GuideBookItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, EchoWiki.MODID);

    public static final EchoBackendRegistryEntry<Item> GUIDE_BOOK =
            EchoBackendRegistryBridge.register(ITEMS, "guide_book",
                    () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }
}
