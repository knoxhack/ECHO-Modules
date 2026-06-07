package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.item.EchoTerminalRemoteItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoTerminal.MODID);

    public static final EchoBackendRegistryEntry<Item> ECHO_TERMINAL_REMOTE =
            EchoBackendRegistryBridge.registerWithId(ITEMS, "echo_terminal_remote",
                    id -> new EchoTerminalRemoteItem(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
                            .stacksTo(1)));

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }
}
