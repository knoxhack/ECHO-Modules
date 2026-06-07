package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.item.GraveKeyItem;
import com.knoxhack.echorecovery.item.RecoveryCompassItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoRecovery.MODID);

    public static final EchoBackendRegistryEntry<Item> GRAVE_KEY = item("grave_key",
        id -> new GraveKeyItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));
    public static final EchoBackendRegistryEntry<Item> RECOVERY_COMPASS = item("recovery_compass",
        id -> new RecoveryCompassItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));
    public static final EchoBackendRegistryEntry<Item> DEATH_RECORD = item("death_record",
        id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(16)));
    public static final EchoBackendRegistryEntry<Item> RECOVERY_TOKEN = item("recovery_token",
        id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));

    static {
        ModBlocks.blockItems().forEach(block -> EchoBackendRegistryBridge.registerWithId(ITEMS, block.id().getPath(),
                id -> new BlockItem(block.get(), new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
                        .useBlockDescriptionPrefix())));
    }

    private ModItems() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    private static EchoBackendRegistryEntry<Item> item(String id,
            java.util.function.Function<net.minecraft.resources.Identifier, Item> factory) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, id, factory);
    }
}
