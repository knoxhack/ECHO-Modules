package com.knoxhack.echo.settlementcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoSettlementCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    static {
        ModBlocks.ALL_BLOCKS.forEach(block -> tracked(blockItem(block)));
    }

    private static EchoBackendRegistryEntry<BlockItem> blockItem(EchoBackendRegistryEntry<? extends Block> block) {
        return EchoBackendRegistryBridge.register(ITEMS, block.id().getPath(), () -> new BlockItem(
            block.get(),
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, block.id())).useBlockDescriptionPrefix()));
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
