package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.block.EchoTerminalBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoTerminal.MODID);
    public static final Object BLOCK_ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoTerminal.MODID);

    public static final EchoBackendRegistryEntry<Block> ECHO_TERMINAL_BLOCK =
            EchoBackendRegistryBridge.registerWithId(BLOCKS, "echo_terminal", id -> new EchoTerminalBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(Registries.BLOCK, id))
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 8)));

    public static final EchoBackendRegistryEntry<BlockItem> ECHO_TERMINAL_BLOCK_ITEM =
            EchoBackendRegistryBridge.registerWithId(BLOCK_ITEMS, "echo_terminal", id -> new BlockItem(
                    ECHO_TERMINAL_BLOCK.get(),
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ITEMS, eventBus);
    }
}
